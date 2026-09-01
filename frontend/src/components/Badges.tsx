import type { CSSProperties } from 'react';
import type { TicketPriority, TicketStatus } from '../types';
import { PRIORITY_LABELS, STATUS_LABELS } from '../types';

type BadgeTone = 'info' | 'warning' | 'success' | 'danger' | 'neutral';

const statusTone: Record<TicketStatus, BadgeTone> = {
  OPEN: 'info',
  IN_PROGRESS: 'warning',
  RESOLVED: 'success',
  CLOSED: 'neutral',
};

const priorityTone: Record<TicketPriority, BadgeTone> = {
  LOW: 'neutral',
  NORMAL: 'info',
  HIGH: 'warning',
  URGENT: 'danger',
};

function toneStyle(tone: BadgeTone): CSSProperties {
  const variable = `var(--app-${tone === 'neutral' ? 'secondary-text' : tone})`;
  return {
    color: variable,
    backgroundColor: `color-mix(in srgb, ${variable} 12%, var(--app-card))`,
    borderColor: `color-mix(in srgb, ${variable} 32%, var(--app-border))`,
  };
}

function Badge({ label, tone }: { label: string; tone: BadgeTone }) {
  return (
    <span
      className="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium border"
      style={toneStyle(tone)}
    >
      {label}
    </span>
  );
}

export function StatusBadge({ status }: { status: TicketStatus }) {
  return <Badge label={STATUS_LABELS[status]} tone={statusTone[status]} />;
}

export function PriorityBadge({ priority }: { priority: TicketPriority }) {
  return <Badge label={PRIORITY_LABELS[priority]} tone={priorityTone[priority]} />;
}
