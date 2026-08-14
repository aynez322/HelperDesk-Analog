import { useState, useEffect, type FormEvent } from 'react';
import { api } from '../../api/client';
import { Layout } from '../../components/Layout';
import type { User } from '../../types';

const fieldClass = 'w-full px-3 py-2 bg-white dark:bg-gray-800 border border-gray-300 dark:border-gray-600 rounded-lg text-sm text-gray-900 dark:text-white focus:outline-none focus:ring-2 focus:ring-indigo-500 dark:focus:ring-indigo-400 focus:border-transparent';

export function UserAdminPage() {
  const [users, setUsers] = useState<User[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const [showCreate, setShowCreate] = useState(false);
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [fullName, setFullName] = useState('');
  const [role, setRole] = useState('AGENT');

  const fetchUsers = async () => {
    setLoading(true);
    try { setUsers(await api.listUsers()); }
    catch (err) { setError(err instanceof Error ? err.message : 'Failed to load users'); }
    finally { setLoading(false); }
  };

  useEffect(() => { fetchUsers(); }, []);

  const handleCreate = async (e: FormEvent) => {
    e.preventDefault(); setError('');
    try { await api.createUser({ email, password, fullName, role }); setSuccess('User created'); setShowCreate(false); setEmail(''); setPassword(''); setFullName(''); setRole('AGENT'); fetchUsers(); }
    catch (err) { setError(err instanceof Error ? err.message : 'Failed to create user'); }
  };

  const handleChangeRole = async (userId: number, newRole: string) => {
    setError('');
    try { await api.changeUserRole(userId, newRole); setSuccess('Role updated'); fetchUsers(); }
    catch (err) { setError(err instanceof Error ? err.message : 'Failed to change role'); }
  };

  return (
    <Layout>
      <div className="p-6">
        <div className="flex items-center justify-between mb-6">
          <h2 className="text-xl font-bold text-gray-900 dark:text-white">User Management</h2>
          <button onClick={() => setShowCreate(!showCreate)} className="px-4 py-2.5 bg-indigo-600 hover:bg-indigo-700 dark:bg-indigo-500 dark:hover:bg-indigo-600 text-white text-sm font-medium rounded-lg transition-colors">{showCreate ? 'Cancel' : 'Add User'}</button>
        </div>

        {error && <div className="text-sm text-red-600 dark:text-red-400 bg-red-50 dark:bg-red-950 border border-red-200 dark:border-red-800 rounded-lg px-3 py-2 mb-4">{error}</div>}
        {success && <div className="text-sm text-green-600 dark:text-green-400 bg-green-50 dark:bg-green-950 border border-green-200 dark:border-green-800 rounded-lg px-3 py-2 mb-4">{success}</div>}

        {showCreate && (
          <form onSubmit={handleCreate} className="bg-white dark:bg-gray-900 rounded-xl border border-gray-200 dark:border-gray-700 p-6 mb-6 space-y-4 transition-colors">
            <h3 className="text-sm font-semibold text-gray-700 dark:text-gray-300">Create New User</h3>
            <div className="grid grid-cols-2 gap-4">
              <div><label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">Full Name</label><input type="text" value={fullName} onChange={(e) => setFullName(e.target.value)} required className={fieldClass} /></div>
              <div><label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">Email</label><input type="email" value={email} onChange={(e) => setEmail(e.target.value)} required className={fieldClass} /></div>
              <div><label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">Password</label><input type="password" value={password} onChange={(e) => setPassword(e.target.value)} required minLength={6} className={fieldClass} /></div>
              <div><label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">Role</label>
                <select value={role} onChange={(e) => setRole(e.target.value)} className={fieldClass}>
                  <option value="CLIENT">Client</option><option value="AGENT">Agent</option><option value="MANAGER">Manager</option>
                </select>
              </div>
            </div>
            <button type="submit" className="px-4 py-2 bg-indigo-600 hover:bg-indigo-700 dark:bg-indigo-500 dark:hover:bg-indigo-600 text-white text-sm font-medium rounded-lg transition-colors">Create User</button>
          </form>
        )}

        {loading ? (
          <div className="flex justify-center py-12"><div className="animate-spin rounded-full h-8 w-8 border-b-2 border-indigo-600 dark:border-indigo-400" /></div>
        ) : (
          <div className="bg-white dark:bg-gray-900 rounded-xl border border-gray-200 dark:border-gray-700 overflow-hidden transition-colors">
            <table className="w-full">
              <thead>
                <tr className="border-b border-gray-200 dark:border-gray-700 bg-gray-50 dark:bg-gray-800">
                  <th className="text-left px-4 py-3 text-xs font-medium text-gray-500 dark:text-gray-400 uppercase">ID</th>
                  <th className="text-left px-4 py-3 text-xs font-medium text-gray-500 dark:text-gray-400 uppercase">Name</th>
                  <th className="text-left px-4 py-3 text-xs font-medium text-gray-500 dark:text-gray-400 uppercase">Email</th>
                  <th className="text-left px-4 py-3 text-xs font-medium text-gray-500 dark:text-gray-400 uppercase">Role</th>
                  <th className="text-left px-4 py-3 text-xs font-medium text-gray-500 dark:text-gray-400 uppercase">Status</th>
                  <th className="text-left px-4 py-3 text-xs font-medium text-gray-500 dark:text-gray-400 uppercase">Actions</th>
                </tr>
              </thead>
              <tbody>
                {users.map((u) => (
                  <tr key={u.id} className="border-b border-gray-100 dark:border-gray-800 hover:bg-gray-50 dark:hover:bg-gray-800">
                    <td className="px-4 py-3 text-sm text-gray-500 dark:text-gray-400 font-mono">#{u.id}</td>
                    <td className="px-4 py-3 text-sm font-medium text-gray-900 dark:text-white">{u.fullName}</td>
                    <td className="px-4 py-3 text-sm text-gray-600 dark:text-gray-400">{u.email}</td>
                    <td className="px-4 py-3"><span className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium border ${u.role === 'MANAGER' ? 'bg-purple-100 dark:bg-purple-950 text-purple-700 dark:text-purple-300 border-purple-200 dark:border-purple-800' : u.role === 'AGENT' ? 'bg-indigo-100 dark:bg-indigo-950 text-indigo-700 dark:text-indigo-300 border-indigo-200 dark:border-indigo-800' : 'bg-gray-100 dark:bg-gray-800 text-gray-600 dark:text-gray-400 border-gray-200 dark:border-gray-700'}`}>{u.role}</span></td>
                    <td className="px-4 py-3"><span className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium ${u.active ? 'bg-green-100 dark:bg-green-950 text-green-700 dark:text-green-300' : 'bg-red-100 dark:bg-red-950 text-red-700 dark:text-red-300'}`}>{u.active ? 'Active' : 'Inactive'}</span></td>
                    <td className="px-4 py-3">
                      <select value={u.role} onChange={(e) => handleChangeRole(u.id, e.target.value)} className="text-xs border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-800 text-gray-900 dark:text-white rounded-lg px-2 py-1 focus:outline-none focus:ring-2 focus:ring-indigo-500 dark:focus:ring-indigo-400">
                        <option value="CLIENT">Client</option><option value="AGENT">Agent</option><option value="MANAGER">Manager</option>
                      </select>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </Layout>
  );
}
