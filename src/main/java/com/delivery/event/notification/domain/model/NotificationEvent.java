package com.delivery.event.notification.domain.model;

import java.time.Instant;
import java.util.Objects;

/**
 * Domain object representing a notification event to be delivered.
 */
public class NotificationEvent extends Audit {

    private String eventId;
    private String eventType;
    private String clientId;
    private String content;
    private String webhookUrl;
    private DeliveryStatus deliveryStatus;
    private int retryCount;
    private String reason;
    private Instant nextAttemptAt;

    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getWebhookUrl() {
        return webhookUrl;
    }

    public void setWebhookUrl(String webhookUrl) {
        this.webhookUrl = webhookUrl;
    }

    public DeliveryStatus getDeliveryStatus() {
        return deliveryStatus;
    }

    public void setDeliveryStatus(DeliveryStatus deliveryStatus) {
        this.deliveryStatus = deliveryStatus;
    }

    public int getRetryCount() {
        return retryCount;
    }

    public void setRetryCount(int retryCount) {
        this.retryCount = retryCount;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public Instant getNextAttemptAt() {
        return nextAttemptAt;
    }

    public void setNextAttemptAt(Instant nextAttemptAt) {
        this.nextAttemptAt = nextAttemptAt;
    }

    public void markPending() {
        this.deliveryStatus = DeliveryStatus.PENDING;
        this.retryCount = 0;
        this.reason = null;
    }

    public void markIgnored(String reason) {
        this.deliveryStatus = DeliveryStatus.IGNORED;
        this.reason = reason;
    }

    public void markFailed(String reason) {
        this.deliveryStatus = DeliveryStatus.FAILED;
        this.reason = reason;
    }

    public void markCompleted() {
        this.deliveryStatus = DeliveryStatus.COMPLETED;
        this.reason = null;
    }

    public boolean belongsTo(String authenticatedClientId) {
        return Objects.equals(this.clientId, authenticatedClientId);
    }
}

