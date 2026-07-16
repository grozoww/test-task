package io.tempo.json.converter.service.employee.map;

import io.tempo.json.converter.dto.Employee;
import org.junit.jupiter.api.Test;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JsonEmployeeMapRouterServiceTest {

    private static final String JSON = "{\"users\": {}}";

    private final FakeMapService firstMapService = new FakeMapService();
    private final FakeMapService secondMapService = new FakeMapService();

    private final JsonEmployeeMapRouterService sut =
            new JsonEmployeeMapRouterService(new ObjectMapper(), List.of(firstMapService, secondMapService));

    @Test
    void map_shouldRouteToFirstSupportingMapService() {
        List<Employee> expectedResult = List.of(Employee.builder().id("100").build());
        firstMapService.supports = false;
        secondMapService.supports = true;
        secondMapService.result = expectedResult;

        List<Employee> actualResult = sut.map(JSON);

        assertThat(actualResult).isEqualTo(expectedResult);
        assertThat(firstMapService.mapped).isFalse();
    }

    @Test
    void map_shouldThrow_whenNoMapServiceSupportsStructure() {
        firstMapService.supports = false;
        secondMapService.supports = false;

        assertThatThrownBy(() -> sut.map("{\"unknown_structure\": []}"))
                .isInstanceOf(UnsupportedJsonStructureException.class)
                .hasMessageContaining("unknown_structure");
    }

    @Test
    void map_shouldThrow_whenPayloadIsNotValidJson() {
        assertThatThrownBy(() -> sut.map("{"))
                .isInstanceOf(JacksonException.class);
    }

    /** Configure via fields; {@code mapped} records whether the router invoked this mapper. */
    private static final class FakeMapService implements JsonEmployeeMapService {

        boolean supports;
        List<Employee> result = List.of();
        boolean mapped;

        @Override
        public boolean supports(JsonNode root) {
            return supports;
        }

        @Override
        public List<Employee> map(JsonNode root) {
            mapped = true;
            return result;
        }
    }
}
