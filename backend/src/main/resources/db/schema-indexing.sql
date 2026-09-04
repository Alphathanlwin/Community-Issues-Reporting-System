-- ============================================================================
-- SCIRS — Performance indexes for high-traffic read/aggregate queries.
--
-- NOT wired into an auto-run migration tool: this project uses Hibernate
-- ddl-auto=update (see application.properties), not Flyway/Liquibase. Apply
-- this once per environment after the tables exist:
--     psql -U postgres -d scirs_db -f src/main/resources/db/schema-indexing.sql
--
-- Every statement uses IF NOT EXISTS, so re-running this script (or running
-- it after Hibernate has already created an equivalent index — see the note
-- on idx_reports_status below) is always a safe no-op.
-- ============================================================================

-- Hibernate already creates this one from @Table(indexes = ...) on the
-- Report entity (report/entity/Report.java) whenever ddl-auto runs. Declared
-- here too so the index still exists if this script is the first thing run
-- against a fresh database (e.g. a teammate restoring a bare dump).
CREATE INDEX IF NOT EXISTS idx_reports_status
    ON reports (status);

-- Staff/admin dashboards filter "reports for my department in status X".
CREATE INDEX IF NOT EXISTS idx_reports_dept_status
    ON reports (department_id, status);

-- Category volume aggregates (dashboard/service/DashboardService.java).
CREATE INDEX IF NOT EXISTS idx_reports_category
    ON reports (category_id);

-- "My reports" list (GET /api/reports/my), newest first.
CREATE INDEX IF NOT EXISTS idx_reports_reporter
    ON reports (reporter_id, created_at DESC);

-- Global recency ordering (admin report list, public map default sort).
CREATE INDEX IF NOT EXISTS idx_reports_created
    ON reports (created_at DESC);

-- Status-history timeline for a single report (GET /api/reports/{id}/history).
CREATE INDEX IF NOT EXISTS idx_status_history_report
    ON report_status_history (report_id, changed_at);

-- Citizen score history / leaderboard point-transaction lookups.
CREATE INDEX IF NOT EXISTS idx_point_tx_user
    ON point_transactions (user_id);
