package io.tempo.json.converter.service.employee.map;

import io.tempo.json.converter.dto.Employee;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;

import java.util.List;
import java.util.Map;

/**
 * Maps a JSON object keyed by employee id, e.g. {@code {"users": {"U-1": {...}, ...}}}.
 */
@Service
public class DictionaryJsonEmployeeMapService implements JsonEmployeeMapService {

    private static final String ROLE_ATTRIBUTE_KEY = "role";

    @Override
    public boolean supports(JsonNode root) {
        return root.path("users").isObject();
    }

    @Override
    public List<Employee> map(JsonNode root) {
        return root.path("users").propertyStream()
                .map(this::mapToEmployee)
                .toList();
    }

    private Employee mapToEmployee(Map.Entry<String, JsonNode> employeeEntry) {
        JsonNode employeeNode = employeeEntry.getValue();
        return Employee.builder()
                .id(employeeEntry.getKey())
                .fullName(employeeNode.path("displayName").asStringOpt().orElse(null))
                .email(employeeNode.path("mainEmail").asStringOpt().orElse(null))
                .role(getEmployeeRole(employeeNode))
                .active(!employeeNode
                        .path("metadata")
                        .path("account_suspended")
                        .asBooleanOpt()
                        .orElse(true))
                .build();
    }

    private String getEmployeeRole(JsonNode employeeNode) {
        JsonNode attributes = employeeNode.path("metadata").path("custom_attributes");

        for (JsonNode attribute : attributes) {
            if (ROLE_ATTRIBUTE_KEY.equals(attribute.path("key").asStringOpt().orElse(null))) {
                return attribute.path("value").asStringOpt().orElse(null);
            }
        }

        return null;
    }
}
