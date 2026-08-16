import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { PlusCircle, MessageSquareText } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { ScoreCard } from "@/components/score/ScoreCard";
import { StatusBadge } from "@/components/common/StatusBadge";
import { EmptyState } from "@/components/common/EmptyState";
import { FeedbackForm } from "@/components/feedback/FeedbackForm";
import { formatDate } from "@/lib/format";
import { useAuth } from "@/context/AuthContext";
import { reportService } from "@/services/reportService";
import { scoreService } from "@/services/scoreService";
import { feedbackService } from "@/services/feedbackService";
import type { Report } from "@/types/report";

const FEEDBACK_ELIGIBLE = new Set(["RESOLVED", "CLOSED"]);

export function HomePage() {
  const { user } = useAuth();
  const [reports, setReports] = useState<Report[] | null>(null);
  const [totalPoints, setTotalPoints] = useState<number | null>(null);
  const [feedbackGiven, setFeedbackGiven] = useState<Record<number, boolean>>({});
  const [feedbackTarget, setFeedbackTarget] = useState<Report | null>(null);

  function refresh() {
    reportService.getMyReports().then((all) => {
      const recent = all.slice(0, 5);
      setReports(recent);
      recent
        .filter((r) => FEEDBACK_ELIGIBLE.has(r.status))
        .forEach((r) => {
          feedbackService
            .getByReportId(r.id)
            .then(() => setFeedbackGiven((current) => ({ ...current, [r.id]: true })))
            .catch(() => setFeedbackGiven((current) => ({ ...current, [r.id]: false })));
        });
    });
    scoreService.getMyScore().then((summary) => setTotalPoints(summary.totalPoints));
  }

  useEffect(refresh, []);

  return (
    <div className="space-y-6">
      <div>
        <p className="text-sm text-text-muted">Welcome back,</p>
        <h2 className="text-xl font-semibold text-text">{user?.fullName}</h2>
      </div>

      {totalPoints !== null && <ScoreCard totalPoints={totalPoints} />}

      <Button asChild size="lg" className="w-full">
        <Link to="/report">
          <PlusCircle className="h-5 w-5" />
          Report an issue
        </Link>
      </Button>

      <div>
        <h3 className="mb-3 text-sm font-semibold text-text">Your recent reports</h3>
        {reports === null ? (
          <div className="space-y-2">
            {[0, 1, 2].map((i) => (
              <div key={i} className="h-16 animate-pulse rounded-lg bg-surface-muted" />
            ))}
          </div>
        ) : reports.length === 0 ? (
          <EmptyState message="No reports yet. Tap Report an issue to send your first one." />
        ) : (
          <div className="space-y-2">
            {reports.map((report) => (
              <Card key={report.id}>
                <CardContent className="flex items-center justify-between gap-3 p-4">
                  <div className="min-w-0">
                    <p className="truncate text-sm font-medium text-text">{report.title}</p>
                    <p className="text-xs text-text-muted">
                      {report.reportCode} · {formatDate(report.createdAt)}
                    </p>
                  </div>
                  <div className="flex shrink-0 items-center gap-2">
                    <StatusBadge status={report.status} />
                    {FEEDBACK_ELIGIBLE.has(report.status) && feedbackGiven[report.id] === false && (
                      <Button size="sm" variant="outline" onClick={() => setFeedbackTarget(report)}>
                        <MessageSquareText className="h-4 w-4" />
                        Rate
                      </Button>
                    )}
                  </div>
                </CardContent>
              </Card>
            ))}
          </div>
        )}
      </div>

      {feedbackTarget && (
        <FeedbackForm
          reportId={feedbackTarget.id}
          reportCode={feedbackTarget.reportCode}
          open={Boolean(feedbackTarget)}
          onOpenChange={(open) => !open && setFeedbackTarget(null)}
          onSubmitted={() => {
            setFeedbackGiven((current) => ({ ...current, [feedbackTarget.id]: true }));
          }}
        />
      )}
    </div>
  );
}
