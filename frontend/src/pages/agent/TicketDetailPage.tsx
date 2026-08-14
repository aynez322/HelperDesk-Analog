import { useState, useEffect } from 'react';
import { useParams, Link } from 'react-router-dom';
import { api } from '../../api/client';
import { Layout } from '../../components/Layout';
import { StatusBadge, PriorityBadge } from '../../components/Badges';
import type { Ticket, Message, TicketStatus, TicketPriority } from '../../types';

const STATUSES: TicketStatus[] = ['OPEN', 'IN_PROGRESS', 'RESOLVED', 'CLOSED'];
const PRIORITIES: TicketPriority[] = ['LOW', 'NORMAL', 'HIGH', 'URGENT'];
const inputClass = 'w-full px-3 py-2 bg-white dark:bg-gray-800 border border-gray-300 dark:border-gray-600 rounded-lg text-sm text-gray-900 dark:text-white focus:outline-none focus:ring-2 focus:ring-indigo-500 dark:focus:ring-indigo-400 focus:border-transparent resize-y placeholder-gray-400 dark:placeholder-gray-500';

export function AgentTicketDetailPage() {
  const { id } = useParams<{ id: string }>();
  const [ticket, setTicket] = useState<Ticket | null>(null);
  const [messages, setMessages] = useState<Message[]>([]);
  const [reply, setReply] = useState('');
  const [loading, setLoading] = useState(true);
  const [sending, setSending] = useState(false);
  const [updating, setUpdating] = useState(false);
  const [error, setError] = useState('');

  const fetchData = async () => {
    if (!id) return;
    setLoading(true);
    try {
      const [t, m] = await Promise.all([api.getTicket(+id), api.getMessages(+id)]);
      setTicket(t); setMessages(m);
    } catch (err) { setError(err instanceof Error ? err.message : 'Failed to load'); }
    finally { setLoading(false); }
  };

  useEffect(() => { fetchData(); }, [id]);

  const handleUpdate = async (updates: { status?: TicketStatus; priority?: TicketPriority; assignedToId?: number }) => {
    if (!id) return;
    setUpdating(true); setError('');
    try { setTicket(await api.updateTicket(+id, updates)); }
    catch (err) { setError(err instanceof Error ? err.message : 'Update failed'); }
    finally { setUpdating(false); }
  };

  const handleReply = async () => {
    if (!id || !reply.trim()) return;
    setSending(true);
    try { const msg = await api.addMessage(+id, { body: reply.trim() }); setMessages((prev) => [...prev, msg]); setReply(''); }
    catch (err) { setError(err instanceof Error ? err.message : 'Failed to send'); }
    finally { setSending(false); }
  };

  if (loading) return <Layout><div className="flex justify-center py-12"><div className="animate-spin rounded-full h-8 w-8 border-b-2 border-indigo-600 dark:border-indigo-400" /></div></Layout>;
  if (!ticket) return <Layout><div className="p-6 text-center text-gray-500 dark:text-gray-400">Ticket not found</div></Layout>;

  return (
    <Layout>
      <div className="p-6">
        <Link to="/agent/tickets" className="text-sm text-indigo-600 dark:text-indigo-400 hover:text-indigo-700 dark:hover:text-indigo-300 mb-4 inline-block">← Back to queue</Link>
        <div className="flex gap-6">
          <div className="flex-1">
            {error && <div className="text-sm text-red-600 dark:text-red-400 bg-red-50 dark:bg-red-950 border border-red-200 dark:border-red-800 rounded-lg px-3 py-2 mb-4">{error}</div>}
            <div className="bg-white dark:bg-gray-900 rounded-xl border border-gray-200 dark:border-gray-700 p-6 mb-4 transition-colors">
              <div className="flex items-start justify-between mb-3"><h1 className="text-lg font-bold text-gray-900 dark:text-white">{ticket.subject}</h1><span className="font-mono text-sm text-gray-400 dark:text-gray-500">#{ticket.id}</span></div>
              <div className="flex flex-wrap gap-2 mb-3"><StatusBadge status={ticket.status} /><PriorityBadge priority={ticket.priority} />{ticket.categoryName && <span className="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium bg-purple-100 dark:bg-purple-950 text-purple-700 dark:text-purple-300 border border-purple-200 dark:border-purple-800">{ticket.categoryName}</span>}</div>
              {ticket.description && <p className="text-sm text-gray-600 dark:text-gray-300 whitespace-pre-wrap">{ticket.description}</p>}
            </div>
            <div className="bg-white dark:bg-gray-900 rounded-xl border border-gray-200 dark:border-gray-700 p-6 mb-4 transition-colors">
              <h3 className="text-sm font-semibold text-gray-700 dark:text-gray-300 mb-4">Conversation</h3>
              <div className="space-y-4">
                {messages.map((msg) => (
                  <div key={msg.id} className={`flex gap-3 ${msg.senderType === 'CLIENT' ? '' : 'flex-row-reverse'}`}>
                    <div className="w-8 h-8 rounded-full bg-gray-100 dark:bg-gray-800 flex items-center justify-center text-xs font-semibold text-gray-500 dark:text-gray-400 shrink-0">{(msg.senderEmail || msg.senderType).charAt(0).toUpperCase()}</div>
                    <div className={`max-w-[70%] rounded-xl px-4 py-3 ${msg.senderType === 'CLIENT' ? 'bg-gray-100 dark:bg-gray-800 text-gray-900 dark:text-white' : 'bg-indigo-50 dark:bg-indigo-950 text-gray-900 dark:text-white'}`}>
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
                <div className="flex justify-end mt-2"><button onClick={handleReply} disabled={sending || !reply.trim()} className="px-4 py-2 bg-indigo-600 hover:bg-indigo-700 dark:bg-indigo-500 dark:hover:bg-indigo-600 text-white text-sm font-medium rounded-lg disabled:opacity-50 transition-colors">{sending ? 'Sending...' : 'Send Reply'}</button></div>
              </div>
            )}
          </div>

          <div className="w-64 shrink-0 space-y-4">
            <div className="bg-white dark:bg-gray-900 rounded-xl border border-gray-200 dark:border-gray-700 p-4 transition-colors">
              <h4 className="text-xs font-semibold text-gray-500 dark:text-gray-400 uppercase tracking-wider mb-3">Status</h4>
              <div className="space-y-1">
                {STATUSES.map((s) => (
                  <button key={s} onClick={() => handleUpdate({ status: s })} disabled={updating || ticket.status === s}
                    className={`w-full text-left px-3 py-2 rounded-lg text-sm transition-colors ${ticket.status === s ? 'bg-indigo-50 dark:bg-indigo-950 text-indigo-700 dark:text-indigo-300 font-medium' : 'text-gray-600 dark:text-gray-400 hover:bg-gray-50 dark:hover:bg-gray-800'}`}>
                    <span className={`inline-block w-2 h-2 rounded-full mr-2 ${s === 'OPEN' ? 'bg-blue-500' : s === 'IN_PROGRESS' ? 'bg-amber-500' : s === 'RESOLVED' ? 'bg-green-500' : 'bg-gray-400'}`} />{s.replace('_', ' ')}
                  </button>
                ))}
              </div>
            </div>
            <div className="bg-white dark:bg-gray-900 rounded-xl border border-gray-200 dark:border-gray-700 p-4 transition-colors">
              <h4 className="text-xs font-semibold text-gray-500 dark:text-gray-400 uppercase tracking-wider mb-3">Priority</h4>
              <div className="space-y-1">
                {PRIORITIES.map((p) => (
                  <button key={p} onClick={() => handleUpdate({ priority: p })} disabled={updating || ticket.priority === p}
                    className={`w-full text-left px-3 py-2 rounded-lg text-sm transition-colors ${ticket.priority === p ? 'bg-indigo-50 dark:bg-indigo-950 text-indigo-700 dark:text-indigo-300 font-medium' : 'text-gray-600 dark:text-gray-400 hover:bg-gray-50 dark:hover:bg-gray-800'}`}>{p.charAt(0) + p.slice(1).toLowerCase()}</button>
                ))}
              </div>
            </div>
            <div className="bg-white dark:bg-gray-900 rounded-xl border border-gray-200 dark:border-gray-700 p-4 transition-colors">
              <h4 className="text-xs font-semibold text-gray-500 dark:text-gray-400 uppercase tracking-wider mb-3">Details</h4>
              <div className="space-y-3 text-sm">
                <div><p className="text-xs text-gray-400 dark:text-gray-500">Assignee</p><p className="text-gray-700 dark:text-gray-300">{ticket.assignedToEmail || 'Unassigned'}</p></div>
                <div><p className="text-xs text-gray-400 dark:text-gray-500">Requester</p><p className="text-gray-700 dark:text-gray-300">{ticket.requesterEmail || ticket.createdByEmail || '—'}</p></div>
                <div><p className="text-xs text-gray-400 dark:text-gray-500">Created</p><p className="text-gray-700 dark:text-gray-300">{new Date(ticket.createdAt).toLocaleString()}</p></div>
                <div><p className="text-xs text-gray-400 dark:text-gray-500">Updated</p><p className="text-gray-700 dark:text-gray-300">{new Date(ticket.updatedAt).toLocaleString()}</p></div>
                {ticket.closedAt && <div><p className="text-xs text-gray-400 dark:text-gray-500">Closed</p><p className="text-gray-700 dark:text-gray-300">{new Date(ticket.closedAt).toLocaleString()}</p></div>}
              </div>
            </div>
          </div>
        </div>
      </div>
    </Layout>
  );
}
