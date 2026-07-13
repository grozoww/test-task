package io.tempo.json.converter.service.employee.load;

import io.tempo.json.converter.dto.Employee;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * End-to-end check of the real pipeline: default data of every registered source flows through
 * the router and its mapper into the combined result.
 */
@SpringBootTest
class EmployeeDataLoadIntegrationTest {

    @Autowired
    private EmployeeDataLoadService employeeDataLoadService;

    @Test
    void getAllEmployees_shouldCombineEmployeesFromAllConfiguredSources() {
        List<Employee> employees = employeeDataLoadService.getAllEmployees();

        assertThat(employees).extracting(Employee::id).containsExactlyInAnyOrder(
                "101", "102",       // legacy system: flat array
                "U-9914", "U-9999", // design: users dictionary
                "U-8899", "U-9900", // engineering: users dictionary
                "e_554", "e_555",   // security: nested data.employees
                "W-2001", "W-2002"  // research: results array
        );

        assertThat(employees).contains(
                Employee.builder()
                        .id("W-2001")
                        .fullName("Grace Hopper")
                        .email("ghopper@navy.mil")
                        .role("Rear Admiral")
                        .active(true)
                        .build(),
                Employee.builder()
                        .id("W-2002")
                        .fullName("Katherine Johnson")
                        .email("kjohnson@navy.mil")
                        .role("Mathematician")
                        .active(false)
                        .build(),
                Employee.builder()
                        .id("101")
                        .fullName("Alice Smith")
                        .email("alice@company.net")
                        .role("Senior Engineer")
                        .active(true)
                        .build());
    }
}
