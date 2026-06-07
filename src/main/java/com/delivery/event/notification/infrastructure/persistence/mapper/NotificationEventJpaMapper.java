package com.delivery.event.notification.infrastructure.persistence.mapper;

import com.delivery.event.notification.domain.model.NotificationEvent;
import com.delivery.event.notification.infrastructure.persistence.jpa.NotificationEventEntity;
import org.mapstruct.Mapper;

/** Maps notification event JPA entities to and from the domain model. */
@Mapper(componentModel = "spring")
public interface NotificationEventJpaMapper {

  NotificationEvent toDomain(NotificationEventEntity entity);

  NotificationEventEntity toEntity(NotificationEvent domain);
}

