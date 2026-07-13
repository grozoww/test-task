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

class ResultsJsonEmployeeMapServiceTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    private final ResultsJsonEmployeeMapService sut = new ResultsJsonEmployeeMapService(objectMapper);

    @ParameterizedTest
    @CsvSource(delimiterString = "|", value = {
            "{\"results\": []} | true",
            "{\"results\": {}} | false",
            "{} | false",
            "[] | false",
            "{\"users\": {}} | false",
            "{\"data\": {\"employees\": []}} | false"
    })
    void supports_shouldAcceptOnlyResultsArrays(String json, boolean expectedResult) {
        assertThat(sut.supports(parse(json))).isEqualTo(expectedResult);
    }

    @Test
    void map_shouldReturnEmployeesByJson() {
        JsonNode root = parse(TestResources.read("/structure/results.json"));
        Employee firstExpectedEmployee = Employee.builder()
                .id("W-2001")
                .fullName("Grace Hopper")
                .email("ghopper@navy.mil")
                .role("Rear Admiral")
                .active(true)
                .build();
        Employee secondExpectedEmployee = Employee.builder()
                .id("W-2002")
                .fullName("Katherine Johnson")
                .email("kjohnson@navy.mil")
                .role("Mathematician")
                .active(false)
                .build();

        List<Employee> actualResult = sut.map(root);

        assertThat(actualResult).containsExactly(firstExpectedEmployee, secondExpectedEmployee);
    }

    @Test
    void map_shouldReturnEmptyList_whenResultsArrayIsEmpty() {
        List<Employee> actualResult = sut.map(parse("{\"results\": []}"));

        assertThat(actualResult).isEmpty();
    }

    @Test
    void map_shouldReturnEmployeeWithNullFields_whenNestedObjectsAreMissing() {
        JsonNode root = parse("{\"results\": [{\"employeeNumber\": \"W-1\"}]}");
        Employee expectedEmployee = Employee.builder()
                .id("W-1")
                .active(false)
                .build();

        List<Employee> actualResult = sut.map(root);

        assertThat(actualResult).containsExactly(expectedEmployee);
    }

    @ParameterizedTest
    @CsvSource(delimiterString = "|", value = {
            "{\"results\": [{\"employmentStatus\": \"ACTIVE\"}]} | true",
            "{\"results\": [{\"employmentStatus\": \"TERMINATED\"}]} | false",
            "{\"results\": [{\"employmentStatus\": \"active\"}]} | false",
            "{\"results\": [{\"employmentStatus\": \"ON_LEAVE\"}]} | false",
            "{\"results\": [{}]} | false"
    })
    void map_shouldMarkEmployeeActive_onlyWhenEmploymentStatusIsActive(String json, boolean expectedResult) {
        List<Employee> actualResult = sut.map(parse(json));

        assertThat(actualResult).isNotEmpty();
        assertThat(actualResult.getFirst().active()).isEqualTo(expectedResult);
    }

    private JsonNode parse(String json) {
        return objectMapper.readTree(json);
    }
}
