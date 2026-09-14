import React, { useCallback, useEffect, useState } from 'react';
import { api } from '../api';
import Modal from './Modal';

export default function AdminView({ profile, onNotice }) {
  const [workspaces, setWorkspaces] = useState([]);
  const [ttl, setTtl] = useState(0);
  const [logs, setLogs] = useState([]);
  const [tab, setTab] = useState('workspaces');
  const [busy, setBusy] = useState(false);

  const loadWorkspaces = useCallback(async () => {
    const data = await api('/v1_0/admin/workspaces');
    setWorkspaces(Array.isArray(data) ? data : []);
  }, []);

  const loadTtl = useCallback(async () => {
    setTtl(await api('/v1_0/admin/settings/cleanup-ttl-hours'));
  }, []);

  const loadLogs = useCallback(async () => {
    const data = await api('/v1_0/admin/sync-logs?limit=200');
    setLogs(Array.isArray(data) ? data : []);
  }, []);

  useEffect(() => {
    loadWorkspaces();
    loadTtl();
    loadLogs();
  }, [loadWorkspaces, loadTtl, loadLogs]);

  const forceDelete = async (ws) => {
    if (!window.confirm(`Force-delete workspace for branch "${ws.branch}" (${ws.owner})?`)) return;
    setBusy(true);
    try {
      await api(`/v1_0/admin/workspaces/${encodeURIComponent(ws.id)}`, { method: 'DELETE' });
      onNotice('success', `Deleted workspace ${ws.id}.`);
      await loadWorkspaces();
    } catch (e) {
      onNotice('error', e.message);
    } finally {
      setBusy(false);
    }
  };

  const saveTtl = async (hours) => {
    setBusy(true);
    try {
      const next = await api('/v1_0/admin/settings/cleanup-ttl-hours', {
        method: 'PUT',
        body: { hours },
      });
      setTtl(next);
      onNotice('success', `Cleanup TTL set to ${next} hours.`);
    } catch (e) {
      onNotice('error', e.message);
    } finally {
      setBusy(false);
    }
  };

  return (
    <div className="admin">
      <div className="tabs">
        <button className={`tab${tab === 'workspaces' ? ' tab-active' : ''}`} onClick={() => setTab('workspaces')}>
          All workspaces
        </button>
        <button className={`tab${tab === 'settings' ? ' tab-active' : ''}`} onClick={() => setTab('settings')}>
          Settings
        </button>
        <button className={`tab${tab === 'sync-logs' ? ' tab-active' : ''}`} onClick={() => setTab('sync-logs')}>
          Sync logs
        </button>
      </div>

      {tab === 'workspaces' && (
        <div className="admin-table-wrap">
          <table className="admin-table">
            <thead>
              <tr><th>Branch</th><th>Owner</th><th>Created</th><th>Last used</th><th>Id</th><th></th></tr>
            </thead>
            <tbody>
              {workspaces.map((ws) => (
                <tr key={ws.id}>
                  <td>{ws.branch}</td>
                  <td>{ws.owner}</td>
                  <td>{new Date(ws.createdAt).toLocaleString()}</td>
                  <td>{new Date(ws.lastAccessedAt).toLocaleString()}</td>
                  <td className="mono">{ws.id}</td>
                  <td>
                    <button className="danger small" disabled={busy} onClick={() => forceDelete(ws)}>Delete</button>
                  </td>
                </tr>
              ))}
              {workspaces.length === 0 && (
                <tr><td colSpan="6" className="muted center">No workspaces.</td></tr>
              )}
            </tbody>
          </table>
        </div>
      )}

      {tab === 'settings' && (
        <div className="settings-card">
          <h3>Workspace cleanup</h3>
          <p className="muted">
            Workspaces are deleted when idle for this many hours. Applies to any user's workspaces.
          </p>
          <div className="btn-row">
            <input
              type="number"
              min="1"
              max="8760"
              value={ttl}
              onChange={(e) => setTtl(parseInt(e.target.value, 10) || 0)}
            />
            <button disabled={busy || ttl < 1} onClick={() => saveTtl(ttl)}>Save</button>
          </div>
        </div>
      )}

      {tab === 'sync-logs' && (
        <div className="admin-table-wrap">
          <table className="admin-table">
            <thead>
              <tr><th>Time</th><th>Level</th><th>User</th><th>Action</th><th>Message</th><th>Workspace</th></tr>
            </thead>
            <tbody>
              {logs.map((log, i) => (
                <tr key={i}>
                  <td className="mono small">{new Date(log.timestamp).toLocaleString()}</td>
                  <td><span className={`badge ${log.level === 'ERROR' ? 'admin' : 'viewer'}`}>{log.level}</span></td>
                  <td>{log.user}</td>
                  <td>{log.action}</td>
                  <td>{log.message}</td>
                  <td className="mono small">{log.workspaceId}</td>
                </tr>
              ))}
              {logs.length === 0 && (
                <tr><td colSpan="6" className="muted center">No sync activity recorded.</td></tr>
              )}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}