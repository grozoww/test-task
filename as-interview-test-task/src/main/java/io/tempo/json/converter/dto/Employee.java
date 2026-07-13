package io.tempo.json.converter.dto;

import lombok.Builder;

@Builder
public record Employee(String id, String fullName, String email, String role, boolean active) {
}
