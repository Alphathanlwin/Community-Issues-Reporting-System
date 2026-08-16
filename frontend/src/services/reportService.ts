import { api } from "@/lib/api";
import type { Report } from "@/types/report";

export const reportService = {
  getMyReports: () => api.get<Report[]>("/reports/my").then((r) => r.data),

  getReport: (id: number) => api.get<Report>(`/reports/${id}`).then((r) => r.data),
};
