const API_BASE = '/api';

function getToken(): string | null {
  return localStorage.getItem('token');
}

async function request<T>(
  path: string,
  options: RequestInit = {},
): Promise<T> {
  const token = getToken();
  const headers: Record<string, string> = {
    'Content-Type': 'application/json',
    ...(options.headers as Record<string, string>),
  };
  if (token) {
    headers['Authorization'] = `Bearer ${token}`;
  }
  const res = await fetch(`${API_BASE}${path}`, { ...options, headers });
  if (!res.ok) {
    const body = await res.json().catch(() => ({}));
    throw new Error(body.message || `Request failed: ${res.status}`);
  }
  if (res.status === 204) return undefined as T;
  return res.json();
}

export const api = {
  // Auth
  login: (data: { email: string; password: string }) =>
    request<import('../types').AuthResponse>('/auth/login', {
      method: 'POST',
      body: JSON.stringify(data),
    }),
  register: (data: { email: string; password: string; fullName: string }) =>
    request<import('../types').AuthResponse>('/auth/register', {
      method: 'POST',
      body: JSON.stringify(data),
    }),

  // Tickets
  createTicket: (data: import('../types').CreateTicketRequest) =>
    request<import('../types').Ticket>('/tickets', {
      method: 'POST',
      body: JSON.stringify(data),
    }),
  listTickets: (params?: Record<string, string>) => {
    const qs = params ? '?' + new URLSearchParams(params).toString() : '';
    return request<import('../types').PageResponse<import('../types').TicketSummary>>(
      `/tickets${qs}`,
    );
  },
  getTicket: (id: number) =>
    request<import('../types').Ticket>(`/tickets/${id}`),
  updateTicket: (id: number, data: import('../types').UpdateTicketRequest) =>
    request<import('../types').Ticket>(`/tickets/${id}`, {
      method: 'PATCH',
      body: JSON.stringify(data),
    }),
  getMessages: (id: number) =>
    request<import('../types').Message[]>(`/tickets/${id}/messages`),
  addMessage: (id: number, data: { body: string }) =>
    request<import('../types').Message>(`/tickets/${id}/messages`, {
      method: 'POST',
      body: JSON.stringify(data),
    }),

  // Admin
  listUsers: () =>
    request<import('../types').User[]>('/admin/users'),
  createUser: (data: { email: string; password: string; fullName: string; role: string }) =>
    request<import('../types').User>('/admin/users', {
      method: 'POST',
      body: JSON.stringify(data),
    }),
  changeUserRole: (id: number, role: string) =>
    request<import('../types').User>(`/admin/users/${id}/role`, {
      method: 'PATCH',
      body: JSON.stringify({ role }),
    }),

  // Me
  me: () =>
    request<{ username: string; authorities: { authority: string }[] }>('/me'),
};
