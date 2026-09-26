# Lakehouse UI — Frontend Architecture Review

> Commit-state audit of the React frontend located at
> `lakehouse-ui-svc/src/main/resources/frontend`.
>
> Scope: `index.html`, `public/`, `vite.config.js`, `package.json`, `test/`
> (Vitest setup), `src/` (JSX source, styles, API client, YAML tooling, hooks,
> component tests). Purpose: high-level
> understanding for the system architect and formulation of isolated feature
> tasks.
>
> Diagrams (PlantUML sources in `diagrams/`):

| Diagram | File |
|---|---|
| Component decomposition | `diagrams/overview.png` |
| Component placement & composition | `diagrams/placement.png` |
| Section switching logic | `diagrams/section-switching.png` |
| Data flow / API integration | `diagrams/data-flow.png` |
| State ownership | `diagrams/state-management.png` |

---

## 1. Architectural Approach & Patterns

The frontend is a **single-page application (SPA)** built with **React 18**,
bundled by **Vite**. The decomposition model is a **flat, component-based,
feature-oriented structure** — close in spirit to a lightweight Feature-Sliced
/ Layered pattern without formal enforcement:

- **Root / app layer** — `main.jsx` (entry, `createRoot`) and `App.jsx`
  (shell: header, section switcher, theme, shared state, deep-link init).
- **Feature sections** — one top-level component per UI domain
  (`ServicesSection`, `CatalogsSection`, `SchedulesSection`,
  `SparkJobsSection`, `VcsSection`, `ModellerSection`). Each section is
  self-contained: it owns its data fetching, its sub-views and its local
  state. The `ModellerSection` (the **Modelling** tab) manages a picker→
  editor→admin flow implemented by a dedicated group of components, but still
  reuses the shared session, CSRF and identity handling.
- **Shared infrastructure** — `api.js` (the only network-access point),
  `styles.css` (global styles + design tokens), `yaml.js` (schema-agnostic
  YAML read/write for configuration documents), `hooks/useResizableSplit.js`
  (shared drag-to-resize behaviour).
- **Sub-view / editor components** — smaller components inside a feature
  section (`LineageTab`, `RelationsTab`, `ModelTab`, `PipelineSection`,
  recursive `TreeNode`); the modeller group adds `WorkspacePicker`,
  `EditorView`, `AdminView`, `FormEditor`, `ErDiagramEditor`,
  `DataLineageDiagramEditor`, `DagEditor`, `Pickers`, `CodeEditor`, `Modal`.

Key code-organization rules that guided the implementation:

1. **One component = one file** in `src/components/`, named after the UI
   concept; tab/sub-view components live next to the section that owns them.
2. **Single source of truth for I/O** — all HTTP access goes through
   `api.js`; components never call `fetch` directly.
3. **Separation of concerns** — `api.js` (transport/auth/CSRF), components
   (render + interaction state), `styles.css` (presentation), `yaml.js`
   (YAML (de)serialization), `hooks/` (transient UI behaviour worth reusing).
   Business logic is deliberately *not* extracted into stores — it stays
   inside the sections and the modeller services behind the BFF.
4. **Lazy-mount, keep-alive sections** — sections are mounted on first use and
   kept alive afterwards (see §6): switching preserves per-section state.
5. **Props-down, state-up** — cross-section data flows top-down as props;
   there is no global store.
6. **URL-driven entry (deep links)** — `App.jsx` reads `section` and
   `workspace` query parameters once (module scope) and uses them as the
   initial section and, for the Modelling section, the workspace to auto-open.
   This enables the "open workspace in a new tab" workflow.

---

![Component decomposition](diagrams/overview.png)

---

## 2. Project & Repository Structure

```
src/main/resources/frontend
├── index.html              # HTML shell, <div id="root">, loads /src/main.jsx
├── package.json            # deps: react, react-dom, @xyflow/react,
│                           #       react-syntax-highlighter; dev: vite,
│                           #       vitest, jsdom, @testing-library/*;
│                           #       scripts: dev / build / preview / test
├── package-lock.json
├── vite.config.js          # dev server :5173, /api proxy -> :8091,
│                           # build outDir -> ../static (served by Spring Boot),
│                           # test block (jsdom, test/setup.js); see §7
├── public/                 # static assets copied verbatim to the build output
│                           #   (favicon.ico)
├── test
│   └── setup.js            # Vitest setup: jsdom polyfills required by
│                           #   React Flow (ResizeObserver, DOMMatrixReadOnly,
│                           #   pointer capture, offsetWidth/Height,
│                           #   getBoundingClientRect)
└── src
    ├── main.jsx            # entry: createRoot(...).render(<App/>)
    ├── App.jsx             # shell: header, section switcher (nav), main
    │                       #       area; global state (active/created sections,
    │                       #       theme, services, catalog, username, errors)
    │                       #       + deep-link init (section / workspace params)
    ├── api.js              # fetch wrappers (CSRF, 401 → login) + all REST calls,
    │                       #       incl. workspaceUrl() deep-link helper
    ├── yaml.js             # hand-rolled YAML (subset) reader/writer used by the
    │                       #       modeller editors
    ├── styles.css          # global CSS: tokens (CSS custom properties),
    │                       #       all component styles (~3.2k lines)
    ├── hooks
    │   └── useResizableSplit.js   # shared drag-to-resize splitter behaviour
    └── components
        ├── ServicesSection.jsx    # service graph (React Flow) + status cards
        ├── CatalogsSection.jsx    # catalog tree (recursive TreeNode) + tabs
        │                          #   pane (dataset / states / columns /
        │                          #   constraints / lineage / model / relations)
        ├── SchedulesSection.jsx   # schedule names + runs + PipelineSection
        ├── SparkJobsSection.jsx   # Spark submissions list + details + actions
        ├── VcsSection.jsx         # VCS sync log + object log (uses
        │                          #   hooks/useResizableSplit)
        ├── ModellerSection.jsx    # Modelling: workspace picker / editor / admin
        ├── WorkspacePicker.jsx    # per-domain branch tree, "my workspaces",
        │                          #   create branch modal; "Open" navigates
        │                          #   via workspaceUrl()
        ├── EditorView.jsx         # per-workspace file tree + editor pane
        │                          #   (the largest single component, ~1k lines)
        ├── AdminView.jsx          # admin-only: all workspaces, cleanup TTL, logs
        ├── FormEditor.jsx         # schema-driven form renderer for config docs
        ├── ErDiagramEditor.jsx    # ER-diagram visual editing (React Flow)
        ├── DataLineageDiagramEditor.jsx  # data-lineage diagram editing
        │                          #   (React Flow; on-canvas Add/Edit/Remove,
        │                          #   picker/confirm modals, full-screen toggle)
        ├── DagEditor.jsx          # generic DAG visual editing (React Flow)
        ├── Pickers.jsx            # reference pickers (data set / script / DS…)
        ├── CodeEditor.jsx         # lightweight code editor w/ syntax highlight
        ├── Modal.jsx              # shared modal shell
        ├── PipelineSection.jsx    # schedule-instance DAG (acts + nested tasks)
        ├── LineageTab.jsx         # dataset lineage graph (React Flow)
        ├── ModelTab.jsx           # model scripts w/ syntax highlighting
        ├── RelationsTab.jsx       # ER-diagram view (React Flow)
        └── __tests__/             # Vitest unit tests for the modeller
                                   #   (DataLineageDiagramEditor 24, EditorView 4,
                                   #   WorkspacePicker 5) — see §7
```

Purpose of each folder/file:

- **`components/`** — feature sections and their sub-views; the only directory
  for UI components. The modeller group forms its own namespace
  (`ModellerSection` + `WorkspacePicker/EditorView/AdminView/FormEditor/
  ErDiagramEditor/DataLineageDiagramEditor/DagEditor/Pickers/CodeEditor/Modal`)
  plus a test folder `__tests__/` for it. No `context/`,
  `services/`, `utils/`, `types/` folders exist; equivalent logic lives inline
  in the components.
- **`hooks/`** — extracted reusable UI behaviour; currently a single
  `useResizableSplit` hook (drag-to-resize panes, `horizontal`/`vertical`).
- **`yaml.js`** — dependency-free YAML subset parser/serializer built for
  lakehouse configuration documents (maps, lists, scalars, quoted strings,
  indent nesting, `|`/`|-`/`|+` block scalars). Comments are dropped; the
  backend re-validates every document with its own YAML parser.
- **`api.js`** — the backend integration layer (see §4).
- **`styles.css`** — a single global stylesheet; theming via CSS custom
  properties switched with the `data-theme` attribute (`light`/`dark`).
- **`App.jsx`** — orchestrator: tells the browser *which component is
  rendered where* (see §6).

---

## 3. State Management

**Technologies: native `useState` only.** There is **no** Redux Toolkit,
Zustand, MobX or React Context anywhere in the codebase. All state is local,
owned by the component that uses it.

Scopes and responsibilities:

- **Module / app-level** (`App.jsx`):
  - `services`, `catalogTree`, `username` — data loaded once at startup and
    passed down as props;
  - `catalogError`, `servicesError` — derived fetch failures;
  - `theme` — `light`/`dark`, read from `localStorage` key `lakehouse-theme`
    and written back on every change (a `useEffect` also sets
    `document.documentElement.dataset.theme`);
  - `activeSection`, `createdSections` — the placement/switching driver (see
    §6): which section is visible and which sections have been mounted;
  - `INITIAL_SECTION` / `INITIAL_WORKSPACE_ID` — read once from
    `location.search` (`?section=…&workspace=…`) and used to pre-activate the
    Modelling section and auto-open a workspace.
- **Section-level** — each `*Section` holds its own data, filters and UI
  flags:
  - `CatalogsSection` (`selectedNode`, `dataSet`, `activeTab`, …);
  - `SchedulesSection`, `SparkJobsSection`, `VcsSection` (listed in
    `state-management.puml`);
  - `ModellerSection` — `profile` (from `/api/user`), `openWorkspace`,
    `adminTab`, `notice` (auto-expiring banner), `autoOpenResolved`;
  - `WorkspacePicker` — `domains`, `workspaces`, `loading`, `working`,
    `branchModal`;
  - `EditorView` — `schemas`, `tree`, `dirs`, `selected`, `selectedFolder`,
    `filter`, `yaml`, `doc`, `keyNameEditable`, `mode` (`form`|`yaml`),
    `dirty`, `busy`, modal flags, `pendingOpen` (unsaved-changes guard);
    `readOnly` is derived from `profile.effectiveRole` + `workspace.own`.
- **Sub-view / tab-level** — `LineageTab`, `RelationsTab`, `ModelTab`,
  `PipelineSection`, `FormEditor`, `ErDiagramEditor`, `DataLineageDiagramEditor`,
  `DagEditor`
  (`SchemaService`'s per-kind schemas inflate `FormEditor`'s form model; the
  graph editors derive their node/edge lists from `doc`).
- **Diagram editors** — `ErDiagramEditor` and `DataLineageDiagramEditor`
  additionally own the loaded referenced DataSet documents and their dialog
  state: `dsByKey`/`dsByPath`, `loading`, `selectedKeyName`/`selectedNodeId`,
  `addOpen`, remove-confirmation target, edit-modal state (`editTarget`,
  `editYaml`, `editDoc`, `editMode` `form`|`yaml`, `editKeyNameEditable`,
  `editBusy`) and the full-screen flag `expanded` (`<-->`/`>-<` toggle).
- **Tool-managed** — `@xyflow/react` (React Flow) keeps its own internal
  store (viewport, node selection, drag state); nodes/edges themselves are
  owned by the section and synced into the flow via props.

Cross-section communication happens exclusively **top-down through props**
and **bottom-up through the single shared shell** (`App.jsx`). Example:
`ModellerSection` passes `profile`, `workspace` and `onNotice` down to
`WorkspacePicker`/`EditorView`; `EditorView` passes `doc` and `onDocChange`
(`onChange` for the data-lineage editor) into
`FormEditor`/`ErDiagramEditor`/`DataLineageDiagramEditor`.

![State ownership](diagrams/state-management.png)

---

## 4. Data Flow & API Integration

**Network library: native Fetch API** (no Axios, no RTK Query, no React
Query). All calls are centralized in `src/api.js`, which exports two wrappers:

- `apiFetch(url, options)` — the original wrapper; for every non-safe method
  (`POST/PUT/PATCH/DELETE`) it reads the CSRF token from the `XSRF-TOKEN`
  cookie and adds the `X-XSRF-TOKEN` header.
- `api(path, { method, body })` — the JSON wrapper used by the modeller
  (`WorkspacePicker`, `EditorView`, `AdminView`). It sets
  `Content-Type: application/json`, attaches the CSRF header for
  state-changing verbs, returns `null` for `204`, normalizes server `error`
  payloads and — on any `401` — redirects the browser to the Keycloak login
  entry point.

**Authentication / token handling.** The UI does not deal with JWT tokens
directly. Authentication is handled server-side by the Spring Boot **BFF**
(`lakehouse-ui-svc`):

- The browser authenticates against **Keycloak** via the OAuth2
  authorization-code flow; on success Spring Security issues a `JSESSIONID`
  session cookie (`HttpOnly`).
- The app runs under the same origin as the BFF, so requests are same-origin
  with credentials sent implicitly.
- **CSRF**: Spring exposes the token in the `XSRF-TOKEN` cookie. Both
  wrappers add the `X-XSRF-TOKEN` header for non-safe verbs.
- **RBAC**: the modeller endpoints are additionally gated by the Keycloak
  realm roles `LAKEHOUSE_MODELLER_VIEWER / _EDITOR / _ADMIN` (with a
  hierarchy `ADMIN > EDITOR > VIEWER`). The frontend also receives
  `effectiveRole` from `GET /api/user` and hides the Admin tab / disables the
  editor for users without the right level.

**Deep linking.** `workspaceUrl(workspaceId)` in `api.js` builds
`?section=modeller&workspace=<id>` on the current path. Workspace picker uses
it both as an `<a target="_blank">` (existing workspaces) and, after a
synchronous `window.open('', '_blank')` (pop-up-safe), as the `location` of
the new tab once `POST /api/vcs/workspace` returns the created workspace.

**Integration layer.** `api.js` also exports typed convenience functions, one
per endpoint — e.g. `fetchCatalogTree`, `fetchDataSet`, `fetchLineage`,
`fetchSchedules`, `fetchScheduleInstanceDAG`, `fetchSparkSubmissions`,
`fetchVcsSyncLogs`, `logout`, … Each performs the request against the BFF,
checks `response.ok`, and returns parsed JSON/text or throws an `Error` that
sections render in `.error-box` blocks.

**How components fetch data.** Sections call the API functions inside
`useEffect`/event handlers and store results with `useState`:
- **Mount-time fetch** — `App.jsx` loads catalog/services/user; `EditorView`
  loads `/api/schema` + workspace tree/dirs; `WorkspacePicker` loads the
  per-domain branches + workspaces; the diagram editors (`ErDiagramEditor`,
  `DataLineageDiagramEditor`) load the referenced DataSet documents on mount
  (`GET /api/workspaces/{id}/files/**`) to build their nodes.
- **User-triggered fetch** — stateless *load* functions bound to buttons
  (dates, filters), e.g. states, schedules, submissions, VCS logs.
- **Drill-down fetch** — selecting a tree node or table row triggers a fetch
  for the detail payload (`fetchDataSet`, `fetchSparkProperties`,
  `fetchScheduleInstanceDAG`, `GET /api/workspaces/{id}/files/**`).
- **In-place DataSet editing** — both diagram editors' **Edit** action opens a
  modal (`kind: DataSet · <key>`) that re-fetches the referenced file, edits it
  via `FormEditor` or a raw-YAML textarea, and **PUTs** the document back
  (`api(path, { method: 'PUT', body: { path, yaml, keyName } })`), after which
  the node is refreshed from the server (`reloadOne`).

**Dev-mode proxying.** `vite.config.js` proxies `/api` → `http://localhost:8091`
so the dev server talks to the BFF exactly like production.

![Data flow](diagrams/data-flow.png)

Backend endpoints consumed (all relative, proxied by the BFF):

| Area | api.js function | Endpoint |
|---|---|---|
| Catalog | `fetchCatalogTree` | `GET /api/catalog/tree` |
| Catalog | `fetchDataSet` / `fetchDataSource` | `GET /api/catalog/dataset/{key}` / `.../datasource/{key}` |
| Catalog | `fetchLineage`, `fetchConstraints`, `fetchScript`, `fetchDataSetModelScript` | `GET /api/catalog/...` |
| Catalog | `fetchStates` | `POST /api/states` |
| Schedules | `fetchSchedules`, `fetchScheduleHeaders`, `fetchScheduleInstanceDAG` | `/api/schedules*` |
| Services | `fetchServices`, `fetchServiceEdges`, `fetchServiceVertices` | `/api/services*` |
| Spark | `fetchSparkSubmissions`, `createSparkSubmission`, `fetchSparkStatus`, `killSparkSubmission`, `killAllSparkSubmissions`, `clearSparkCompleted`, `fetchSparkProperties` | `/api/spark-proxy/*` |
| VCS | `fetchVcsSyncLogs`, `fetchVcsObjectLogs` | `/api/vcs/logs`, `/api/vcs/objects` (both filterable by `domainKeyName`) |
| User | `fetchCurrentUser`, `logout` | `GET /api/user`, `POST /logout` |
| Modeller | `api()` calls in `WorkspacePicker` / `EditorView` / `AdminView` | `GET /api/vcs/workspaces`, `/api/vcs/branches`, `GET/POST/DELETE /api/vcs/workspace[/{id}]`, `POST /api/vcs/branch`, `POST /api/vcs/review/{id}`, `POST /api/vcs/workspace/{id}/restore`, `GET /api/schema[/{kind}]`, `/api/workspaces/{id}/tree|dirs|files`, `POST /api/workspaces/{id}/files(rename|move)`, `POST /api/workspaces/{id}/dirs(move)`, `/api/admin/workspaces`, `/api/admin/settings/cleanup-ttl-hours`, `/api/admin/sync-logs` |

---

## 5. Component Layer & Styling

**Component library / design system: none. Custom components + plain CSS.**
There is no Ant Design / Material UI / Tailwind / styled-components / CSS
Modules. Reusability is a simple **component decomposition**:

- **Feature sections** (`*Section`) — each is a self-contained page-level
  unit rendered inside the shell's `main` area.
- **Tab components** (`.tabs` / `.tab-list` / `.tab` / `.tab-content`) — the
  shared in-page navigation pattern (used in Catalog, Model, VCS and the
  Modeller's `My workspaces / Admin` switch).
- **Generic presentational helpers** defined in-module and reused locally:
  `Field`, `ServicePropertiesTable`, `DescriptionList`, `StatesTab`,
  `ColumnsTab`, `ConstraintsTab`, `TreeNode` (recursive tree rendering).
- **Modeller primitives** — the modeller has its own reusable building
  blocks:
  - `Modal` — shared dialog shell (branch creation, confirmations, review).
  - `CodeEditor` — lightweight editor with line numbers, syntax highlighting
    and a keyword/Jinja completion model (no third-party editor).
  - `Pickers` — reference pickers (data set key, script key, namespace,
    data source / task / service-group / driver) backed by providers that
    *read the workspace documents themselves*.
  - `FormEditor` — a **schema-driven renderer**: every configuration kind is
    described by a `KindSchema` (`GET /api/schema`) and `FormEditor` renders
    scalar fields, lists/objects, code (`code` type), and named-item pickers
    from that schema, producing/updating the same `doc` object.
  - `ErDiagramEditor` / `DataLineageDiagramEditor` / `DagEditor` — graph
    editors built on React Flow that translate the YAML document to/from
    nodes+edges. The diagram editors share an **on-canvas toolbar**
    (top-left `Add`/`Edit`/`Remove`), a data-set **reference picker**
    (`DataSetKeyPickerModal`), a remove **confirmation modal**, an **in-modal
    edit form** (form/YAML, reusing `FormEditor`) and a **full-screen expand
    toggle** (`<-->`/`>-<`, class `.diagram-expand` + `.diagram-fullscreen`
    overlay on the host). `DagEditor` is a generic node/edge editor driven by
    `nodeField`/`edgeField` props.
- **Graph/diagram views** — use **React Flow (`@xyflow/react` v12)** with
  custom node types:
  - `ServiceNode` (ServicesSection) — status-colored `UP`/`DOWN` node;
  - `PipelineActNode` / `PipelineTaskNode` (PipelineSection) — activity
    containers with nested task tiles (`parentId` + `extent: 'parent'`);
  - `LineageNode` (LineageTab) — colored center node vs. side nodes;
  - `EntityNode` (RelationsTab) — ER entity card with columns and handles on
    all four sides;
  - `LineageNode` (`DataLineageDiagramEditor`) — data-lineage node whose
    arrows are derived from each placed data set's `sources` map (missing
    sources render as dashed "missing" nodes);
  - `ErDiagramEditor`/`DataLineageDiagramEditor`/`DagEditor` reuse flow nodes
    with entity/edge handles for visual editing of ER, data-lineage and DAG
    documents.

**Styling approach: global `styles.css` (~3.2k lines).** Layout is driven by CSS
flex/grid utility classes and a consistent set of component classes; the
modeller styles are namespaced by `.modeller`, `.editor`, `.picker`, `.tabs`,
`.banner`, `.lineage-*`, `.er-*`, `.diagram-expand` etc. Theming is done
exclusively through **CSS custom properties**
(tokens): `--bg`, `--panel`, `--border`, `--text`, `--muted`, `--accent`,
`--up`, `--down`, ... Two palettes are declared under `:root[data-theme='light']`
and `:root[data-theme='dark']`; the `data-theme` attribute is toggled by
`App.jsx`.

---

## 6. Routing & Navigation

### 6.1 Overview

**There is no URL-based router (no React Router).** Navigation is pure
component state, complemented by a URL-entry deep link. The app is a single
page with one interactive feature visible at a time, switched by a top
navigation bar. Three complementary mechanisms implement navigation:

1. **Section switcher** — the navigation bar in `App.jsx` decides *which
   feature section is placed in the main area* (component placement).
2. **In-section tab state** — `activeTab` in each section decides *which
   sub-view* (tab) is placed inside the section body.
3. **Deep link on load** — `?section=…&workspace=…` query parameters define
   the initial section and (for `section=modeller`) the workspace to auto-open.

### 6.2 Component placement logic

The shell (`App.jsx`) defines a fixed vertical composition:

```
<div class="app">                        <!-- full-width page -->
  <header class="app-header">
    <h1>Lakehouse</h1>
    <div class="header-actions">   user label · Switch user · theme toggle
  <nav class="section-switcher">   Services | Catalog | Schedules | SparkJobs | VCS | Modelling
  <main class="app-main">          one section-pane per feature (see below)
```

Inside `main`, the placed sections are:

| Placement key | Button | Rendered component |
|---|---|---|
| `services` | Services | `ServicesSection` (graph + status cards) |
| `catalog` | Catalog | `CatalogsSection` (tree + tabs pane) |
| `schedules` | Schedules | `SchedulesSection` (names + runs + `PipelineSection`) |
| `sparkjobs` | SparkJobs | `SparkJobsSection` (table + details pane) |
| `vcs` | VCS | `VcsSection` (log / objects tabs) |
| `modeller` | Modelling | `ModellerSection` (picker → editor / admin) |

Each placement key renders as:

```jsx
{createdSections.has(key) && (
  <div className="section-pane" hidden={activeSection !== key}>
    <Section .../>
  </div>
)}
```

Composition rules:

- **Deep-link init**: `INITIAL_SECTION`/`INITIAL_WORKSPACE_ID` are read from
  `location.search`; `activeSection`/`createdSections` start from
  `INITIAL_SECTION`, and `ModellerSection` receives `initialWorkspaceId` to
  auto-open.
- **Lazy mount**: a section is mounted only after its navigation button has
  been clicked at least once (`createdSections` guards the render).
  `services` is pre-mounted because it is the initial section.
- **Keep-alive**: a mounted section is *never unmounted*. Switching hides it
  with the HTML `hidden` attribute. Per-section `useState` data (filters,
  selection, loaded tables) therefore survives navigation.
- **Nested placement**: inside sections the same two-column + splitter
  pattern (`catalogs-layout`, `catalog-pane`, `catalog-splitter`) is reused
  to place two panels side-by-side (tree ↔ tabs, names ↔ runs, table ↔
  details, graph ↔ details, log ↔ objects). Splitters are draggable and
  resize panels via the `%` width/height; the `PipelineSection` in Schedules
  is rendered *below* the layout, not in a tab. The modeller `EditorView`
  uses an `aside.sidebar` (file tree) + `section.main` (editor) split; the
  pane divider is handled by `hooks/useResizableSplit`.

![Component placement](diagrams/placement.png)

### 6.3 Switching between sections

State kept in `App.jsx`:

- `activeSection` — the placement key of the visible section
  (initial value `INITIAL_SECTION || 'services'`);
- `createdSections` — `Set` of placement keys that have been mounted
  (initial `new Set([INITIAL_SECTION || 'services'])`).

The single transition function is `activateSection(section)`:

```js
const activateSection = (section) => {
  setActiveSection(section);
  setCreatedSections((current) =>
    current.has(section) ? current : new Set([...current, section])
  );
};
```

Behaviour:

1. Clicking a nav button calls `activateSection(key)`.
2. `activeSection` is set → all `section-pane` divs recompute their `hidden`
   attribute; only the pane matching the key becomes visible.
3. The key is added to `createdSections` if absent → the pane (and its
   component) receives its first mount, preserving the lazy-mount invariant.
4. Any previously mounted pane stays mounted but hidden → its interactive
   state is retained when switching back.

![Section switching](diagrams/section-switching.png)

### 6.4 In-section navigation

Inside sections, `activeTab` (`useState`) drives the same reveal/hide pattern
with `props`/state instead of `hidden`:

- **CatalogsSection** — `TableTabs` switches among Dataset / States / Columns
  / Constraints / Lineage / Model / Relations; the tree selection resets
  `activeTab` to `'dataset'`.
- **ModelTab** — vertical tab rail toggles Scripts / Complete.
- **VcsSection** — VCSLog / VCSObjectsSearch tabs.
- **ModellerSection** — for admins a `tabbed` header offers My workspaces /
  Admin; both render `WorkspacePicker` and `AdminView` respectively.
- **WorkspacePicker → EditorView** — the modifier flow is *state driven*
  (`openWorkspace` in `ModellerSection`): the workspace picker "Open" action
  navigates into the editor for the selected `(domain, branch)` set (either in the same tab via
  `onOpen`, or in a new tab via `workspaceUrl()`). `EditorView` sub-navigates
  with `selected`/`mode` (`form`↔`yaml`) and its modal flags.
- **DataSourcePanel** — DataSource / Service tabs.

### 6.5 "Protected routes"

There are no client-side guards. Access protection is entirely **server-side
in the BFF** (Spring Security): unauthenticated requests are redirected to
the Keycloak login and the session cookie gates every `/api` call. The
frontend just renders the auth state it sees (username label, `logout` →
`POST /logout` + reload). Modeller mutations require at minimum the
`LAKEHOUSE_MODELLER_VIEWER/EDITOR/ADMIN` role depending on the endpoint; the
modeller also enforces per-user workspace ownership (`workspace.own`), and
readers/viewers get a read-only editor (`readOnly`).

---

## 7. Build & Configuration Tools

| Concern | Tool / config |
|---|---|
| Bundler / dev server | **Vite 6** (`vite.config.js`) |
| React plugin | **@vitejs/plugin-react** |
| Build output | `build.outDir = '../static'`, `emptyOutDir: true` → produced static bundle is served by Spring Boot from classpath `static/` |
| Static assets | `public/` → copied into `static/` (e.g. `favicon.ico`) |
| Dev proxy | `/api` → `http://localhost:8091` (`server.proxy`) |
| Module format / target | ESM (`"type": "module"`), Vite default targets |
| Language | **JavaScript (JSX)** — no TypeScript |
| Linting / formatting | **None configured** (no ESLint, no Prettier, no `lint` script) |
| Tests | **Vitest 3 + React Testing Library** (jsdom): `npm test` runs `vitest run`; configured in the `test` block of `vite.config.js` (`globals`, `environment: 'jsdom'`, `setupFiles: ['./test/setup.js']`, `include: src/**/*.test.{js,jsx}`). The current suite covers `DataLineageDiagramEditor` (24 tests: graph translation, position persistence, drag, Add/Edit/Remove + modal flows, full-screen toggle), `EditorView` (4 tests, incl. the multi-domain workspace leave-on-review flow) and `WorkspacePicker` (5 tests, incl. per-domain branch preselection). `test/setup.js` polyfills the browser APIs React Flow needs in jsdom (ResizeObserver, `DOMMatrixReadOnly`, pointer capture, `getBoundingClientRect`, `offsetWidth/Height`). |
| Package manager | npm (`package.json` + `package-lock.json`) |

Scripts: `dev` (`vite`), `build` (`vite build`), `preview` (`vite preview`),
`test` (`vitest run`).

Notable runtime dependencies:

- `react` / `react-dom` `^18.3.1`;
- `@xyflow/react` `^12.11.2` — React Flow: flow graphs in Services (graph),
  Schedules (pipeline DAG), Catalog (lineage, relations) and the modeller's
  ER/DAG editors;
- `react-syntax-highlighter` `^16.1.1` — PrismLight syntax highlighting for
  SQL / Scala / Python / R / Go / Java model scripts.

Notable *non*-dependencies: the modeller's YAML handling (`yaml.js`) is
hand-rolled on purpose — no `js-yaml` dependency, and the code editor
(`CodeEditor.jsx`) is a custom implementation (no `react-codemirror` /
`monaco`).

Development/test dependencies: `vitest`, `jsdom`, `@testing-library/react`,
`@testing-library/jest-dom` (all scoped to the `test` toolchain of §7); no
TypeScript or linting tooling is present.

---

## 8. Architectural Recommendations (Technical Debt)

Priorities for refactoring before the application scales:

1. **Hand-rolled YAML subset (`yaml.js`).** The parser covers maps, lists,
   scalars, `|`-blocks — but drops comments/anchors by design and cannot
   round-trip arbitrary documents. The backend re-validates everything, so the
   risk is bounded, but a real `js-yaml` (or a backend round-trip endpoint)
   would make the editor lossless and future-proof.
2. **Automated verification covers one editor well.** A Vitest + React Testing
   Library suite guards `DataLineageDiagramEditor` (graph↔doc translation, drag
   persistence, Add/Edit/Remove modal flows), and there is initial coverage of
   `EditorView` and `WorkspacePicker`, which is where the multi-domain workspace
   logic lives. The rest of the app — `FormEditor`, `ErDiagramEditor`, the
   section switcher, the review flow — still has no lint, no type check and no
   tests; the regression net for those surfaces is a manual browser session.
   Minimum viable step: add ESLint + extend the RTL suite to the remaining
   modeller editors and save/restore flow; later a Playwright suite for the
   section switcher and the full editor workflow.
3. **No TypeScript.** All state flows through dynamic JS objects and props
   (`doc` from `parseYaml`, `KindSchema.fields`, `profile`, `WorkspaceResponse`)
   whose shapes are only documented implicitly. Typing `api.js` responses,
   the modeller DTOs and section props would prevent a whole class of
   "undefined is not a function" regressions.
4. **Single monolithic `styles.css` (~3.2k lines).** Global className coupling
   makes isolated feature work risky: shared tokens are good, but section
   styles should be co-located (CSS Modules or scoped files) so a change in
   one feature cannot silently break another — with four canvas surfaces in
   the modeller (form, ER, data-lineage, DAG) this becomes concrete.
5. **No formal state, routing or data-fetching layer.** The keep-alive +
   `hidden` switcher (plus the `section`/`workspace` deep links) is simple
   today, but as sections grow: (a) introduce `@tanstack/react-query` (or
   similar) to dedupe/centralize the manual `fetch`-then-`setState` flows and
   loading/error handling; (b) consider React Router with URL-driven tabs and
   the workspace id in the path to make the new-tab workspace workflow
   bookmarkable and back/forward-safe; (c) extract cross-section state into a
   context/selector store only when the coupling becomes bidirectional and
   reusable beyond `App`.
6. **Layout & graph-layout logic still duplicated.** `hooks/useResizableSplit`
   now covers the VCS and modeller UIs, but `CatalogsSection`,
   `SchedulesSection`, `SparkJobsSection` and `PipelineSection` still
   re-implement the splitter inline. Likewise the layered-graph layout
   algorithm exists in `ServicesSection`, `PipelineSection` (`computeLayers`)
   and the modeller `DagEditor`/`ErDiagramEditor`/`DataLineageDiagramEditor` —
   unify into a single graph-layout utility.
7. **Mixed imperatives inside components.** `FormEditor` (~1.3k lines) and
   `EditorView` (~1k lines) mix data fetching, complex forms, graph building
   and dialogs in one file. Splitting each into *container/hook + view +
   schema/DAG-transform* modules would match the FSD/layer intent of §1 and
   make the files unit-testable.
8. **No caching of heavy catalog/graph data.** Every dataset selection
   re-fetches the dataset and (in `RelationsTab`) its neighbors;
   schedules/pipeline re-fetch per run selection; `EditorView` builds its
   picker providers by re-reading workspace documents per open. A small keyed
   cache in the API layer would cut repeated network chatter.
9. **Error handling is per-component and duplicated.** `api()` normalizes the
   modeller errors, but the other sections repeat `response.ok` checks +
   `.error-box` rendering. Centralize error mapping in `api.js` and render via
   a small `FetchState`/error-boundary pattern.
10. **RBAC logic lives in two places.** `SecurityConfig` gates HTTP routes
    (`hasRole`) while `UserContext`/`UserController` compute `effectiveRole`
    for the frontend. The two must stay in sync as rules evolve; consider a
    single `ModellerRole`/authorization helper shared by both.
11. **Accessibility gaps.** Icon-only tab buttons, color-only status badges
    and the modeller's drag-and-drop tree rely mainly on color/outline; add
    `aria-*`/keyboard support, semantic `nav`/`tablist`, and focus management
    for the switcher and editor.

---

### Appendix A — PlantUML sources

All diagrams are generated from PlantUML sources kept in
`doc/arch/diagrams/*.puml`:

```bash
java -Djava.awt.headless=true -jar plantuml.jar -tpng diagrams/*.puml
```