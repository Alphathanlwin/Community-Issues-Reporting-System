import { api } from "@/lib/api";
import type { CreateFeedbackRequest, Feedback } from "@/types/feedback";

export const feedbackService = {
  create: (data: CreateFeedbackRequest) => api.post<Feedback>("/feedback", data).then((r) => r.data),

  getByReportId: (reportId: number) => api.get<Feedback>(`/feedback/${reportId}`).then((r) => r.data),
};
