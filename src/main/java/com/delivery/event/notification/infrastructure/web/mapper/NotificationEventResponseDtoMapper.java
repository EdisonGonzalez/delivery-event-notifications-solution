package com.delivery.event.notification.infrastructure.web.mapper;

import com.delivery.event.notification.domain.model.NotificationEvent;
import com.delivery.event.notification.infrastructure.web.dto.NotificationEventResponseDto;
import org.mapstruct.Mapper;

/** Maps notification events to their HTTP response DTO representation. */
@Mapper(componentModel = "spring")
public interface NotificationEventResponseDtoMapper {

  NotificationEventResponseDto toResponseDto(NotificationEvent event);
}

