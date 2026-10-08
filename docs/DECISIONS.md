# Technical decisions

## Inventory concurrency: pessimistic locks plus optimistic versioning

Order confirmation locks the order row first, then acquires write locks for all
existing inventory balances in stable product-ID order. Availability for every
line is checked while those locks are held, before any balance or movement is
changed. PostgreSQL therefore serializes competing confirmations against the
same product/warehouse balance: with ten units and two seven-unit orders, the
first confirmation commits and the second observes three units and fails with
the predictable domain message `Insufficient stock`. The transaction rolls
back in full, leaving the losing order in `DRAFT` and creating no movement for
it.

`InventoryBalance` also carries a JPA `@Version` field as a second-line guard
against writes that do not follow the locking service path. Pessimistic locks
are the primary mechanism for confirmation because the operation needs to
validate and mutate multiple rows atomically; optimistic retries would make
the multi-line all-or-nothing check and user-visible conflict less direct.
Balance locks are ordered consistently to reduce deadlock risk. A PostgreSQL
Testcontainers integration test races two independent transactions and
asserts final quantity, order states, and movement count.

## Application architecture: modular monolith

Business modules live in one Spring Boot deployment and one PostgreSQL database. REST controllers remain thin, services own transaction boundaries, and entities enforce local state invariants. This keeps the portfolio project easy to run and test while preserving module boundaries without introducing distributed infrastructure.

## Database evolution: PostgreSQL plus Flyway

Flyway migrations are the source of schema changes and Hibernate uses schema validation. PostgreSQL-specific constraints, locking, and Testcontainers tests are part of the correctness story; H2 is not treated as a substitute.

## Authentication scope and demo profile status

Spring Security currently enforces role checks at routes, but the application does not yet provide the planned JWT login/token validation workflow. Consequently, C-02 is explicitly pending and demo credentials/data are not advertised as available. Demo profile configuration remains distinct from production, and no seed executes in production.
