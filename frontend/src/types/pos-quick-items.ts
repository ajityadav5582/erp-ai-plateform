import type { PosQuickItem } from "@/services/pos-quick-item.service";

/** A named tab/page on the terminal quick-items canvas. */
export interface QuickItemTab {
  id: string;
  name: string;
  /** Items placed on this tab's grid. */
  items: PosQuickItem[];
}

/** Discriminated tile kinds rendered on the grid. */
export type QuickTileKind = "item" | "folder";

/** A folder tile groups several catalogue items behind one grid slot. */
export interface QuickItemFolder {
  id: string;
  name: string;
  displayColor: string | null;
  displayIcon: string | null;
  gridRow: number;
  gridCol: number;
  sortOrder: number;
  children: PosQuickItem[];
}

/** Unified tile model consumed by the canvas. */
export type QuickTile =
  | { kind: "item"; id: number; gridRow: number; gridCol: number; sortOrder: number; item: PosQuickItem }
  | { kind: "folder"; id: string; gridRow: number; gridCol: number; sortOrder: number; folder: QuickItemFolder };

/** Batch-add drawer selection. */
export interface BatchAddSelection {
  itemIds: number[];
}
