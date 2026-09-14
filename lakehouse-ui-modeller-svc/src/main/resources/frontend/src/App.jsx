import React, { useCallback, useEffect, useRef, useState } from 'react';
import { api, login, logout } from './api';
import LoginView from './components/LoginView';
import WorkspacePicker from './components/WorkspacePicker';
import EditorView from './components/EditorView';
import AdminView from './components/AdminView';

const THEME_KEY = 'lakehouse-modeller-theme';
const INACTIVITY_MINUTES = 30;

function getInitialTheme() {
  try {
    return localStorage.getItem(THEME_KEY) || 'light';
  } catch (e) {
    return 'light';
  }
}

export default function App() {
  const [profile, setProfile] = useState(null);
  const [busy, setBusy] = useState(true);
  const [notice, setNotice] = useState(null);
  const [openWorkspace, setOpenWorkspace] = useState(null);
  const [theme, setTheme] = useState(getInitialTheme);

  const showNotice = useCallback((kind, message) => {
    setNotice({ kind, message });
    window.setTimeout(() => setNotice(null), 6000);
  }, []);

  // Wall-clock of the last user interaction; drives the inactivity timeout.
  const lastActivity = useRef(Date.now());

  useEffect(() => {
    const bump = () => { lastActivity.current = Date.now(); };
    const events = ['pointermove', 'keydown', 'click', 'wheel', 'touchstart'];
    for (const name of events) window.addEventListener(name, bump, { passive: true });
    return () => {
      for (const name of events) window.removeEventListener(name, bump);
    };
  }, []);

  useEffect(() => {
    document.documentElement.setAttribute('data-theme', theme);
    try {
      localStorage.setItem(THEME_KEY, theme);
    } catch (e) {
      /* ignore */
    }
  }, [theme]);

  const toggleTheme = () => {
    setTheme((current) => (current === 'dark' ? 'light' : 'dark'));
  };

  useEffect(() => {
    (async () => {
      try {
        // In the BFF mode the session is a server-side JSESSIONID cookie; the
        // identity profile is resolved through the same cookie. A 401 response
        // is handled inside api() and redirects to /oauth2/authorization/keycloak.
        const me = await api('/v1_0/auth/me');
        if (me) setProfile(me);
      } catch (e) {
        if (e.message && e.message.includes('Session expired')) return; // redirect started
        showNotice('error', e.message);
      } finally {
        setBusy(false);
      }
    })();
  }, [showNotice]);

  // Inactivity timeout: the backend owns the session lifetime, so the frontend
  // only hands the user over to the Spring Security logout endpoint when they
  // have been idle for the configured window.
  useEffect(() => {
    if (!profile) return undefined;
    const inactiveMs = INACTIVITY_MINUTES * 60000;
    const timer = window.setInterval(async () => {
      if (Date.now() - lastActivity.current >= inactiveMs) {
        await logout();
      }
    }, 60000);
    return () => window.clearInterval(timer);
  }, [profile]);

  if (busy) {
    return <div className="center-screen"><span className="spinner" /><span className="muted">Loading…</span></div>;
  }

  return (
    <div className="app">
      {notice && <div className={`banner ${notice.kind} fixed-notice`}>{notice.message}</div>}
      {profile && (
        <header className="topbar">
          <div className="brand">Lakehouse Modeller</div>
          <div className="topbar-right">
            <span className="muted">{profile.name} ({profile.username})</span>
            <span className={`badge ${profile.effectiveRole?.toLowerCase()}`}>{profile.effectiveRole || 'none'}</span>
            <button className="theme-toggle" onClick={toggleTheme}>
              {theme === 'dark' ? 'Day mode' : 'Night mode'}
            </button>
            <button onClick={logout}>Sign out</button>
          </div>
        </header>
      )}
      <main className="content">
        {!profile ? (
          <LoginView onLogin={login} />
        ) : openWorkspace ? (
          <EditorView
            profile={profile}
            workspace={openWorkspace}
            onBack={() => setOpenWorkspace(null)}
            onNotice={(kind, message) => showNotice(kind, message)}
          />
        ) : profile.effectiveRole === 'ADMIN' ? (
          <div className="tabbed">
            <div className="tabs">
              <button className="tab" onClick={() => setOpenWorkspace(null)}>My workspaces</button>
              <button className="tab tab-active" onClick={() => setOpenWorkspace(null)}>Admin</button>
            </div>
            <AdminView profile={profile} onNotice={(k, m) => showNotice(k, m)} />
          </div>
        ) : (
          <WorkspacePicker
            profile={profile}
            defaultView="workspaces"
            onOpen={(ws) => setOpenWorkspace(ws)}
            onNotice={(k, m) => showNotice(k, m)}
          />
        )}
      </main>
    </div>
  );
}