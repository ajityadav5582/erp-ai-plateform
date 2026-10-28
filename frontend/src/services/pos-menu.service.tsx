import { api } from "./api";

/** POS menu item as returned by the backend. */
export interface PosMenuItem {
  title: string;
  href: string;
  icon: string;
  permission: string;
  description: string;
}

/** Full POS menu response. */
export interface PosMenuResponse {
  items: PosMenuItem[];
}

const POS_MENU_URL = "/pos/menu";

/** POS menu API endpoints with RTK Query caching. */
export const posMenuApi = api.injectEndpoints({
  endpoints: (build) => ({
    getPosMenu: build.query<PosMenuResponse, void>({
      query: () => ({ url: POS_MENU_URL, method: "GET" }),
      providesTags: ["PosMenu"],
    }),
  }),
});

export const { useGetPosMenuQuery } = posMenuApi;

/** The grid icon, also used as the fallback for an unknown icon name. */
const GridIcon: React.ComponentType<{ className?: string }> = ({ className }) => (
  <svg className={className} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
    <rect width="7" height="7" x="3" y="3" rx="1" />
    <rect width="7" height="7" x="14" y="3" rx="1" />
    <rect width="7" height="7" x="14" y="14" rx="1" />
    <rect width="7" height="7" x="3" y="14" rx="1" />
  </svg>
);

/** Maps backend icon names to inline SVG icon components. */
export const posIconMap: Record<string, React.ComponentType<{ className?: string }>> = {
  scan: ({ className }) => (
    <svg className={className} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
      <path d="M3 7V5a2 2 0 0 1 2-2h2" />
      <path d="M17 3h2a2 2 0 0 1 2 2v2" />
      <path d="M21 17v2a2 2 0 0 1-2 2h-2" />
      <path d="M7 21H5a2 2 0 0 1-2-2v-2" />
      <path d="M7 8h10" />
      <path d="M7 12h10" />
      <path d="M7 16h6" />
    </svg>
  ),
  receipt: ({ className }) => (
    <svg className={className} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
      <path d="M4 2v20l2-1 2 1 2-1 2 1 2-1 2 1 2-1 2 1V2l-2 1-2-1-2 1-2-1-2 1-2-1-2 1Z" />
      <path d="M8 7h8" />
      <path d="M8 11h8" />
      <path d="M8 15h5" />
    </svg>
  ),
  clock: ({ className }) => (
    <svg className={className} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
      <circle cx="12" cy="12" r="10" />
      <polyline points="12 6 12 12 16 14" />
    </svg>
  ),
  monitor: ({ className }) => (
    <svg className={className} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
      <rect width="20" height="14" x="2" y="3" rx="2" />
      <line x1="8" x2="16" y1="21" y2="21" />
      <line x1="12" x2="12" y1="17" y2="21" />
    </svg>
  ),
  grid: GridIcon,
};

/** Resolves a backend icon name to its component, falling back to the grid icon. */
export function getPosIcon(name: string): React.ComponentType<{ className?: string }> {
  return posIconMap[name] ?? GridIcon;
}
