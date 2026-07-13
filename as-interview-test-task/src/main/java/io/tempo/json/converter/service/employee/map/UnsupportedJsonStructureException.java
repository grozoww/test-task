package io.tempo.json.converter.service.employee.map;

/**
 * Thrown when no {@link JsonEmployeeMapService} recognizes the structure of an incoming payload.
 */
public class UnsupportedJsonStructureException extends RuntimeException {

    public UnsupportedJsonStructureException(String message) {
        super(message);
    }
}
