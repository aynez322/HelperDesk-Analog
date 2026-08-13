import { useState, useEffect } from 'react';
import { useParams, Link } from 'react-router-dom';
import { api } from '../../api/client';
import { Layout } from '../../components/Layout';
import { StatusBadge, PriorityBadge } from '../../components/Badges';
import type { Ticket, Message } from '../../types';

const inputClass = 'w-full px-3 py-2 bg-white dark:bg-gray-800 border border-gray-300 dark:border-gray-600 rounded-lg text-sm text-gray-900 dark:text-white focus:outline-none focus:ring-2 focus:ring-indigo-500 dark:focus:ring-indigo-400 focus:border-transparent resize-y placeholder-gray-400 dark:placeholder-gray-500';

export function ClientTicketDetailPage() {
  const { id } = useParams<{ id: string }>();
  const [ticket, setTicket] = useState<Ticket | null>(null);
  const [messages, setMessages] = useState<Message[]>([]);
  const [reply, setReply] = useState('');
  const [loading, setLoading] = useState(true);
  const [sending, setSending] = useState(false);
  const [error, setError] = useState('');

  useEffect(() => {
    if (!id) return;
    setLoading(true);
    Promise.all([api.getTicket(+id), api.getMessages(+id)])
      .then(([t, m]) => { setTicket(t); setMessages(m); })
      .catch((err) => setError(err.message))
      .finally(() => setLoading(false));
  }, [id]);

  const handleReply = async () => {
    if (!id || !reply.trim()) return;
    setSending(true);
    try {
      const msg = await api.addMessage(+id, { body: reply.trim() });
      setMessages((prev) => [...prev, msg]);
      setReply('');
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to send');
    } finally { setSending(false); }
  };

  if (loading) return <Layout><div className="flex justify-center py-12"><div className="animate-spin rounded-full h-8 w-8 border-b-2 border-indigo-600 dark:border-indigo-400" /></div></Layout>;
  if (!ticket) return <Layout><div className="p-6 text-center text-gray-500 dark:text-gray-400">Ticket not found</div></Layout>;

  return (
    <Layout>
      <div className="p-6 max-w-4xl mx-auto">
        <div className="mb-4"><Link to="/tickets" className="text-sm text-indigo-600 dark:text-indigo-400 hover:text-indigo-700 dark:hover:text-indigo-300">← Back to tickets</Link></div>
        {error && <div className="text-sm text-red-600 dark:text-red-400 bg-red-50 dark:bg-red-950 border border-red-200 dark:border-red-800 rounded-lg px-3 py-2 mb-4">{error}</div>}

        <div className="bg-white dark:bg-gray-900 rounded-xl border border-gray-200 dark:border-gray-700 p-6 mb-4 transition-colors">
          <div className="flex items-start justify-between mb-3">
            <h1 className="text-lg font-bold text-gray-900 dark:text-white">{ticket.subject}</h1>
            <span className="font-mono text-sm text-gray-400 dark:text-gray-500">#{ticket.id}</span>
          </div>
          <div className="flex flex-wrap gap-2 mb-3"><StatusBadge status={ticket.status} /><PriorityBadge priority={ticket.priority} />{ticket.categoryName && <span className="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium bg-purple-100 dark:bg-purple-950 text-purple-700 dark:text-purple-300 border border-purple-200 dark:border-purple-800">{ticket.categoryName}</span>}</div>
          {ticket.description && <p className="text-sm text-gray-600 dark:text-gray-300 mt-3 whitespace-pre-wrap">{ticket.description}</p>}
          <div className="grid grid-cols-2 sm:grid-cols-4 gap-3 mt-4 pt-4 border-t border-gray-100 dark:border-gray-800">
            <div><p className="text-xs text-gray-400 dark:text-gray-500">Assignee</p><p className="text-sm text-gray-700 dark:text-gray-300">{ticket.assignedToEmail || 'Unassigned'}</p></div>
            <div><p className="text-xs text-gray-400 dark:text-gray-500">Created</p><p className="text-sm text-gray-700 dark:text-gray-300">{new Date(ticket.createdAt).toLocaleString()}</p></div>
            <div><p className="text-xs text-gray-400 dark:text-gray-500">Updated</p><p className="text-sm text-gray-700 dark:text-gray-300">{new Date(ticket.updatedAt).toLocaleString()}</p></div>
            {ticket.closedAt && <div><p className="text-xs text-gray-400 dark:text-gray-500">Closed</p><p className="text-sm text-gray-700 dark:text-gray-300">{new Date(ticket.closedAt).toLocaleString()}</p></div>}
          </div>
        </div>

        <div className="bg-white dark:bg-gray-900 rounded-xl border border-gray-200 dark:border-gray-700 p-6 mb-4 transition-colors">
          <h3 className="text-sm font-semibold text-gray-700 dark:text-gray-300 mb-4">Conversation</h3>
          <div className="space-y-4">
            {messages.map((msg) => (
              <div key={msg.id} className={`flex gap-3 ${msg.senderType === 'AGENT' || msg.senderType === 'SYSTEM' ? '' : 'flex-row-reverse'}`}>
                <div className="w-8 h-8 rounded-full bg-gray-100 dark:bg-gray-800 flex items-center justify-center text-xs font-semibold text-gray-500 dark:text-gray-400 shrink-0">{(msg.senderEmail || msg.senderType).charAt(0).toUpperCase()}</div>
                <div className={`max-w-[70%] rounded-xl px-4 py-3 ${msg.senderType === 'AGENT' || msg.senderType === 'SYSTEM' ? 'bg-gray-100 dark:bg-gray-800 text-gray-900 dark:text-white' : 'bg-indigo-50 dark:bg-indigo-950 text-gray-900 dark:text-white'}`}>
                  <div className="flex items-center gap-2 mb-1"><span className="text-xs font-medium text-gray-500 dark:text-gray-400">{msg.senderEmail || msg.senderType}</span><span className="text-xs text-gray-400 dark:text-gray-500">{new Date(msg.createdAt).toLocaleTimeString()}</span></div>
                  <p className="text-sm whitespace-pre-wrap">{msg.body}</p>
                </div>
              </div>
            ))}
            {messages.length === 0 && <p className="text-sm text-gray-400 dark:text-gray-500 text-center py-4">No messages yet</p>}
          </div>
        </div>

        {ticket.status !== 'CLOSED' && (
          <div className="bg-white dark:bg-gray-900 rounded-xl border border-gray-200 dark:border-gray-700 p-4 transition-colors">
            <textarea value={reply} onChange={(e) => setReply(e.target.value)} rows={3} placeholder="Type your reply..." maxLength={5000} className={inputClass} />
            <div className="flex justify-end mt-2">
              <button onClick={handleReply} disabled={sending || !reply.trim()} className="px-4 py-2 bg-indigo-600 hover:bg-indigo-700 dark:bg-indigo-500 dark:hover:bg-indigo-600 text-white text-sm font-medium rounded-lg disabled:opacity-50 transition-colors">
                {sending ? 'Sending...' : 'Send Reply'}
              </button>
            </div>
          </div>
        )}
      </div>
    </Layout>
  );
}
