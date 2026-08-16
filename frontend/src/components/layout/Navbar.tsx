import { useNavigate } from "react-router-dom";
import { LogOut } from "lucide-react";
import { NotificationBell } from "@/components/notification/NotificationBell";
import { Button } from "@/components/ui/button";
import { useAuth } from "@/context/AuthContext";

/** Top navbar: brand + notification bell + user menu (ui-rules.md Console Shell). */
export function Navbar() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();

  function handleLogout() {
    logout();
    navigate("/login");
  }

  return (
    <header className="flex h-14 items-center justify-between border-b border-border bg-surface px-4">
      <span className="text-base font-semibold text-text">SCIRS Console</span>
      <div className="flex items-center gap-3">
        <NotificationBell />
        <div className="flex items-center gap-2">
          <span className="hidden text-sm text-text-muted sm:inline">{user?.fullName}</span>
          <Button variant="ghost" size="icon" onClick={handleLogout} aria-label="Log out">
            <LogOut className="h-4 w-4" />
          </Button>
        </div>
      </div>
    </header>
  );
}
