This document has been consolidated into the top-level `README.md`.

Import the Postman collection file at `docs/postman/DeliveryEventNotifications.postman_collection.json` and follow the "
Postman collection and quick test guide" section in `README.md` for detailed instructions, the `psql` helper command to
create a subscription, and the recommended flow to exercise health, listing, retrieval and replay flows.

Current behavior reminder:

- There is no REST endpoint to create subscriptions yet.
- Active subscriptions must be created directly in the database.
- The outbound webhook destination currently comes from the `webhookUrl` field sent in `POST /notification_events`.
- The `subscriptions` table is still required as an active-subscription check before delivery.

If you still want an independent short README here, or prefer a dev-only HTTP endpoint to create subscriptions from
Postman, tell me and I will add it.
