# Lakehouse UI Modeller — Frontend Architecture Review

> Commit-state audit of the React frontend located at
> `lakehouse-ui-modeller-svc/src/main/resources/frontend`.
>
> Scope: `index.html`, `vite.config.js`, `package.json`, `src/` (JSX source,
> styles, API client, YAML subset helpers). Purpose:
> high-level understanding for the system architect and formulation of
> isolated feature tasks.
>
> Diagrams (PlantUML sources in `../diagrams/`):

| Diagram | File |
|---|---|
| Module architecture (backend + React frontend + BFF auth) | `../diagrams/architecture.png` |
| Component decomposition | `../diagrams/frontend-components.png` |
| Authentication & data flow | `../diagrams/data-flow.png` |
| State ownership | `../diagrams/state-management.png` |

---

## 1. Architectural Approach & Patterns

The frontend is a **single-page React application** built with **React 18** and
bundled by **Vite**. The decomposition model is a **flat, component-based,
feature-oriented structure** — close in spirit to a lightweight
Feature-Sliced/Layered pattern without formal enforcement:

- **Root / app layer** — `main.jsx` (entry, `createRoot`) and `App.jsx`
  (shell: top bar, login/workspaces/editor/admin view switching, global
  profile, notices, theme).
- **Feature top-level views** — one component per UI domain:
  `LoginView`, `WorkspacePicker`, `EditorView`, `AdminView`.
- **Shared infrastructure** — `api.js` (single network-access point; BFF
  transport, CSRF, auth redirects), `yaml.js` (YAML subset
  parser/serializer), `index.css` (global styles + design tokens).
- **Editor internals** — `FormEditor` (schema-driven forms for the whole
  editable document), `DagEditor` (React Flow graphs for DAG-kind fields),
  `ErDiagramEditor` (React Flow ER-diagram kind), `Pickers` (catalog/read-only
  reference pickers), `CodeEditor` (lightweight syntax-highlighted editor for
  script/string fields), `Modal` (shared overlay).

Key code-organization rules that guided the implementation:

1. **One component = one file** in `src/components/`, named after the UI
   concept.
2. **Single source of truth for I/O** — all HTTP access goes through
   `api.js`; components never call `fetch` directly.
3. **Separation of concerns** — auth handling (redirects, session cookie,
   CSRF) and data access in `api.js`, YAML round-tripping in `yaml.js`,
   rendering + interaction in components, presentation in `index.css`. There
   is no client-side OIDC/token code: the React frontend does not build
   authorize URLs, does not exchange codes and never sees a token.
4. **Props-down, state-up** — cross-view data flows top-down as props; there
   is no global store.
5. **Server-side session (BFF)** — authentication is handled by the Spring
   Boot backend through the Keycloak authorization-code flow; the browser
   only carries a `JSESSIONID` cookie and never stores tokens in
   `localStorage`. The only `localStorage` key is the UI theme.
6. **Dynamic, schema-driven editing** — the editor renders backend-provided
   form schemas (from `SchemaService`) instead of hard-coded field lists; DAG
   and ERD kinds get graph editors.

## 2. Project & Repository Structure

```
src/main/resources/frontend/
├── index.html                # app shell, single mount point
├── package.json              # React 18.3.1, @xyflow/react 12.11.2, Vite 6.4.3
├── vite.config.js            # build.outDir = ../static; dev proxy /v1_0,
│                             #   /oauth2, /logout -> http://localhost:8081
└── src/
    ├── main.jsx              # entry: ReactDOM.createRoot
    ├── App.jsx               # shell, view switching, profile/notices,
    │                         #   theme, inactivity-based sign-out
    ├── api.js                # fetch wrapper (BFF: JSESSIONID + CSRF,
    │                         #   401 -> login redirect, JSON, error normalize)
    ├── yaml.js               # lightweight YAML subset parser/serializer (form mode)
    ├── index.css             # design tokens + layout styling
    └── components/
        ├── LoginView.jsx     # login card ("Sign in" -> /oauth2/authorization/keycloak)
        ├── WorkspacePicker.jsx  # branches, my workspaces, open/create workspace
        ├── EditorView.jsx    # workspace tree, form/raw YAML modes, new/save/delete/review
        ├── FormEditor.jsx    # generic schema-driven form renderer
        ├── DagEditor.jsx     # @xyflow/react graph editor for DAG-kind fields
        ├── ErDiagramEditor.jsx  # @xyflow/react entity-relationship editor
        ├── CodeEditor.jsx    # syntax-highlighted editor for script/string fields
        ├── Pickers.jsx       # catalog / read-only reference picker modals
        ├── Modal.jsx         # shared overlay dialog
        └── AdminView.jsx     # admin workspaces, cleanup TTL, sync logs
```

![Component decomposition](../diagrams/frontend-components.png)

Responsibilities:

- `main.jsx` / `App.jsx` — bootstrap and application shell;
- `api.js` — the only backend transport. In the **BFF mode** the browser
  attaches the `JSESSIONID` cookie automatically (same-origin); no `Bearer`
  header is ever sent. State-changing requests add the CSRF token read from
  the `XSRF-TOKEN` cookie as an `X-XSRF-TOKEN` header. A 401 response calls
  `redirectToLogin()` (guarded, once) to `/oauth2/authorization/keycloak`;
  errors are normalized to `Error` with a message;
- `yaml.js` — parser/serializer for the YAML subset used in the metadata
  files (needed because the offline environment had no YAML library);
- `components/` — feature and shared components.

## 3. State Management

There is **no global store** (no Redux/Zustand/MobX). All state is React state
(`useState`), lifted to the level that needs it and passed down as props.

- **App-level global state** (`App.jsx`): `profile` (user + effective role
  resolved once from `/v1_0/auth/me` through the server session),
  `openWorkspace` (currently edited workspace), `notice` (transient banner),
  `theme` (`light`/`dark`, persisted under the `lakehouse-modeller-theme`
  `localStorage` key). There is **no client-side session state** — the
  server owns the session (`JSESSIONID` cookie); `App.jsx` only watches an
  inactivity timer (30 minutes without interaction) and then signs the user
  out via `POST /logout`.
- **EditorView local state**: `schemas` (fetched once), `tree` (workspace file
  tree), `selected` (active file), `doc`/`yaml` (form object vs raw text),
  `mode` (form/yaml), `dirty`, `busy`, and the modal flags (`newModal`,
  `reviewModal`, `deleteModal`).
- **FormEditor / DagEditor / ErDiagramEditor local state**: per-field values;
  the graph editors additionally keep node positions in a **module-level
  `Map`** so layout survives re-renders within the session.
- **AdminView local state**: admin workspaces, sync logs, TTL form.

![State ownership](../diagrams/state-management.png)

## 4. Data Flow & API Integration

- **Network library**: the native **Fetch API** wrapped by `api.js`; there is
  no Axios/React Query/RTK Query.
- **Authentication (BFF)**: OAuth 2.0 **authorization-code flow** against
  Keycloak executed **server-side by the Spring Boot backend** (Spring
  Security OAuth2 client, registration `keycloak`). The browser is redirected
  to `/oauth2/authorization/keycloak`; after login Keycloak redirects back to
  `/login/oauth2/code/keycloak` where the backend exchanges the code
  (`client_secret_post`) and issues an authenticated session cookie
  (`JSESSIONID`, HttpOnly). Subsequent `/v1_0` calls are same-origin: the
  cookie is attached implicitly and no token ever reaches the browser. Realm
  roles are mapped from the ID-token/user-info claims and normalized through a
  role hierarchy (`ADMIN > EDITOR > VIEWER`); the effective role is exposed
  via `/v1_0/auth/me` and enforced per-endpoint with `hasRole(...)`.
- **CSRF**: Spring Security stores the CSRF token in the `XSRF-TOKEN` cookie
  (readable by JS). `api.js` reads it and adds an `X-XSRF-TOKEN` header to
  every non-safe method (`POST/PUT/DELETE`). Safe methods are sent without it.
- **401 handling**: `api.js` turns a 401 into `redirectToLogin()` (a guarded
  full-page redirect to `/oauth2/authorization/keycloak`) and throws
  "Session expired, please sign in again."; the frontend reloads with a fresh
  session after the flow.
- **Login / logout**: `login()` and `logout()` in `api.js` are plain
  redirects — `GET /oauth2/authorization/keycloak` to enter the flow and
  `POST /logout` (Spring Security logout, CSRF-protected) followed by a
  reload of `/`.
- **Public config**: `/v1_0/auth/config` (whitelisted) still returns the OIDC
  parameters (issuer, authorization/token endpoints, client id, scope, auth
  strategy, inactivity minutes). In the BFF mode the frontend does not consume
  it — it is kept for diagnostic/informational clients.
- **Backend integration layer**: `api.js` + the controllers under `/v1_0`
  (`Auth`, `Vcs`, `Editor`, `Schema`, `Admin`). Components fetch data on
  mount or on user action; the editor refreshes the file tree after every
  create/save/delete.
- **Form schemas**: `EditorView` loads `/v1_0/schema` once; `FormEditor`
  renders fields of type `string|integer|double|boolean|datetime|map|object|list|dag`
  (the `dag` type is delegated to `DagEditor`, which edits nodes from a
  sibling list field and edges from a `dag`-typed field; ER-diagram kinds are
  delegated to `ErDiagramEditor`).

![Authentication & data flow](../diagrams/data-flow.png)

## 5. Component Layer & Styling

- **No component library / design system** — plain CSS (`index.css`) with CSS
  custom properties (design tokens), semantic class names, and utility-state
  classes such as `badge viewer|editor|admin`, `banner success|error`,
  `spinner`, `muted`, `primary`/`danger` buttons. Theming switches the
  `data-theme` attribute on the document root (`light`/`dark` palettes).
- **Reusability structure** — a practical component approach: shared
  primitives (`Modal`, `CodeEditor`, `Pickers`), a generic schema-driven
  editor (`FormEditor`), and feature components composed per view
  (`WorkspacePicker`, `EditorView`, `AdminView`, `DagEditor`,
  `ErDiagramEditor`). A "New file" and "Submit review" form are implemented as
  local modal components inside `EditorView`.
- **Graph rendering** uses `@xyflow/react` (React Flow v12) for the DAG-kind
  and ER-diagram-kind documents; positions are managed in-memory and not
  persisted to the file.

## 6. Routing & Navigation

There is **no router** (no React Router). View switching is **state-driven**
in `App.jsx`:

- no `profile` → `LoginView` ("Sign in" is a full-page redirect to the BFF
  login entry point);
- `profile` && no `openWorkspace` → `WorkspacePicker` (or `AdminView` tab for
  the ADMIN role);
- `profile` && `openWorkspace` → `EditorView`.

The OIDC callback is handled **server-side** (Spring Security completes the
code exchange and redirects back to `/`); the frontend performs no URL
rewriting and does not parse the redirect. The only navigation side effect is the login
redirect on 401 (`redirectToLogin()`). Protected views are driven by the
effective role in `profile` and re-validated server-side by `hasRole(...)`
checks on every `/v1_0` endpoint.

## 7. Build & Configuration Tools

- **Vite 6.4.3** + `@vitejs/plugin-react 4.7.0`. `vite.config.js`: base `/`,
  `build.outDir = ../static` (served by the Spring Boot jar),
  `emptyOutDir: true`, dev server on `:5173` proxying `/v1_0`, `/oauth2` and
  `/logout` to `http://localhost:8081` (identical path set as in production).
- **package.json** is pinned to exact versions (`react/react-dom 18.3.1`,
  `@xyflow/react 12.11.2`). Build scripts: `npm run dev`, `npm run build`.
- **No linter/formatter** configured (no ESLint/Prettier config) and **no
  TypeScript** — plain JSX modules.
- The backend ships the built frontend: Maven excludes `frontend/**` from
  resources, `static/**` is packaged into the jar's `BOOT-INF/classes/static`.

## 8. Architectural Recommendations (Technical Debt)

1. **No TypeScript, no tests, no linting for the frontend** — the codebase is
   plain JSX; introduce `tsc`/ESLint and a small Vitest coverage around
   `yaml.js`/`api.js` (pure functions) before the editor grows.
2. **Custom YAML subset parser** (`yaml.js`) — hand-written, YAML-subset only.
   It serves the offline constraint, but semantically full docs (anchors,
   multi-line scalars, custom tags) are unsupported. Prefer a maintained YAML
   library as soon as the environment allows.
3. **Single `App.jsx` view switcher** — login/workspaces/editor/admin tabs are
   entangled in one component; introduce a router (React Router) or a
   use-case-selected view registry as views multiply.
4. **Session expiry is abrupt** — the server owns the session, so an expired
   `JSESSIONID` surfaces as a mid-edit 401 that reloads the page. Consider a
   proactive re-check of `/v1_0/auth/me` before long operations, a friendlier
   "session expired" dialog instead of an immediate redirect, and autosave of
   the dirty document. The client-side inactivity sign-out (30 minutes) is
   wall-clock-based and independent of the server-side session lifetime.
5. **`DagEditor` / `ErDiagramEditor` positions are not persisted** —
   module-level `Map`; acceptable for a session, but a "save layout" feature
   would require storing positions with the file (or a per-user preference).
6. **`encodePath` is a single call-site helper** — duplicated in several
   components; extract into a shared utility module.
7. **Backend-served schema is the only field source** — if `lakehouse-common`
   DTOs change, `SchemaService`/`ConfigKind` must be kept in sync; add a
   contract test asserting each kind resolves.
8. **No i18n** — UI strings are hard-coded English; the RU/EN documentation is
   maintained separately (see `doc/` and `doc-ru/`).
9. **`/v1_0/auth/config` is now informational only** — it was created for the
   browser-native PKCE flow of the retired integration mode and is no longer
   consumed by the frontend in BFF mode; decide between removing it and
   documenting it as a diagnostic endpoint.
10. **Dual-mode security** — the same service is both a session-based BFF and
    an OAuth2/JWT resource server. Keep the bearer-token path restricted to
    server-to-server clients and ensure no browser-facing endpoint accepts it.