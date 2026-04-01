# 🎨 P7 Frontend: UI/UX Documentation

The P7 frontend is a sleek, modern dashboard built with React 18, focusing on speed, responsiveness, and a premium aesthetic.

---

## 🎨 Design System

- **Colors**:
    - `Primary`: `#3d5afe` (Brand Blue)
    - `Dark`: `#0b1629` (Deep Navy)
    - `Background`: `#f8fafc` (Slate Gray)
- **Typography**: Inter (Google Fonts) for high readability.
- **Interactions**: Smooth hover states, micro-animations (tailwind-animate), and glassmorphism effects for modals.

---

## 🗺 Page & Component Breakdown

### 🏠 Public Pages
- **Landing Page**: Clean, professional redesign showcasing product features, pricing tables, and targeted call-to-actions.
- **Auth Flow**: Unified Login, Registration, and Password Recovery pages.

### 📊 Private Dashboard
- **Dashboard Overview**: Summary statistics (Total Links, Total Clicks).
- **My Links**: Searchable table of shortened URLs with consolidated action menus. Validates link creation limits based on user plan (FREE vs PRO).
- **Link Analytics**: Visual breakdowns of traffic sources and device data (PRO plan exclusive). Export individual link reports to PDF.
- **Link Sharing**: Integrated social sharing (WhatsApp, X, etc.) with a branded modal and one-click copy.
- **Profile**: Settings, profile updates, and subscription management.

### 🛡 Admin Dashboard
- **User Management**: Global list of users with plan-based statistics, PRO/FREE filtering, client-side PDF exports, and activation/deactivation toggles.
- **Audit History**: Searchable, time-filtered administrative log of all critical backend operations.
- **Global Overview**: Enhanced links table displaying original URL, click count, creator names, status tags, and platform-wide monitoring.

---

## 🏗 Key Components

- **`AuthLayout`**: Shared wrapper for all authentication pages providing consistency.
- **`Sidebar`**: Collapsible navigation component with persistent state that adapts based on user roles and screen size.
- **`Navbar`**: Fixed-top header featuring a toggle for the sidebar, user identity dropdown, and a real-time **Notification Bell**.
- **`ShareModal`**: Stylized modal with icons and URL intents for quick link distribution across social platforms.
- **`ActionMenu`**: A premium reusable dropdown for comprehensive link actions (Compact Status Toggle, Edit, Copy, QR, Stats).
- **`QrModal`**: Handles asynchronous fetching and rendering of QR code images.

---

## 🔄 State Management

### TanStack Query (React Query)
Used extensively for:
- Fetching links and analytics data.
- Handling mutations (Link creation, status toggling, deletions).
- **Invalidation**: On successful update, specific query keys (e.g., `['links']`) are invalidated to trigger automatic UI refreshes.

### Context API
Used for **Authentication** (`AuthContext`):
- Stores current user object and JWT.
- Exposes `login`, `register`, and `logout` functions globally.

---

## 🛠 Technical Commands

| Command | Result |
| :--- | :--- |
| `npm run dev` | Starts local development server |
| `npm run build` | Compiles for production (dist/ folder) |
| `npm run preview` | Previews the production build locally |

---

## 📱 Responsiveness
The dashboard is built with a **Mobile-First** approach:
- Sidebar collapses into a hamburger menu on small screens.
- Tables scroll horizontally on mobile to prevent layout breaking.
- Grid systems adapt from 1 to 3 columns based on viewport width.
