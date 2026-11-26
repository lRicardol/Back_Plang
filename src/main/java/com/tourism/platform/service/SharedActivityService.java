package com.tourism.platform.service;

import com.tourism.platform.dto.SharedActivityDecisionRequest;
import com.tourism.platform.dto.SharedActivityResponse;
import org.springframework.lang.NonNull;
import java.util.List;

public interface SharedActivityService {

    /**
     * Share an activity with another user.
     *
     * @param activityId     id of the activity being shared
     * @param receiverId     id of the user receiving the shared activity
     * @param senderUsername username of the sender for audit/notification purposes
     * @return SharedActivityResponse containing share metadata
     */
    SharedActivityResponse shareActivity(@NonNull Long activityId, @NonNull Long receiverId, String senderUsername);

    /**
     * Resolve a shared activity (accept/reject or take action).
     *
     * @param sharedActivityId id of the shared activity record
     * @param request          decision request DTO containing action details
     * @param receiverUsername username of the receiver performing the resolution
     * @return SharedActivityResponse reflecting the result of the resolution
     */
    SharedActivityResponse resolveSharedActivity(@NonNull Long sharedActivityId, SharedActivityDecisionRequest request, String receiverUsername);

    List<SharedActivityResponse> getSharedActivities(String username);

    List<SharedActivityResponse> shareAllActivities(@NonNull Long travelPlanId, @NonNull Long receiverId, String senderUsername);
}
