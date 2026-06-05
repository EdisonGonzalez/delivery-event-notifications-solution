create table if not exists subscriptions (
    id uuid primary key,
    client_id varchar(120) not null,
    event_type varchar(120) not null,
    target_url text not null,
    active boolean not null,
    created_by varchar(120) not null,
    created_at timestamp with time zone not null,
    updated_by varchar(120) not null,
    updated_at timestamp with time zone not null,
    channel varchar(60) not null,
    correlation_id varchar(120) not null,
    version bigint not null
);

create table if not exists notification_events (
    id uuid primary key,
    event_id varchar(36) not null unique,
    event_type varchar(120) not null,
    client_id varchar(120) not null,
    content text not null,
    webhook_url text not null,
    delivery_status varchar(30) not null,
    retry_count integer not null,
    reason text null,
    next_attempt_at timestamp with time zone null,
    created_by varchar(120) not null,
    created_at timestamp with time zone not null,
    updated_by varchar(120) not null,
    updated_at timestamp with time zone not null,
    channel varchar(60) not null,
    correlation_id varchar(120) not null,
    version bigint not null
);

create index if not exists idx_notification_events_status_next_attempt
    on notification_events (delivery_status, next_attempt_at);

create index if not exists idx_notification_events_client_status
    on notification_events (client_id, delivery_status);

create index if not exists idx_subscriptions_client_event_active
    on subscriptions (client_id, event_type, active);

