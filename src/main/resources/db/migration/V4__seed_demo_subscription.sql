insert into subscriptions (
    id,
    client_id,
    event_type,
    target_url,
    active,
    created_by,
    created_at,
    updated_by,
    updated_at,
    channel,
    correlation_id,
    version
)
values (
    '55555555-5555-5555-5555-555555555555',
    'client-b',
    'ORDER_CREATED',
    'https://webhook.site/replace-me',
    true,
    'seed',
    now(),
    'seed',
    now(),
    'api',
    'seed-sub-order-created-client-b',
    0
)
on conflict (id) do nothing;

