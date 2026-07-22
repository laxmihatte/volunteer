package org.sfa.volunteer.service.impl;

import java.util.List;

import org.springframework.stereotype.Service;
import org.sfa.volunteer.dto.request.CreateOrganizationRequest;
import org.sfa.volunteer.dto.response.OrganizationResponse;
import org.sfa.volunteer.model.Organization;
import org.sfa.volunteer.repository.OrganizationRepository;
import org.sfa.volunteer.service.OrganizationService;

import org.springframework.beans.factory.annotation.Autowired;

@Service
public class OrganizationServiceImpl implements OrganizationService {
    private final OrganizationRepository organizationRepository;

    @Autowired
    public OrganizationServiceImpl(OrganizationRepository organizationRepository) {
        this.organizationRepository = organizationRepository;
    }

    @Override
    public List<OrganizationResponse> searchByName(String name) {
        return organizationRepository.findByOrgNameContainingIgnoreCase(name)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }
    @Override
    public OrganizationResponse createOrganization(CreateOrganizationRequest request) {
        Organization organization = Organization.builder()
                .orgName(request.orgName())
                .orgType(request.orgType())
                .orgSize(request.orgSize())
                .street(request.street())
                .cityName(request.cityName())
                .stateId(request.stateId())
                .zipCode(request.zipCode())
                .phone(request.phone())
                .email(request.email())
                .webUrl(request.webUrl())
                .mission(request.mission())
                .build();

        Organization saved = organizationRepository.save(organization);
        return mapToResponse(saved);
    }
    private OrganizationResponse mapToResponse(Organization org) {
        return OrganizationResponse.builder()
                .orgId(org.getOrgId())
                .orgName(org.getOrgName())
                .cityName(org.getCityName())
                .stateId(org.getStateId())
                .build();
    }
}