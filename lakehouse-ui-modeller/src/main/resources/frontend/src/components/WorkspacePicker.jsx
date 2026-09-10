import React, { useCallback, useEffect, useState } from 'react';
import { api } from '../api';
import Modal from './Modal';

export default function WorkspacePicker({ session, profile, onOpen, onNotice, defaultView }) {
  const [branches, setBranches] = useState([]);
  const [workspaces, setWorkspaces] = useState([]);
  const [loading, setLoading] = useState(true);
  const [branchModal, setBranchModal] = useState(false);
  const [working, setWorking] = useState(false);

  const canEdit = profile.effectiveRole === 'EDITOR' || profile.effectiveRole === 'ADMIN';
  const token = session?.accessToken;

  const load = useCallback(async () => {
    setLoading(true);
    try {
      const [branchesData, wsData] = await Promise.all([
        api('/v1_0/vcs/branches', { token }),
        api('/v1_0/vcs/workspaces', { token }),
      ]);
      setBranches(Array.isArray(branchesData) ? branchesData : []);
      setWorkspaces(Array.isArray(wsData) ? wsData : []);
    } catch (e) {
      onNotice('error', e.message);
    } finally {
      setLoading(false);
    }
  }, [token, onNotice]);

  useEffect(() => { load(); }, [load]);

  const openBranch = async (branch) => {
    setWorking(true);
    try {
      const ws = await api('/v1_0/vcs/workspace', {
        method: 'POST',
        token,
        body: { branch, files: [] },
      });
      onOpen(ws);
    } catch (e) {
      onNotice('error', e.message);
    } finally {
      setWorking(false);
    }
  };

  const createBranch = async ({ branch, baseBranch }) => {
    setWorking(true);
    try {
      await api('/v1_0/vcs/branch', {
        method: 'POST',
        token,
        body: { branch, baseBranch },
      });
      setBranchModal(false);
      onNotice('success', `Branch "${branch}" created.`);
      await load();
    } catch (e) {
      onNotice('error', e.message);
    } finally {
      setWorking(false);
    }
  };

  if (loading) {
    return <div className="stage"><span className="spinner" /><span className="muted">Loading workspaces…</span></div>;
  }

  return (
    <div className="picker">
      <div className="picker-column">
        <div className="picker-head">
          <h2>Open a branch</h2>
          {canEdit && <button onClick={() => setBranchModal(true)}>New branch</button>}
        </div>
        <div className="branch-list">
          {branches.length === 0 && <p className="muted">No branches found.</p>}
          {branches.map((branch) => (
            <div className="branch-row" key={branch}>
              <span className="branch-name">{branch}</span>
              <button
                className="primary"
                disabled={working}
                onClick={() => openBranch(branch)}
              >
                Open
              </button>
            </div>
          ))}
        </div>
      </div>

      <div className="picker-column">
        <div className="picker-head">
          <h2>My workspaces</h2>
          <button onClick={load}>Refresh</button>
        </div>
        <div className="workspace-list">
          {workspaces.length === 0 && <p className="muted">Nothing open yet — open a branch to get started.</p>}
          {workspaces.map((ws) => (
            <div className="workspace-row" key={ws.id}>
              <div className="workspace-info">
                <span className="branch-name">{ws.branch}</span>
                <span className="muted small">
                  owner {ws.owner} · opened{' '}
                  {new Date(ws.createdAt).toLocaleString()} · last used{' '}
                  {new Date(ws.lastAccessedAt).toLocaleString()}
                </span>
              </div>
              <button onClick={() => onOpen(ws)}>Open</button>
            </div>
          ))}
        </div>
      </div>

      {defaultView === 'admin' && (
        <p className="muted">Note: enable the Admin tab via your role profile.</p>
      )}

      {branchModal && (
        <BranchModal
          branches={branches}
          onClose={() => setBranchModal(false)}
          onSubmit={createBranch}
          busy={working}
        />
      )}
    </div>
  );
}

function BranchModal({ branches, onClose, onSubmit, busy }) {
  const [branch, setBranch] = useState('');
  const [baseBranch, setBaseBranch] = useState(branches[0] || 'main');
  const submit = (e) => {
    e.preventDefault();
    if (branch.trim()) onSubmit({ branch: branch.trim(), baseBranch });
  };
  return (
    <Modal title="Create branch" onClose={onClose}>
      <form onSubmit={submit}>
        <div className="field">
          <span className="label">Branch name</span>
          <input value={branch} onChange={(e) => setBranch(e.target.value)} placeholder="feature/my-change" autoFocus />
        </div>
        <div className="field">
          <span className="label">Base branch</span>
          <select value={baseBranch} onChange={(e) => setBaseBranch(e.target.value)}>
            {branches.map((b) => <option key={b} value={b}>{b}</option>)}
          </select>
        </div>
        <div className="btn-row">
          <button type="submit" className="primary" disabled={busy || !branch.trim()}>Create</button>
          <button type="button" onClick={onClose}>Cancel</button>
        </div>
      </form>
    </Modal>
  );
}