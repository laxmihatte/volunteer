package org.sfa.volunteer.service;

import org.sfa.volunteer.dto.request.CreateOrganizationRequest;
import org.sfa.volunteer.dto.response.OrganizationResponse;
import org.sfa.volunteer.dto.response.OrganizationDetailsResponse;

import java.util.List;

public interface OrganizationService {

    List<OrganizationResponse> searchByName(String name);

    OrganizationResponse createOrganization(CreateOrganizationRequest request);

    List<OrganizationDetailsResponse> getOrganizationsByUserId(String userId);
}