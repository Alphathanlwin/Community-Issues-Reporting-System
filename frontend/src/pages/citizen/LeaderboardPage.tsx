import { useEffect, useState } from "react";
import { LeaderboardList } from "@/components/score/LeaderboardList";
import { useAuth } from "@/context/AuthContext";
import { scoreService } from "@/services/scoreService";
import type { LeaderboardEntry } from "@/types/score";

export function LeaderboardPage() {
  const { user } = useAuth();
  const [entries, setEntries] = useState<LeaderboardEntry[] | null>(null);

  useEffect(() => {
    scoreService.getLeaderboard(100).then(setEntries);
  }, []);

  if (entries === null) {
    return (
      <div className="space-y-2">
        {[0, 1, 2, 3].map((i) => (
          <div key={i} className="h-14 animate-pulse rounded-lg bg-surface-muted" />
        ))}
      </div>
    );
  }

  return <LeaderboardList entries={entries} currentUserId={user?.userId} />;
}
