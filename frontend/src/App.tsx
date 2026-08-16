import { Navigate, Route, Routes } from "react-router-dom";
import { ProtectedRoute } from "@/routes/ProtectedRoute";
import { CitizenShell } from "@/components/layout/CitizenShell";
import { ConsoleShell } from "@/components/layout/ConsoleShell";
import { LoginPage } from "@/pages/auth/LoginPage";
import { HomePage } from "@/pages/citizen/HomePage";
import { LeaderboardPage } from "@/pages/citizen/LeaderboardPage";
import { ScorePage } from "@/pages/citizen/ScorePage";
import { ProfilePage } from "@/pages/citizen/ProfilePage";
import { DashboardPage } from "@/pages/console/DashboardPage";
import { SettingsPage } from "@/pages/console/SettingsPage";
import { NotificationsPage } from "@/pages/shared/NotificationsPage";
import { ComingSoonPage } from "@/pages/shared/ComingSoonPage";
import { useAuth } from "@/context/AuthContext";

function RoleHome() {
  const { user } = useAuth();
  if (!user) return <Navigate to="/login" replace />;
  return <Navigate to={user.role === "CITIZEN" ? "/home" : "/console"} replace />;
}

export function App() {
  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route path="/" element={<RoleHome />} />

      <Route element={<ProtectedRoute allowedRoles={["CITIZEN"]} />}>
        <Route element={<CitizenShell />}>
          <Route path="/home" element={<HomePage />} />
          <Route path="/map" element={<ComingSoonPage title="Community map" />} />
          <Route path="/report" element={<ComingSoonPage title="Report an issue" />} />
          <Route path="/leaderboard" element={<LeaderboardPage />} />
          <Route path="/score" element={<ScorePage />} />
          <Route path="/notifications" element={<NotificationsPage />} />
          <Route path="/profile" element={<ProfilePage />} />
        </Route>
      </Route>

      <Route element={<ProtectedRoute allowedRoles={["ADMIN", "STAFF"]} />}>
        <Route element={<ConsoleShell />}>
          <Route path="/console" element={<DashboardPage />} />
          <Route path="/console/notifications" element={<NotificationsPage />} />
          <Route path="/console/settings" element={<SettingsPage />} />
        </Route>
      </Route>

      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
}
