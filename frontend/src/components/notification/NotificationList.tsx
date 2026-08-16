import { formatDistanceToNow } from "@/lib/format";
import { EmptyState } from "@/components/common/EmptyState";
import { Bell } from "lucide-react";
import { cn } from "@/lib/utils";
import type { Notification } from "@/types/notification";

interface NotificationListProps {
  notifications: Notification[];
  isLoading?: boolean;
  onMarkRead: (id: number) => void;
}

export function NotificationList({ notifications, isLoading, onMarkRead }: NotificationListProps) {
  if (isLoading) {
    return (
      <div className="space-y-2 p-4">
        {[0, 1, 2].map((i) => (
          <div key={i} className="h-14 animate-pulse rounded-md bg-surface-muted" />
        ))}
      </div>
    );
  }

  if (notifications.length === 0) {
    return <EmptyState icon={Bell} message="No notifications yet. We'll let you know when something changes." />;
  }

  return (
    <ul className="divide-y divide-border">
      {notifications.map((n) => (
        <li
          key={n.id}
          className={cn("flex items-start justify-between gap-3 px-4 py-3", !n.read && "bg-brand/5")}
        >
          <div>
            <p className="text-sm font-medium text-text">{n.title}</p>
            <p className="text-sm text-text-muted">{n.message}</p>
            <p className="mt-1 text-xs text-text-muted tabular-nums">{formatDistanceToNow(n.createdAt)}</p>
          </div>
          {!n.read && (
            <button
              onClick={() => onMarkRead(n.id)}
              className="shrink-0 whitespace-nowrap text-xs font-medium text-brand hover:underline"
            >
              Mark read
            </button>
          )}
        </li>
      ))}
    </ul>
  );
}
