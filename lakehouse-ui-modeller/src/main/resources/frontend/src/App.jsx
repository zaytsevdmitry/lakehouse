import React, { useCallback, useEffect, useRef, useState } from 'react';
import { api } from './api';
import {
  buildAuthorizeUrl, clearSession, exchangeCode, loadSession, parseCallback, refreshAccessToken,
  saveSession, sessionFromTokens,
} from './auth';
import LoginView from './components/LoginView';
import WorkspacePicker from './components/WorkspacePicker';
import EditorView from './components/EditorView';
import AdminView from './components/AdminView';

const THEME_KEY = 'lakehouse-modeller-theme';

function getInitialTheme() {
  try {
    return localStorage.getItem(THEME_KEY) || 'light';
  } catch (e) {
    return 'light';
  }
}

export default function App() {
  const [config, setConfig] = useState(null);
  const [session, setSession] = useState(() => loadSession());
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

  const signOut = useCallback(() => {
    clearSession();
    setSession(null);
    setProfile(null);
    setOpenWorkspace(null);
  }, []);

  const refreshProfile = useCallback(async (token) => {
    const me = await api('/v1_0/auth/me', { token });
    setProfile(me);
    return me;
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
        const cfg = await api('/v1_0/auth/config');
        setConfig(cfg);

        const callback = parseCallback(window.location.href);
        if (callback.code) {
          const tokens = await exchangeCode(cfg, callback.code, callback.redirectUri, callback.state);
          const fresh = sessionFromTokens(tokens);
          saveSession(fresh);
          setSession(fresh);
          window.history.replaceState({}, '', '/');
          const me = await refreshProfile(fresh.accessToken);
          if (me) {
            setOpenWorkspace(null);
          }
        } else if (session) {
          try {
            await refreshProfile(session.accessToken);
          } catch (e) {
            clearSession();
            setSession(null);
            setProfile(null);
          }
        } else if (callback.error) {
          showNotice('error', `Authentication failed: ${callback.error}`);
        } else {
          // No session and the URL carries no OAuth callback: redirect straight
          // to the Keycloak login page instead of showing a Sign-in button.
          const url = await buildAuthorizeUrl(cfg);
          window.location.href = url;
          return;
        }
      } catch (e) {
        showNotice('error', e.message);
      } finally {
        setBusy(false);
      }
    })();
  }, []); // eslint-disable-line react-hooks/exhaustive-deps

  useEffect(() => {
    const handler = () => {
      setSession(null);
      setProfile(null);
      setOpenWorkspace(null);
    };
    window.addEventListener('lakehouse:session-expired', handler);
    return () => window.removeEventListener('lakehouse:session-expired', handler);
  }, []);

  // Session lifecycle: the access token is refreshed silently while the user
  // is active (Keycloak tokens last about five minutes); when the user stays
  // idle longer than the configured inactivity window the session is closed.
  useEffect(() => {
    if (!config || !profile || !session) return undefined;
    const minutes = config.inactivityMinutes || 30;
    const inactiveMs = minutes * 60000;
    const timer = window.setInterval(async () => {
      if (Date.now() - lastActivity.current >= inactiveMs) {
        signOut();
        showNotice('info', `Signed out after ${minutes} min of inactivity.`);
        return;
      }
      if (session.expiresAt - Date.now() < 120000) {
        try {
          if (!session.refreshToken) throw new Error('refresh token unavailable');
          const tokens = await refreshAccessToken(config, session.refreshToken);
          const fresh = sessionFromTokens(tokens);
          saveSession(fresh);
          setSession(fresh);
        } catch (e) {
          signOut();
          showNotice('error', 'Your session expired. Please sign in again.');
        }
      }
    }, 60000);
    return () => window.clearInterval(timer);
  }, [config, profile, session, signOut, showNotice]);

  const startLogin = async () => {
    setBusy(true);
    try {
      const url = await buildAuthorizeUrl(config);
      window.location.href = url;
    } catch (e) {
      showNotice('error', e.message);
      setBusy(false);
    }
  };

  const logout = signOut;

  if (busy) {
    return <div className="center-screen"><span className="spinner" /><span className="muted">Loading…</span></div>;
  }

  return (
    <div className="app">
      {notice && <div className={`banner ${notice.kind} fixed-notice`}>{notice.message}</div>}
      {profile && (
        <header className="topbar">
          <div className="brand">Lakehouse Configurator</div>
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
          <LoginView config={config} onLogin={startLogin} />
        ) : openWorkspace ? (
          <EditorView
            session={session}
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
            <AdminView session={session} profile={profile} onNotice={(k, m) => showNotice(k, m)} />
          </div>
        ) : (
          <WorkspacePicker
            session={session}
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