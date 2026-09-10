const SESSION_KEY = 'lakehouse.modeller.session.v1';
const PKCE_KEY = 'lakehouse.modeller.pkce.v1'; // session prefix used to namespace state-keyed verifiers

function verifierKey(state) { return PKCE_KEY + '.' + state; }

export function decodeJwt(token) {
  try {
    const payload = token.split('.')[1];
    const normalized = payload.replace(/-/g, '+').replace(/_/g, '/');
    return JSON.parse(decodeURIComponent(
      atob(normalized).split('').map((c) =>
        '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2),
      ).join(''),
    ));
  } catch (e) {
    return {};
  }
}

function b64url(bytes) {
  return btoa(String.fromCharCode(...bytes))
    .replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/, '');
}

export function randomString(length = 32) {
  const bytes = new Uint8Array(length);
  crypto.getRandomValues(bytes);
  return b64url(bytes);
}

const SHA256_K = [
  0x428a2f98, 0x71374491, 0xb5c0fbcf, 0xe9b5dba5, 0x3956c25b, 0x59f111f1, 0x923f82a4, 0xab1c5ed5,
  0xd807aa98, 0x12835b01, 0x243185be, 0x550c7dc3, 0x72be5d74, 0x80deb1fe, 0x9bdc06a7, 0xc19bf174,
  0xe49b69c1, 0xefbe4786, 0x0fc19dc6, 0x240ca1cc, 0x2de92c6f, 0x4a7484aa, 0x5cb0a9dc, 0x76f988da,
  0x983e5152, 0xa831c66d, 0xb00327c8, 0xbf597fc7, 0xc6e00bf3, 0xd5a79147, 0x06ca6351, 0x14292967,
  0x27b70a85, 0x2e1b2138, 0x4d2c6dfc, 0x53380d13, 0x650a7354, 0x766a0abb, 0x81c2c92e, 0x92722c85,
  0xa2bfe8a1, 0xa81a664b, 0xc24b8b70, 0xc76c51a3, 0xd192e819, 0xd6990624, 0xf40e3585, 0x106aa070,
  0x19a4c116, 0x1e376c08, 0x2748774c, 0x34b0bcb5, 0x391c0cb3, 0x4ed8aa4a, 0x5b9cca4f, 0x682e6ff3,
  0x748f82ee, 0x78a5636f, 0x84c87814, 0x8cc70208, 0x90befffa, 0xa4506ceb, 0xbef9a3f7, 0xc67178f2,
];

function rotr32(value, shift) {
  return (value >>> shift) | (value << (32 - shift));
}

function sha256(bytes) {
  const H = [0x6a09e667, 0xbb67ae85, 0x3c6ef372, 0xa54ff53a,
    0x510e527f, 0x9b05688c, 0x1f83d9ab, 0x5be0cd19];
  const bitLen = bytes.length * 8;
  const padded = new Uint8Array(((bytes.length + 8) >> 6 << 6) + 64);
  padded.set(bytes);
  padded[bytes.length] = 0x80;
  const hi = Math.floor(bitLen / 0x100000000);
  const lo = bitLen >>> 0;
  let padI = padded.length - 8;
  padded[padI++] = hi >>> 24 & 0xff;
  padded[padI++] = hi >>> 16 & 0xff;
  padded[padI++] = hi >>> 8 & 0xff;
  padded[padI++] = hi & 0xff;
  padded[padI++] = lo >>> 24 & 0xff;
  padded[padI++] = lo >>> 16 & 0xff;
  padded[padI++] = lo >>> 8 & 0xff;
  padded[padI] = lo & 0xff;

  const w = new Array(64);
  for (let block = 0; block < padded.length; block += 64) {
    for (let t = 0; t < 16; t++) {
      const o = block + 4 * t;
      w[t] = (padded[o] << 24) | (padded[o + 1] << 16) | (padded[o + 2] << 8) | padded[o + 3];
    }
    for (let t = 16; t < 64; t++) {
      const w15 = w[t - 15];
      const w2 = w[t - 2];
      const s0 = rotr32(w15, 7) ^ rotr32(w15, 18) ^ (w15 >>> 3);
      const s1 = rotr32(w2, 17) ^ rotr32(w2, 19) ^ (w2 >>> 10);
      w[t] = (w[t - 16] + s0 + w[t - 7] + s1) | 0;
    }

    let a = H[0], b = H[1], c = H[2], d = H[3], e = H[4], f = H[5], g = H[6], h = H[7];
    for (let t = 0; t < 64; t++) {
      const S1 = rotr32(e, 6) ^ rotr32(e, 11) ^ rotr32(e, 25);
      const ch = (e & f) ^ (~e & g);
      const temp1 = (h + S1 + ch + SHA256_K[t] + w[t]) | 0;
      const S0 = rotr32(a, 2) ^ rotr32(a, 13) ^ rotr32(a, 22);
      const maj = (a & b) ^ (a & c) ^ (b & c);
      const temp2 = (S0 + maj) | 0;
      h = g; g = f; f = e; e = (d + temp1) | 0; d = c; c = b; b = a; a = (temp1 + temp2) | 0;
    }
    H[0] = (H[0] + a) | 0; H[1] = (H[1] + b) | 0; H[2] = (H[2] + c) | 0; H[3] = (H[3] + d) | 0;
    H[4] = (H[4] + e) | 0; H[5] = (H[5] + f) | 0; H[6] = (H[6] + g) | 0; H[7] = (H[7] + h) | 0;
  }

  const out = new Uint8Array(32);
  for (let i = 0; i < 8; i++) {
    out[4 * i] = H[i] >>> 24 & 0xff;
    out[4 * i + 1] = H[i] >>> 16 & 0xff;
    out[4 * i + 2] = H[i] >>> 8 & 0xff;
    out[4 * i + 3] = H[i] & 0xff;
  }
  return out;
}

export async function sha256Base64url(value) {
  const bytes = new TextEncoder().encode(value);
  if (crypto.subtle) {
    const digest = await crypto.subtle.digest('SHA-256', bytes);
    return b64url(new Uint8Array(digest));
  }
  // Fallback for non-secure contexts (plain HTTP on a non-localhost host):
  // `crypto.subtle` is undefined there, so PKCE uses a pure-JS SHA-256.
  return b64url(sha256(bytes));
}

export function loadSession() {
  try {
    const raw = localStorage.getItem(SESSION_KEY);
    return raw ? JSON.parse(raw) : null;
  } catch (e) {
    return null;
  }
}

export function saveSession(session) {
  localStorage.setItem(SESSION_KEY, JSON.stringify(session));
}

export function clearSession() {
  localStorage.removeItem(SESSION_KEY);
}

/**
 * Starts the Keycloak authorization-code + PKCE flow.
 * Returns the authorization URL; the verifier is stored so it survives the round trip.
 */
export function buildAuthorizeUrl(config) {
  const state = randomString(24);
  const verifier = randomString(48);
  // The server scope value is a comma/space separated list (application.yml style);
  // OAuth 2.0 (RFC 6749) requires a space-delimited `scope` parameter.
  const scope = (config.scope || 'openid profile email')
    .split(/[,\s]+/).map((s) => s.trim()).filter(Boolean).join(' ');
  return sha256Base64url(verifier).then((challenge) => {
    const url = new URL(config.authorizationEndpoint);
    url.searchParams.set('response_type', 'code');
    url.searchParams.set('client_id', config.clientId);
    url.searchParams.set('redirect_uri', window.location.origin);
    url.searchParams.set('scope', scope);
    url.searchParams.set('state', state);
    url.searchParams.set('code_challenge', challenge);
    url.searchParams.set('code_challenge_method', 'S256');
    // Key the verifier by its state so an overlapping authorize call cannot
    // orphan the pairing of the flow the browser actually follows.
    sessionStorage.setItem(verifierKey(state), verifier);
    return url.toString();
  });
}

export function parseCallback(url) {
  const parsed = new URL(url);
  const code = parsed.searchParams.get('code');
  const state = parsed.searchParams.get('state');
  const error = parsed.searchParams.get('error');
  // Must match the value used during authorize (window.location.origin, no
  // trailing slash): Keycloak validates it byte-for-byte on the code exchange.
  return { code, state, redirectUri: window.location.origin, error };
}

export async function exchangeCode(config, code, redirectUri, state) {
  const verifier = state ? sessionStorage.getItem(verifierKey(state)) : null;
  const response = await fetch(config.tokenEndpoint, {
    method: 'POST',
    headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
    body: new URLSearchParams({
      grant_type: 'authorization_code',
      client_id: config.clientId,
      code,
      redirect_uri: redirectUri,
      code_verifier: verifier || '',
    }),
  });
  if (!response.ok) {
    clearPkce();
    let detail = response.statusText;
    try {
      detail = (await response.json()).error_description || detail;
    } catch (e) {
      /* ignore */
    }
    throw new Error(`Token exchange failed (${response.status}): ${detail}`);
  }
  clearPkce();
  return response.json();
}

/**
 * Refreshes the access token in the background before it expires. Keycloak's
 * access tokens are short-lived (about five minutes), so a silent refresh keeps
 * the session alive for as long as the user remains active.
 */
export async function refreshAccessToken(config, refreshToken) {
  const response = await fetch(config.tokenEndpoint, {
    method: 'POST',
    headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
    body: new URLSearchParams({
      grant_type: 'refresh_token',
      client_id: config.clientId,
      refresh_token: refreshToken,
    }),
  });
  if (!response.ok) {
    let detail = response.statusText;
    try {
      detail = (await response.json()).error_description || detail;
    } catch (e) {
      /* ignore */
    }
    throw new Error(`Session refresh failed (${response.status}): ${detail}`);
  }
  return response.json();
}

function clearPkce() {
  Object.keys(sessionStorage)
    .filter((k) => k.startsWith(PKCE_KEY))
    .forEach((k) => sessionStorage.removeItem(k));
}

export function sessionFromTokens(tokenData) {
  const claims = decodeJwt(tokenData.access_token);
  const roles = Array.isArray(claims.realm_access?.roles)
    ? claims.realm_access.roles
    : Array.isArray(claims.resource_access?.['lakehouse']?.roles)
      ? claims.resource_access['lakehouse'].roles
      : [];
  return {
    accessToken: tokenData.access_token,
    refreshToken: tokenData.refresh_token,
    expiresAt: Date.now() + (tokenData.expires_in || 300) * 1000,
    username: claims.preferred_username || claims.sub || 'anonymous',
    name: claims.name || claims.preferred_username || 'Anonymous',
    email: claims.email || '',
    roles,
  };
}