import { Outlet, useLocation } from "react-router-dom";
import { BottomTabBar } from "@/components/layout/BottomTabBar";
import { NotificationBell } from "@/components/notification/NotificationBell";

const TITLES: Record<string, string> = {
  "/home": "SCIRS",
  "/map": "Community map",
  "/report": "Report an issue",
  "/leaderboard": "Leaderboard",
  "/profile": "Profile",
  "/notifications": "Notifications",
  "/score": "My score",
};

/** Mobile-first shell for citizens: header + scrollable content + fixed bottom tabs. */
export function CitizenShell() {
  const location = useLocation();
  const title = TITLES[location.pathname] ?? "SCIRS";

  return (
    <div className="min-h-screen bg-surface-muted pb-20">
      <header className="sticky top-0 z-30 flex h-14 items-center justify-between border-b border-border bg-surface px-4">
        <h1 className="text-base font-semibold text-text">{title}</h1>
        <NotificationBell />
      </header>
      <main className="mx-auto max-w-md px-4 py-4">
        <Outlet />
      </main>
      <BottomTabBar />
    </div>
  );
}
