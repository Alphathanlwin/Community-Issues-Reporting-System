export type ReportStatus =
  | "PENDING_APPROVAL"
  | "REJECTED"
  | "ASSIGNED"
  | "IN_PROGRESS"
  | "RESOLVED"
  | "CLOSED";

export type ReportPriority = "LOW" | "NORMAL" | "HIGH" | "URGENT";

export interface ReportImage {
  id: number;
  imageUrl: string;
  imageType: "REPORT_PHOTO" | "RESOLUTION_PHOTO";
  uploadedAt: string;
}

export interface Report {
  id: number;
  reportCode: string;
  title: string;
  description: string;
  status: ReportStatus;
  priority: ReportPriority;
  latitude: number;
  longitude: number;
  addressText: string | null;
  categoryId: number;
  categoryName: string;
  departmentId: number | null;
  departmentName: string | null;
  reporterId: number;
  reporterName: string;
  images: ReportImage[];
  createdAt: string;
}

export interface ReportStatusHistoryEntry {
  id: number;
  oldStatus: ReportStatus | null;
  newStatus: ReportStatus;
  changedById: number;
  changedByName: string;
  remarks: string | null;
  changedAt: string;
}
