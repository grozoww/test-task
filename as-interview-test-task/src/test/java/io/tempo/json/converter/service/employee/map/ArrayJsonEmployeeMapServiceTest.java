package io.tempo.json.converter.service.employee.map;

import io.tempo.json.converter.TestResources;
import io.tempo.json.converter.dto.Employee;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ArrayJsonEmployeeMapServiceTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    private final ArrayJsonEmployeeMapService sut = new ArrayJsonEmployeeMapService(objectMapper);

    @ParameterizedTest
    @CsvSource(delimiterString = "|", value = {
            "[] | true",
            "{} | false",
            "{\"users\": {}} | false",
            "{\"data\": {\"employees\": []}} | false",
            "{\"results\": []} | false"
    })
    void supports_shouldAcceptOnlyTopLevelArrays(String json, boolean expectedResult) {
        assertThat(sut.supports(parse(json))).isEqualTo(expectedResult);
    }

    @Test
    void map_shouldReturnEmployeesByJson() {
        JsonNode root = parse(TestResources.read("/structure/array.json"));
        Employee firstExpectedEmployee = Employee.builder()
                .id("101")
                .fullName("Alice Smith")
                .email("alice@company.net")
                .role("Senior Engineer")
                .active(true)
                .build();
        Employee secondExpectedEmployee = Employee.builder()
                .id("102")
                .fullName("Bob Jones")
                .email("bob@company.net")
                .role("Designer")
                .active(false)
                .build();

        List<Employee> actualResult = sut.map(root);

        assertThat(actualResult).containsExactly(firstExpectedEmployee, secondExpectedEmployee);
    }

    @Test
    void map_shouldReturnEmployeesByJson_whenFieldsAreNull() {
        JsonNode root = parse("""
                [
                  {
                    "user_id": 0,
                    "first_name": null,
                    "last_name": null,
                    "contact_email": null,
                    "job_title": null,
                    "is_active": 0,
                    "joined_date": null
                  }
                ]
                """);
        Employee expectedEmployee = Employee.builder()
                .id("0")
                .active(false)
                .build();

        List<Employee> actualResult = sut.map(root);

        assertThat(actualResult).containsExactly(expectedEmployee);
    }

    @ParameterizedTest
    @CsvSource(delimiterString = "|", value = {
            "[{\"user_id\": 0,\"first_name\": null, \"last_name\": null}] | NULL",
            "[{\"user_id\": 0,\"first_name\": \"Alice\", \"last_name\": null}] | Alice",
            "[{\"user_id\": 0,\"first_name\": null, \"last_name\": \"Smith\"}] | Smith",
            "[{\"user_id\": 0,\"first_name\": \"Alice\", \"last_name\": \"Smith\"}] | Alice Smith"
    }, nullValues = "NULL")
    void map_shouldReturnCorrectEmployeeFullName(String json, String expectedResult) {
        List<Employee> actualResult = sut.map(parse(json));

        assertThat(actualResult).isNotEmpty();
        assertThat(actualResult.getFirst().fullName()).isEqualTo(expectedResult);
    }

    @Test
    void map_shouldReturnEmployeeWithNullFields_whenFieldsAreMissing() {
        JsonNode root = parse("[{\"first_name\": \"Ghost\"}]");
        Employee expectedEmployee = Employee.builder()
                .fullName("Ghost")
                .active(false)
                .build();

        List<Employee> actualResult = sut.map(root);

        assertThat(actualResult).containsExactly(expectedEmployee);
    }

    @Test
    void map_shouldReturnEmptyList_whenJsonArrayIsEmpty() {
        List<Employee> actualResult = sut.map(parse("[]"));

        assertThat(actualResult).isEmpty();
    }

    @Test
    void map_shouldThrow_whenArrayElementsDoNotMatchStructure() {
        JsonNode root = parse("[{\"user_id\": \"not-a-number\"}]");

        assertThatThrownBy(() -> sut.map(root)).isInstanceOf(JacksonException.class);
    }

    private JsonNode parse(String json) {
        return objectMapper.readTree(json);
    }
}
