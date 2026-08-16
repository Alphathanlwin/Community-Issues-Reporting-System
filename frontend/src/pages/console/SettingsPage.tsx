import { useNavigate } from "react-router-dom";
import { LogOut, Mail, Shield } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { PageHeader } from "@/components/common/PageHeader";
import { useAuth } from "@/context/AuthContext";

export function SettingsPage() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();

  if (!user) return null;

  function handleLogout() {
    logout();
    navigate("/login");
  }

  return (
    <div className="max-w-md">
      <PageHeader title="Settings" />
      <Card>
        <CardContent className="space-y-3 p-6">
          <p className="font-semibold text-text">{user.fullName}</p>
          <div className="flex items-center gap-2 text-sm text-text-muted">
            <Mail className="h-4 w-4" />
            {user.email}
          </div>
          <div className="flex items-center gap-2 text-sm text-text-muted">
            <Shield className="h-4 w-4" />
            {user.role.charAt(0) + user.role.slice(1).toLowerCase()}
          </div>
        </CardContent>
      </Card>
      <Button variant="outline" className="mt-4" onClick={handleLogout}>
        <LogOut className="h-4 w-4" />
        Log out
      </Button>
    </div>
  );
}
