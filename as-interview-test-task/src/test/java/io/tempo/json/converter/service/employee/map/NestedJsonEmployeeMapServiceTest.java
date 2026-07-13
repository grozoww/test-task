package io.tempo.json.converter.service.employee.map;

import io.tempo.json.converter.TestResources;
import io.tempo.json.converter.dto.Employee;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class NestedJsonEmployeeMapServiceTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    private final NestedJsonEmployeeMapService sut = new NestedJsonEmployeeMapService();

    @ParameterizedTest
    @CsvSource(delimiterString = "|", value = {
            "{\"data\": {\"employees\": []}} | true",
            "{\"data\": {\"employees\": {}}} | false",
            "{\"data\": {}} | false",
            "{} | false",
            "[] | false",
            "{\"users\": {}} | false"
    })
    void supports_shouldAcceptOnlyNestedEmployeeArrays(String json, boolean expectedResult) {
        assertThat(sut.supports(parse(json))).isEqualTo(expectedResult);
    }

    @Test
    void map_shouldReturnEmployeesByJson() {
        JsonNode root = parse(TestResources.read("/structure/nested.json"));
        Employee firstExpectedEmployee = Employee.builder()
                .id("e_554")
                .fullName("Sarah Connor")
                .email("s.connor@tech.io")
                .role("Chief Officer")
                .active(true)
                .build();
        Employee secondExpectedEmployee = Employee.builder()
                .id("e_555")
                .fullName("John Doe")
                .email("j.doe@tech.io")
                .role("Junior Dev")
                .active(false)
                .build();

        List<Employee> actualResult = sut.map(root);

        assertThat(actualResult).containsExactly(firstExpectedEmployee, secondExpectedEmployee);
    }

    @Test
    void map_shouldReturnEmptyList_whenEmployeesArrayIsEmpty() {
        List<Employee> actualResult = sut.map(parse("{\"data\": {\"employees\": []}}"));

        assertThat(actualResult).isEmpty();
    }

    @ParameterizedTest
    @CsvSource(delimiterString = "|", value = {
            "{\"data\": {\"employees\": [{}]}} | NULL",
            "{\"data\": {\"employees\": [{\"profile\": {\"name\": {}}}]}} | NULL",
            "{\"data\": {\"employees\": [{\"profile\": {\"name\": {\"last\": \"Connor\"}}}]}} | Connor",
            "{\"data\": {\"employees\": [{\"profile\": {\"name\": {\"first\": \"Sarah\"}}}]}} | Sarah",
            "{\"data\": {\"employees\": [{\"profile\": {\"name\": {\"first\": \"Sarah\",\"last\": \"Connor\"}}}]}} | Sarah Connor"
    }, nullValues = "NULL")
    void map_shouldReturnCorrectEmployeeFullName(String json, String expectedResult) {
        List<Employee> actualResult = sut.map(parse(json));

        assertThat(actualResult).isNotEmpty();
        assertThat(actualResult.getFirst().fullName()).isEqualTo(expectedResult);
    }

    @ParameterizedTest
    @CsvSource(delimiterString = "|", value = {
            "{\"data\": {\"employees\": [{\"attributes\": {\"status\": \"active\"}}]}} | true",
            "{\"data\": {\"employees\": [{\"attributes\": {\"status\": \"ACTIVE\"}}]}} | true",
            "{\"data\": {\"employees\": [{\"attributes\": {\"status\": \"on_leave\"}}]}} | false",
            "{\"data\": {\"employees\": [{\"attributes\": {}}]}} | false",
            "{\"data\": {\"employees\": [{}]}} | false"
    })
    void map_shouldMarkEmployeeActive_onlyWhenStatusIsActive(String json, boolean expectedResult) {
        List<Employee> actualResult = sut.map(parse(json));

        assertThat(actualResult).isNotEmpty();
        assertThat(actualResult.getFirst().active()).isEqualTo(expectedResult);
    }

    private JsonNode parse(String json) {
        return objectMapper.readTree(json);
    }
}
