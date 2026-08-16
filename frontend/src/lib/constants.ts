export const TOKEN_STORAGE_KEY = "scirs.token";
export const USER_STORAGE_KEY = "scirs.user";

export const REPORT_STATUS_LABELS: Record<string, string> = {
  PENDING_APPROVAL: "Pending approval",
  REJECTED: "Denied",
  ASSIGNED: "Assigned",
  IN_PROGRESS: "In progress",
  RESOLVED: "Resolved",
  CLOSED: "Closed",
};

export const ACCOUNT_STATUS_LABELS: Record<string, string> = {
  PENDING: "Pending",
  APPROVED: "Approved",
  REJECTED: "Rejected",
  SUSPENDED: "Suspended",
};

export const PRIORITY_LABELS: Record<string, string> = {
  LOW: "Low",
  NORMAL: "Normal",
  HIGH: "High",
  URGENT: "Urgent",
};
