# API examples

The REST base path is `/api/v1`. Business routes require authenticated access; the API documentation and health check are public. Use the login endpoint to obtain a short-lived signed bearer token.

## Local/demo credentials

The `demo` Spring profile seeds only an ADMIN user, and only when `DEMO_ADMIN_PASSWORD` is configured with at least 16 characters. Set `JWT_SECRET` to an independent random value of at least 32 bytes. Keep both values in a local `.env` file (ignored by Git) or a secret manager; never reuse production secrets. `.env.example` contains placeholders only. The seed does not run in `dev`, `test`, or `prod`.

Demo account email defaults to `demo-admin@example.invalid` and can be overridden with `DEMO_ADMIN_EMAIL`. No demo password is shipped or logged.

## Login

```http
POST /api/v1/auth/login
Content-Type: application/json

{"email":"user@example.com","password":"<user-password>"}
```

Successful responses include `accessToken` and `tokenType: Bearer`; supply `Authorization: Bearer <accessToken>` for business requests. Invalid, unknown, and disabled accounts share an unauthorized response. Credentials and tokens must never be written to logs.

## Health

```http
GET /actuator/health
```

## List products

```http
GET /api/v1/products?page=0&size=20
Authorization: Bearer <accessToken>
```

## Create a draft order

```http
POST /api/v1/orders
Content-Type: application/json

{"warehouseId":"00000000-0000-0000-0000-000000000001"}
```

## Add an order item

```http
POST /api/v1/orders/00000000-0000-0000-0000-000000000002/items
Content-Type: application/json

{"productId":"00000000-0000-0000-0000-000000000003","quantity":2,"unitPrice":12.50}
```

## Confirm idempotently

```http
POST /api/v1/orders/00000000-0000-0000-0000-000000000002/confirm
Idempotency-Key: order-confirmation-unique-key
```

Repeated confirmation with the same actor, order, and key does not consume stock a second time. Reusing that key for another order conflicts.

## Cancel or fulfill

```http
POST /api/v1/orders/00000000-0000-0000-0000-000000000002/cancel
POST /api/v1/orders/00000000-0000-0000-0000-000000000002/fulfill
```

## Inventory and dashboard

```http
GET /api/v1/inventory/receipts?lowStock=true&page=0&size=20
GET /api/v1/inventory/receipts/movements?page=0&size=20
GET /api/v1/dashboard
```

Examples intentionally contain no usable credentials or bearer tokens. See Swagger UI for request/response schemas. JWT expiry is configurable with `JWT_EXPIRATION_SECONDS`; production requires a strong externally managed `JWT_SECRET`.
