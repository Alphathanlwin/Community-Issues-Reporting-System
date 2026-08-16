import { Outlet } from "react-router-dom";
import { Navbar } from "@/components/layout/Navbar";
import { Sidebar } from "@/components/layout/Sidebar";

/** Desktop-first shell for admin/staff: navbar + sidebar + main content. */
export function ConsoleShell() {
  return (
    <div className="flex h-screen flex-col">
      <Navbar />
      <div className="flex flex-1 overflow-hidden">
        <Sidebar />
        <main className="flex-1 overflow-y-auto bg-surface-muted p-6">
          <Outlet />
        </main>
      </div>
    </div>
  );
}
