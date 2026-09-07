import { Client, type IMessage, type StompSubscription } from '@stomp/stompjs';
import type { Message } from '../types';

/**
 * Thin STOMP client wrapper for the live-chat backend.
 * Endpoint /ws (plain WebSocket, no SockJS); auth via Authorization header
 * attached by @stomp/stompjs using the connection's connectHeaders.
 */
export class ChatClient {
  private client: Client | null = null;
  private subscriptions: StompSubscription[] = [];

  connect(token: string | null): Promise<void> {
    return new Promise((resolve, reject) => {
      this.client = new Client({
        brokerURL: `ws://${window.location.host}/ws`,
        reconnectDelay: 3000,
        connectHeaders: token ? { Authorization: `Bearer ${token}` } : {},
        onConnect: () => resolve(),
        onStompError: (frame) =>
          reject(new Error(frame.headers['message'] || 'STOMP error')),
      });
      this.client.activate();
    });
  }

  /**
   * Subscribes to a ticket's message stream. Returns an unsubscribe function.
   */
  onMessage(ticketId: number, handler: (m: Message) => void): () => void {
    if (!this.client) return () => {};
    const sub = this.client.subscribe(`/topic/tickets/${ticketId}`, (frame: IMessage) => {
      handler(JSON.parse(frame.body) as Message);
    });
    this.subscriptions.push(sub);
    return () => sub.unsubscribe();
  }

  /** Subscribes to new-LIVE-ticket notifications for the agent inbox. */
  onAgentInbox(handler: (t: { id: number; subject: string; priority: string }) => void): () => void {
    if (!this.client) return () => {};
    const sub = this.client.subscribe('/topic/agents/inbox', (frame: IMessage) => {
      handler(JSON.parse(frame.body));
    });
    this.subscriptions.push(sub);
    return () => sub.unsubscribe();
  }

  /** Sends a chat message to /app/chat.send. */
  send(body: string, ticketId?: number, requesterEmail?: string) {
    this.client?.publish({
      destination: '/app/chat.send',
      body: JSON.stringify({ body, ticketId, requesterEmail }),
    });
  }

  disconnect() {
    this.subscriptions.forEach((s) => s.unsubscribe());
    this.subscriptions = [];
    this.client?.deactivate();
    this.client = null;
  }
}
