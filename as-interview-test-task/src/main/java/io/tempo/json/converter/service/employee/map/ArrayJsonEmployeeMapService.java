package io.tempo.json.converter.service.employee.map;

import io.tempo.json.converter.dto.ArrayEmployeeDTO;
import io.tempo.json.converter.dto.Employee;
import org.springframework.stereotype.Service;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

/**
 * Maps a flat JSON array of employee objects, e.g. {@code [{"user_id": 101, ...}, ...]}.
 */
@Service
public class ArrayJsonEmployeeMapService implements JsonEmployeeMapService {

    private static final TypeReference<List<ArrayEmployeeDTO>> ARRAY_TYPE_REFERENCE = new TypeReference<>() {
    };

    /** The source encodes active employees as {@code "is_active": 1}; anything else is inactive. */
    private static final Integer ACTIVE_FLAG = 1;

    private final ObjectMapper objectMapper;

    public ArrayJsonEmployeeMapService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean supports(JsonNode root) {
        return root.isArray();
    }

    @Override
    public List<Employee> map(JsonNode root) {
        return objectMapper.convertValue(root, ARRAY_TYPE_REFERENCE)
                .stream()
                .map(this::mapToEmployee)
                .toList();
    }

    private Employee mapToEmployee(ArrayEmployeeDTO dto) {
        return Employee.builder()
                .id(dto.userId() != null ? String.valueOf(dto.userId()) : null)
                .fullName(getFullEmployeeName(dto))
                .email(dto.email())
                .role(dto.jobTitle())
                .active(ACTIVE_FLAG.equals(dto.isActive()))
                .build();
    }

    private String getFullEmployeeName(ArrayEmployeeDTO dto) {
        if (dto.firstName() != null && dto.lastName() != null) {
            return "%s %s".formatted(dto.firstName(), dto.lastName());
        }
        return dto.firstName() != null ? dto.firstName() : dto.lastName();
    }
}
