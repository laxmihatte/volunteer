package org.sfa.volunteer.service.impl;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;

import org.sfa.volunteer.dto.request.GetNotificationsRequest;
import org.sfa.volunteer.dto.request.UpsertLastSeenRequest;
import org.sfa.volunteer.dto.response.GetNotificationsResponse;
import org.sfa.volunteer.dto.response.NotificationResponse;
import org.sfa.volunteer.dto.response.UpsertLastSeenResponse;
import org.sfa.volunteer.enums.StatusType;
import org.sfa.volunteer.exception.UserNotFoundException;
import org.sfa.volunteer.repository.NotificationsRepository;
import org.sfa.volunteer.repository.UserNotificationStatusRepository;
import org.sfa.volunteer.service.NotificationService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class NotificationServiceImpl implements NotificationService {

    private final NotificationsRepository notificationsRepo;
    private final UserNotificationStatusRepository userNtfStatusRepo;

    public NotificationServiceImpl(
            NotificationsRepository notificationsRepo,
            UserNotificationStatusRepository userNtfStatusRepo) {

        this.notificationsRepo = notificationsRepo;
        this.userNtfStatusRepo = userNtfStatusRepo;
    }

    @Override
    public GetNotificationsResponse getNotificationCounts(GetNotificationsRequest request) {

        String userId = request.userId();
        int totalNewNotificationsCount = 0;

        // 1. Count total notifications
        int totalNotificationsCount = notificationsRepo.countAllNotifications(userId);

        // 2. Query watermark table for userId
        Timestamp watermarkTs = userNtfStatusRepo.getLastSeenTimestamp(userId);

        // If watermark is null → user has never seen/read notifications
        if (watermarkTs == null) {
            // default to totalNtfCount because user has never seen any notifications.
            totalNewNotificationsCount = totalNotificationsCount;
        } else {
            // 3. Count new notifications (ntf.createDttm > watermarkTimestamp)
            totalNewNotificationsCount = notificationsRepo.countNewNotifications(userId, watermarkTs);
        }

        // return only the counts with blank/null notifications data
        return GetNotificationsResponse.builder()
                .notifications(null)
                .totalCount(totalNotificationsCount)
                .newNotificationsCount(totalNewNotificationsCount)
                .build();

    }

    @Override
    public GetNotificationsResponse getNotifications(GetNotificationsRequest request) {

        String userId = request.userId();
        int rowStart = request.rowStart();
        int rowEnd = request.rowEnd();

        // // 1. Get watermark timestamp (last seen)
        Timestamp lastSeen = userNtfStatusRepo.getLastSeenTimestamp(userId);
        final Timestamp watermarkTs = (lastSeen != null) ? lastSeen : Timestamp.from(Instant.now());

        // 2. Fetch paginated notifications sorted by createDttm DESC
        int limit = rowEnd - rowStart + 1; // number of records per page
        int offset = rowStart;
        Pageable pageable = PageRequest.of(offset, limit);
        List<NotificationResponse> notifications = notificationsRepo.findNotifications(userId, pageable);

        // 3. Override status field with "new" or "old"
        List<NotificationResponse> items = notifications.stream()
                .map(n -> {
                    boolean isNew = n.createDttm().after(watermarkTs);

                    return NotificationResponse.builder()
                            .notificationId(n.notificationId())
                            .status(isNew ? "new" : "old") // <-- HERE
                            .typeName(n.typeName())
                            .message(n.message())
                            .createDttm(n.createDttm())
                            .build();
                })
                .toList();

        // 3. Count totals
        int totalCount = notificationsRepo.countAllNotifications(userId);
        int newNotificationsCount = notificationsRepo.countNewNotifications(userId, watermarkTs);

        // 5. Return final response
        return GetNotificationsResponse.builder()
                .notifications(items)
                .totalCount(totalCount)
                .newNotificationsCount(newNotificationsCount)
                .build();

    }

    @Transactional
    @Override
    public UpsertLastSeenResponse upsertLastSeen(UpsertLastSeenRequest request) {

        boolean rowCreated = false;
        boolean rowUpdated = false;
        String userId = request.userId();

        if (userId == null || userId.isBlank()) {
            throw new UserNotFoundException(userId);
        }

        // Always returns UTC/GMT
        Instant timeGMT = Instant.now();
        // format it to Timestamp
        Timestamp watermarkTs = Timestamp.from(timeGMT);

        // TODO Fault handling
        if (userNtfStatusRepo.existsByUserId(userId) == 0) {
            rowCreated = userNtfStatusRepo.createLastSeenTimestamp(userId, watermarkTs) > 0;
        } else {
            rowUpdated = userNtfStatusRepo.updateLastSeenTimestamp(userId, watermarkTs) > 0;
        }

        // TODO
        return UpsertLastSeenResponse.builder()
                .created(rowCreated)
                .updated(rowUpdated)
                .message(rowCreated ? "Created" : rowUpdated ? "Updated" : "failed")
                .build();
    }

}