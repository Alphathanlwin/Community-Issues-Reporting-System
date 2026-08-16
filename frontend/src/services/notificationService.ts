import { api } from "@/lib/api";
import type { Notification } from "@/types/notification";

export const notificationService = {
  list: (unreadOnly = false) =>
    api.get<Notification[]>("/notifications", { params: { unreadOnly } }).then((r) => r.data),

  unreadCount: () => api.get<{ count: number }>("/notifications/unread-count").then((r) => r.data.count),

  markRead: (id: number) => api.patch<Notification>(`/notifications/${id}/read`).then((r) => r.data),

  markAllRead: () => api.patch<void>("/notifications/read-all"),
};
