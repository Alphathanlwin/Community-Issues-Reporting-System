import { api } from "@/lib/api";
import type { LeaderboardEntry } from "@/types/score";
import type { ScoreSummary } from "@/types/score";

export const scoreService = {
  getLeaderboard: (limit = 50) =>
    api.get<LeaderboardEntry[]>("/leaderboard", { params: { limit } }).then((r) => r.data),

  getMyScore: () => api.get<ScoreSummary>("/score/me").then((r) => r.data),
};
