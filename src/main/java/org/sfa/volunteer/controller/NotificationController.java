package org.sfa.volunteer.controller;

import org.sfa.volunteer.dto.common.SaayamResponse;
import org.sfa.volunteer.dto.common.SaayamStatusCode;
import org.sfa.volunteer.dto.request.GetNotificationsRequest;
import org.sfa.volunteer.dto.request.UpsertLastSeenRequest;
import org.sfa.volunteer.dto.response.GetNotificationsResponse;
import org.sfa.volunteer.dto.response.UpsertLastSeenResponse;
import org.sfa.volunteer.service.NotificationService;
import org.sfa.volunteer.util.ResponseBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/0.0.1/notifications")
public class NotificationController {

        private final NotificationService notificationService;
        private final ResponseBuilder responseBuilder;

        @Autowired
        public NotificationController(NotificationService notificationService, ResponseBuilder responseBuilder) {
                this.notificationService = notificationService;
                this.responseBuilder = responseBuilder;

        }

        @GetMapping("/{userId}/counts")
        public SaayamResponse<GetNotificationsResponse> getNotificationCounts(
                        @PathVariable String userId) throws Exception {

                GetNotificationsRequest request = GetNotificationsRequest.builder().userId(userId).build();
                GetNotificationsResponse response = notificationService.getNotificationCounts(request);
                return responseBuilder.buildSuccessResponse(
                                SaayamStatusCode.SUCCESS,
                                response);
        }

        @GetMapping("/{userId}")
        public SaayamResponse<GetNotificationsResponse> getNotifications(
                        @PathVariable String userId, @RequestParam int rowStart, @RequestParam int rowEnd)
                        throws Exception {

                GetNotificationsRequest request = GetNotificationsRequest.builder().userId(userId).rowStart(rowStart)
                                .rowEnd(rowEnd).build();
                GetNotificationsResponse response = notificationService.getNotifications(request);
                return responseBuilder.buildSuccessResponse(
                                SaayamStatusCode.SUCCESS,
                                response);
        }

        @PostMapping("/lastseen")
        public SaayamResponse<UpsertLastSeenResponse> updateLastSeen(
                        @Valid @RequestBody UpsertLastSeenRequest request) throws Exception {

                UpsertLastSeenResponse response = notificationService.upsertLastSeen(request);
                return responseBuilder.buildSuccessResponse(
                                SaayamStatusCode.SUCCESS,
                                response);
        }
}