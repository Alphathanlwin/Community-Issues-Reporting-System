export interface LeaderboardEntry {
  rank: number;
  userId: number;
  fullName: string;
  scorePoints: number;
}

export interface PointTransaction {
  id: number;
  points: number;
  reason: "REPORT_APPROVED" | "REPORT_RESOLVED" | "FEEDBACK_GIVEN" | "REPORT_REJECTED";
  reportId: number | null;
  reportCode: string | null;
  createdAt: string;
}

export interface ScoreSummary {
  totalPoints: number;
  history: PointTransaction[];
}
