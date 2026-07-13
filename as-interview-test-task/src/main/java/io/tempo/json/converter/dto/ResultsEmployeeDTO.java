package io.tempo.json.converter.dto;

public record ResultsEmployeeDTO(
        String employeeNumber,
        Personal personal,
        Work work,
        String employmentStatus) {

    public record Personal(String fullName) {
    }

    public record Work(String emailAddress, String position) {
    }
}
