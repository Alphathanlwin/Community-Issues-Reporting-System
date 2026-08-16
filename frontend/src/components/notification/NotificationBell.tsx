import { useCallback, useEffect, useRef, useState } from "react";
import { Bell } from "lucide-react";
import { Link } from "react-router-dom";
import { notificationService } from "@/services/notificationService";
import { NotificationList } from "@/components/notification/NotificationList";
import type { Notification } from "@/types/notification";

/** Unread badge polled every 60s per ui-rules.md; click opens a short recent list. */
export function NotificationBell() {
  const [unreadCount, setUnreadCount] = useState(0);
  const [isOpen, setIsOpen] = useState(false);
  const [notifications, setNotifications] = useState<Notification[]>([]);
  const [isLoading, setIsLoading] = useState(false);
  const containerRef = useRef<HTMLDivElement>(null);

  const refreshUnreadCount = useCallback(() => {
    notificationService
      .unreadCount()
      .then(setUnreadCount)
      .catch(() => {
        // best-effort; the bell just won't update this cycle
      });
  }, []);

  useEffect(() => {
    refreshUnreadCount();
    const interval = setInterval(refreshUnreadCount, 60_000);
    return () => clearInterval(interval);
  }, [refreshUnreadCount]);

  useEffect(() => {
    function handleClickOutside(event: MouseEvent) {
      if (containerRef.current && !containerRef.current.contains(event.target as Node)) {
        setIsOpen(false);
      }
    }
    document.addEventListener("mousedown", handleClickOutside);
    return () => document.removeEventListener("mousedown", handleClickOutside);
  }, []);

  function togglePanel() {
    const next = !isOpen;
    setIsOpen(next);
    if (next) {
      setIsLoading(true);
      notificationService
        .list()
        .then((list) => setNotifications(list.slice(0, 8)))
        .finally(() => setIsLoading(false));
    }
  }

  function handleMarkRead(id: number) {
    notificationService.markRead(id).then(() => {
      setNotifications((current) => current.map((n) => (n.id === id ? { ...n, read: true } : n)));
      refreshUnreadCount();
    });
  }

  return (
    <div className="relative" ref={containerRef}>
      <button
        onClick={togglePanel}
        className="relative flex h-10 w-10 items-center justify-center rounded-full text-text-muted hover:bg-surface-muted hover:text-text"
        aria-label="Notifications"
      >
        <Bell className="h-5 w-5" />
        {unreadCount > 0 && (
          <span className="absolute right-1 top-1 flex h-4 min-w-4 items-center justify-center rounded-full bg-destructive px-1 text-[10px] font-semibold text-destructive-foreground tabular-nums">
            {unreadCount > 99 ? "99+" : unreadCount}
          </span>
        )}
      </button>

      {isOpen && (
        <div className="absolute right-0 z-50 mt-2 w-80 rounded-lg border border-border bg-surface shadow-lg">
          <div className="flex items-center justify-between border-b border-border px-4 py-3">
            <p className="text-sm font-semibold text-text">Notifications</p>
            <Link to="/notifications" onClick={() => setIsOpen(false)} className="text-xs font-medium text-brand hover:underline">
              View all
            </Link>
          </div>
          <div className="max-h-96 overflow-y-auto">
            <NotificationList notifications={notifications} isLoading={isLoading} onMarkRead={handleMarkRead} />
          </div>
        </div>
      )}
    </div>
  );
}
