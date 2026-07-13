package io.tempo.json.converter.service.employee.map;

import io.tempo.json.converter.dto.Employee;
import io.tempo.json.converter.dto.ResultsEmployeeDTO;
import org.springframework.stereotype.Service;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

/**
 * Maps a JSON document with a {@code results} array, e.g.
 * {@code {"results": [{"employeeNumber": ..., "personal": {...}, "work": {...}}]}}.
 */
@Service
public class ResultsJsonEmployeeMapService implements JsonEmployeeMapService {

    /** Per the source contract, exactly "ACTIVE" means active; any other status does not. */
    private static final String ACTIVE_EMPLOYMENT_STATUS = "ACTIVE";

    private static final TypeReference<List<ResultsEmployeeDTO>> RESULTS_TYPE_REFERENCE = new TypeReference<>() {
    };

    private final ObjectMapper objectMapper;

    public ResultsJsonEmployeeMapService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean supports(JsonNode root) {
        return root.path("results").isArray();
    }

    @Override
    public List<Employee> map(JsonNode root) {
        return objectMapper.convertValue(root.path("results"), RESULTS_TYPE_REFERENCE)
                .stream()
                .map(this::mapToEmployee)
                .toList();
    }

    private Employee mapToEmployee(ResultsEmployeeDTO dto) {
        return Employee.builder()
                .id(dto.employeeNumber())
                .fullName(dto.personal() != null ? dto.personal().fullName() : null)
                .email(dto.work() != null ? dto.work().emailAddress() : null)
                .role(dto.work() != null ? dto.work().position() : null)
                .active(ACTIVE_EMPLOYMENT_STATUS.equals(dto.employmentStatus()))
                .build();
    }
}
