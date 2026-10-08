import React, { useCallback, useEffect, useState } from 'react';
import { api, fetchCurrentUser } from '../api.js';
import WorkspacePicker from './WorkspacePicker.jsx';
import EditorView from './EditorView.jsx';
import AdminView from './AdminView.jsx';

/**
 * Modelling section of the lakehouse UI.
 *
 * Rendered inside the existing app shell, it reuses the shared Spring Security
 * session, CSRF handling and the /api/user identity profile, so it does not
 * manage login/logout or its own theme.
 */
export default function ModellerSection({ initialWorkspaceId }) {
  const [profile, setProfile] = useState(null);
  const [openWorkspace, setOpenWorkspace] = useState(null);
  const [adminTab, setAdminTab] = useState('workspaces');
  const [notice, setNotice] = useState(null);
  const [autoOpenResolved, setAutoOpenResolved] = useState(false);

  const showNotice = useCallback((kind, message) => {
    setNotice({ kind, message });
    window.setTimeout(() => setNotice(null), 6000);
  }, []);

  useEffect(() => {
    let cancelled = false;
    fetchCurrentUser()
      .then((me) => {
        if (!cancelled) setProfile(me);
      })
      .catch((e) => {
        if (!cancelled) showNotice('error', e.message);
      });
    return () => {
      cancelled = true;
    };
  }, [showNotice]);

  useEffect(() => {
    if (!initialWorkspaceId || !profile || autoOpenResolved) return undefined;
    let cancelled = false;
    (async () => {
      try {
        const list = await api('/api/vcs/workspaces');
        if (cancelled) return;
        const workspace = (Array.isArray(list) ? list : [])
          .find((candidate) => candidate.id === initialWorkspaceId);
        if (workspace) setOpenWorkspace(workspace);
        else showNotice('error', 'Workspace not found or no longer available.');
      } catch (e) {
        if (!cancelled) showNotice('error', e.message);
      } finally {
        if (!cancelled) setAutoOpenResolved(true);
      }
    })();
    return () => {
      cancelled = true;
    };
  }, [initialWorkspaceId, profile, autoOpenResolved, showNotice]);

  if (!profile) {
    return (
      <div className="modeller">
        <div className="center-screen">
          <span className="spinner" />
          <span className="muted">Loading…</span>
        </div>
      </div>
    );
  }

  const isAdmin = profile.effectiveRole === 'ADMIN';

  return (
    <div className="modeller">
      {notice && <div className={`banner ${notice.kind} fixed-notice`}>{notice.message}</div>}
      <main className="content">
        {openWorkspace ? (
          <EditorView
            profile={profile}
            workspace={openWorkspace}
            onBack={() => setOpenWorkspace(null)}
            onNotice={showNotice}
          />
        ) : isAdmin ? (
          <div className="tabbed">
            <div className="tabs">
              <button
                className={`tab ${adminTab === 'workspaces' ? 'tab-active' : ''}`}
                onClick={() => setAdminTab('workspaces')}
              >
                My workspaces
              </button>
              <button
                className={`tab ${adminTab === 'admin' ? 'tab-active' : ''}`}
                onClick={() => setAdminTab('admin')}
              >
                Admin
              </button>
            </div>
            {adminTab === 'workspaces' ? (
              <WorkspacePicker
                profile={profile}
                defaultView="workspaces"
                onOpen={(ws) => setOpenWorkspace(ws)}
                onNotice={showNotice}
              />
            ) : (
              <AdminView profile={profile} onNotice={showNotice} />
            )}
          </div>
        ) : (
          <WorkspacePicker
            profile={profile}
            defaultView="workspaces"
            onOpen={(ws) => setOpenWorkspace(ws)}
            onNotice={showNotice}
          />
        )}
      </main>
    </div>
  );
}