export async function fetchCatalogTree() {
  const response = await fetch('/api/catalog/tree');
  if (!response.ok) {
    throw new Error(`Failed to load catalog: ${response.status} ${await response.text()}`);
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
