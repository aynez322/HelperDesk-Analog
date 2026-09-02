import { useEffect, useRef, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import { ChatClient } from '../../chat/ChatClient';
import { api } from '../../api/client';
import { Layout } from '../../components/Layout';

/**
 * "Start Live Chat" entry point for authenticated users with an urgent issue.
 *
 * Flow:
 * 1. Connect to STOMP (JWT auth from AuthContext).
 * 2. On first message, publish to /app/chat.send WITHOUT a ticketId — the
 *    backend mints a LIVE ticket (URGENT/OPEN) and broadcasts it to the agents
 *    inbox.
 * 3. The minted ticket belongs to the current user, so we poll their OWN ticket
 *    list (newest first) until the freshly created LIVE ticket shows up, then
 *    navigate to it — the detail page renders the LiveChat panel. This avoids
 *    relying on private STOMP user destinations (the project broker uses /topic
 *    broadcasts only).
 */
export function StartLiveChatPage() {
  const { token } = useAuth();
  const navigate = useNavigate();
  const [input, setInput] = useState('');
  const [connected, setConnected] = useState(false);
  const [waiting, setWaiting] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const chatRef = useRef<ChatClient | null>(null);
  const pollRef = useRef<ReturnType<typeof setInterval> | null>(null);

  useEffect(() => {
    let alive = true;
    const client = new ChatClient();
    chatRef.current = client;

    client
      .connect(token)
      .then(() => {
        if (alive) setConnected(true);
      })
      .catch((e) => {
        if (alive) setError(String(e));
      });

    return () => {
      alive = false;
      if (pollRef.current) clearInterval(pollRef.current);
      client.disconnect();
    };
  }, [token]);

  const send = () => {
    const body = input.trim();
    if (!body || !chatRef.current || waiting) return;
    setInput('');
    setWaiting(true);
    setError(null);
    chatRef.current.send(body, undefined, undefined); // no ticketId → mints LIVE

    // Poll own tickets (newest first) until the freshly minted LIVE ticket is the top row.
    const startedFrom = Date.now();
    pollRef.current = setInterval(async () => {
      try {
        const res = await api.listTickets({ sort: 'createdAt,desc', size: '1' });
        const top = res.content[0];
        if (top && top.type === 'LIVE') {
          if (pollRef.current) clearInterval(pollRef.current);
          navigate(`/tickets/${top.id}`);
        }
      } catch {
        /* transient — keep polling */
      }
      if (Date.now() - startedFrom > 15000) {
        if (pollRef.current) clearInterval(pollRef.current);
        setWaiting(false);
        setError('Nu am reușit să pornim chat-ul live. Încearcă din nou.');
      }
    }, 600);
  };

  return (
    <Layout>
      <div className="max-w-2xl mx-auto p-6">
        <div className="flex items-center justify-between mb-6">
          <div>
            <h2 className="text-xl font-bold text-gray-900 dark:text-white">Live Chat</h2>
            <p className="text-sm text-gray-500 dark:text-gray-400 mt-1">
              Ai o problemă urgentă? Un agent se va conecta aici în timp real.
            </p>
          </div>
          <span className="text-sm" style={{ color: 'var(--app-success)' }} hidden={!connected}>
            ● connected
          </span>
        </div>

        {error && (
          <div className="text-sm text-red-600 dark:text-red-400 bg-red-50 dark:bg-red-950 border border-red-200 dark:border-red-800 rounded-lg px-3 py-2 mb-4">
            {error}
          </div>
        )}

        <div className="flex flex-col h-[380px] border rounded-lg overflow-hidden app-card app-text app-border shadow-sm">
          <div className="px-4 py-3 border-b app-border text-sm app-secondary-text">
            Descrieți pe scurt problema. Veți fi conectat(ă) cu primul agent disponibil.
          </div>

          <div className="flex-1 flex items-center justify-center px-4 app-background">
            {waiting ? (
              <div className="text-center">
                <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-indigo-600 dark:border-indigo-400 mx-auto" />
                <p className="mt-3 text-sm text-gray-500 dark:text-gray-400">
                  Se caută un agent… Veți fi redirecționat(ă) spre conversație.
                </p>
              </div>
            ) : (
              <p className="text-sm text-gray-500 dark:text-gray-400">
                Scrieți primul mesaj pentru a începe chat-ul live.
              </p>
            )}
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
              placeholder="Scrieți aici mesajul dvs. inițial…"
              disabled={!connected || waiting}
              className="flex-1 px-3 py-2 text-sm rounded-lg app-card app-text app-border placeholder:text-slate-400 focus:outline-none focus:ring-2"
              style={{ outlineColor: 'var(--app-primary)' }}
            />
            <button
              type="submit"
              disabled={!connected || waiting}
              className="px-4 py-2 text-sm font-medium text-white rounded-lg disabled:opacity-50 transition-colors"
              style={{ backgroundColor: 'var(--app-primary)' }}
              onMouseEnter={(e) => { e.currentTarget.style.backgroundColor = 'var(--app-primary-hover)'; }}
              onMouseLeave={(e) => { e.currentTarget.style.backgroundColor = 'var(--app-primary)'; }}
            >
              Start chat
            </button>
          </form>
        </div>
      </div>
    </Layout>
  );
}