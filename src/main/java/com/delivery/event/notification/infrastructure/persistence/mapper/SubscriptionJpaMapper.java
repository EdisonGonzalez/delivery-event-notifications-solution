package com.delivery.event.notification.infrastructure.persistence.mapper;

import com.delivery.event.notification.domain.model.Subscription;
import com.delivery.event.notification.infrastructure.persistence.jpa.SubscriptionEntity;
import org.mapstruct.Mapper;

/** Maps subscription JPA entities to and from the domain model. */
@Mapper(componentModel = "spring")
public interface SubscriptionJpaMapper {

  Subscription toDomain(SubscriptionEntity entity);
}

