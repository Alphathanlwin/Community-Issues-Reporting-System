export interface Feedback {
  id: number;
  reportId: number;
  reportCode: string;
  citizenId: number;
  citizenName: string;
  rating: number;
  comment: string | null;
  createdAt: string;
}

export interface CreateFeedbackRequest {
  reportId: number;
  rating: number;
  comment?: string;
}
