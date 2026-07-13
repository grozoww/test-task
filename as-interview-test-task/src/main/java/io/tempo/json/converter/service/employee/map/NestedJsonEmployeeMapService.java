package io.tempo.json.converter.service.employee.map;

import io.tempo.json.converter.dto.Employee;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;

import java.util.List;

/**
 * Maps a nested JSON document, e.g. {@code {"data": {"employees": [{"_id": ..., "profile": ...}]}}}.
 */
@Service
public class NestedJsonEmployeeMapService implements JsonEmployeeMapService {

    private static final String ACTIVE_STATUS = "active";

    @Override
    public boolean supports(JsonNode root) {
        return root.path("data").path("employees").isArray();
    }

    @Override
    public List<Employee> map(JsonNode root) {
        return root.path("data").path("employees").valueStream()
                .map(this::mapToEmployee)
                .toList();
    }

    private Employee mapToEmployee(JsonNode employeeNode) {
        return Employee.builder()
                .id(employeeNode.path("_id").asStringOpt().orElse(null))
                .fullName(getEmployeeFullName(employeeNode))
                .email(employeeNode.path("contact").path("workEmail").asStringOpt().orElse(null))
                .role(employeeNode.path("attributes").path("role").asStringOpt().orElse(null))
                .active(isActive(employeeNode))
                .build();
    }

    private String getEmployeeFullName(JsonNode employeeNode) {
        String first = getNamePart(employeeNode, "first");
        String last = getNamePart(employeeNode, "last");
        if (first != null && last != null) {
            return "%s %s".formatted(first, last);
        }
        return first != null ? first : last;
    }

    private String getNamePart(JsonNode employeeNode, String part) {
        return employeeNode
                .path("profile")
                .path("name")
                .path(part)
                .asStringOpt()
                .orElse(null);
    }

    private boolean isActive(JsonNode employeeNode) {
        return employeeNode
                .path("attributes")
                .path("status")
                .asStringOpt()
                .map(ACTIVE_STATUS::equalsIgnoreCase)
                .orElse(false);
    }
}
