export type NotificationType =
  | "NEW_REPORT"
  | "URGENT_REPORT"
  | "REPORT_WAITING_TOO_LONG"
  | "DEPARTMENT_MENTION"
  | "STATUS_CHANGED"
  | "REPORT_APPROVED"
  | "REPORT_REJECTED"
  | "REPORT_COMPLETED"
  | "ACCOUNT_APPROVED"
  | "ACCOUNT_REJECTED";

export interface Notification {
  id: number;
  type: NotificationType;
  title: string;
  message: string;
  read: boolean;
  reportId: number | null;
  reportCode: string | null;
  createdAt: string;
}
