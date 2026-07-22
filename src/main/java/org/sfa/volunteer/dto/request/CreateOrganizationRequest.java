package org.sfa.volunteer.dto.request;

import jakarta.validation.constraints.NotBlank;
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
        String phone,
        String email,
        String webUrl,
        String mission
) {}