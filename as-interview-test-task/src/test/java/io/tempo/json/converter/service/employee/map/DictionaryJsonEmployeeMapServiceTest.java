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

class DictionaryJsonEmployeeMapServiceTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    private final DictionaryJsonEmployeeMapService sut = new DictionaryJsonEmployeeMapService();

    @ParameterizedTest
    @CsvSource(delimiterString = "|", value = {
            "{\"users\": {}} | true",
            "{\"users\": []} | false",
            "{\"users\": \"none\"} | false",
            "{} | false",
            "[] | false",
            "{\"results\": []} | false"
    })
    void supports_shouldAcceptOnlyUsersDictionaries(String json, boolean expectedResult) {
        assertThat(sut.supports(parse(json))).isEqualTo(expectedResult);
    }

    @Test
    void map_shouldReturnEmployeesByJson() {
        JsonNode root = parse(TestResources.read("/structure/dictionary.json"));
        Employee firstExpectedEmployee = Employee.builder()
                .id("U-8899")
                .fullName("Michael Scott")
                .email("mscott@paper.biz")
                .role("Manager")
                .active(true)
                .build();
        Employee secondExpectedEmployee = Employee.builder()
                .id("U-9900")
                .fullName("Pam Beesly")
                .email("pam@paper.biz")
                .role("Admin")
                .active(false)
                .build();

        List<Employee> actualResult = sut.map(root);

        assertThat(actualResult).containsExactly(firstExpectedEmployee, secondExpectedEmployee);
    }

    @Test
    void map_shouldReturnEmptyList_whenUsersDictionaryIsEmpty() {
        List<Employee> actualResult = sut.map(parse("{\"users\": {}}"));

        assertThat(actualResult).isEmpty();
    }

    @ParameterizedTest
    @CsvSource(delimiterString = "|", value = {
            "{\"users\":{\"U-8899\":{\"metadata\": {}}}} | NULL",
            "{\"users\":{\"U-8899\":{\"metadata\": {\"custom_attributes\": []}}}} | NULL",
            "{\"users\":{\"U-8899\":{\"metadata\": {\"custom_attributes\": [{\"value\": \"Admin\"}]}}}} | NULL",
            "{\"users\":{\"U-8899\":{\"metadata\": {\"custom_attributes\": [{\"key\": \"role\"}]}}}} | NULL",
            "{\"users\":{\"U-8899\":{\"metadata\": {\"custom_attributes\": [{ \"key\": \"role\", \"value\": \"Admin\" }]}}}} | Admin"
    }, nullValues = "NULL")
    void map_shouldReturnCorrectRole_whenCustomAttributesVary(String json, String expectedResult) {
        List<Employee> actualResult = sut.map(parse(json));

        assertThat(actualResult).isNotEmpty();
        assertThat(actualResult.getFirst().role()).isEqualTo(expectedResult);
    }

    @ParameterizedTest
    @CsvSource(delimiterString = "|", value = {
            "{\"users\":{\"U-1\":{\"metadata\": {\"account_suspended\": false}}}} | true",
            "{\"users\":{\"U-1\":{\"metadata\": {\"account_suspended\": true}}}} | false",
            "{\"users\":{\"U-1\":{\"metadata\": {}}}} | false",
            "{\"users\":{\"U-1\":{}}} | false"
    })
    void map_shouldMarkEmployeeActive_onlyWhenAccountIsNotSuspended(String json, boolean expectedResult) {
        List<Employee> actualResult = sut.map(parse(json));

        assertThat(actualResult).isNotEmpty();
        assertThat(actualResult.getFirst().active()).isEqualTo(expectedResult);
    }

    private JsonNode parse(String json) {
        return objectMapper.readTree(json);
    }
}
