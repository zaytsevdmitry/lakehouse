The purpose of this requirement is to document the current state (architectural overview) of the React frontend application located at `yarina-ui-svc/src/main/resources`.

This document will serve as a technical foundation for the system architect to gain a high-level understanding of the codebase and optimize planning for future feature development tasks.

All descriptions should be placed in doc/arch/architecture.md . Follow the style specified in the document.

Please analyze your source code in the specified directory and provide a detailed report structured as follows:

### 1. Architectural Approach & Patterns
- What architectural decomposition pattern is being used (e.g., FSD (Feature-Sliced Design), Layered Architecture, Clean Architecture, or a standard Component-based structure)?
- What are the key code organization and Separation of Concerns (SoC) rules that guided your code generation?

### 2. Project & Repository Structure
- Provide a tree diagram of the key directories and files (starting from `src/` or the frontend root).
- Briefly describe the purpose of each main folder (e.g., components, hooks, context, services, utils, types).

### 3. State Management
- How are global and local application states organized? What technologies are used (Redux Toolkit, Zustand, React Context, MobX, native useState)?
- Describe the key stores/contexts and their respective responsibilities.

### 4. Data Flow & API Integration
- Which library is used for network requests (Axios, Fetch API, RTK Query, React Query)?
- How is authentication handled, and how are tokens passed?
- Describe the backend integration layer (where API clients reside and how components fetch data).

### 5. Component Layer & Component Registry (UI Elements)
- **Component Inventory & UI Elements:**
    Provide a comprehensive inventory of all custom UI controls, forms, inputs, and canvas/graph elements developed within the application. For each notable control (e.g., FormEditor, CodeEditor, Diagram Editors, Pickers, TreeNodes), detail its internal structure, configurable properties (props), and state requirements.
- **Storage & Location:**
    Explicitly define where these elements are stored in the codebase. Clarify the file-organization boundaries: how generic components are separated from domain-specific or feature-bound views (e.g., why all views reside directly in `src/components/` and whether any modular sub-folders or component namespaces are planned for cleaner scaling).
- **State & Prop Flows within Components:**
    Detail how these elements receive data and bubble up user interactions (the concrete implementation of the props-down, state-up pattern for visual components).
- **Design System & Styling Foundation:**
    Which component library or design system is used as the foundation (Ant Design, Material UI, Tailwind CSS, Styled Components, CSS Modules)? How is UI component reusability structured (Atomic design, Molecule approach, or custom decomposition)?
    Используйте код с осторожностью.

### 6. Routing & Navigation
- How is routing configured (e.g., React Router v6, link management, Protected Routes)?

### 7. Build & Configuration Tools
- What tools are used for building and linting the code (Vite, Webpack, Babel, ESLint, TypeScript configuration)?

### 8. Architectural Recommendations (Technical Debt)
- Identify any current bottlenecks or weak spots that require refactoring before the application scales further.

### 9. Data Access Model (RBAC)
- Describe the role-based access control (RBAC) model: which roles exist, what each role can see and do, where the frontend obtains permissions from (JWT claims, /me endpoint), and where access decisions are made in the code (route guards, conditional rendering, API-level checks).