interface PageHeaderProps {
  title: string;
  description?: string;
}

export function PageHeader({ title, description }: PageHeaderProps) {
  return (
    <div className="mb-6">
      <h1 className="text-2xl font-semibold tracking-tight text-text">{title}</h1>
      {description && <p className="mt-1 text-sm text-text-muted">{description}</p>}
    </div>
  );
}
