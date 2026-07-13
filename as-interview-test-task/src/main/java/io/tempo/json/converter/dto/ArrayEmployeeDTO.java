package io.tempo.json.converter.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ArrayEmployeeDTO(
        @JsonProperty("user_id") Long userId,
        @JsonProperty("first_name") String firstName,
        @JsonProperty("last_name") String lastName,
        @JsonProperty("contact_email") String email,
        @JsonProperty("job_title") String jobTitle,
        @JsonProperty("is_active") Integer isActive) {
}
