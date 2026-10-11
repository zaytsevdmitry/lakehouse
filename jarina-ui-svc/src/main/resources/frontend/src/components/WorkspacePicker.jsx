import React, { useCallback, useEffect, useState } from 'react';
import { api, workspaceUrl } from '../api.js';
import Modal from './Modal';

export default function WorkspacePicker({ profile, onOpen, onNotice, defaultView }) {
  const [domains, setDomains] = useState([]);
  const [workspaces, setWorkspaces] = useState([]);
  const [selections, setSelections] = useState({});
  const [loading, setLoading] = useState(true);
  const [branchModal, setBranchModal] = useState(false);
  const [working, setWorking] = useState(false);

  const canEdit = profile.effectiveRole === 'EDITOR' || profile.effectiveRole === 'ADMIN';

  const load = useCallback(async () => {
    setLoading(true);
    try {
      const [branchesData, wsData] = await Promise.all([
        api('/api/vcs/branches'),
        api('/api/vcs/workspaces'),
      ]);
      setDomains(Array.isArray(branchesData) ? branchesData : []);
      setWorkspaces(Array.isArray(wsData) ? wsData : []);
    } catch (e) {
      onNotice('error', e.message);
    } finally {
      setLoading(false);
    }
  }, [onNotice]);

  useEffect(() => { load(); }, [load]);

  useEffect(() => {
    setSelections((previous) => {
      const next = {};
      domains.forEach((domain) => {
        const branches = domain.branches || [];
        if (!branches.length) return;
        const current = previous[domain.domain];
        next[domain.domain] = branches.includes(current)
          ? current
          : branches.includes(domain.branchMain) ? domain.branchMain : branches[0];
      });
      return next;
    });
  }, [domains]);

  const openWorkspace = async () => {
    const entries = Object.entries(selections)
      .filter(([, branch]) => !!branch)
      .map(([domain, branch]) => ({ domain, branch }));
    if (entries.length === 0) {
      onNotice('error', 'Select at least one branch to open.');
      return;
    }
    // the tab must be opened synchronously inside the click handler, before the
    // await hands control back to the browser (otherwise pop-up blockers kick in);
    // it is navigated to the workspace deep link once the workspace is created
    const tab = window.open('', '_blank');
    setWorking(true);
    try {
      const ws = await api('/api/vcs/workspace', {
        method: 'POST',
        body: { branches: entries },
      });
      if (tab) tab.location = workspaceUrl(ws.id);
      else onOpen(ws);
    } catch (e) {
      if (tab) tab.close();
      onNotice('error', e.message);
    } finally {
      setWorking(false);
    }
  };

  const createBranch = async ({ domain, branch, baseBranch }) => {
    setWorking(true);
    try {
      await api('/api/vcs/branch', {
        method: 'POST',
        body: { domain, branch, baseBranch },
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

  const deleteWorkspace = async (ws) => {
    const label = (ws.branches || [])
      .map((sel) => `${sel.domain} (${sel.branch})`)
      .join(', ');
    if (!window.confirm(`Delete workspace for "${label}"?`)) return;
    setWorking(true);
    try {
      await api(`/api/vcs/workspace/${encodeURIComponent(ws.id)}`, { method: 'DELETE' });
      onNotice('success', `Deleted workspace "${label}".`);
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

  const entries = Object.entries(selections).filter(([, branch]) => !!branch);

  return (
    <div className="picker">
      <div className="picker-column">
        <div className="picker-head">
          <h2>Open repositories</h2>
          {canEdit && <button onClick={() => setBranchModal(true)}>New branch</button>}
        </div>
        <div className="repository-tree">
          {domains.length === 0 && <p className="muted">No repositories found.</p>}
          {domains.map((domain) => (
            <DomainRow
              key={domain.domain}
              domain={domain}
              selected={(domain.branches || []).includes(selections[domain.domain])
                ? selections[domain.domain]
                : ''}
              onSelect={(branch) =>
                setSelections((prev) => ({ ...prev, [domain.domain]: branch }))
              }
            />
          ))}
          <div className="repository-selection">
            <span className="branch-name">
              {entries.length > 0
                ? entries.map(([domain, branch]) => `${domain} (${branch})`).join(', ')
                : 'Select at least one branch'}
            </span>
            <button
              className="primary"
              disabled={working || entries.length === 0}
              onClick={openWorkspace}
            >
              Open
            </button>
          </div>
        </div>
      </div>

      <div className="picker-column">
        <div className="picker-head">
          <h2>My workspaces</h2>
          <button onClick={load}>Refresh</button>
        </div>
        <div className="workspace-list">
          {workspaces.length === 0 && <p className="muted">Nothing open yet — pick branches to get started.</p>}
          {workspaces.map((ws) => (
            <div className="workspace-row" key={ws.id}>
              <div className="workspace-info">
                <span className="branch-name">
                  {(ws.branches || []).map((sel) => `${sel.domain} (${sel.branch})`).join(', ') || ws.id}
                </span>
                <span className="muted small">
                  owner {ws.owner} · opened{' '}
                  {new Date(ws.createdAt).toLocaleString()} · last used{' '}
                  {new Date(ws.lastAccessedAt).toLocaleString()}
                </span>
              </div>
              <div className="workspace-row-actions">
                <a
                  className="button-link"
                  href={workspaceUrl(ws.id)}
                  target="_blank"
                  rel="noopener noreferrer"
                >
                  Open
                </a>
                <button className="danger" disabled={working} onClick={() => deleteWorkspace(ws)}>Delete</button>
              </div>
            </div>
          ))}
        </div>
      </div>

      {defaultView === 'admin' && (
        <p className="muted">Note: enable the Admin tab via your role profile.</p>
      )}

      {branchModal && (
        <BranchModal
          domains={domains}
          onClose={() => setBranchModal(false)}
          onSubmit={createBranch}
          busy={working}
        />
      )}
    </div>
  );
}

function DomainRow({ domain, selected, onSelect }) {
  const [open, setOpen] = useState(true);
  const branches = domain.branches || [];
  return (
    <div className="repository-node">
      <button
        type="button"
        className="repository-root"
        aria-expanded={open}
        onClick={() => setOpen((value) => !value)}
      >
        <span className="repository-caret" aria-hidden="true">{open ? '▾' : '▸'}</span>
        <span className="repository-name">{domain.domain}</span>
        <span className="repository-count">{branches.length}</span>
      </button>
      {open && (
        <div className="repository-branches">
          {branches.length === 0 && <p className="muted small">No branches found.</p>}
          {branches.map((branch) => (
            <label
              className={`repository-branch${selected === branch ? ' selected' : ''}`}
              key={branch}
            >
              <input
                type="checkbox"
                checked={selected === branch}
                onChange={() => onSelect(selected === branch ? '' : branch)}
              />
              <span className="repository-branch-name">{branch}</span>
              {branch === domain.branchMain && (
                <span className="repository-default">default</span>
              )}
            </label>
          ))}
        </div>
      )}
    </div>
  );
}

function BranchModal({ domains, onClose, onSubmit, busy }) {
  const [branch, setBranch] = useState('');
  const [domain, setDomain] = useState(domains[0]?.domain || '');
  const domainRow = domains.find((d) => d.domain === domain);
  const branches = (domainRow && domainRow.branches) || [];
  const [baseBranch, setBaseBranch] = useState(
    branches.includes(domainRow?.branchMain) ? domainRow.branchMain : branches[0] || 'main'
  );
  const submit = (e) => {
    e.preventDefault();
    if (branch.trim() && domain) onSubmit({ domain, branch: branch.trim(), baseBranch });
  };
  return (
    <Modal title="Create branch" onClose={onClose}>
      <form onSubmit={submit}>
        <div className="field">
          <span className="label">Domain</span>
          <select
            value={domain}
            onChange={(e) => {
              const next = domains.find((d) => d.domain === e.target.value);
              const nextBranches = next?.branches || [];
              setDomain(e.target.value);
              setBaseBranch(nextBranches.includes(next?.branchMain)
                ? next.branchMain
                : nextBranches[0] || 'main');
            }}
          >
            {domains.map((d) => <option key={d.domain} value={d.domain}>{d.domain}</option>)}
          </select>
        </div>
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
          <button type="submit" className="primary" disabled={busy || !branch.trim() || !domain}>Create</button>
          <button type="button" onClick={onClose}>Cancel</button>
        </div>
      </form>
    </Modal>
  );
}