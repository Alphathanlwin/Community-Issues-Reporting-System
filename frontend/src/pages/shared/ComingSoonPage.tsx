import { Construction } from "lucide-react";
import { EmptyState } from "@/components/common/EmptyState";

interface ComingSoonPageProps {
  title: string;
}

/** Honest placeholder for routes that exist in the nav but aren't built this phase. */
export function ComingSoonPage({ title }: ComingSoonPageProps) {
  return (
    <div>
      <h1 className="mb-6 text-2xl font-semibold tracking-tight text-text">{title}</h1>
      <EmptyState icon={Construction} message={`${title} is coming in a future update.`} />
    </div>
  );
}
