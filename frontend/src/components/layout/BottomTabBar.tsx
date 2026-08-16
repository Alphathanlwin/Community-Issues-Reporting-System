import { NavLink } from "react-router-dom";
import { Home, Map, PlusCircle, Trophy, User } from "lucide-react";
import { cn } from "@/lib/utils";

const TABS = [
  { to: "/home", label: "Home", icon: Home, raised: false },
  { to: "/map", label: "Map", icon: Map, raised: false },
  { to: "/report", label: "Report", icon: PlusCircle, raised: true },
  { to: "/leaderboard", label: "Board", icon: Trophy, raised: false },
  { to: "/profile", label: "Me", icon: User, raised: false },
] as const;

/** Fixed bottom tab bar, five items, the centre Report action visually raised (ui-rules.md). */
export function BottomTabBar() {
  return (
    <nav className="fixed inset-x-0 bottom-0 z-40 border-t border-border bg-surface">
      <div className="mx-auto flex max-w-md items-center justify-between px-2">
        {TABS.map(({ to, label, icon: Icon, raised }) => (
          <NavLink
            key={to}
            to={to}
            className={({ isActive }) =>
              cn(
                "flex min-h-[44px] flex-1 flex-col items-center justify-center gap-0.5 py-2 text-xs font-medium text-text-muted",
                isActive && "text-brand",
                raised && "relative",
              )
            }
          >
            {raised ? (
              <span className="-mt-6 flex h-12 w-12 items-center justify-center rounded-full bg-brand text-brand-foreground shadow-lg">
                <Icon className="h-6 w-6" />
              </span>
            ) : (
              <Icon className="h-5 w-5" />
            )}
            <span className={raised ? "mt-0.5" : undefined}>{label}</span>
          </NavLink>
        ))}
      </div>
    </nav>
  );
}
