import { Trophy } from "lucide-react";
import { cn } from "@/lib/utils";
import { EmptyState } from "@/components/common/EmptyState";
import type { LeaderboardEntry } from "@/types/score";

interface LeaderboardListProps {
  entries: LeaderboardEntry[];
  currentUserId?: number;
}

const MEDAL_STYLE: Record<number, string> = {
  1: "text-amber-500",
  2: "text-slate-400",
  3: "text-amber-700",
};

/** Ranked list; the current citizen's row is highlighted (ui-rules.md: "own row pinned"). */
export function LeaderboardList({ entries, currentUserId }: LeaderboardListProps) {
  if (entries.length === 0) {
    return <EmptyState icon={Trophy} message="No scores yet. Be the first to report an issue and start earning points." />;
  }

  const isCurrentUserRanked = entries.some((e) => e.userId === currentUserId);

  return (
    <div className="overflow-hidden rounded-lg border border-border bg-surface">
      <ul className="divide-y divide-border">
        {entries.map((entry) => (
          <li key={entry.userId} className={cn("flex items-center justify-between px-4 py-3", entry.userId === currentUserId && "bg-brand/5")}>
            <div className="flex items-center gap-3">
              <span className={cn("w-6 text-center text-sm font-semibold tabular-nums", MEDAL_STYLE[entry.rank] ?? "text-text-muted")}>
                {entry.rank}
              </span>
              <span className={cn("text-sm", entry.userId === currentUserId ? "font-semibold text-brand" : "text-text")}>
                {entry.fullName}
                {entry.userId === currentUserId && " (you)"}
              </span>
            </div>
            <span className="text-sm font-semibold tabular-nums text-text">{entry.scorePoints} pts</span>
          </li>
        ))}
      </ul>
      {currentUserId !== undefined && !isCurrentUserRanked && (
        <p className="border-t border-border px-4 py-3 text-sm text-text-muted">
          You're not on the board yet — submit and get reports resolved to start earning points.
        </p>
      )}
    </div>
  );
}
