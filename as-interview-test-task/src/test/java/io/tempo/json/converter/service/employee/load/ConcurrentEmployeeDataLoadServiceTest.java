package io.tempo.json.converter.service.employee.load;

import io.tempo.json.converter.dao.DataSourceDAO;
import io.tempo.json.converter.dto.Employee;
import io.tempo.json.converter.service.employee.map.MapRouterService;
import io.tempo.json.converter.service.employee.map.UnsupportedJsonStructureException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConcurrentEmployeeDataLoadServiceTest {

    private static final Duration TEST_TIMEOUT = Duration.ofSeconds(5);

    @Mock
    private MapRouterService<Employee> mapRouterService;

    @Test
    void getAllEmployees_shouldCombineEmployeesFromAllSources_inSourceOrder() {
        Employee firstEmployee = employee("1");
        Employee secondEmployee = employee("2");
        Employee thirdEmployee = employee("U-1");
        Employee fourthEmployee = employee("U-2");

        DataSourceDAO firstSource = sourceReturning("firstSourceJson");
        DataSourceDAO secondSource = sourceReturning("secondSourceJson");
        DataSourceDAO thirdSource = sourceReturning("thirdSourceJson");
        DataSourceDAO fourthSource = sourceReturning("fourthSourceJson");

        when(mapRouterService.map("firstSourceJson")).thenReturn(List.of(firstEmployee, secondEmployee));
        when(mapRouterService.map("secondSourceJson")).thenReturn(List.of(thirdEmployee));
        when(mapRouterService.map("thirdSourceJson")).thenReturn(List.of(fourthEmployee));
        when(mapRouterService.map("fourthSourceJson")).thenReturn(List.of());

        List<Employee> actualResult =
                loadService(TEST_TIMEOUT, firstSource, secondSource, thirdSource, fourthSource).getAllEmployees();

        assertThat(actualResult).containsExactly(firstEmployee, secondEmployee, thirdEmployee, fourthEmployee);
    }

    @Test
    void getAllEmployees_shouldReturnUniqueEmployeesList() {
        Employee firstEmployee = employee("U-1");
        Employee duplicateOfFirstEmployee = employee("U-1");
        Employee secondEmployee = employee("U-2");

        DataSourceDAO firstSource = sourceReturning("firstSourceJson");
        DataSourceDAO secondSource = sourceReturning("secondSourceJson");

        when(mapRouterService.map("firstSourceJson")).thenReturn(List.of(firstEmployee));
        when(mapRouterService.map("secondSourceJson")).thenReturn(List.of(duplicateOfFirstEmployee, secondEmployee));

        List<Employee> actualResult = loadService(TEST_TIMEOUT, firstSource, secondSource).getAllEmployees();

        assertThat(actualResult).containsExactly(firstEmployee, secondEmployee);
    }

    @Test
    void getAllEmployees_shouldSkipFailingSources_andKeepHealthyOnes() {
        Employee expectedEmployee = employee("1");

        DataSourceDAO unreachableSource = mock(DataSourceDAO.class);
        when(unreachableSource.getData()).thenThrow(new IllegalStateException("source is down"));

        DataSourceDAO unroutableSource = sourceReturning("unroutableJson");
        when(mapRouterService.map("unroutableJson"))
                .thenThrow(new UnsupportedJsonStructureException("unknown structure"));

        DataSourceDAO healthySource = sourceReturning("healthyJson");
        when(mapRouterService.map("healthyJson")).thenReturn(List.of(expectedEmployee));

        List<Employee> actualResult =
                loadService(TEST_TIMEOUT, unreachableSource, unroutableSource, healthySource).getAllEmployees();

        assertThat(actualResult).containsExactly(expectedEmployee);
    }

    @Test
    void getAllEmployees_shouldReturnEmptyList_whenAllSourcesFail() {
        DataSourceDAO unreachableSource = mock(DataSourceDAO.class);
        when(unreachableSource.getData()).thenThrow(new IllegalStateException("source is down"));

        List<Employee> actualResult = loadService(TEST_TIMEOUT, unreachableSource).getAllEmployees();

        assertThat(actualResult).isEmpty();
    }

    @Test
    void getAllEmployees_shouldSkipSources_thatDoNotRespondWithinTimeout() {
        Employee expectedEmployee = employee("1");

        DataSourceDAO slowSource = mock(DataSourceDAO.class);
        when(slowSource.getData()).thenAnswer(invocation -> {
            try {
                TimeUnit.SECONDS.sleep(5);
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
            }
            return "slowSourceJson";
        });

        DataSourceDAO fastSource = sourceReturning("fastSourceJson");
        when(mapRouterService.map("fastSourceJson")).thenReturn(List.of(expectedEmployee));

        List<Employee> actualResult =
                loadService(Duration.ofMillis(300), slowSource, fastSource).getAllEmployees();

        assertThat(actualResult).containsExactly(expectedEmployee);
    }

    @Test
    void getAllEmployees_shouldReturnFullEmployeesList_withManySources() {
        List<DataSourceDAO> sources = new ArrayList<>();
        List<Employee> expectedResult = new ArrayList<>();

        for (int i = 1; i <= 100; i++) {
            String json = "JSON-" + i;
            Employee employee = employee(String.valueOf(i));
            expectedResult.add(employee);

            DataSourceDAO source = mock(DataSourceDAO.class);
            when(source.getData()).thenAnswer(invocation -> {
                TimeUnit.MILLISECONDS.sleep(ThreadLocalRandom.current().nextInt(100));
                return json;
            });
            sources.add(source);

            when(mapRouterService.map(json)).thenReturn(List.of(employee));
        }

        List<Employee> actualResult =
                new ConcurrentEmployeeDataLoadService(mapRouterService, sources, TEST_TIMEOUT).getAllEmployees();

        assertThat(actualResult).containsExactlyElementsOf(expectedResult);
    }

    private ConcurrentEmployeeDataLoadService loadService(Duration timeout, DataSourceDAO... sources) {
        return new ConcurrentEmployeeDataLoadService(mapRouterService, List.of(sources), timeout);
    }

    private DataSourceDAO sourceReturning(String json) {
        DataSourceDAO source = mock(DataSourceDAO.class);
        when(source.getData()).thenReturn(json);
        return source;
    }

    private static Employee employee(String id) {
        return Employee.builder().id(id).build();
    }
}
