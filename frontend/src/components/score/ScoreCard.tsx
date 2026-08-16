import { Award } from "lucide-react";
import { Card, CardContent } from "@/components/ui/card";

interface ScoreCardProps {
  totalPoints: number;
}

export function ScoreCard({ totalPoints }: ScoreCardProps) {
  return (
    <Card>
      <CardContent className="flex items-center gap-4 p-6">
        <div className="flex h-12 w-12 items-center justify-center rounded-full bg-brand/10 text-brand">
          <Award className="h-6 w-6" />
        </div>
        <div>
          <p className="text-sm text-text-muted">Your total score</p>
          <p className="text-3xl font-semibold tabular-nums text-text">{totalPoints}</p>
        </div>
      </CardContent>
    </Card>
  );
}
