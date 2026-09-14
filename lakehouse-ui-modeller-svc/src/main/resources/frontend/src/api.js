const LOGIN_URL = '/oauth2/authorization/keycloak';

let loginRedirectPending = false;

function redirectToLogin() {
  if (loginRedirectPending) return;
  loginRedirectPending = true;
  window.location.href = LOGIN_URL;
}

function getCsrfToken() {
  const match = document.cookie.match(/(?:^|;\s*)XSRF-TOKEN=([^;]*)/);
  return match ? decodeURIComponent(match[1]) : null;
}

/**
 * Minimal fetch wrapper for /v1_0 endpoints in the BFF mode.
 * The browser attaches the JSESSIONID cookie automatically; no Bearer header is
 * ever sent. State-changing requests carry the CSRF token read from the
 * XSRF-TOKEN cookie that Spring Security sets for the SPA.
 */
export async function api(path, { method = 'GET', body } = {}) {
  const headers = {};
  if (body !== undefined) headers['Content-Type'] = 'application/json';
  const verb = String(method).toUpperCase();
  if (verb !== 'GET' && verb !== 'HEAD' && verb !== 'OPTIONS') {
    const csrf = getCsrfToken();
    if (csrf !== null) headers['X-XSRF-TOKEN'] = csrf;
  }
  const response = await fetch(path, {
    method: verb,
    headers,
    body: body !== undefined ? JSON.stringify(body) : undefined,
  });

  if (response.status === 401) {
    redirectToLogin();
    throw new Error('Session expired, please sign in again.');
  }

  if (response.status === 204) return null;

  let payload = null;
  const contentType = response.headers.get('content-type') || '';
  if (contentType.includes('application/json')) {
    payload = await response.json().catch(() => null);
  }

  if (!response.ok) {
    const message = payload && payload.error ? payload.error : `Request failed (${response.status})`;
    throw new Error(message);
  }
  return payload;
}

/** Sends the user to the Spring Security BFF login entry point. */
export function login() {
  window.location.href = LOGIN_URL;
}

/** Sends the user to the Spring Security logout endpoint, then back to the app. */
export async function logout() {
  try {
    await api('/logout', { method: 'POST' });
  } catch (e) {
    // The session may already be gone; always return to the app entry point.
  }
  window.location.href = '/';
}

export function encodePath(path) {
  return String(path).split('/').map(encodeURIComponent).join('/');
}