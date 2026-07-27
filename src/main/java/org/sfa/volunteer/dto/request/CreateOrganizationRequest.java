package org.sfa.volunteer.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import org.hibernate.validator.constraints.URL;
import lombok.Builder;

@Builder
public record CreateOrganizationRequest(
        @NotBlank(message = "Organization name is required")
        String orgName,
        String orgType,
        String orgSize,
        String street,
        String cityName,
        String stateId,
        @NotBlank(message = "Zip code is required")
        String zipCode,
        @Pattern(regexp = "^[+]?[0-9\\s()-]{7,20}$", message = "Invalid phone number")
        String phone,
        @Email(message = "Invalid email format")
        String email,
        @URL(message = "Invalid URL")
        String webUrl,
        String mission
) {}