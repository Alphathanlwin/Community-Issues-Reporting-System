-- One-off: point existing report_images rows at the bundled demo photos.
-- MockDataSeeder is idempotent and won't re-run, so the rows it created
-- earlier still hold picsum.photos / relative /uploads URLs. A fresh seed
-- (empty DB + SEED_MOCK_DATA=true) produces these URLs automatically; this
-- script fixes an already-seeded database.
--
-- Run against the Supabase Postgres (DataGrip, psql, or the Supabase SQL editor).
-- The base URL matches app.public-base-url (default http://localhost:8080) —
-- change it here if the backend is reachable elsewhere.

BEGIN;

WITH cat_photo(category, path) AS (
    VALUES
        ('Pothole / Damaged Road Surface',        '/seed-images/damaged_pavement.jpg'),
        ('Damaged Footpath or Pedestrian Bridge', '/seed-images/damaged_bridge.jpg'),
        ('Unsafe or Damaged Public Building',     '/seed-images/damaged_building.jpg'),
        ('Illegal or Unsafe Construction',        '/seed-images/damaged_public_building.jpeg'),
        ('Water Pipe Leak or Burst Main',         '/seed-images/pipeleakes.jpeg'),
        ('Water Supply Failure',                  '/seed-images/pipeleakes.jpeg'),
        ('Blocked Drain or Clogged Culvert',      '/seed-images/blocked_drain.jpg'),
        ('Street Flooding',                       '/seed-images/street_flooding.jpg'),
        ('Uncollected Garbage',                   '/seed-images/garbagecollection.jpeg'),
        ('Illegal Dumping',                       '/seed-images/illegal_dumping.jpg'),
        ('Damaged Park or Playground Equipment',  '/seed-images/illegal_dumping_in_parks.jpeg'),
        ('Fallen Tree or Overgrown Vegetation',   '/seed-images/street_trees_problem.jpg'),
        ('Street Light Outage',                   '/seed-images/streetlight_power_outages.jpg'),
        ('Power Outage or Exposed Cable',         '/seed-images/unsafe_structured_building.jpg')
)
UPDATE report_images ri
SET image_url = 'http://localhost:8080' || cp.path
FROM reports r
JOIN categories c   ON c.id = r.category_id
JOIN cat_photo cp   ON cp.category = c.name
WHERE ri.report_id = r.id;

-- Sanity check before committing:
SELECT c.name AS category, ri.image_type, ri.image_url, count(*)
FROM report_images ri
JOIN reports r     ON r.id = ri.report_id
JOIN categories c  ON c.id = r.category_id
GROUP BY 1, 2, 3
ORDER BY 1, 2;

COMMIT;
