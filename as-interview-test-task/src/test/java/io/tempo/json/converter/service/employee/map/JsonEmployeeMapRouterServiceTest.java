package io.tempo.json.converter.service.employee.map;

import io.tempo.json.converter.dto.Employee;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JsonEmployeeMapRouterServiceTest {

    private static final String JSON = "{\"users\": {}}";

    @Mock
    private JsonEmployeeMapService firstMapService;

    @Mock
    private JsonEmployeeMapService secondMapService;

    private JsonEmployeeMapRouterService sut;

    @BeforeEach
    void setUp() {
        sut = new JsonEmployeeMapRouterService(new ObjectMapper(), List.of(firstMapService, secondMapService));
    }

    @Test
    void map_shouldRouteToFirstSupportingMapService() {
        List<Employee> expectedResult = List.of(Employee.builder().id("100").build());
        when(firstMapService.supports(any())).thenReturn(false);
        when(secondMapService.supports(any())).thenReturn(true);
        when(secondMapService.map(any())).thenReturn(expectedResult);

        List<Employee> actualResult = sut.map(JSON);

        assertThat(actualResult).isEqualTo(expectedResult);
        verify(firstMapService, never()).map(any());
    }

    @Test
    void map_shouldThrow_whenNoMapServiceSupportsStructure() {
        when(firstMapService.supports(any())).thenReturn(false);
        when(secondMapService.supports(any())).thenReturn(false);

        assertThatThrownBy(() -> sut.map("{\"unknown_structure\": []}"))
                .isInstanceOf(UnsupportedJsonStructureException.class)
                .hasMessageContaining("unknown_structure");
    }

    @Test
    void map_shouldThrow_whenPayloadIsNotValidJson() {
        assertThatThrownBy(() -> sut.map("{"))
                .isInstanceOf(JacksonException.class);
    }
}
