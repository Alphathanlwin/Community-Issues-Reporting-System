-- ============================================================================
-- SCIRS / Kinn Htout — Non-Destructive Safe Demo Dataset
--
-- File: docs/seed-demo-safe.sql
-- Purpose: Populates the Admin Dashboard, Staff Dashboard, and Staff GIS Map
--          with realistic Yangon municipal demo data WITHOUT deleting,
--          updating, or modifying any existing user accounts or reports.
--
-- Safety Guarantees:
-- 1. 100% Additive: No TRUNCATE or DELETE on existing data.
-- 2. Transaction Protected: Runs inside BEGIN ... COMMIT.
-- 3. Strict Namespacing:
--    - Demo citizens: demo.citizen01@demo.scirs.gov to demo.citizen09@demo.scirs.gov
--    - Demo staff:    demo.staff.<dept>@demo.scirs.gov
--    - Demo reports:  RPT-DEMO-000001 to RPT-DEMO-000030
-- 4. Dynamic Foreign Keys: Looks up department_id, category_id, and role_id
--    dynamically by name — works with any existing serial ID sequences.
-- 5. Safe Password: All demo accounts use BCrypt-hashed password: Demo@12345
-- 6. Re-alignment: Automatically re-syncs all PostgreSQL sequences at completion.
-- ============================================================================

BEGIN;

-- ----------------------------------------------------------------------------
-- 1. SEED DEMO USERS (CITIZENS & DEPARTMENT STAFF)
-- ----------------------------------------------------------------------------
-- Password hash for 'Demo@12345' (Standard Spring Security BCrypt strength 10):
-- $2a$10$VIS1XlneTPfxev78lTAYWesGdYq3EQwgpm5KXZPrIFxPvUG6X2lHG

-- Demo Citizens (9 accounts: 7 Approved, 1 Pending for Admin queue, 1 Suspended)
INSERT INTO users (full_name, email, phone, password_hash, role_id, account_status, date_of_birth, nrc_number, score_points, is_active, created_at, updated_at)
VALUES
    ('Htet Myat Aung',  'demo.citizen01@demo.scirs.gov', '+95999900001', '$2a$10$VIS1XlneTPfxev78lTAYWesGdYq3EQwgpm5KXZPrIFxPvUG6X2lHG', (SELECT id FROM roles WHERE name = 'CITIZEN'), 'APPROVED',  '1998-04-12', '12/DEMO(N)000001', 125, true, NOW() - INTERVAL '45 days', NOW() - INTERVAL '1 day'),
    ('Ei Phyu Zin',     'demo.citizen02@demo.scirs.gov', '+95999900002', '$2a$10$VIS1XlneTPfxev78lTAYWesGdYq3EQwgpm5KXZPrIFxPvUG6X2lHG', (SELECT id FROM roles WHERE name = 'CITIZEN'), 'APPROVED',  '1995-08-20', '12/DEMO(N)000002',  95, true, NOW() - INTERVAL '30 days', NOW() - INTERVAL '2 days'),
    ('Thurein Soe',     'demo.citizen03@demo.scirs.gov', '+95999900003', '$2a$10$VIS1XlneTPfxev78lTAYWesGdYq3EQwgpm5KXZPrIFxPvUG6X2lHG', (SELECT id FROM roles WHERE name = 'CITIZEN'), 'APPROVED',  '2001-01-15', '12/DEMO(N)000003',  80, true, NOW() - INTERVAL '20 days', NOW() - INTERVAL '1 day'),
    ('Yadanar Kyaw',    'demo.citizen04@demo.scirs.gov', '+95999900004', '$2a$10$VIS1XlneTPfxev78lTAYWesGdYq3EQwgpm5KXZPrIFxPvUG6X2lHG', (SELECT id FROM roles WHERE name = 'CITIZEN'), 'APPROVED',  '1992-11-05', '12/DEMO(N)000004',  60, true, NOW() - INTERVAL '15 days', NOW() - INTERVAL '3 days'),
    ('Min Khant Thu',   'demo.citizen05@demo.scirs.gov', '+95999900005', '$2a$10$VIS1XlneTPfxev78lTAYWesGdYq3EQwgpm5KXZPrIFxPvUG6X2lHG', (SELECT id FROM roles WHERE name = 'CITIZEN'), 'APPROVED',  '1999-06-30', '12/DEMO(N)000005',  45, true, NOW() - INTERVAL '10 days', NOW() - INTERVAL '1 day'),
    ('Su Myat Htwe',    'demo.citizen06@demo.scirs.gov', '+95999900006', '$2a$10$VIS1XlneTPfxev78lTAYWesGdYq3EQwgpm5KXZPrIFxPvUG6X2lHG', (SELECT id FROM roles WHERE name = 'CITIZEN'), 'APPROVED',  '1997-03-25', '12/DEMO(N)000006',  35, true, NOW() - INTERVAL '7 days',  NOW() - INTERVAL '2 days'),
    ('Lin Myat Oo',     'demo.citizen07@demo.scirs.gov', '+95999900007', '$2a$10$VIS1XlneTPfxev78lTAYWesGdYq3EQwgpm5KXZPrIFxPvUG6X2lHG', (SELECT id FROM roles WHERE name = 'CITIZEN'), 'APPROVED',  '1994-09-18', '12/DEMO(N)000007',  20, true, NOW() - INTERVAL '4 days',  NOW() - INTERVAL '1 day'),
    ('Khin Sandar Win', 'demo.citizen08@demo.scirs.gov', '+95999900008', '$2a$10$VIS1XlneTPfxev78lTAYWesGdYq3EQwgpm5KXZPrIFxPvUG6X2lHG', (SELECT id FROM roles WHERE name = 'CITIZEN'), 'PENDING',   '2002-12-04', '12/DEMO(N)000008',   0, true, NOW() - INTERVAL '2 hours', NOW() - INTERVAL '2 hours'),
    ('Aung Thu Hein',   'demo.citizen09@demo.scirs.gov', '+95999900009', '$2a$10$VIS1XlneTPfxev78lTAYWesGdYq3EQwgpm5KXZPrIFxPvUG6X2lHG', (SELECT id FROM roles WHERE name = 'CITIZEN'), 'SUSPENDED', '1990-07-11', '12/DEMO(N)000009',   0, true, NOW() - INTERVAL '60 days', NOW() - INTERVAL '5 days')
ON CONFLICT (email) DO NOTHING;

-- Demo Staff (7 accounts: One for each municipal department, score_points explicitly set to 0)
INSERT INTO users (full_name, email, phone, password_hash, role_id, account_status, department_id, score_points, is_active, created_at, updated_at)
VALUES
    ('Daw Tin Tin Win (Roads Officer)',     'demo.staff.roads@demo.scirs.gov',     '+95999910001', '$2a$10$VIS1XlneTPfxev78lTAYWesGdYq3EQwgpm5KXZPrIFxPvUG6X2lHG', (SELECT id FROM roles WHERE name = 'STAFF'), 'APPROVED', (SELECT id FROM departments WHERE name = 'Roads & Bridges Department'), 0, true, NOW() - INTERVAL '90 days', NOW() - INTERVAL '1 day'),
    ('U Kyaw Min (Buildings Inspector)',   'demo.staff.buildings@demo.scirs.gov', '+95999910002', '$2a$10$VIS1XlneTPfxev78lTAYWesGdYq3EQwgpm5KXZPrIFxPvUG6X2lHG', (SELECT id FROM roles WHERE name = 'STAFF'), 'APPROVED', (SELECT id FROM departments WHERE name = 'Buildings Department'), 0, true, NOW() - INTERVAL '90 days', NOW() - INTERVAL '1 day'),
    ('Daw Mya Mya Aye (Water Engineer)',    'demo.staff.water@demo.scirs.gov',     '+95999910003', '$2a$10$VIS1XlneTPfxev78lTAYWesGdYq3EQwgpm5KXZPrIFxPvUG6X2lHG', (SELECT id FROM roles WHERE name = 'STAFF'), 'APPROVED', (SELECT id FROM departments WHERE name = 'Water & Sanitation Department'), 0, true, NOW() - INTERVAL '90 days', NOW() - INTERVAL '1 day'),
    ('U Ko Ko Aung (Drainage Supervisor)',  'demo.staff.drainage@demo.scirs.gov',  '+95999910004', '$2a$10$VIS1XlneTPfxev78lTAYWesGdYq3EQwgpm5KXZPrIFxPvUG6X2lHG', (SELECT id FROM roles WHERE name = 'STAFF'), 'APPROVED', (SELECT id FROM departments WHERE name = 'Drainage Management Department'), 0, true, NOW() - INTERVAL '90 days', NOW() - INTERVAL '1 day'),
    ('U Zaw Myo (Cleansing Coordinator)',   'demo.staff.cleansing@demo.scirs.gov', '+95999910005', '$2a$10$VIS1XlneTPfxev78lTAYWesGdYq3EQwgpm5KXZPrIFxPvUG6X2lHG', (SELECT id FROM roles WHERE name = 'STAFF'), 'APPROVED', (SELECT id FROM departments WHERE name = 'Urban Environmental Conservation & Cleansing Department'), 0, true, NOW() - INTERVAL '90 days', NOW() - INTERVAL '1 day'),
    ('Daw Khin Lay (Parks Officer)',        'demo.staff.parks@demo.scirs.gov',     '+95999910006', '$2a$10$VIS1XlneTPfxev78lTAYWesGdYq3EQwgpm5KXZPrIFxPvUG6X2lHG', (SELECT id FROM roles WHERE name = 'STAFF'), 'APPROVED', (SELECT id FROM departments WHERE name = 'Playgrounds, Parks & Gardens Department'), 0, true, NOW() - INTERVAL '90 days', NOW() - INTERVAL '1 day'),
    ('U Yan Naing (YESC Electrician)',      'demo.staff.yesc@demo.scirs.gov',      '+95999910007', '$2a$10$VIS1XlneTPfxev78lTAYWesGdYq3EQwgpm5KXZPrIFxPvUG6X2lHG', (SELECT id FROM roles WHERE name = 'STAFF'), 'APPROVED', (SELECT id FROM departments WHERE name = 'Yangon Electricity Supply Corporation (YESC)'), 0, true, NOW() - INTERVAL '90 days', NOW() - INTERVAL '1 day')
ON CONFLICT (email) DO NOTHING;


-- ----------------------------------------------------------------------------
-- 2. SEED DEMO REPORTS VIA TEMPORARY IN-SESSION DATA TABLE
-- ----------------------------------------------------------------------------
-- Creates a temporary table that automatically self-destructs on COMMIT.
CREATE TEMP TABLE temp_demo_specs (
    code VARCHAR,
    title VARCHAR,
    description TEXT,
    cat_name VARCHAR,
    reporter_email VARCHAR,
    staff_email VARCHAR,
    status VARCHAR,
    priority VARCHAR,
    lat NUMERIC,
    lng NUMERIC,
    address VARCHAR,
    created_interval INTERVAL,
    approved_interval INTERVAL,
    resolved_interval INTERVAL,
    closed_interval INTERVAL,
    rejection_reason VARCHAR,
    before_photo VARCHAR,
    after_photo VARCHAR,
    comment_body TEXT,
    feedback_rating INT,
    feedback_comment TEXT
) ON COMMIT DROP;

INSERT INTO temp_demo_specs VALUES
-- Group A: PENDING_APPROVAL (5 reports for Admin Approvals Queue)
('RPT-DEMO-000001', 'Sparking Transformer on University Avenue', 'Low-voltage pole transformer is sparking intermittently and burning smell detected.', 'Power Outage or Exposed Cable', 'demo.citizen01@demo.scirs.gov', 'demo.staff.yesc@demo.scirs.gov', 'PENDING_APPROVAL', 'URGENT', 16.8465, 96.1332, 'University Avenue Road, Kamayut', INTERVAL '3 hours', NULL, NULL, NULL, NULL, '/seed-images/unsafe_structured_building.jpg', NULL, NULL, NULL, NULL),
('RPT-DEMO-000002', 'Deep Pothole Causing Traffic Jam at Hledan', 'Deep crater near bus stop forcing buses into oncoming lane.', 'Pothole / Damaged Road Surface', 'demo.citizen02@demo.scirs.gov', 'demo.staff.roads@demo.scirs.gov', 'PENDING_APPROVAL', 'HIGH', 16.8272, 96.1298, 'Hledan Center Junction, Kamayut', INTERVAL '6 hours', NULL, NULL, NULL, NULL, '/seed-images/damaged_pavement.jpg', NULL, NULL, NULL, NULL),
('RPT-DEMO-000003', 'Overflowing Municipal Garbage Bins near Market', 'Bins have not been emptied for 4 days, waste spilling into vegetable stalls.', 'Uncollected Garbage', 'demo.citizen03@demo.scirs.gov', 'demo.staff.cleansing@demo.scirs.gov', 'PENDING_APPROVAL', 'NORMAL', 16.7725, 96.1598, 'Thein Gyi Market, Latha', INTERVAL '12 hours', NULL, NULL, NULL, NULL, '/seed-images/garbagecollection.jpeg', NULL, NULL, NULL, NULL),
('RPT-DEMO-000004', 'Broken Concrete Slab on Pedestrian Overpass', 'Missing footing slab exposes reinforcing bars on bridge steps.', 'Damaged Footpath or Pedestrian Bridge', 'demo.citizen04@demo.scirs.gov', 'demo.staff.roads@demo.scirs.gov', 'PENDING_APPROVAL', 'NORMAL', 16.8155, 96.1310, 'Pyay Road Overpass, Kamayut', INTERVAL '18 hours', NULL, NULL, NULL, NULL, '/seed-images/damaged_bridge.jpg', NULL, NULL, NULL, NULL),
('RPT-DEMO-000005', 'Fallen Acacia Branch Obstructing Side Alley', 'Storm knocked down large tree limb blocking emergency ambulance access.', 'Fallen Tree or Overgrown Vegetation', 'demo.citizen05@demo.scirs.gov', 'demo.staff.parks@demo.scirs.gov', 'PENDING_APPROVAL', 'LOW', 16.8080, 96.1510, 'Inya Road Ward 2, Bahan', INTERVAL '22 hours', NULL, NULL, NULL, NULL, '/seed-images/street_trees_problem.jpg', NULL, NULL, NULL, NULL),

-- Group B: REJECTED (3 reports)
('RPT-DEMO-000006', 'Private Generator Noise in Residential Compound', 'Neighbour generator operates loudly after midnight.', 'Unsafe or Damaged Public Building', 'demo.citizen06@demo.scirs.gov', 'demo.staff.buildings@demo.scirs.gov', 'REJECTED', 'LOW', 16.8020, 96.1435, 'Golden Valley Ward 1, Bahan', INTERVAL '2 days', NULL, NULL, NULL, 'Private residential noise dispute — please contact Ward Administration Office.', '/seed-images/damaged_building.jpg', NULL, NULL, NULL, NULL),
('RPT-DEMO-000007', 'Unauthorised Commercial Rooftop Signboard', 'Advertising board erected on private apartment without visible municipal seal.', 'Illegal or Unsafe Construction', 'demo.citizen07@demo.scirs.gov', 'demo.staff.buildings@demo.scirs.gov', 'REJECTED', 'LOW', 16.8390, 96.1365, 'Sanchaung Street, Sanchaung', INTERVAL '3 days', NULL, NULL, NULL, 'Private advertising signage under YCDC licensing board jurisdiction.', '/seed-images/damaged_public_building.jpeg', NULL, NULL, NULL, NULL),
('RPT-DEMO-000008', 'Duplicate Pothole Report on Strand Road', 'Identical pothole already reported earlier today.', 'Pothole / Damaged Road Surface', 'demo.citizen01@demo.scirs.gov', 'demo.staff.roads@demo.scirs.gov', 'REJECTED', 'NORMAL', 16.7690, 96.1680, 'Strand Road near Jetty, Botahtaung', INTERVAL '4 days', NULL, NULL, NULL, 'Duplicate report — consolidated into active dispatch ticket.', '/seed-images/damaged_pavement.jpg', NULL, NULL, NULL, NULL),

-- Group C: ASSIGNED (6 reports in queue awaiting field pickup)
('RPT-DEMO-000009', 'High-Pressure Water Main Burst Flooding Street', 'Underground main cracked, shooting 2m fountain of clean water onto roadway.', 'Water Pipe Leak or Burst Main', 'demo.citizen02@demo.scirs.gov', 'demo.staff.water@demo.scirs.gov', 'ASSIGNED', 'URGENT', 16.7885, 96.1730, 'Strand Road Corner, Botahtaung', INTERVAL '14 hours', INTERVAL '12 hours', NULL, NULL, NULL, '/seed-images/pipeleakes.jpeg', NULL, NULL, NULL, NULL),
('RPT-DEMO-000010', 'Clogged Storm Drain Overflowing into Sidewalk', 'Plastic debris has completely sealed culvert opening, causing knee-high standing water.', 'Blocked Drain or Clogged Culvert', 'demo.citizen03@demo.scirs.gov', 'demo.staff.drainage@demo.scirs.gov', 'ASSIGNED', 'HIGH', 16.7760, 96.1620, '42nd Street Lower Block, Botahtaung', INTERVAL '20 hours', INTERVAL '18 hours', NULL, NULL, NULL, '/seed-images/blocked_drain.jpg', NULL, NULL, NULL, NULL),
('RPT-DEMO-000011', 'Illegal Construction Waste Dumping on Vacant Lot', 'Truck discharged broken masonry and rebar onto public pathway.', 'Illegal Dumping', 'demo.citizen04@demo.scirs.gov', 'demo.staff.cleansing@demo.scirs.gov', 'ASSIGNED', 'NORMAL', 16.8520, 96.1920, 'Thanthumar Road, Thingangyun', INTERVAL '28 hours', INTERVAL '24 hours', NULL, NULL, NULL, '/seed-images/illegal_dumping.jpg', NULL, NULL, NULL, NULL),
('RPT-DEMO-000012', 'Broken Children Playground Slide at People Park', 'Metal slide joint fractured with jagged edges exposed.', 'Damaged Park or Playground Equipment', 'demo.citizen05@demo.scirs.gov', 'demo.staff.parks@demo.scirs.gov', 'ASSIGNED', 'LOW', 16.7970, 96.1395, 'People''s Park Playground, Dagon', INTERVAL '36 hours', INTERVAL '30 hours', NULL, NULL, NULL, '/seed-images/illegal_dumping_in_parks.jpeg', NULL, NULL, NULL, NULL),
('RPT-DEMO-000013', 'Streetlight Array Inoperative for 300m', '6 consecutive lamps dark along main highway corridor.', 'Street Light Outage', 'demo.citizen06@demo.scirs.gov', 'demo.staff.yesc@demo.scirs.gov', 'ASSIGNED', 'HIGH', 16.8320, 96.1420, 'Yankin Road, Yankin', INTERVAL '42 hours', INTERVAL '36 hours', NULL, NULL, NULL, '/seed-images/streetlight_power_outages.jpg', NULL, NULL, NULL, NULL),
('RPT-DEMO-000014', 'Crumbling Public Concrete Canopy at Clinic Entry', 'Spalling concrete falling near patient waiting queue.', 'Unsafe or Damaged Public Building', 'demo.citizen07@demo.scirs.gov', 'demo.staff.buildings@demo.scirs.gov', 'ASSIGNED', 'HIGH', 16.8720, 96.1540, 'Insein Township Public Health Clinic', INTERVAL '46 hours', INTERVAL '40 hours', NULL, NULL, NULL, '/seed-images/damaged_public_building.jpeg', NULL, NULL, NULL, NULL),

-- Group D: IN_PROGRESS (6 reports — Staggered for SLA Breach, At-Risk, and On-Track!)
-- 1. URGENT BREACHED (35h elapsed vs 24h target = 11h overdue!)
('RPT-DEMO-000015', 'Substation Cable Exposed to Foot Traffic', 'Heavy underground feeder cable exposed after trench collapse.', 'Power Outage or Exposed Cable', 'demo.citizen01@demo.scirs.gov', 'demo.staff.yesc@demo.scirs.gov', 'IN_PROGRESS', 'URGENT', 16.8395, 96.1215, 'Hlaing Campus Road, Hlaing', INTERVAL '35 hours', INTERVAL '33 hours', NULL, NULL, NULL, '/seed-images/unsafe_structured_building.jpg', NULL, 'Crew on site. Waiting for heavy backhoe to trench cable channel securely.', NULL, NULL),

-- 2. HIGH BREACHED (60h elapsed vs 48h target = 12h overdue!)
('RPT-DEMO-000016', 'Road Subsidence Forming Sinkhole on Inya Road', 'Road depression widening rapidly after torrential rains.', 'Pothole / Damaged Road Surface', 'demo.citizen02@demo.scirs.gov', 'demo.staff.roads@demo.scirs.gov', 'IN_PROGRESS', 'HIGH', 16.8305, 96.1555, 'Inya Road near Lake, Bahan', INTERVAL '60 hours', INTERVAL '56 hours', NULL, NULL, NULL, '/seed-images/damaged_pavement.jpg', NULL, 'Sub-base gravel compaction in progress. Asphalt delivery scheduled.', NULL, NULL),

-- 3. URGENT AT-RISK (21h elapsed vs 24h target = 3h remaining!)
('RPT-DEMO-000017', 'Drinking Water Supply Disruption to Ward 4', 'No piped water reaching approximately 80 residential compounds.', 'Water Supply Failure', 'demo.citizen03@demo.scirs.gov', 'demo.staff.water@demo.scirs.gov', 'IN_PROGRESS', 'URGENT', 16.8490, 96.1745, 'Thingangyun Ward 4 Main Road', INTERVAL '21 hours', INTERVAL '19 hours', NULL, NULL, NULL, '/seed-images/pipeleakes.jpeg', NULL, 'Pressure booster valve replacement underway. Testing pressure manifold.', NULL, NULL),

-- 4. HIGH AT-RISK (40h elapsed vs 48h target = 8h remaining!)
('RPT-DEMO-000018', 'Street Flooding Paralyzing Baho Road Transit', 'Flood water 0.5m deep stalling small cars and motorbikes.', 'Street Flooding', 'demo.citizen04@demo.scirs.gov', 'demo.staff.drainage@demo.scirs.gov', 'IN_PROGRESS', 'HIGH', 16.8110, 96.1185, 'Baho Road, Sanchaung', INTERVAL '40 hours', INTERVAL '38 hours', NULL, NULL, NULL, '/seed-images/street_flooding.jpg', NULL, 'Mobile dewatering pump deployed on site. Clearing outflow channel.', NULL, NULL),

-- 5. NORMAL ON-TRACK (14h elapsed vs 72h target)
('RPT-DEMO-000019', 'Waste Accumulation Behind Central Railway Station', 'Commercial packaging dumped along rail perimeter fence.', 'Illegal Dumping', 'demo.citizen05@demo.scirs.gov', 'demo.staff.cleansing@demo.scirs.gov', 'IN_PROGRESS', 'NORMAL', 16.7815, 96.1625, 'Railway Station Perimeter, Mingalar Taung Nyunt', INTERVAL '14 hours', INTERVAL '12 hours', NULL, NULL, NULL, '/seed-images/illegal_dumping.jpg', NULL, 'Waste collection compactor truck en route with 6 sanitary workers.', NULL, NULL),

-- 6. NORMAL ON-TRACK (26h elapsed vs 72h target)
('RPT-DEMO-000020', 'Damaged Stone Footpath Leading to Monastery', 'Paving flagstones broken and slippery for elderly pedestrians.', 'Damaged Footpath or Pedestrian Bridge', 'demo.citizen06@demo.scirs.gov', 'demo.staff.roads@demo.scirs.gov', 'IN_PROGRESS', 'NORMAL', 16.8560, 96.1285, 'Monastery Approach Road, Hlaing', INTERVAL '26 hours', INTERVAL '24 hours', NULL, NULL, NULL, '/seed-images/damaged_bridge.jpg', NULL, 'New paving stone slabs unloaded on site. Re-laying mortar bedding.', NULL, NULL),

-- Group E: RESOLVED (6 reports with resolution photos and ratings)
('RPT-DEMO-000021', 'Critical Main Highway Pothole Repaired', 'Severe pothole at roundabout that caused motorcycle hazard.', 'Pothole / Damaged Road Surface', 'demo.citizen01@demo.scirs.gov', 'demo.staff.roads@demo.scirs.gov', 'RESOLVED', 'HIGH', 16.8055, 96.1505, 'Kabar Aye Pagoda Road, Bahan', INTERVAL '4 days', INTERVAL '3 days', INTERVAL '1 day', NULL, NULL, '/seed-images/damaged_pavement.jpg', '/seed-images/resolved_road.jpg', 'Hot asphalt patch applied, rolled, and sealed. Traffic resumed smoothly.', 5, 'Excellent quick repair! Road is completely smooth again now.'),
('RPT-DEMO-000022', 'Dark Dangerous Bus Stop Relit with LED Luminaire', 'Streetlight out near high school bus shelter for over two weeks.', 'Street Light Outage', 'demo.citizen02@demo.scirs.gov', 'demo.staff.yesc@demo.scirs.gov', 'RESOLVED', 'HIGH', 16.8335, 96.1415, 'Yankin Road Bus Stop, Yankin', INTERVAL '5 days', INTERVAL '4 days', INTERVAL '18 hours', NULL, NULL, '/seed-images/streetlight_power_outages.jpg', '/seed-images/resolved_streetlight.jpg', 'Replaced ballast unit and upgraded to 120W LED fixture. Tested photodiode.', 5, 'So much safer for evening commuters and students. Thank you!'),
('RPT-DEMO-000023', 'Major Water Main Leak Repaired and Resurfaced', 'Pressurised water pipe cracked underneath pedestrian pathway.', 'Water Pipe Leak or Burst Main', 'demo.citizen03@demo.scirs.gov', 'demo.staff.water@demo.scirs.gov', 'RESOLVED', 'URGENT', 16.8485, 96.1742, 'Thingangyun Ward 3 Connector', INTERVAL '6 days', INTERVAL '5 days', INTERVAL '2 days', NULL, NULL, '/seed-images/pipeleakes.jpeg', '/seed-images/resolved_water_supply.jpg', 'Fitted ductile iron repair sleeve and pressure tested up to 6 bar.', 4, 'Piped water pressure restored to normal within 2 days.'),
('RPT-DEMO-000024', 'Overflowing Garbage Cleared & Permanent Dumpster Placed', 'Debris pile-up at corner of residential lane fully evacuated.', 'Uncollected Garbage', 'demo.citizen04@demo.scirs.gov', 'demo.staff.cleansing@demo.scirs.gov', 'RESOLVED', 'NORMAL', 16.8610, 96.2005, 'North Okkalapa Ward 3 Lane', INTERVAL '7 days', INTERVAL '6 days', INTERVAL '2 days', NULL, NULL, '/seed-images/garbagecollection.jpeg', '/seed-images/resolved_garbage.jpg', 'Waste removed with hydraulic compactor. Area disinfected with lime powder.', 5, 'Cleanest this corner has been in months! Appreciate the hard work.'),
('RPT-DEMO-000025', 'Storm Drain Cleared Before Monsoon Deluge', 'Culvert under major crossroad desilted and cleared of tree roots.', 'Blocked Drain or Clogged Culvert', 'demo.citizen05@demo.scirs.gov', 'demo.staff.drainage@demo.scirs.gov', 'RESOLVED', 'HIGH', 16.8705, 96.1505, 'Insein Road Cross-Culvert, Insein', INTERVAL '8 days', INTERVAL '7 days', INTERVAL '3 days', NULL, NULL, '/seed-images/blocked_drain.jpg', '/seed-images/resolved_drain.jpg', 'Excavated 4.5 cubic meters of silt and sediment. Free-flow verified.', 4, 'Heavy rains yesterday did not flood the road. Drainage works well.'),
('RPT-DEMO-000026', 'Playground Equipment Refurbished & Swings Replaced', 'Children park swings repaired with new high-tensile coated chains.', 'Damaged Park or Playground Equipment', 'demo.citizen06@demo.scirs.gov', 'demo.staff.parks@demo.scirs.gov', 'RESOLVED', 'NORMAL', 16.8185, 96.1605, 'Aung San Stadium Park Playground', INTERVAL '10 days', INTERVAL '9 days', INTERVAL '4 days', NULL, NULL, '/seed-images/illegal_dumping_in_parks.jpeg', '/seed-images/resolved_playground.jpg', 'Chains replaced, wooden seat planks varnished, and safety bolts torqued.', 5, 'Neighborhood children are playing safely again. Great response.'),

-- Group F: CLOSED (4 reports — verified terminal)
('RPT-DEMO-000027', 'Sparking Overhead Electric Cable Secured', 'Low-hanging 230V service drop tensioned and re-insulated.', 'Power Outage or Exposed Cable', 'demo.citizen01@demo.scirs.gov', 'demo.staff.yesc@demo.scirs.gov', 'CLOSED', 'URGENT', 16.7975, 96.1555, 'Thin Gan Gyun Street, Tarmwe', INTERVAL '15 days', INTERVAL '14 days', INTERVAL '12 days', INTERVAL '11 days', NULL, '/seed-images/unsafe_structured_building.jpg', '/seed-images/resolved_power.jpg', 'New weatherhead insulator installed. Clearance from rooftop now 3.5m.', 5, 'Fast emergency response. Very satisfied.'),
('RPT-DEMO-000028', 'Asphalt Road Surface Resurfacing Near Roundabout', 'Widespread alligator cracking and rutting milled and repaved.', 'Pothole / Damaged Road Surface', 'demo.citizen02@demo.scirs.gov', 'demo.staff.roads@demo.scirs.gov', 'CLOSED', 'HIGH', 16.7985, 96.1655, 'Tarmwe Roundabout, Tarmwe', INTERVAL '20 days', INTERVAL '19 days', INTERVAL '15 days', INTERVAL '14 days', NULL, '/seed-images/damaged_pavement.jpg', '/seed-images/resolved_road.jpg', 'Full-depth asphalt patching completed over 45 square meters.', 4, 'Smooth road surface restored.'),
('RPT-DEMO-000029', 'Township Registry Hall Ceiling Plaster Restored', 'Water-damaged ceiling panels replaced with moisture-resistant drywall.', 'Unsafe or Damaged Public Building', 'demo.citizen03@demo.scirs.gov', 'demo.staff.buildings@demo.scirs.gov', 'CLOSED', 'NORMAL', 16.7955, 96.1455, 'Kyauktada Township Hall Public Restroom', INTERVAL '25 days', INTERVAL '24 days', INTERVAL '18 days', INTERVAL '17 days', NULL, '/seed-images/damaged_building.jpg', '/seed-images/resolved_public_building.jpg', 'Damaged rafters replaced, gypsum ceiling rehung and repainted.', 5, 'Clean and safe environment restored for visitors.'),
('RPT-DEMO-000030', 'Drainage Cleanout Along Merchant Street', 'Storm runoff blockage fully pumped and debris removed before heavy rain.', 'Blocked Drain or Clogged Culvert', 'demo.citizen04@demo.scirs.gov', 'demo.staff.drainage@demo.scirs.gov', 'CLOSED', 'HIGH', 16.7835, 96.1505, 'Merchant Street, Pabedan', INTERVAL '30 days', INTERVAL '29 days', INTERVAL '22 days', INTERVAL '21 days', NULL, '/seed-images/blocked_drain.jpg', '/seed-images/resolved_drain.jpg', 'Full line flusher used to hydro-jet main conduit.', 4, 'Drainage working without any backup.');


-- ----------------------------------------------------------------------------
-- 3. INSERT REPORTS & ALL CHILD RELATIONAL RECORDS
-- ----------------------------------------------------------------------------
DO $$
DECLARE
    r RECORD;
    v_report_id BIGINT;
    v_cat_id BIGINT;
    v_dept_id BIGINT;
    v_reporter_id BIGINT;
    v_staff_id BIGINT;
    v_admin_id BIGINT;
    v_created_at TIMESTAMP;
    v_approved_at TIMESTAMP;
    v_resolved_at TIMESTAMP;
    v_closed_at TIMESTAMP;
BEGIN
    SELECT u.id INTO v_admin_id FROM users u JOIN roles ro ON ro.id = u.role_id WHERE ro.name = 'ADMIN' LIMIT 1;
    IF v_admin_id IS NULL THEN
        SELECT u2.id INTO v_admin_id FROM users u2 LIMIT 1;
    END IF;

    FOR r IN SELECT * FROM temp_demo_specs LOOP
        -- Skip if report code already exists (idempotency)
        IF NOT EXISTS (SELECT 1 FROM reports WHERE report_code = r.code) THEN
            SELECT id, department_id INTO v_cat_id, v_dept_id FROM categories WHERE name = r.cat_name LIMIT 1;
            SELECT id INTO v_reporter_id FROM users WHERE email = r.reporter_email LIMIT 1;
            SELECT id INTO v_staff_id FROM users WHERE email = r.staff_email LIMIT 1;

            v_created_at := NOW() - r.created_interval;
            v_approved_at := CASE WHEN r.approved_interval IS NOT NULL THEN NOW() - r.approved_interval ELSE NULL END;
            v_resolved_at := CASE WHEN r.resolved_interval IS NOT NULL THEN NOW() - r.resolved_interval ELSE NULL END;
            v_closed_at := CASE WHEN r.closed_interval IS NOT NULL THEN NOW() - r.closed_interval ELSE NULL END;

            -- 1. Insert Report
            INSERT INTO reports (
                report_code, title, description, category_id, department_id,
                reporter_id, assigned_staff_id, status, priority, priority_score,
                latitude, longitude, address_text, duplicate_checked, is_anonymous,
                rejection_reason, created_at, updated_at, approved_at, approved_by,
                resolved_at, closed_at
            ) VALUES (
                r.code, r.title, r.description, v_cat_id,
                CASE WHEN r.status IN ('PENDING_APPROVAL', 'REJECTED') THEN NULL ELSE v_dept_id END,
                v_reporter_id,
                CASE WHEN r.status IN ('IN_PROGRESS', 'RESOLVED', 'CLOSED') THEN v_staff_id ELSE NULL END,
                r.status, r.priority,
                CASE r.priority WHEN 'URGENT' THEN 85 WHEN 'HIGH' THEN 65 WHEN 'NORMAL' THEN 40 ELSE 20 END,
                r.lat, r.lng, r.address, false, false,
                r.rejection_reason, v_created_at, NOW(),
                v_approved_at, CASE WHEN v_approved_at IS NOT NULL THEN v_admin_id ELSE NULL END,
                v_resolved_at, v_closed_at
            ) RETURNING id INTO v_report_id;

            -- 2. Initial Report Photo
            IF r.before_photo IS NOT NULL THEN
                INSERT INTO report_images (report_id, image_url, image_type, uploaded_by, uploaded_at)
                VALUES (v_report_id, r.before_photo, 'REPORT_PHOTO', v_reporter_id, v_created_at);
            END IF;

            -- 3. Initial Status History
            INSERT INTO report_status_history (report_id, old_status, new_status, changed_by, remarks, changed_at)
            VALUES (v_report_id, NULL, 'PENDING_APPROVAL', v_reporter_id, 'Citizen submitted incident report', v_created_at);

            -- 4. Branching History & Notifications
            IF r.status = 'REJECTED' THEN
                INSERT INTO report_status_history (report_id, old_status, new_status, changed_by, remarks, changed_at)
                VALUES (v_report_id, 'PENDING_APPROVAL', 'REJECTED', v_admin_id, r.rejection_reason, v_created_at + INTERVAL '2 hours');

                INSERT INTO notifications (recipient_id, report_id, type, title, message, is_read, created_at)
                VALUES (v_reporter_id, v_report_id, 'REPORT_REJECTED', 'Report Rejected', 'Your report was rejected: ' || r.rejection_reason, false, v_created_at + INTERVAL '2 hours');

                INSERT INTO point_transactions (user_id, report_id, points, reason, created_at)
                VALUES (v_reporter_id, v_report_id, -5, 'REPORT_REJECTED', v_created_at + INTERVAL '2 hours');

            ELSIF r.status IN ('ASSIGNED', 'IN_PROGRESS', 'RESOLVED', 'CLOSED') THEN
                INSERT INTO report_status_history (report_id, old_status, new_status, changed_by, remarks, changed_at)
                VALUES (v_report_id, 'PENDING_APPROVAL', 'ASSIGNED', v_admin_id, 'Approved and dispatched to department queue', v_approved_at);

                INSERT INTO notifications (recipient_id, report_id, type, title, message, is_read, created_at)
                VALUES (v_reporter_id, v_report_id, 'REPORT_APPROVED', 'Report Approved', 'Your report "' || r.title || '" has been approved and routed.', true, v_approved_at);

                IF v_staff_id IS NOT NULL THEN
                    INSERT INTO notifications (recipient_id, report_id, type, title, message, is_read, created_at)
                    VALUES (v_staff_id, v_report_id, 'NEW_REPORT', 'New Work Order Assigned', 'Report ' || r.code || ' was assigned to your department queue.', false, v_approved_at);
                END IF;

                INSERT INTO point_transactions (user_id, report_id, points, reason, created_at)
                VALUES (v_reporter_id, v_report_id, 10, 'REPORT_APPROVED', v_approved_at);

                IF r.status IN ('IN_PROGRESS', 'RESOLVED', 'CLOSED') THEN
                    INSERT INTO report_status_history (report_id, old_status, new_status, changed_by, remarks, changed_at)
                    VALUES (v_report_id, 'ASSIGNED', 'IN_PROGRESS', v_staff_id, 'Field crew deployed on site for inspection/repair', v_approved_at + INTERVAL '3 hours');

                    IF r.comment_body IS NOT NULL THEN
                        INSERT INTO report_comments (report_id, author_id, body, created_at)
                        VALUES (v_report_id, v_staff_id, r.comment_body, v_approved_at + INTERVAL '3 hours');
                    END IF;
                END IF;

                IF r.status IN ('RESOLVED', 'CLOSED') THEN
                    IF r.after_photo IS NOT NULL THEN
                        INSERT INTO report_images (report_id, image_url, image_type, uploaded_by, uploaded_at)
                        VALUES (v_report_id, r.after_photo, 'RESOLUTION_PHOTO', v_staff_id, v_resolved_at);
                    END IF;

                    INSERT INTO report_status_history (report_id, old_status, new_status, changed_by, remarks, changed_at)
                    VALUES (v_report_id, 'IN_PROGRESS', 'RESOLVED', v_staff_id, 'Field remediation complete. Resolution photo attached.', v_resolved_at);

                    INSERT INTO notifications (recipient_id, report_id, type, title, message, is_read, created_at)
                    VALUES (v_reporter_id, v_report_id, 'REPORT_COMPLETED', 'Issue Resolved', 'Your report "' || r.title || '" has been marked as resolved by municipal staff.', false, v_resolved_at);

                    INSERT INTO point_transactions (user_id, report_id, points, reason, created_at)
                    VALUES (v_reporter_id, v_report_id, 25, 'REPORT_RESOLVED', v_resolved_at);

                    IF r.feedback_rating IS NOT NULL THEN
                        INSERT INTO feedback (report_id, citizen_id, rating, comment, created_at)
                        VALUES (v_report_id, v_reporter_id, r.feedback_rating, r.feedback_comment, v_resolved_at + INTERVAL '4 hours');

                        INSERT INTO point_transactions (user_id, report_id, points, reason, created_at)
                        VALUES (v_reporter_id, v_report_id, 5, 'FEEDBACK_GIVEN', v_resolved_at + INTERVAL '4 hours');
                    END IF;
                END IF;

                IF r.status = 'CLOSED' THEN
                    INSERT INTO report_status_history (report_id, old_status, new_status, changed_by, remarks, changed_at)
                    VALUES (v_report_id, 'RESOLVED', 'CLOSED', v_admin_id, 'Resolution verified and closed by municipal administration.', v_closed_at);
                END IF;
            END IF;
        END IF;
    END LOOP;
END $$;


-- ----------------------------------------------------------------------------
-- 4. RE-SYNC CITIZEN CACHED SCORE TOTALS
-- ----------------------------------------------------------------------------
UPDATE users u
SET score_points = COALESCE((
    SELECT SUM(points) FROM point_transactions pt WHERE pt.user_id = u.id
), 0)
WHERE u.email LIKE 'demo.citizen%@demo.scirs.gov';


-- ----------------------------------------------------------------------------
-- 5. RE-ALIGN ALL POSTGRESQL SERIAL PRIMARY KEY SEQUENCES
-- ----------------------------------------------------------------------------
SELECT setval(pg_get_serial_sequence('users', 'id'), COALESCE(MAX(id), 1)) FROM users;
SELECT setval(pg_get_serial_sequence('reports', 'id'), COALESCE(MAX(id), 1)) FROM reports;
SELECT setval(pg_get_serial_sequence('report_images', 'id'), COALESCE(MAX(id), 1)) FROM report_images;
SELECT setval(pg_get_serial_sequence('report_status_history', 'id'), COALESCE(MAX(id), 1)) FROM report_status_history;
SELECT setval(pg_get_serial_sequence('report_comments', 'id'), COALESCE(MAX(id), 1)) FROM report_comments;
SELECT setval(pg_get_serial_sequence('feedback', 'id'), COALESCE(MAX(id), 1)) FROM feedback;
SELECT setval(pg_get_serial_sequence('notifications', 'id'), COALESCE(MAX(id), 1)) FROM notifications;
SELECT setval(pg_get_serial_sequence('point_transactions', 'id'), COALESCE(MAX(id), 1)) FROM point_transactions;

COMMIT;

-- ----------------------------------------------------------------------------
-- 6. VERIFICATION SUMMARY
-- ----------------------------------------------------------------------------
SELECT 'Demo Citizens Inserted' AS metric, COUNT(*) AS count FROM users WHERE email LIKE 'demo.citizen%@demo.scirs.gov'
UNION ALL
SELECT 'Demo Staff Inserted', COUNT(*) FROM users WHERE email LIKE 'demo.staff.%@demo.scirs.gov'
UNION ALL
SELECT 'Demo Reports Inserted', COUNT(*) FROM reports WHERE report_code LIKE 'RPT-DEMO-%'
UNION ALL
SELECT 'Demo Report Images', COUNT(*) FROM report_images ri JOIN reports r ON r.id = ri.report_id WHERE r.report_code LIKE 'RPT-DEMO-%'
UNION ALL
SELECT 'Demo Status Histories', COUNT(*) FROM report_status_history rsh JOIN reports r ON r.id = rsh.report_id WHERE r.report_code LIKE 'RPT-DEMO-%'
UNION ALL
SELECT 'Demo Feedback Reviews', COUNT(*) FROM feedback fb JOIN reports r ON r.id = fb.report_id WHERE r.report_code LIKE 'RPT-DEMO-%'
UNION ALL
SELECT 'Demo Notifications', COUNT(*) FROM notifications n JOIN reports r ON r.id = n.report_id WHERE r.report_code LIKE 'RPT-DEMO-%';
