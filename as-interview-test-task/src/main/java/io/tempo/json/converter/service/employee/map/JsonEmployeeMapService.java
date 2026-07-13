package io.tempo.json.converter.service.employee.map;

import io.tempo.json.converter.dto.Employee;
import tools.jackson.databind.JsonNode;

import java.util.List;

/**
 * Maps one JSON structure into {@link Employee}s. Implementations declare which structure they
 * understand via {@link #supports(JsonNode)}; {@link #map(JsonNode)} is only called for payloads
 * the implementation supports.
 */
public interface JsonEmployeeMapService {

    boolean supports(JsonNode root);

    List<Employee> map(JsonNode root);
}
