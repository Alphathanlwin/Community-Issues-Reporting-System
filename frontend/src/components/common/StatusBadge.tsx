import {
  Clock,
  XCircle,
  ClipboardCheck,
  RefreshCw,
  CheckCircle2,
  Archive,
  ShieldCheck,
  ShieldAlert,
  ShieldX,
  ShieldOff,
  Flame,
  Minus,
  ArrowUp,
  type LucideIcon,
} from "lucide-react";
import { cn } from "@/lib/utils";

type BadgeKind = "report" | "account" | "priority";

const REPORT_STATUS_CONFIG: Record<string, { label: string; icon: LucideIcon; className: string }> = {
  PENDING_APPROVAL: { label: "Pending approval", icon: Clock, className: "bg-status-pending/10 text-status-pending" },
  REJECTED: { label: "Denied", icon: XCircle, className: "bg-status-rejected/10 text-status-rejected" },
  ASSIGNED: { label: "Assigned", icon: ClipboardCheck, className: "bg-status-assigned/10 text-status-assigned" },
  IN_PROGRESS: { label: "In progress", icon: RefreshCw, className: "bg-status-progress/10 text-status-progress" },
  RESOLVED: { label: "Resolved", icon: CheckCircle2, className: "bg-status-resolved/10 text-status-resolved" },
  CLOSED: { label: "Closed", icon: Archive, className: "bg-status-closed/10 text-status-closed" },
};

const ACCOUNT_STATUS_CONFIG: Record<string, { label: string; icon: LucideIcon; className: string }> = {
  PENDING: { label: "Pending", icon: Clock, className: "bg-status-pending/10 text-status-pending" },
  APPROVED: { label: "Approved", icon: ShieldCheck, className: "bg-status-resolved/10 text-status-resolved" },
  REJECTED: { label: "Rejected", icon: ShieldX, className: "bg-status-rejected/10 text-status-rejected" },
  SUSPENDED: { label: "Suspended", icon: ShieldOff, className: "bg-status-closed/10 text-status-closed" },
};

const PRIORITY_CONFIG: Record<string, { label: string; icon: LucideIcon; className: string }> = {
  LOW: { label: "Low", icon: Minus, className: "bg-status-closed/10 text-status-closed" },
  NORMAL: { label: "Normal", icon: Minus, className: "bg-status-assigned/10 text-status-assigned" },
  HIGH: { label: "High", icon: ArrowUp, className: "bg-status-pending/10 text-status-pending" },
  URGENT: { label: "Urgent", icon: Flame, className: "bg-priority-urgent/10 text-priority-urgent" },
};

const CONFIG_BY_KIND: Record<BadgeKind, Record<string, { label: string; icon: LucideIcon; className: string }>> = {
  report: REPORT_STATUS_CONFIG,
  account: ACCOUNT_STATUS_CONFIG,
  priority: PRIORITY_CONFIG,
};

interface StatusBadgeProps {
  status: string;
  kind?: BadgeKind;
  className?: string;
}

/**
 * The only component allowed to render a report/account/priority status.
 * Always renders an icon alongside the label — color is never the only signal.
 */
export function StatusBadge({ status, kind = "report", className }: StatusBadgeProps) {
  const config = CONFIG_BY_KIND[kind][status] ?? {
    label: status,
    icon: ShieldAlert,
    className: "bg-surface-muted text-text-muted",
  };
  const Icon = config.icon;

  return (
    <span
      className={cn(
        "inline-flex items-center gap-1.5 rounded-full px-2.5 py-1 text-xs font-medium",
        config.className,
        className,
      )}
    >
      <Icon className="h-3.5 w-3.5" />
      {config.label}
    </span>
  );
}
