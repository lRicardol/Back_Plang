package com.tourism.platform.dto;

/**
 * Response DTO returned after sharing an activity. Includes identifiers and
 * any metadata required by the client to render shared activity information.
 */

import com.tourism.platform.model.SharedActivityStatus;
import java.time.LocalDateTime;

public class SharedActivityResponse {
    private Long id;
    private Long activityId;
    private Long senderId;
    private Long receiverId;
    private SharedActivityStatus status;
    private boolean sharedPlan;
    private String activityName;
    private String activityDescription;
    private String activityLocation;
    private LocalDateTime activityStartTime;
    private LocalDateTime activityEndTime;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getActivityId() {
        return activityId;
    }

    public void setActivityId(Long activityId) {
        this.activityId = activityId;
    }

    public Long getSenderId() {
        return senderId;
    }

    public void setSenderId(Long senderId) {
        this.senderId = senderId;
    }

    public Long getReceiverId() {
        return receiverId;
    }

    public void setReceiverId(Long receiverId) {
        this.receiverId = receiverId;
    }

    public SharedActivityStatus getStatus() {
        return status;
    }

    public void setStatus(SharedActivityStatus status) {
        this.status = status;
    }

    public boolean isSharedPlan() {
        return sharedPlan;
    }

    public void setSharedPlan(boolean sharedPlan) {
        this.sharedPlan = sharedPlan;
    }

    public String getActivityName() { return activityName; }
    public void setActivityName(String activityName) { this.activityName = activityName; }
    public String getActivityDescription() { return activityDescription; }
    public void setActivityDescription(String activityDescription) { this.activityDescription = activityDescription; }
    public String getActivityLocation() { return activityLocation; }
    public void setActivityLocation(String activityLocation) { this.activityLocation = activityLocation; }
    public LocalDateTime getActivityStartTime() { return activityStartTime; }
    public void setActivityStartTime(LocalDateTime activityStartTime) { this.activityStartTime = activityStartTime; }
    public LocalDateTime getActivityEndTime() { return activityEndTime; }
    public void setActivityEndTime(LocalDateTime activityEndTime) { this.activityEndTime = activityEndTime; }
}
