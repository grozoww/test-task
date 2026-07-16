package io.tempo.json.converter.service.employee.load;

import io.tempo.json.converter.dao.DataSourceDAO;
import io.tempo.json.converter.dto.Employee;
import io.tempo.json.converter.service.employee.map.MapRouterService;
import io.tempo.json.converter.service.employee.map.UnsupportedJsonStructureException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

class ConcurrentEmployeeDataLoadServiceTest {

    private static final Duration TEST_TIMEOUT = Duration.ofSeconds(5);

    private final StubMapRouter mapRouterService = new StubMapRouter();

    @Test
    void getAllEmployees_shouldCombineEmployeesFromAllSources_inSourceOrder() {
        Employee firstEmployee = employee("1");
        Employee secondEmployee = employee("2");
        Employee thirdEmployee = employee("U-1");
        Employee fourthEmployee = employee("U-2");

        mapRouterService.route("firstSourceJson", firstEmployee, secondEmployee);
        mapRouterService.route("secondSourceJson", thirdEmployee);
        mapRouterService.route("thirdSourceJson", fourthEmployee);
        mapRouterService.route("fourthSourceJson");

        List<Employee> actualResult = loadService(TEST_TIMEOUT,
                sourceReturning("firstSourceJson"),
                sourceReturning("secondSourceJson"),
                sourceReturning("thirdSourceJson"),
                sourceReturning("fourthSourceJson")
        ).getAllEmployees();

        assertThat(actualResult).containsExactly(firstEmployee, secondEmployee, thirdEmployee, fourthEmployee);
    }

    @Test
    void getAllEmployees_shouldReturnUniqueEmployeesList() {
        Employee firstEmployee = employee("U-1");
        Employee duplicateOfFirstEmployee = employee("U-1");
        Employee secondEmployee = employee("U-2");

        mapRouterService.route("firstSourceJson", firstEmployee);
        mapRouterService.route("secondSourceJson", duplicateOfFirstEmployee, secondEmployee);

        List<Employee> actualResult = loadService(TEST_TIMEOUT,
                sourceReturning("firstSourceJson"),
                sourceReturning("secondSourceJson")
        ).getAllEmployees();

        assertThat(actualResult).containsExactly(firstEmployee, secondEmployee);
    }

    @Test
    @ExtendWith(OutputCaptureExtension.class)
    void getAllEmployees_shouldKeepBothRecords_andWarn_whenSourcesConflictOnSameId(CapturedOutput output) {
        Employee employeeFromFirstSystem = Employee.builder().id("U-1").role("Engineer").build();
        Employee sameIdWithDifferentRole = Employee.builder().id("U-1").role("Manager").build();

        mapRouterService.route("firstSourceJson", employeeFromFirstSystem);
        mapRouterService.route("secondSourceJson", sameIdWithDifferentRole);

        List<Employee> actualResult = loadService(TEST_TIMEOUT,
                sourceReturning("firstSourceJson"),
                sourceReturning("secondSourceJson")
        ).getAllEmployees();

        assertThat(actualResult).containsExactly(employeeFromFirstSystem, sameIdWithDifferentRole);
        assertThat(output).contains("Employee id U-1 maps to 2 records with conflicting fields");
    }

    @Test
    void getAllEmployees_shouldSkipFailingSources_andKeepHealthyOnes() {
        Employee expectedEmployee = employee("1");

        DataSourceDAO unreachableSource = () -> {
            throw new IllegalStateException("source is down");
        };
        // "unroutableJson" is not registered with the router, so mapping it throws
        DataSourceDAO unroutableSource = sourceReturning("unroutableJson");

        DataSourceDAO healthySource = sourceReturning("healthyJson");
        mapRouterService.route("healthyJson", expectedEmployee);

        List<Employee> actualResult =
                loadService(TEST_TIMEOUT, unreachableSource, unroutableSource, healthySource).getAllEmployees();

        assertThat(actualResult).containsExactly(expectedEmployee);
    }

    @Test
    void getAllEmployees_shouldReturnEmptyList_whenAllSourcesFail() {
        DataSourceDAO unreachableSource = () -> {
            throw new IllegalStateException("source is down");
        };

        List<Employee> actualResult = loadService(TEST_TIMEOUT, unreachableSource).getAllEmployees();

        assertThat(actualResult).isEmpty();
    }

    @Test
    void getAllEmployees_shouldSkipSources_thatDoNotRespondWithinTimeout() {
        Employee expectedEmployee = employee("1");

        DataSourceDAO slowSource = sourceReturningAfter("slowSourceJson", Duration.ofSeconds(5));
        DataSourceDAO fastSource = sourceReturning("fastSourceJson");
        mapRouterService.route("fastSourceJson", expectedEmployee);

        List<Employee> actualResult =
                loadService(Duration.ofMillis(300), slowSource, fastSource).getAllEmployees();

        assertThat(actualResult).containsExactly(expectedEmployee);
    }

    @Test
    void getAllEmployees_shouldNotHoldCaller_whenSourceIgnoresCancellation() {
        Employee expectedEmployee = employee("1");

        DataSourceDAO stuckSource = uninterruptibleSourceBlockingFor(Duration.ofSeconds(10));
        DataSourceDAO fastSource = sourceReturning("fastSourceJson");
        mapRouterService.route("fastSourceJson", expectedEmployee);

        long startNanos = System.nanoTime();
        List<Employee> actualResult =
                loadService(Duration.ofMillis(300), stuckSource, fastSource).getAllEmployees();
        Duration elapsed = Duration.ofNanos(System.nanoTime() - startNanos);

        assertThat(actualResult).containsExactly(expectedEmployee);
        // far below the 10s the stuck source keeps blocking after cancellation
        assertThat(elapsed).isLessThan(Duration.ofSeconds(5));
    }

    @Test
    void getAllEmployees_shouldReturnFullEmployeesList_withManySources() {
        List<DataSourceDAO> sources = new ArrayList<>();
        List<Employee> expectedResult = new ArrayList<>();

        for (int i = 1; i <= 100; i++) {
            String json = "JSON-" + i;
            Employee employee = employee(String.valueOf(i));
            expectedResult.add(employee);

            sources.add(sourceReturningAfter(json, Duration.ofMillis(ThreadLocalRandom.current().nextInt(100))));
            mapRouterService.route(json, employee);
        }

        List<Employee> actualResult =
                new ConcurrentEmployeeDataLoadService(mapRouterService, sources, TEST_TIMEOUT).getAllEmployees();

        assertThat(actualResult).containsExactlyElementsOf(expectedResult);
    }

    private ConcurrentEmployeeDataLoadService loadService(Duration timeout, DataSourceDAO... sources) {
        return new ConcurrentEmployeeDataLoadService(mapRouterService, List.of(sources), timeout);
    }

    private static DataSourceDAO sourceReturning(String json) {
        return () -> json;
    }

    private static DataSourceDAO sourceReturningAfter(String json, Duration delay) {
        return () -> {
            try {
                TimeUnit.MILLISECONDS.sleep(delay.toMillis());
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
            }
            return json;
        };
    }

    private static DataSourceDAO uninterruptibleSourceBlockingFor(Duration blockFor) {
        return () -> {
            long deadlineNanos = System.nanoTime() + blockFor.toNanos();
            while (System.nanoTime() < deadlineNanos) {
                try {
                    TimeUnit.MILLISECONDS.sleep(100);
                } catch (InterruptedException ignored) {
                    // simulates non-interruptible I/O: cancellation does not unblock this source
                }
            }
            return "uninterruptibleSourceJson";
        };
    }

    private static Employee employee(String id) {
        return Employee.builder().id(id).build();
    }

    /**
     * Hand-rolled router stub: {@link #map} returns whatever {@link #route} registered for the
     * payload and throws {@link UnsupportedJsonStructureException} for anything unregistered -
     * the same contract as the real router facing an unknown structure.
     */
    private static final class StubMapRouter implements MapRouterService<Employee> {

        private final Map<String, List<Employee>> routes = new HashMap<>();

        void route(String json, Employee... employees) {
            routes.put(json, List.of(employees));
        }

        @Override
        public List<Employee> map(String json) {
            List<Employee> employees = routes.get(json);
            if (employees == null) {
                throw new UnsupportedJsonStructureException("no registered route for payload " + json);
            }
            return employees;
        }
    }
}
