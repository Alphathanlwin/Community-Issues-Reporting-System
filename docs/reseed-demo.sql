-- Wipe the demo dataset so MockDataSeeder re-creates it fresh on the next boot
-- (with the current 14 categories, 30 reports, and the bundled /seed-images photos).
--
-- KEEPS: roles, departments, categories, the admin account, and any real
--        (non-demo) user accounts.
-- REMOVES: every report and its children, all notifications / feedback /
--          point transactions, and the seeded demo citizens + department staff.
--
-- Run in DataGrip / psql / the Supabase SQL editor, THEN:
--   1. set SEED_MOCK_DATA=true in backend/.env
--   2. cd backend && ./mvnw spring-boot:run   (wait for "Started ScirsApplication", then Ctrl+C)
--   3. set SEED_MOCK_DATA=false again
--   4. refresh the app — reports now show the real photos

BEGIN;

-- Children of reports first (FK order); notifications / point_transactions
-- also reference users, but deleting all rows is fine here.
-- (If report_confirmations errors with "does not exist", just delete this line.)
DELETE FROM report_confirmations;
DELETE FROM report_images;
DELETE FROM report_status_history;
DELETE FROM report_comments;
DELETE FROM feedback;
DELETE FROM notifications;
DELETE FROM point_transactions;
DELETE FROM reports;

-- Seeded demo accounts only — admin and real signups are untouched.
DELETE FROM users
WHERE email LIKE 'citizen%@example.com'
   OR email IN (
        'staff.roads@scirs.gov', 'staff.buildings@scirs.gov',
        'staff.water@scirs.gov', 'staff.drainage@scirs.gov',
        'staff.sanitation@scirs.gov', 'staff.parks@scirs.gov',
        'staff.electricity@scirs.gov'
   );

-- Reset the cached score on anyone left (real users keep their own totals
-- once they earn points again; this just clears demo-era leftovers).
UPDATE users SET score_points = 0
WHERE score_points <> 0
  AND id NOT IN (SELECT DISTINCT user_id FROM point_transactions);

-- Verify before committing:
SELECT 'reports' t, count(*) FROM reports
UNION ALL SELECT 'report_images', count(*) FROM report_images
UNION ALL SELECT 'demo users left', count(*) FROM users WHERE email LIKE 'citizen%@example.com'
UNION ALL SELECT 'departments', count(*) FROM departments
UNION ALL SELECT 'categories', count(*) FROM categories
UNION ALL SELECT 'users total', count(*) FROM users;

COMMIT;
