package org.sfa.volunteer.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sfa.volunteer.dto.common.SaayamStatusCode;
import org.sfa.volunteer.exception.GlobalExceptionHandler;
import org.sfa.volunteer.exception.OrganizationNotFoundException;
import org.sfa.volunteer.service.OrganizationService;
import org.sfa.volunteer.util.MessageSourceUtil;
import org.sfa.volunteer.util.ResponseBuilder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class OrganizationExceptionHandlingTest {

    private static final String BASE_PATH = "/0.0.1/organizations";
    private static final String ORG_ID = "ORG-X";
    private static final String USER_ID = "USER-1";
    private static final String NOT_FOUND_MESSAGE = "not found";
    private static final String BAD_VALUE_MESSAGE = "bad value";

    @Mock
    private OrganizationService organizationService;

    @Mock
    private ResponseBuilder responseBuilder;

    @Mock
    private MessageSourceUtil messageSourceUtil;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        OrganizationController controller =
                new OrganizationController(organizationService, responseBuilder);
        GlobalExceptionHandler advice =
                new GlobalExceptionHandler(messageSourceUtil, responseBuilder);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(advice)
                .build();
    }

    @Test
    void linkOrganization_unknownOrg_returns404() throws Exception {
        doThrow(new OrganizationNotFoundException(ORG_ID))
                .when(organizationService).linkOrganization(USER_ID, ORG_ID);
        when(messageSourceUtil.getMessage(any(), any())).thenReturn(NOT_FOUND_MESSAGE);

        mockMvc.perform(post(BASE_PATH + "/" + ORG_ID + "/users/" + USER_ID))
                .andExpect(status().isNotFound());

        verify(responseBuilder).buildErrorResponse(
                eq(404), eq(SaayamStatusCode.ORGANIZATION_NOT_FOUND), eq(NOT_FOUND_MESSAGE));
    }

    @Test
    void getUserOrganizations_illegalArgument_returns400() throws Exception {
        when(organizationService.getOrganizationsByUserId(USER_ID))
                .thenThrow(new IllegalArgumentException(BAD_VALUE_MESSAGE));

        mockMvc.perform(get(BASE_PATH + "/users/" + USER_ID))
                .andExpect(status().isBadRequest());

        verify(responseBuilder).buildErrorResponse(
                eq(400), eq(SaayamStatusCode.BAD_REQUEST), eq(BAD_VALUE_MESSAGE));
    }
}