import { useEffect, useState } from "react";
import { ScoreCard } from "@/components/score/ScoreCard";
import { PointHistory } from "@/components/score/PointHistory";
import { scoreService } from "@/services/scoreService";
import type { ScoreSummary } from "@/types/score";

export function ScorePage() {
  const [summary, setSummary] = useState<ScoreSummary | null>(null);

  useEffect(() => {
    scoreService.getMyScore().then(setSummary);
  }, []);

  if (summary === null) {
    return <div className="h-32 animate-pulse rounded-lg bg-surface-muted" />;
  }

  return (
    <div className="space-y-6">
      <ScoreCard totalPoints={summary.totalPoints} />
      <div>
        <h3 className="mb-3 text-sm font-semibold text-text">Point history</h3>
        <PointHistory history={summary.history} />
      </div>
    </div>
  );
}
