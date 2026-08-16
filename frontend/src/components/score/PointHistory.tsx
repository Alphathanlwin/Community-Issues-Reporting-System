import { History } from "lucide-react";
import { EmptyState } from "@/components/common/EmptyState";
import { formatDate } from "@/lib/format";
import { cn } from "@/lib/utils";
import type { PointTransaction } from "@/types/score";

const REASON_LABELS: Record<PointTransaction["reason"], string> = {
  REPORT_APPROVED: "Report approved",
  REPORT_RESOLVED: "Report resolved",
  FEEDBACK_GIVEN: "Feedback submitted",
  REPORT_REJECTED: "Report denied",
};

interface PointHistoryProps {
  history: PointTransaction[];
}

export function PointHistory({ history }: PointHistoryProps) {
  if (history.length === 0) {
    return <EmptyState icon={History} message="No point history yet. Report an issue to start earning points." />;
  }

  return (
    <ul className="divide-y divide-border rounded-lg border border-border bg-surface">
      {history.map((tx) => (
        <li key={tx.id} className="flex items-center justify-between px-4 py-3">
          <div>
            <p className="text-sm font-medium text-text">{REASON_LABELS[tx.reason]}</p>
            <p className="text-xs text-text-muted">
              {tx.reportCode ?? "—"} · {formatDate(tx.createdAt)}
            </p>
          </div>
          <span className={cn("text-sm font-semibold tabular-nums", tx.points >= 0 ? "text-status-resolved" : "text-status-rejected")}>
            {tx.points >= 0 ? "+" : ""}
            {tx.points}
          </span>
        </li>
      ))}
    </ul>
  );
}
