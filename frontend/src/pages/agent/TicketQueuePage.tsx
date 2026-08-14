import { useState, useEffect, useMemo } from 'react';
import { Link } from 'react-router-dom';
import { api } from '../../api/client';
import { Layout } from '../../components/Layout';
import { StatusBadge, PriorityBadge } from '../../components/Badges';
import type { TicketSummary, TicketStatus, TicketPriority } from '../../types';

export function AgentTicketQueuePage() {
  const [allTickets, setAllTickets] = useState<TicketSummary[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [filterStatus, setFilterStatus] = useState<TicketStatus | ''>('');
  const [filterPriority, setFilterPriority] = useState<TicketPriority | ''>('');

  // Fetch all tickets once for counts
  const fetchAll = async () => {
    setLoading(true);
    setError('');
    try {
      const res = await api.listTickets({ sort: 'createdAt,desc', size: '200' });
      setAllTickets(res.content);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to load tickets');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchAll();
  }, []);

  // Derive counts from ALL tickets (always show totals)
  const counts = useMemo(() => ({
    all: allTickets.length,
    open: allTickets.filter((t) => t.status === 'OPEN').length,
    inProgress: allTickets.filter((t) => t.status === 'IN_PROGRESS').length,
    resolved: allTickets.filter((t) => t.status === 'RESOLVED').length,
    urgent: allTickets.filter((t) => t.priority === 'URGENT').length,
  }), [allTickets]);

  // Filter display tickets client-side
  const displayed = useMemo(() => {
    let list = allTickets;
    if (filterStatus) list = list.filter((t) => t.status === filterStatus);
    if (filterPriority) list = list.filter((t) => t.priority === filterPriority);
    return list;
  }, [allTickets, filterStatus, filterPriority]);

  return (
    <Layout>
      <div className="p-6">
        <h2 className="text-xl font-bold text-gray-900 dark:text-white mb-6">Ticket Queue</h2>

        {/* Quick stats */}
        <div className="grid grid-cols-5 gap-3 mb-6">
          {[
            { label: 'All', value: counts.all, active: filterStatus === '' && filterPriority === '' },
            { label: 'Open', value: counts.open, active: filterStatus === 'OPEN' },
            { label: 'In Progress', value: counts.inProgress, active: filterStatus === 'IN_PROGRESS' },
            { label: 'Resolved', value: counts.resolved, active: filterStatus === 'RESOLVED' },
            { label: 'Urgent', value: counts.urgent, active: filterPriority === 'URGENT' },
          ].map((stat) => (
            <button
              key={stat.label}
              onClick={() => {
                if (stat.label === 'All') { setFilterStatus(''); setFilterPriority(''); }
                else if (stat.label === 'Urgent') { setFilterStatus(''); setFilterPriority('URGENT'); }
                else { setFilterStatus(stat.label.toUpperCase().replace(' ', '_') as TicketStatus); setFilterPriority(''); }
              }}
              className={`rounded-xl border p-4 text-left transition-colors ${
                stat.active
                  ? 'border-indigo-300 dark:border-indigo-600 bg-indigo-50 dark:bg-indigo-950'
                  : 'border-gray-200 dark:border-gray-700 bg-white dark:bg-gray-900 hover:bg-gray-50 dark:hover:bg-gray-800'
              }`}
            >
              <p className="text-2xl font-bold text-gray-900 dark:text-white">{stat.value}</p>
              <p className="text-xs text-gray-500 dark:text-gray-400">{stat.label}</p>
            </button>
          ))}
        </div>

        {error && (
          <div className="text-sm text-red-600 dark:text-red-400 bg-red-50 dark:bg-red-950 border border-red-200 dark:border-red-800 rounded-lg px-3 py-2 mb-4">{error}</div>
        )}

        {loading ? (
          <div className="flex justify-center py-12">
            <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-indigo-600 dark:border-indigo-400" />
          </div>
        ) : (
          <div className="bg-white dark:bg-gray-900 rounded-xl border border-gray-200 dark:border-gray-700 overflow-hidden transition-colors">
            <table className="w-full">
              <thead>
                <tr className="border-b border-gray-200 dark:border-gray-700 bg-gray-50 dark:bg-gray-800">
                  <th className="text-left px-4 py-3 text-xs font-medium text-gray-500 dark:text-gray-400 uppercase">ID</th>
                  <th className="text-left px-4 py-3 text-xs font-medium text-gray-500 dark:text-gray-400 uppercase">Subject</th>
                  <th className="text-left px-4 py-3 text-xs font-medium text-gray-500 dark:text-gray-400 uppercase">Requester</th>
                  <th className="text-left px-4 py-3 text-xs font-medium text-gray-500 dark:text-gray-400 uppercase">Status</th>
                  <th className="text-left px-4 py-3 text-xs font-medium text-gray-500 dark:text-gray-400 uppercase">Priority</th>
                  <th className="text-left px-4 py-3 text-xs font-medium text-gray-500 dark:text-gray-400 uppercase">Assignee</th>
                  <th className="text-left px-4 py-3 text-xs font-medium text-gray-500 dark:text-gray-400 uppercase">Created</th>
                </tr>
              </thead>
              <tbody>
                {displayed.map((t) => (
                  <tr key={t.id} className="border-b border-gray-100 dark:border-gray-800 hover:bg-gray-50 dark:hover:bg-gray-800 transition-colors">
                    <td className="px-4 py-3 text-sm text-gray-500 dark:text-gray-400 font-mono">#{t.id}</td>
                    <td className="px-4 py-3">
                      <Link to={`/agent/tickets/${t.id}`} className="text-sm font-medium text-gray-900 dark:text-white hover:text-indigo-600 dark:hover:text-indigo-400">
                        {t.subject}
                      </Link>
                    </td>
                    <td className="px-4 py-3 text-sm text-gray-600 dark:text-gray-400">{t.requesterEmail || '—'}</td>
                    <td className="px-4 py-3"><StatusBadge status={t.status} /></td>
                    <td className="px-4 py-3"><PriorityBadge priority={t.priority} /></td>
                    <td className="px-4 py-3 text-sm text-gray-600 dark:text-gray-400">{t.assignedToEmail || '—'}</td>
                    <td className="px-4 py-3 text-sm text-gray-500 dark:text-gray-400">{new Date(t.createdAt).toLocaleDateString()}</td>
                  </tr>
                ))}
                {displayed.length === 0 && (
                  <tr>
                    <td colSpan={7} className="px-4 py-12 text-center text-sm text-gray-400 dark:text-gray-500">No tickets found</td>
                  </tr>
                )}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </Layout>
  );
}
