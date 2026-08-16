import type { LucideIcon } from "lucide-react";
import { Inbox } from "lucide-react";

interface EmptyStateProps {
  icon?: LucideIcon;
  message: string;
}

/** Centred icon + one plain sentence, per ui-rules.md — never a bare empty table/list. */
export function EmptyState({ icon: Icon = Inbox, message }: EmptyStateProps) {
  return (
    <div className="flex flex-col items-center justify-center gap-3 py-16 text-center">
      <Icon className="h-10 w-10 text-text-muted" />
      <p className="max-w-xs text-sm text-text-muted">{message}</p>
    </div>
  );
}
