import { LayoutDashboard } from "lucide-react";
import { PageHeader } from "@/components/common/PageHeader";
import { EmptyState } from "@/components/common/EmptyState";
import { useAuth } from "@/context/AuthContext";

/** Minimal landing page — the full admin/staff dashboard (stat cards, charts) is later-phase work. */
export function DashboardPage() {
  const { user } = useAuth();

  return (
    <div>
      <PageHeader title={`Welcome, ${user?.fullName}`} description="SCIRS console" />
      <EmptyState
        icon={LayoutDashboard}
        message="The full dashboard (stat cards, charts, recent activity) is coming in a future update. Notifications are available from the bell above."
      />
    </div>
  );
}
