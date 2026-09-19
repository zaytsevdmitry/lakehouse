function getCsrfToken() {
  const match = document.cookie.match(/(?:^|;\s*)XSRF-TOKEN=([^;]*)/);
  return match ? decodeURIComponent(match[1]) : null;
}

export async function apiFetch(url, options = {}) {
  const method = (options.method || 'GET').toUpperCase();
  if (!['GET', 'HEAD', 'OPTIONS', 'TRACE'].includes(method)) {
    const token = getCsrfToken();
    if (token !== null) {
      const headers = new Headers(options.headers || {});
      headers.set('X-XSRF-TOKEN', token);
      options = { ...options, headers };
    }
  }
  return fetch(url, options);
}

export async function fetchCatalogTree() {
  const response = await apiFetch('/api/catalog/tree');
  if (!response.ok) {
    throw new Error(`Failed to load catalog: ${response.status} ${await response.text()}`);
  }
  return response.json();
}

export async function fetchDataSet(keyName) {
  const response = await apiFetch(`/api/catalog/dataset/${encodeURIComponent(keyName)}`);
  if (!response.ok) {
    throw new Error(`Failed to load data set: ${response.status} ${await response.text()}`);
  }
  return response.json();
}

export async function fetchDataSource(keyName) {
  const response = await apiFetch(`/api/catalog/datasource/${encodeURIComponent(keyName)}`);
  if (!response.ok) {
    throw new Error(`Failed to load data source: ${response.status} ${await response.text()}`);
  }
  return response.json();
}

export async function fetchLineage(keyName) {
  const response = await apiFetch(`/api/catalog/dataset/${encodeURIComponent(keyName)}/lineage`);
  if (!response.ok) {
    throw new Error(`Failed to load lineage: ${response.status} ${await response.text()}`);
  }
  return response.json();
}

export async function fetchConstraints(keyName) {
  const response = await apiFetch(`/api/catalog/dataset/${encodeURIComponent(keyName)}/constraints`);
  if (!response.ok) {
    throw new Error(`Failed to load constraints: ${response.status} ${await response.text()}`);
  }
  return response.json();
}

export async function fetchScript(key) {
  const response = await apiFetch(`/api/catalog/script/${encodeURIComponent(key)}`);
  if (!response.ok) {
    throw new Error(`Failed to load script: ${response.status} ${await response.text()}`);
  }
  return response.text();
}

export async function fetchDataSetModelScript(keyName) {
  const response = await apiFetch(`/api/catalog/dataset/${encodeURIComponent(keyName)}/model-script`);
  if (!response.ok) {
    throw new Error(`Failed to load model script: ${response.status} ${await response.text()}`);
  }
  return response.text();
}

export async function fetchStates(dataSetKeyName, fromDate, toDate) {
  const response = await apiFetch('/api/states', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ dataSetKeyName, fromDate, toDate }),
  });
  if (!response.ok) {
    throw new Error(`Failed to load states: ${response.status} ${await response.text()}`);
  }
  return response.json();
}

export async function fetchSchedules(fromDate, toDate, names = []) {
  const response = await apiFetch('/api/schedules', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ fromDate, toDate, names }),
  });
  if (!response.ok) {
    throw new Error(`Failed to load schedules: ${response.status} ${await response.text()}`);
  }
  return response.json();
}

export async function fetchScheduleHeaders() {
  const response = await apiFetch('/api/schedules/headers');
  if (!response.ok) {
    throw new Error(`Failed to load schedule headers: ${response.status} ${await response.text()}`);
  }
  return response.json();
}

export async function fetchScheduleInstanceDAG(id) {
  const response = await apiFetch(`/api/schedules/dag/${id}`);
  if (!response.ok) {
    throw new Error(`Failed to load schedule instance DAG: ${response.status} ${await response.text()}`);
  }
  return response.json();
}

export async function fetchServices() {
  const response = await apiFetch('/api/services');
  if (!response.ok) {
    throw new Error(`Failed to load services: ${response.status} ${await response.text()}`);
  }
  return response.json();
}

export async function fetchServiceEdges() {
  const response = await apiFetch('/api/services/edges');
  if (!response.ok) {
    throw new Error(`Failed to load service edges: ${response.status} ${await response.text()}`);
  }
  return response.json();
}

export async function fetchServiceVertices() {
  const response = await apiFetch('/api/services/vertices');
  if (!response.ok) {
    throw new Error(`Failed to load service vertices: ${response.status} ${await response.text()}`);
  }
  return response.json();
}

function buildQuery(params) {
  const search = new URLSearchParams();
  Object.entries(params).forEach(([key, value]) => {
    if (value !== null && value !== undefined && value !== '') {
      search.append(key, String(value));
    }
  });
  const query = search.toString();
  return query ? `?${query}` : '';
}

export async function fetchSparkSubmissions({ limit, lastId, id, status, dateFrom, dateTo }) {
  const response = await apiFetch(
    `/api/spark-proxy/submissions${buildQuery({ limit, last_id: lastId, id, status, date_from: dateFrom, date_to: dateTo })}`
  );
  if (!response.ok) {
    throw new Error(`Failed to load submissions: ${response.status} ${await response.text()}`);
  }
  return response.json();
}

export async function fetchSparkProperties(id) {
  const response = await apiFetch(`/api/spark-proxy/submissions/${id}/spark-properties`);
  if (!response.ok) {
    throw new Error(`Failed to load spark properties: ${response.status} ${await response.text()}`);
  }
  return response.json();
}

export async function createSparkSubmission(request) {
  const response = await apiFetch('/api/spark-proxy/submissions', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(request),
  });
  if (!response.ok) {
    throw new Error(`Failed to create submission: ${response.status} ${await response.text()}`);
  }
  return response.json();
}

export async function fetchSparkStatus(submissionId) {
  const response = await apiFetch(`/api/spark-proxy/submissions/status/${submissionId}`);
  if (!response.ok) {
    throw new Error(`Failed to load submission status: ${response.status} ${await response.text()}`);
  }
  return response.json();
}

export async function killSparkSubmission(submissionId) {
  const response = await apiFetch(`/api/spark-proxy/submissions/kill/${submissionId}`, { method: 'POST' });
  if (!response.ok) {
    throw new Error(`Failed to kill submission: ${response.status} ${await response.text()}`);
  }
  return response.json();
}

export async function killAllSparkSubmissions() {
  const response = await apiFetch('/api/spark-proxy/submissions/killall', { method: 'POST' });
  if (!response.ok) {
    throw new Error(`Failed to kill all submissions: ${response.status} ${await response.text()}`);
  }
  return response.json();
}

export async function clearSparkCompleted() {
  const response = await apiFetch('/api/spark-proxy/submissions/clear', { method: 'POST' });
  if (!response.ok) {
    throw new Error(`Failed to clear submissions: ${response.status} ${await response.text()}`);
  }
  return response.json();
}

export async function fetchCurrentUser() {
  const response = await apiFetch('/api/user');
  if (!response.ok) {
    throw new Error(`Failed to load current user: ${response.status} ${await response.text()}`);
  }
  return response.json();
}

export async function fetchVcsSyncLogs({ from, to, status, commitId }) {
  const response = await apiFetch(
    `/api/vcs/logs${buildQuery({ from, to, status, commitId })}`
  );
  if (!response.ok) {
    throw new Error(`Failed to load VCS sync log: ${response.status} ${await response.text()}`);
  }
  return response.json();
}

export async function fetchVcsObjectLogs({ commitId, kind, from, to, filePath, objectName }) {
  const response = await apiFetch(
    `/api/vcs/objects${buildQuery({ commitId, kind, from, to, filePath, objectName })}`
  );
  if (!response.ok) {
    throw new Error(`Failed to load VCS object log: ${response.status} ${await response.text()}`);
  }
  return response.json();
}


const LOGIN_URL = '/oauth2/authorization/keycloak';

let loginRedirectPending = false;

/** Sends the user to the Spring Security BFF login entry point. */
export function login() {
  window.location.href = LOGIN_URL;
}

function redirectToLogin() {
  if (loginRedirectPending) return;
  loginRedirectPending = true;
  window.location.href = LOGIN_URL;
}

/**
 * Generic API wrapper for the modelling endpoints.
 * The browser attaches the JSESSIONID cookie automatically; no Bearer header is
 * ever sent. State-changing requests carry the CSRF token read from the
 * XSRF-TOKEN cookie that Spring Security sets for the SPA. A 401 response
 * hands the user over to the login entry point.
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

/** Encodes each path segment individually so slashes inside keys stay intact. */
export function encodePath(path) {
  return String(path).split('/').map(encodeURIComponent).join('/');
}

/**
 * Deep link that opens the Modelling editor for the given workspace.
 * {@code App}/{@code ModellerSection} read these query parameters on load and
 * auto-open the editor, so the link can be used as a new browser tab target.
 */
export function workspaceUrl(workspaceId) {
  const params = new URLSearchParams({ section: 'modeller', workspace: workspaceId });
  return `${window.location.pathname}?${params.toString()}`;
}

export async function logout() {
  await apiFetch('/logout', { method: 'POST' });
  window.location.href = '/';
}

