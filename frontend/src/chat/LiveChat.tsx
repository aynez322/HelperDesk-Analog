import { useEffect, useRef, useState } from 'react';
import type { Message } from '../types';
import { api } from '../api/client';
import { ChatClient } from './ChatClient';
import { useAuth } from '../context/AuthContext';

interface LiveChatProps {
  /** Live wire this chat to an existing ticket's message stream. */
  ticketId: number;
  requesterEmail?: string;
}

/**
 * Live-chat panel wired to a ticket over STOMP.
 * - Loads existing history via REST on mount.
 * - Subscribes to /topic/tickets/{id} and appends new messages in real time.
 * - Sends follow-ups to /app/chat.send with the ticket id.
 *
 * Both CLIENT and AGENT can use this panel on a ticket detail page; the
 * backend enforces ownership/staff rules at the service layer.
 */
export function LiveChat({ ticketId, requesterEmail }: LiveChatProps) {
  const { token } = useAuth();
  const [messages, setMessages] = useState<Message[]>([]);
  const [input, setInput] = useState('');
  const [connected, setConnected] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const chatRef = useRef<ChatClient | null>(null);
  const listRef = useRef<HTMLDivElement | null>(null);

  useEffect(() => {
    let alive = true;
    const client = new ChatClient();
    chatRef.current = client;

    // Existing history via REST (works for owner / staff).
    void api
      .getMessages(ticketId)
      .then((history) => {
        if (alive) setMessages(history);
      })
      .catch(() => {
        // owner/staff only; a forbidden read is fine — WS still streams
      });

    client
      .connect(token)
      .then(() => {
        if (!alive) return;
        setConnected(true);
        client.onMessage(ticketId, (m) => setMessages((prev) => prev.concat(m)));
      })
      .catch((e) => {
        if (alive) setError(String(e));
      });

    return () => {
      alive = false;
      client.disconnect();
    };
  }, [token, ticketId]);

  useEffect(() => {
    listRef.current?.scrollTo({ top: listRef.current.scrollHeight });
  }, [messages]);

  const send = () => {
    const body = input.trim();
    if (!body || !chatRef.current) return;
    setInput('');
    chatRef.current.send(body, ticketId, requesterEmail);
  };

  return (
    <div className="flex flex-col h-[420px] border rounded-lg overflow-hidden app-card app-text app-border shadow-sm">
      <div className="px-4 py-3 border-b app-border font-semibold flex items-center justify-between">
        <span>Live chat</span>
        {connected ? (
          <span className="text-xs font-normal" style={{ color: 'var(--app-success)' }}>● connected</span>
        ) : (
          <span className="text-xs app-secondary-text font-normal">connecting…</span>
        )}
      </div>

      {error && <div className="px-4 py-2 text-sm border-b" style={{ color: 'var(--app-danger)', backgroundColor: 'color-mix(in srgb, var(--app-danger) 12%, var(--app-card))', borderColor: 'color-mix(in srgb, var(--app-danger) 30%, var(--app-border))' }}>{error}</div>}

      <div ref={listRef} className="flex-1 overflow-y-auto px-4 py-3 space-y-2 app-background">
        {messages.length === 0 && (
          <p className="text-sm app-secondary-text">Niciun mesaj încă. Spune-ne cu ce te putem ajuta!</p>
        )}
        {messages.map((m) => (
          <div key={m.id} className={`flex ${m.senderType === 'AGENT' ? 'justify-end' : 'justify-start'}`}>
            <div
              className={`max-w-[75%] rounded-lg px-3 py-2 text-sm border ${
                m.senderType === 'AGENT'
                  ? 'border-blue-200 dark:border-blue-800'
                  : 'app-border'
              }`}
              style={m.senderType === 'AGENT'
                ? { backgroundColor: 'color-mix(in srgb, var(--app-primary) 18%, var(--app-card))', color: 'var(--app-text)' }
                : { backgroundColor: 'color-mix(in srgb, var(--app-border) 45%, var(--app-card))', color: 'var(--app-text)' }}
            >
              {m.body}
            </div>
          </div>
        ))}
      </div>

      <form
        onSubmit={(e) => {
          e.preventDefault();
          send();
        }}
        className="border-t app-border p-2 flex gap-2 app-card"
      >
        <input
          value={input}
          onChange={(e) => setInput(e.target.value)}
          placeholder="Scrieți un mesaj…"
          className="flex-1 px-3 py-2 text-sm rounded-lg app-card app-text app-border placeholder:text-slate-400 focus:outline-none focus:ring-2"
          style={{ outlineColor: 'var(--app-primary)' }}
        />
        <button
          type="submit"
          className="px-4 py-2 text-sm font-medium text-white rounded-lg disabled:opacity-50 transition-colors"
          style={{ backgroundColor: 'var(--app-primary)' }}
          onMouseEnter={(e) => { e.currentTarget.style.backgroundColor = 'var(--app-primary-hover)'; }}
          onMouseLeave={(e) => { e.currentTarget.style.backgroundColor = 'var(--app-primary)'; }}
          disabled={!connected}
        >
          Trimite
        </button>
      </form>
    </div>
  );
}
