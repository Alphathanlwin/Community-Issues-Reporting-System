import { NavLink } from "react-router-dom";
import { LayoutDashboard, Bell, Settings } from "lucide-react";
import { cn } from "@/lib/utils";

// Only routes actually implemented this session are listed — the full
// console (Reports, Map, Departments, Approvals, …) is later-phase work;
// ui-rules.md's "never render a link the user cannot open" rule applies
// just as much to unbuilt pages as to unauthorized ones.
const NAV_ITEMS = [
  { to: "/console", label: "Dashboard", icon: LayoutDashboard },
  { to: "/console/notifications", label: "Notifications", icon: Bell },
  { to: "/console/settings", label: "Settings", icon: Settings },
] as const;

/** Fixed-width sidebar, highlights the active module (ui-rules.md Console Shell). */
export function Sidebar() {
  return (
    <aside className="hidden w-[250px] shrink-0 border-r border-border bg-surface md:block">
      <nav className="flex flex-col gap-1 p-3">
        {NAV_ITEMS.map(({ to, label, icon: Icon }) => (
          <NavLink
            key={to}
            to={to}
            end={to === "/console"}
            className={({ isActive }) =>
              cn(
                "flex items-center gap-3 rounded-md px-3 py-2 text-sm font-medium text-text-muted hover:bg-surface-muted hover:text-text",
                isActive && "bg-brand/10 text-brand",
              )
            }
          >
            <Icon className="h-4 w-4" />
            {label}
          </NavLink>
        ))}
      </nav>
    </aside>
  );
}
