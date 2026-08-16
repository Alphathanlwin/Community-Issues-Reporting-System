import { useEffect, useState } from "react";
import { Button } from "@/components/ui/button";
import { NotificationList } from "@/components/notification/NotificationList";
import { PageHeader } from "@/components/common/PageHeader";
import { notificationService } from "@/services/notificationService";
import type { Notification } from "@/types/notification";

export function NotificationsPage() {
  const [notifications, setNotifications] = useState<Notification[] | null>(null);

  function refresh() {
    notificationService.list().then(setNotifications);
  }

  useEffect(refresh, []);

  function handleMarkRead(id: number) {
    notificationService.markRead(id).then(refresh);
  }

  function handleMarkAllRead() {
    notificationService.markAllRead().then(refresh);
  }

  const hasUnread = notifications?.some((n) => !n.read) ?? false;

  return (
    <div>
      <div className="mb-4 flex items-center justify-between">
        <PageHeader title="Notifications" />
        {hasUnread && (
          <Button variant="outline" size="sm" onClick={handleMarkAllRead}>
            Mark all read
          </Button>
        )}
      </div>
      <div className="overflow-hidden rounded-lg border border-border bg-surface">
        <NotificationList notifications={notifications ?? []} isLoading={notifications === null} onMarkRead={handleMarkRead} />
      </div>
    </div>
  );
}
