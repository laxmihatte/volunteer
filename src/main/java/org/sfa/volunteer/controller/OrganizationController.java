package org.sfa.volunteer.controller;

import java.util.List;
import org.sfa.volunteer.model.UserOrgMap;
import org.sfa.volunteer.service.UserOrgMapService;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PathVariable;
import jakarta.validation.Valid;
import org.sfa.volunteer.dto.request.CreateOrganizationRequest;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.sfa.volunteer.dto.response.OrganizationResponse;
import org.sfa.volunteer.dto.common.SaayamResponse;
import org.sfa.volunteer.dto.common.SaayamStatusCode;
import org.sfa.volunteer.dto.response.OrganizationDetailsResponse;
import org.sfa.volunteer.service.OrganizationService;
import org.sfa.volunteer.util.ResponseBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/0.0.1/organizations")
public class OrganizationController {

    private final UserOrgMapService userOrgMapService;
    private final OrganizationService organizationService;
    private final ResponseBuilder responseBuilder;

    @Autowired
    public OrganizationController(OrganizationService organizationService,
                                  ResponseBuilder responseBuilder,
                                  UserOrgMapService userOrgMapService) {
        this.organizationService = organizationService;
        this.responseBuilder = responseBuilder;
        this.userOrgMapService = userOrgMapService;
    }

    @GetMapping("/search")
    public SaayamResponse<List<OrganizationResponse>> search(@RequestParam String name) {
        List<OrganizationResponse> results = organizationService.searchByName(name);
        return responseBuilder.buildSuccessResponse(
                SaayamStatusCode.SUCCESS,
                results
        );
    }
    @PutMapping("/users/{userId}/organization/{orgId}")
    public SaayamResponse<UserOrgMap> linkUserToOrg(
            @PathVariable String userId,
            @PathVariable String orgId) {
        UserOrgMap result = userOrgMapService.linkUserToOrg(userId, orgId);
        return responseBuilder.buildSuccessResponse(SaayamStatusCode.SUCCESS, result);
    }
    @GetMapping("/users/{userId}/organizations")
    public SaayamResponse<List<OrganizationDetailsResponse>> getUserOrganizations(
            @PathVariable String userId) {
        List<OrganizationDetailsResponse> organizations = organizationService.getOrganizationsByUserId(userId);
        return responseBuilder.buildSuccessResponse(SaayamStatusCode.SUCCESS, organizations);
    }
    @PostMapping
    public SaayamResponse<OrganizationResponse> createOrganization(
            @Valid @RequestBody CreateOrganizationRequest request) {
        OrganizationResponse response = organizationService.createOrganization(request);
        return responseBuilder.buildSuccessResponse(SaayamStatusCode.ORGANIZATION_CREATED, response);
    }
}