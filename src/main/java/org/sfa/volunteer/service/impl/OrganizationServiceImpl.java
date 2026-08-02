package org.sfa.volunteer.service.impl;

import java.util.List;
import java.util.Optional;

import org.sfa.volunteer.exception.OrganizationNotFoundException;
import org.sfa.volunteer.model.OrgType;
import org.sfa.volunteer.model.OrgSize;
import org.sfa.volunteer.dto.common.SaayamStatusCode;
import org.springframework.stereotype.Service;
import org.sfa.volunteer.dto.request.CreateOrganizationRequest;
import org.sfa.volunteer.dto.response.OrganizationResponse;
import org.sfa.volunteer.dto.response.OrganizationDetailsResponse;
import org.sfa.volunteer.model.Organization;
import org.sfa.volunteer.model.UserOrgMap;
import org.sfa.volunteer.repository.OrganizationRepository;
import org.sfa.volunteer.repository.UserOrgMapRepository;
import org.sfa.volunteer.service.OrganizationService;

import org.springframework.beans.factory.annotation.Autowired;

@Service
public class OrganizationServiceImpl implements OrganizationService {
    private final OrganizationRepository organizationRepository;
    private final UserOrgMapRepository userOrgMapRepository;

    @Autowired
    public OrganizationServiceImpl(OrganizationRepository organizationRepository,
                                   UserOrgMapRepository userOrgMapRepository) {
        this.organizationRepository = organizationRepository;
        this.userOrgMapRepository = userOrgMapRepository;
    }

    @Override
    public List<OrganizationResponse> searchByName(String name) {
        return organizationRepository.findByOrgNameContainingIgnoreCase(name)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }
    
    @Override
    public List<OrganizationDetailsResponse> getOrganizationsByUserId(String userId) {
        return userOrgMapRepository.findByUserId(userId)
                .stream()
                .map(UserOrgMap::getOrgId)
                .map(organizationRepository::findById)
                .flatMap(Optional::stream)
                .map(this::mapToDetailsResponse)
                .toList();
    }

    private OrganizationDetailsResponse mapToDetailsResponse(Organization org) {
        return OrganizationDetailsResponse.builder()
                .orgId(org.getOrgId())
                .orgName(org.getOrgName())
                .orgType(org.getOrgType())
                .orgSize(org.getOrgSize())
                .phone(org.getPhone())
                .email(org.getEmail())
                .webUrl(org.getWebUrl())
                .street(org.getStreet())
                .cityName(org.getCityName())
                .stateId(org.getStateId())
                .zipCode(org.getZipCode())
                .mission(org.getMission())
                .build();
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
    @Override
    public void linkOrganization(String userId, String orgId) {

       Organization organization = organizationRepository.findById(orgId)
        .orElseThrow(() -> new OrganizationNotFoundException(orgId));

    UserOrgMap userOrgMap = UserOrgMap.builder()
            .userId(userId)
            .orgId(organization.getOrgId())
            .userRole("VOLUNTEER")
            .build();

    userOrgMapRepository.save(userOrgMap);
   }
}