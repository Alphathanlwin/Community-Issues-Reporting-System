-- ============================================================================
-- SCIRS — Enum CHECK-constraint fix-ups for databases created before a new
-- enum value was added.
--
-- WHY THIS EXISTS: with `@Enumerated(EnumType.STRING)`, Hibernate 6 emits a
-- `CHECK (col IN (...))` constraint listing the enum values that existed when
-- the column was first created. `ddl-auto=update` NEVER alters an existing
-- CHECK constraint, so adding a value to a Java enum leaves older databases
-- rejecting inserts that use the new value (500 "An unexpected error
-- occurred"). Fresh databases are fine — Hibernate writes the full list.
--
-- NOT wired into an auto-run migration tool (this project uses ddl-auto, not
-- Flyway/Liquibase). Apply once per pre-existing environment:
--     psql -U postgres -d scirs_db -f src/main/resources/db/schema-constraints.sql
--
-- Idempotent: DROP ... IF EXISTS then re-ADD with the current full value set,
-- reusing Hibernate's own constraint name so a patched DB matches a fresh one.
-- ============================================================================

-- point_transactions.reason — PointReason. Older DBs are missing
-- CONFIRMATION_GIVEN (the "me too" award), SUPPORT_GIVEN (the community-feed
-- "Support" award) and SUPPORT_REMOVED (its reversal when support is toggled
-- off).
ALTER TABLE point_transactions
    DROP CONSTRAINT IF EXISTS point_transactions_reason_check;
ALTER TABLE point_transactions
    ADD CONSTRAINT point_transactions_reason_check
    CHECK (reason IN (
        'REPORT_APPROVED',
        'REPORT_RESOLVED',
        'FEEDBACK_GIVEN',
        'REPORT_REJECTED',
        'CONFIRMATION_GIVEN',
        'SUPPORT_GIVEN',
        'SUPPORT_REMOVED'
    ));
