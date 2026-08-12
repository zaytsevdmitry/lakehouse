export async function fetchCatalogTree() {
  const response = await fetch('/api/catalog/tree');
  if (!response.ok) {
    throw new Error(`Failed to load catalog: ${response.status} ${await response.text()}`);
  }
  return response.json();
}

export async function fetchDataSet(keyName) {
  const response = await fetch(`/api/catalog/dataset/${encodeURIComponent(keyName)}`);
  if (!response.ok) {
    throw new Error(`Failed to load data set: ${response.status} ${await response.text()}`);
  }
  return response.json();
}

export async function fetchLineage(keyName) {
  const response = await fetch(`/api/catalog/dataset/${encodeURIComponent(keyName)}/lineage`);
  if (!response.ok) {
    throw new Error(`Failed to load lineage: ${response.status} ${await response.text()}`);
  }
  return response.json();
}

export async function fetchConstraints(keyName) {
  const response = await fetch(`/api/catalog/dataset/${encodeURIComponent(keyName)}/constraints`);
  if (!response.ok) {
    throw new Error(`Failed to load constraints: ${response.status} ${await response.text()}`);
  }
  return response.json();
}

export async function fetchStates(dataSetKeyName, fromDate, toDate) {
  const response = await fetch('/api/states', {
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
  const response = await fetch('/api/schedules', {
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
  const response = await fetch('/api/schedules/headers');
  if (!response.ok) {
    throw new Error(`Failed to load schedule headers: ${response.status} ${await response.text()}`);
  }
  return response.json();
}

export async function fetchScheduleInstanceDAG(id) {
  const response = await fetch(`/api/schedules/dag/${id}`);
  if (!response.ok) {
    throw new Error(`Failed to load schedule instance DAG: ${response.status} ${await response.text()}`);
  }
  return response.json();
}

export async function fetchServices() {
  const response = await fetch('/api/services');
  if (!response.ok) {
    throw new Error(`Failed to load services: ${response.status} ${await response.text()}`);
  }
  return response.json();
}
