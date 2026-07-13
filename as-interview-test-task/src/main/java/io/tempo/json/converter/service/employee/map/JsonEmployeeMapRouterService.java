package io.tempo.json.converter.service.employee.map;

import io.tempo.json.converter.dto.Employee;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Map;

/**
 * Parses an incoming payload once and routes it to the first {@link JsonEmployeeMapService}
 * that supports its structure. New structures are integrated by adding a new
 * {@link JsonEmployeeMapService} bean; this router needs no changes.
 */
@Service
public class JsonEmployeeMapRouterService implements MapRouterService<Employee> {

    private final ObjectMapper objectMapper;
    private final List<JsonEmployeeMapService> mapServices;

    public JsonEmployeeMapRouterService(ObjectMapper objectMapper, List<JsonEmployeeMapService> mapServices) {
        this.objectMapper = objectMapper;
        this.mapServices = List.copyOf(mapServices);
    }

    @Override
    public List<Employee> map(String json) {
        JsonNode root = objectMapper.readTree(json);
        return mapServices.stream()
                .filter(mapService -> mapService.supports(root))
                .findFirst()
                .orElseThrow(() -> new UnsupportedJsonStructureException(describeStructure(root)))
                .map(root);
    }

    private static String describeStructure(JsonNode root) {
        List<String> topLevelFields = root.propertyStream().map(Map.Entry::getKey).toList();
        return "No employee mapper supports %s payload with top-level fields %s"
                .formatted(root.getClass().getSimpleName(), topLevelFields);
    }
}
