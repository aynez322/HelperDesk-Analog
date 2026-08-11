export interface User {
  id: number;
  email: string;
  fullName: string;
  role: string;
  active: boolean;
  createdAt: string;
}

export interface AuthResponse {
  token: string;
  email: string;
  role: string;
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface RegisterRequest {
  email: string;
  password: string;
  fullName: string;
}

export type TicketType = 'FORM' | 'LIVE';
export type TicketStatus = 'OPEN' | 'IN_PROGRESS' | 'RESOLVED' | 'CLOSED';
export type TicketPriority = 'LOW' | 'NORMAL' | 'HIGH' | 'URGENT';
export type SenderType = 'CLIENT' | 'AGENT' | 'SYSTEM';

export interface Ticket {
  id: number;
  subject: string;
  description?: string;
  type: TicketType;
  status: TicketStatus;
  priority: TicketPriority;
  categoryId?: number;
  categoryName?: string;
  createdById?: number;
  createdByEmail?: string;
  assignedToId?: number;
  assignedToEmail?: string;
  requesterEmail?: string;
  createdAt: string;
  updatedAt: string;
  closedAt?: string;
}

export interface TicketSummary {
  id: number;
  subject: string;
  type: TicketType;
  status: TicketStatus;
  priority: TicketPriority;
  categoryName?: string;
  assignedToEmail?: string;
  requesterEmail?: string;
  createdAt: string;
  updatedAt: string;
}

export interface Message {
  id: number;
  ticketId: number;
  senderId?: number;
  senderEmail?: string;
  senderType: SenderType;
  body: string;
  createdAt: string;
}

export interface CreateTicketRequest {
  subject: string;
  description?: string;
  categoryId?: number;
  requesterEmail?: string;
}

export interface UpdateTicketRequest {
  status?: TicketStatus;
  priority?: TicketPriority;
  assignedToId?: number;
}

export interface AddMessageRequest {
  body: string;
}

export interface CreateUserRequest {
  email: string;
  password: string;
  fullName: string;
  role: string;
}

export interface ChangeRoleRequest {
  role: string;
}

export interface PageResponse<T> {
  content: T[];
  totalPages: number;
  totalElements: number;
  number: number;
  size: number;
}

export const STATUS_LABELS: Record<TicketStatus, string> = {
  OPEN: 'Open',
  IN_PROGRESS: 'In Progress',
  RESOLVED: 'Resolved',
  CLOSED: 'Closed',
};

export const PRIORITY_LABELS: Record<TicketPriority, string> = {
  LOW: 'Low',
  NORMAL: 'Normal',
  HIGH: 'High',
  URGENT: 'Urgent',
};

export const PRIORITY_ORDER: Record<TicketPriority, number> = {
  LOW: 0,
  NORMAL: 1,
  HIGH: 2,
  URGENT: 3,
};
