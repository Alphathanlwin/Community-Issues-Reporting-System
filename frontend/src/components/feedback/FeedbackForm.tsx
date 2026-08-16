import { useState } from "react";
import { Star } from "lucide-react";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { Button } from "@/components/ui/button";
import { Textarea } from "@/components/ui/textarea";
import { Label } from "@/components/ui/label";
import { cn } from "@/lib/utils";
import { feedbackService } from "@/services/feedbackService";
import { useToast } from "@/context/ToastContext";

interface FeedbackFormProps {
  reportId: number;
  reportCode: string;
  open: boolean;
  onOpenChange: (open: boolean) => void;
  onSubmitted: () => void;
}

/** 1–5 star rating + comment on a resolved report, per ui-rules.md. */
export function FeedbackForm({ reportId, reportCode, open, onOpenChange, onSubmitted }: FeedbackFormProps) {
  const [rating, setRating] = useState(0);
  const [hoverRating, setHoverRating] = useState(0);
  const [comment, setComment] = useState("");
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const { toast } = useToast();

  async function handleSubmit() {
    if (rating < 1) {
      setError("Please choose a star rating.");
      return;
    }
    setIsSubmitting(true);
    setError(null);
    try {
      await feedbackService.create({ reportId, rating, comment: comment.trim() || undefined });
      toast({ title: "Feedback submitted", description: "Thanks for helping us improve.", variant: "success" });
      onSubmitted();
      onOpenChange(false);
      setRating(0);
      setComment("");
    } catch (err: any) {
      setError(err?.response?.data?.message ?? "Could not submit feedback. Please try again.");
    } finally {
      setIsSubmitting(false);
    }
  }

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>Rate this resolution</DialogTitle>
          <DialogDescription>Report {reportCode} — how was it handled?</DialogDescription>
        </DialogHeader>

        <div className="flex justify-center gap-1 py-2">
          {[1, 2, 3, 4, 5].map((star) => (
            <button
              key={star}
              type="button"
              onClick={() => setRating(star)}
              onMouseEnter={() => setHoverRating(star)}
              onMouseLeave={() => setHoverRating(0)}
              className="p-1"
              aria-label={`${star} star${star === 1 ? "" : "s"}`}
            >
              <Star
                className={cn(
                  "h-8 w-8",
                  (hoverRating || rating) >= star ? "fill-status-pending text-status-pending" : "text-border",
                )}
              />
            </button>
          ))}
        </div>

        <div className="space-y-1.5">
          <Label htmlFor="feedback-comment">Comment (optional)</Label>
          <Textarea
            id="feedback-comment"
            placeholder="What went well, or what could be better?"
            value={comment}
            onChange={(e) => setComment(e.target.value)}
            maxLength={1000}
          />
        </div>

        {error && <p className="text-sm text-destructive">{error}</p>}

        <DialogFooter>
          <Button variant="outline" onClick={() => onOpenChange(false)}>
            Cancel
          </Button>
          <Button onClick={handleSubmit} disabled={isSubmitting}>
            {isSubmitting ? "Submitting…" : "Submit feedback"}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
