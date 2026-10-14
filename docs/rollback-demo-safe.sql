-- ============================================================================
-- SCIRS / Kinn Htout — Targeted Rollback for Safe Demo Dataset
--
-- File: docs/rollback-demo-safe.sql
-- Purpose: Safely removes ONLY the demo data created by seed-demo-safe.sql.
--          Does NOT affect any existing administrative accounts, real citizens,
--          real reports, departments, or categories.
--
-- Safe Target Filters:
-- - Reports: report_code LIKE 'RPT-DEMO-%'
-- - Users:   email LIKE '%@demo.scirs.gov'
-- ============================================================================

BEGIN;

-- 1. Remove Child Records of Demo Reports & Demo Users
DELETE FROM feedback 
WHERE report_id IN (SELECT id FROM reports WHERE report_code LIKE 'RPT-DEMO-%')
   OR citizen_id IN (SELECT id FROM users WHERE email LIKE '%@demo.scirs.gov');

DELETE FROM report_images 
WHERE report_id IN (SELECT id FROM reports WHERE report_code LIKE 'RPT-DEMO-%')
   OR uploaded_by IN (SELECT id FROM users WHERE email LIKE '%@demo.scirs.gov');

DELETE FROM report_status_history 
WHERE report_id IN (SELECT id FROM reports WHERE report_code LIKE 'RPT-DEMO-%')
   OR changed_by IN (SELECT id FROM users WHERE email LIKE '%@demo.scirs.gov');

DELETE FROM report_comments 
WHERE report_id IN (SELECT id FROM reports WHERE report_code LIKE 'RPT-DEMO-%')
   OR author_id IN (SELECT id FROM users WHERE email LIKE '%@demo.scirs.gov');

-- report_confirmations / report_supports (if table exists)
DO $$
BEGIN
    IF EXISTS (SELECT FROM information_schema.tables WHERE table_name = 'report_confirmations') THEN
        DELETE FROM report_confirmations 
        WHERE report_id IN (SELECT id FROM reports WHERE report_code LIKE 'RPT-DEMO-%')
           OR citizen_id IN (SELECT id FROM users WHERE email LIKE '%@demo.scirs.gov');
    END IF;

    IF EXISTS (SELECT FROM information_schema.tables WHERE table_name = 'report_supports') THEN
        DELETE FROM report_supports 
        WHERE report_id IN (SELECT id FROM reports WHERE report_code LIKE 'RPT-DEMO-%')
           OR citizen_id IN (SELECT id FROM users WHERE email LIKE '%@demo.scirs.gov');
    END IF;
END $$;

DELETE FROM notifications 
WHERE report_id IN (SELECT id FROM reports WHERE report_code LIKE 'RPT-DEMO-%')
   OR recipient_id IN (SELECT id FROM users WHERE email LIKE '%@demo.scirs.gov')
   OR subject_user_id IN (SELECT id FROM users WHERE email LIKE '%@demo.scirs.gov');

DELETE FROM point_transactions 
WHERE report_id IN (SELECT id FROM reports WHERE report_code LIKE 'RPT-DEMO-%')
   OR user_id IN (SELECT id FROM users WHERE email LIKE '%@demo.scirs.gov');

-- 2. Remove Demo Reports
DELETE FROM reports 
WHERE report_code LIKE 'RPT-DEMO-%';

-- 3. Remove Demo Citizens & Demo Staff
DELETE FROM users 
WHERE email LIKE '%@demo.scirs.gov';

-- 4. Re-align PostgreSQL Sequences
SELECT setval(pg_get_serial_sequence('users', 'id'), COALESCE(MAX(id), 1)) FROM users;
SELECT setval(pg_get_serial_sequence('reports', 'id'), COALESCE(MAX(id), 1)) FROM reports;
SELECT setval(pg_get_serial_sequence('report_images', 'id'), COALESCE(MAX(id), 1)) FROM report_images;
SELECT setval(pg_get_serial_sequence('report_status_history', 'id'), COALESCE(MAX(id), 1)) FROM report_status_history;
SELECT setval(pg_get_serial_sequence('report_comments', 'id'), COALESCE(MAX(id), 1)) FROM report_comments;
SELECT setval(pg_get_serial_sequence('feedback', 'id'), COALESCE(MAX(id), 1)) FROM feedback;
SELECT setval(pg_get_serial_sequence('notifications', 'id'), COALESCE(MAX(id), 1)) FROM notifications;
SELECT setval(pg_get_serial_sequence('point_transactions', 'id'), COALESCE(MAX(id), 1)) FROM point_transactions;

COMMIT;

-- 5. Verification Query (Should all return 0)
SELECT 'Remaining Demo Users' AS check_metric, COUNT(*) AS count FROM users WHERE email LIKE '%@demo.scirs.gov'
UNION ALL
SELECT 'Remaining Demo Reports', COUNT(*) FROM reports WHERE report_code LIKE 'RPT-DEMO-%';
