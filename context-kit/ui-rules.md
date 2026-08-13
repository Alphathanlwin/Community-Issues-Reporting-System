# 7. SCIRS — UI Rules

Design and behaviour rules for the React 19 + TypeScript + Tailwind + shadcn/ui frontend. SCIRS has **two distinct shells** in one codebase — do not mix them.

| Shell | Users | Primary device | Navigation |
|-------|-------|----------------|------------|
| **Citizen shell** | Citizens | Mobile-first (360–430 px), works on desktop | Bottom tab bar |
| **Console shell** | Admin, Government Staff | Desktop-first (1280 px+), usable on tablet | Top navbar + left sidebar |

`ProtectedRoute` picks the shell from the JWT role after login. A citizen never sees the sidebar; an admin/staff never sees the bottom tab bar.

---

## General UI Principles

| Principle | Detail |
|-----------|--------|
| Framework | React 19 + TypeScript |
| Styling | Tailwind CSS + shadcn/ui primitives |
| Style direction | Civic and plain — high legibility, generous tap targets, no decorative flourish that competes with map data |
| Consistency | Every list page follows the same pattern; every form follows the same pattern |
| Component reuse | Shared `components/common/` for DataTable, StatusBadge, EmptyState, ConfirmDialog, PageHeader, StatCard |
| Quality floor | Responsive to 360 px, visible keyboard focus, `prefers-reduced-motion` respected, colour is never the only status signal |

---

## Design Tokens

Defined once in `tailwind.config.ts` and used everywhere. Never hardcode a hex in a component.

| Token | Use |
|-------|-----|
| `brand` | Primary actions, active nav item, links |
| `surface` / `surface-muted` | Page background, card background |
| `border` | Hairlines, table row dividers |
| `text` / `text-muted` | Body copy, secondary labels |
| `status-pending` | Amber — awaiting approval |
| `status-assigned` | Blue — routed to a department |
| `status-progress` | Indigo — work underway |
| `status-resolved` | Green — fixed |
| `status-closed` | Slate — finished and archived |
| `status-rejected` | Red — denied |
| `priority-urgent` | Red accent for the urgent flag |

Typography: one display face for page titles and stat numbers, one body face for everything else, `tabular-nums` for all counts, scores, and table figures.

---

## Status Badges

`StatusBadge` is the only component allowed to render a report status. It renders the label **and** an icon so status is never colour-only.

| Status | Label | Token |
|--------|-------|-------|
| `PENDING_APPROVAL` | Pending approval | `status-pending` |
| `REJECTED` | Denied | `status-rejected` |
| `ASSIGNED` | Assigned | `status-assigned` |
| `IN_PROGRESS` | In progress | `status-progress` |
| `RESOLVED` | Resolved | `status-resolved` |
| `CLOSED` | Closed | `status-closed` |

Account statuses (`PENDING`, `APPROVED`, `REJECTED`, `SUSPENDED`) and priorities (`LOW`, `NORMAL`, `HIGH`, `URGENT`) use the same component with their own token map.

---

## Citizen Shell (Mobile-First)

### Layout

```
┌──────────────────────────────┐
│ Header: title + notification │
├──────────────────────────────┤
│                              │
│        Page content          │
│        (scrollable)          │
│                              │
├──────────────────────────────┤
│ 🏠Home 🗺Map ➕Report 🏆Board 👤Me │
└──────────────────────────────┘
```

- Bottom tab bar is fixed, five items, active item uses `brand`
- The centre **Report** action is visually raised — it is the primary job of the app
- Minimum tap target 44×44 px; primary buttons are full-width on mobile
- Content max-width `max-w-md mx-auto` so the layout stays sane on desktop

### Pages

| Route | Page | Content |
|-------|------|---------|
| `/home` | Home | Greeting, own score card, own recent reports (3–5 rows with status badge), primary "Report an issue" button |
| `/map` | Map | Full-height Leaflet map, filter chips (All, Electricity, Roads, Water, Sanitation, Parks, Buildings), tap a pin → bottom sheet with summary |
| `/report` | New report | The submission flow (below) |
| `/report/:id` | Report detail | Photos, status timeline, department, resolution photo, feedback form when resolved |
| `/leaderboard` | Leaderboard | Ranked list; the current user's row is pinned and highlighted |
| `/score` | My score | Point total + point history list (reason and date per row) |
| `/profile` | Profile | Account details, edit profile, logout |

### Report Submission Flow

```
┌──────────────────────────────┐
│  Report an issue             │
├──────────────────────────────┤
│  Location                    │
│  [ mini map + "Use my        │
│    location" / drag pin ]    │
│                              │
│  Category *   [ select ▾ ]   │
│  What's wrong? *             │
│   ( ) Common problem chips   │
│   ( ) Or describe it         │
│  [ text area                ]│
│                              │
│  Photo        [ 📷 Add photo ]│
│  [ thumbnail preview  ✕ ]    │
│                              │
│  [      Submit report      ] │
└──────────────────────────────┘
```

Rules:
- GPS is captured automatically on open; if permission is denied, show the map with a draggable pin and a plain message explaining what to do
- The problem statement offers **preset chips per category** (features file: "options / type in") and a free-text area — picking a chip fills the text area, which stays editable
- One photo minimum is encouraged but not required; show an image preview with a remove control
- Disable Submit and show "Submitting…" during upload; never allow a double submit
- On success: toast "Report submitted — it will appear on the map once approved", then navigate to the report detail
- If the citizen's account is still `PENDING`, the Report tab shows an explanatory state instead of the form

---

## Console Shell (Admin & Staff, Desktop)

### Layout

```
┌────────────────────────────────────────────────┐
│ Navbar: SCIRS · search · 🔔 badge · user menu  │
├───────────┬────────────────────────────────────┤
│           │                                    │
│  Sidebar  │           Main content             │
│  (nav)    │                                    │
│           │                                    │
└───────────┴────────────────────────────────────┘
```

- Sidebar width fixed (≈250 px), collapsible on tablet, highlights the active module
- Sidebar items are **role-filtered** — never render a link the user cannot open
- Notification bell shows the unread count from `/api/notifications/unread-count`, polled every 60 s

### Sidebar Navigation

| Item | Route | Roles |
|------|-------|-------|
| Dashboard | `/console` | Admin, Staff |
| Reports | `/console/reports` | Admin, Staff |
| Map View | `/console/map` | Admin, Staff |
| Departments | `/console/departments` | Admin, Staff |
| Report Approvals | `/console/report-approvals` | Admin |
| Citizen Accounts | `/console/citizens` | Admin |
| Staff Accounts | `/console/staff` | Admin |
| Account Approvals | `/console/account-approvals` | Admin |
| Categories | `/console/categories` | Admin |
| Notifications | `/console/notifications` | Admin, Staff |
| Settings / Profile | `/console/settings` | Admin, Staff |

### Admin Dashboard

Four stat cards across the top — **Pending accounts**, **Pending reports**, **Total citizens**, **Total reports** — each linking to the matching queue. Below: two panels side by side, "10 most recent registrations" and "Reports awaiting approval" with inline Approve / Deny actions.

### Staff Dashboard

```
┌───────────┬───────────┬───────────┬───────────┐
│  Total    │ Resolved  │ Remaining │   New     │
│  Reports  │           │           │  Today    │
└───────────┴───────────┴───────────┴───────────┘
┌──────────────────────────┬─────────────────────┐
│  Map (compact)           │  Monthly reports    │
│                          │  per department     │
└──────────────────────────┴─────────────────────┘
┌────────────────────────────────────────────────┐
│  Recent reports (10 rows, status badges)       │
└────────────────────────────────────────────────┘
```

### Reports List Page

```
┌────────────────────────────────────────────────────────────┐
│ Reports                                                     │
├────────────────────────────────────────────────────────────┤
│ [Search…] [Status ▾] [Category ▾] [Department ▾] [Date ▾]   │
├────┬──────────┬──────────┬──────────┬────────┬─────────────┤
│Code│ Title    │ Category │ Dept     │ Status │ Actions     │
│…012│ Pothole  │ Roads    │ Roads    │ 🔵 Assigned│ [View]   │
├────┴──────────┴──────────┴──────────┴────────┴─────────────┤
│ Showing 1–10 of 137                        [Pagination]     │
└────────────────────────────────────────────────────────────┘
```

Rules: actions column last; View opens the detail page (not a modal, because it has tabs); staff see only their department's rows, enforced server-side; empty state is a centred `EmptyState` with a plain message, never a bare table.

### Report Detail Page (Console)

Tabs: **Overview** (photos, description, map snippet, reporter), **Timeline** (status history), **Comments** (internal department notes), **Resolution** (completion photo upload + citizen feedback once given).

Action bar: `Change status ▾`, `Assign department ▾` (admin), `Set priority ▾`, `Add comment`. Changing status opens a dialog with a remarks field; moving to `RESOLVED` requires at least one completion photo and blocks submit until one is attached.

### Full Map View

Full-screen Leaflet map with clustering, a filter rail (category, status, priority, date range), and a slide-over panel with the report summary when a pin is clicked.

### Departments Page

Per-department cards (open / in progress / resolved counts), a pie chart of workload share, a bar chart of monthly volume, and a performance table (average resolution hours, average citizen rating).

---

## Shared Component Standards

### Forms
- shadcn `Form`, `Input`, `Select`, `Textarea`; required fields marked with `*`
- Validation errors appear directly under the field in `text-destructive`, mirroring the backend `errors` map
- Buttons: `Cancel` (secondary, returns without saving) and a specific primary verb — `Save changes`, `Create staff account`, `Submit report` — never "Submit"
- Disable the primary button and show a spinner label during a request

### Tables
- shadcn `Table` with hover rows; the actions column is always last
- Pagination when a list can exceed 10 rows
- Sticky header on long tables

### Dialogs
- Destructive confirmations use `ConfirmDialog`: title "Delete category", body naming the record, `Cancel` and a `Delete` button in the destructive style
- Approve / deny dialogs require a reason field when denying

### Toasts and Alerts
- Success, error, warning, info via shadcn `Toast`, top-right, auto-dismiss after 5 s
- The action name stays consistent: a button that says "Approve" produces a toast that says "Approved"

### Loading, Empty and Error States
- **Loading:** skeleton rows for tables and cards; a spinner only for whole-page loads
- **Empty:** centred icon + one plain sentence that invites the next action — "No reports yet. Tap Report to send your first one."
- **Error:** state what went wrong and what to do; never a raw stack trace. 401/403 → clear the token and redirect to login. 404 → "That report doesn't exist or was removed."
- **No chart data:** "No data available for the selected period."

---

## Copy Rules

- Name things the way a citizen would: "Report an issue", not "Create report entity"
- Sentence case for all labels and buttons
- Active voice; the button says exactly what happens
- Errors explain the fix: "Your account is awaiting approval. You'll get a notification once it's approved."
- Status labels use the human wording in the badge table above, never the raw enum name

---

## Frontend File Organisation

```
src/
├── components/
│   ├── common/          ← DataTable, StatusBadge, EmptyState, ConfirmDialog, PageHeader, StatCard
│   ├── layout/          ← CitizenShell, ConsoleShell, Sidebar, Navbar, BottomTabBar
│   ├── map/             ← ReportMap, MapFilters, PinPopup
│   ├── report/          ← ReportForm, ReportCard, StatusTimeline, ImageUploader
│   ├── user/            ← CitizenTable, StaffTable, ApprovalQueue, CreateStaffForm
│   ├── dashboard/       ← StatCards, DepartmentCharts, CategoryChart
│   ├── notification/    ← NotificationBell, NotificationList
│   ├── score/           ← LeaderboardList, ScoreCard, PointHistory
│   └── auth/            ← LoginForm, RegisterForm
├── pages/
│   ├── citizen/         ← Home, Map, NewReport, ReportDetail, Leaderboard, Score, Profile
│   └── console/         ← Dashboard, Reports, ReportDetail, MapView, Departments,
│                          Citizens, Staff, Approvals, Categories, Settings
├── services/            ← One API service file per module
├── types/               ← TypeScript interfaces mirroring backend DTOs
├── hooks/               ← useAuth, useReports, useNotifications, useGeolocation
├── context/             ← AuthContext
├── routes/              ← ProtectedRoute, router config
├── lib/                 ← api client, formatters, constants
├── App.tsx
└── main.tsx
```
