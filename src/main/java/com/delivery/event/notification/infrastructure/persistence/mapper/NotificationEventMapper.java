package com.delivery.event.notification.infrastructure.persistence.mapper;

import com.delivery.event.notification.domain.model.NotificationEvent;
import com.delivery.event.notification.domain.model.Subscription;
import com.delivery.event.notification.infrastructure.persistence.jpa.NotificationEventEntity;
import com.delivery.event.notification.infrastructure.persistence.jpa.SubscriptionEntity;

/**
 * Maps between domain objects and JPA entities.
 */
public final class NotificationEventMapper {

    private NotificationEventMapper() {
    }

    public static NotificationEvent toDomain(NotificationEventEntity entity) {
        NotificationEvent domain = new NotificationEvent();
        domain.setId(entity.getId());
        domain.setCreatedAt(entity.getCreatedAt());
        domain.setCreatedBy(entity.getCreatedBy());
        domain.setUpdatedAt(entity.getUpdatedAt());
        domain.setUpdatedBy(entity.getUpdatedBy());
        domain.setChannel(entity.getChannel());
        domain.setCorrelationId(entity.getCorrelationId());
        domain.setVersion(entity.getVersion());
        domain.setEventId(entity.getEventId());
        domain.setEventType(entity.getEventType());
        domain.setClientId(entity.getClientId());
        domain.setContent(entity.getContent());
        domain.setWebhookUrl(entity.getWebhookUrl());
        domain.setDeliveryStatus(entity.getDeliveryStatus());
        domain.setRetryCount(entity.getRetryCount());
        domain.setReason(entity.getReason());
        domain.setNextAttemptAt(entity.getNextAttemptAt());
        return domain;
    }

    public static NotificationEventEntity toEntity(NotificationEvent domain) {
        NotificationEventEntity entity = new NotificationEventEntity();
        entity.setId(domain.getId());
        entity.setCreatedAt(domain.getCreatedAt());
        entity.setCreatedBy(domain.getCreatedBy());
        entity.setUpdatedAt(domain.getUpdatedAt());
        entity.setUpdatedBy(domain.getUpdatedBy());
        entity.setChannel(domain.getChannel());
        entity.setCorrelationId(domain.getCorrelationId());
        entity.setVersion(domain.getVersion());
        entity.setEventId(domain.getEventId());
        entity.setEventType(domain.getEventType());
        entity.setClientId(domain.getClientId());
        entity.setContent(domain.getContent());
        entity.setWebhookUrl(domain.getWebhookUrl());
        entity.setDeliveryStatus(domain.getDeliveryStatus());
        entity.setRetryCount(domain.getRetryCount());
        entity.setReason(domain.getReason());
        entity.setNextAttemptAt(domain.getNextAttemptAt());
        return entity;
    }

    public static Subscription toDomain(SubscriptionEntity entity) {
        Subscription domain = new Subscription();
        domain.setId(entity.getId());
        domain.setCreatedAt(entity.getCreatedAt());
        domain.setCreatedBy(entity.getCreatedBy());
        domain.setUpdatedAt(entity.getUpdatedAt());
        domain.setUpdatedBy(entity.getUpdatedBy());
        domain.setChannel(entity.getChannel());
        domain.setCorrelationId(entity.getCorrelationId());
        domain.setVersion(entity.getVersion());
        domain.setClientId(entity.getClientId());
        domain.setEventType(entity.getEventType());
        domain.setTargetUrl(entity.getTargetUrl());
        domain.setActive(entity.isActive());
        return domain;
    }
}

