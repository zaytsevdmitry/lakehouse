The purpose of this requirement is to document the current state (architectural overview) of the React frontend application located at `lakehouse-ui-svc/src/main/resources`.

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

### 5. Component Layer & Styling
- Which component library or design system is used as the foundation (Ant Design, Material UI, Tailwind CSS, Styled Components, CSS Modules)?
- How is UI component reusability structured (Atomic design, Molecule approach, or something else)?

### 6. Routing & Navigation
- How is routing configured (e.g., React Router v6, link management, Protected Routes)?

### 7. Build & Configuration Tools
- What tools are used for building and linting the code (Vite, Webpack, Babel, ESLint, TypeScript configuration)?

### 8. Architectural Recommendations (Technical Debt)
- Identify any current bottlenecks or weak spots that require refactoring before the application scales further.
