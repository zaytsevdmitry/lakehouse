import { clearSession } from './auth';

/**
 * Minimal authenticated fetch wrapper for /v1_0 endpoints.
 * Attaches the Bearer token, parses JSON, normalises errors to Error with a message.
 */
export async function api(path, { method = 'GET', body, token } = {}) {
  const headers = {};
  if (body !== undefined) headers['Content-Type'] = 'application/json';
  if (token) headers['Authorization'] = `Bearer ${token}`;
  const response = await fetch(path, {
    method,
    headers,
    body: body !== undefined ? JSON.stringify(body) : undefined,
  });

  if (response.status === 401) {
    clearSession();
    window.dispatchEvent(new CustomEvent('lakehouse:session-expired'));
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

export function encodePath(path) {
  return String(path).split('/').map(encodeURIComponent).join('/');
}