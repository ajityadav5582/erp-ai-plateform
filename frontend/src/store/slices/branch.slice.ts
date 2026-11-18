import { createAsyncThunk, createSlice, type PayloadAction } from "@reduxjs/toolkit";
import { axiosInstance } from "@/services/http-client";
import type { MyBranch } from "@/services/branch.service";

/**
 * localStorage key under which the active branch selection is persisted so
 * it survives page reloads and is restored by the app shell.
 */
export const ACTIVE_BRANCH_SELECTION_STORAGE_KEY = "erpai.activeBranchSelection";

/**
 * Shape of the active branch selection carried by the app shell header.
 *
 * The selected branch is chosen by the user in the topbar and is the
 * implicit context for all branch-scoped operations (sales orders, purchase
 * orders, POS items, etc.). When {@code branchId} is {@code null} the
 * request is treated as "all branches" and the backend filters by company
 * instead of a single branch.
 */
export interface BranchSelection {
  branchId: number | null;
  branchName: string | null;
}

const initialState: {
  /** The currently selected branch id, or null for "all branches". */
  activeBranchId: number | null;
  /** The display name of the active branch, or null for "all branches". */
  activeBranchName: string | null;
  /** The branches the current user can switch to for the active company. */
  availableBranches: MyBranch[];
  /** True once the branch list has been fetched for the current company. */
  loaded: boolean;
  /** True while fetching the user's branches. */
  loading: boolean;
  /** Error message from the last fetch, or null. */
  error: string | null;
} = {
  activeBranchId: null,
  activeBranchName: null,
  availableBranches: [],
  loaded: false,
  loading: false,
  error: null,
};

/**
 * Fetches the branches assigned to the current user for the active company.
 *
 * Uses the {@code GET /identity/branches/my} endpoint exposed by the backend
 * {@code BranchController}. The result is cached by RTK Query, so this thunk
 * only hits the network when the active company changes.
 */
export const fetchMyBranches = createAsyncThunk<
  MyBranch[],
  void,
  { rejectValue: string }
>("branch/fetchMyBranches", async (_, { rejectWithValue }) => {
  try {
    const response = await axiosInstance.get<MyBranch[]>("/identity/branches/my");
    return response.data ?? [];
  } catch (err) {
    const message =
      (err as { response?: { data?: { message?: string } } })?.response?.data
        ?.message ?? "Failed to load branches.";
    return rejectWithValue(message);
  }
});

/**
 * Slice holding the active branch context for the app shell.
 *
 * The active branch id is persisted in localStorage keyed by company so that
 * switching companies does not resurrect the previous company's selection.
 */
const branchSlice = createSlice({
  name: "branch",
  initialState,
  reducers: {
    /**
     * Persist a branch selection (called by the topbar switcher and by the
     * app shell on company switch).
     */
    setBranchSelection: (
      state,
      action: PayloadAction<BranchSelection>
    ) => {
      state.activeBranchId = action.payload.branchId;
      state.activeBranchName = action.payload.branchName;
      if (typeof window !== "undefined") {
        window.localStorage.setItem(
          ACTIVE_BRANCH_SELECTION_STORAGE_KEY,
          JSON.stringify({
            branchId: action.payload.branchId,
            branchName: action.payload.branchName,
          })
        );
      }
    },

    /**
     * Restore the persisted selection from localStorage (called on app load
     * and after a company switch).
     */
    restoreBranchSelection: (state) => {
      if (typeof window === "undefined") return;
      try {
        const stored = window.localStorage.getItem(
          ACTIVE_BRANCH_SELECTION_STORAGE_KEY
        );
        if (!stored) return;
        const candidate = JSON.parse(stored) as Partial<BranchSelection>;
        state.activeBranchId = candidate.branchId ?? null;
        state.activeBranchName = candidate.branchName ?? null;
      } catch {
        window.localStorage.removeItem(ACTIVE_BRANCH_SELECTION_STORAGE_KEY);
      }
    },

    /** Clear the branch selection (logout / session expiry). */
    clearBranchSelection: (state) => {
      state.activeBranchId = null;
      state.activeBranchName = null;
      if (typeof window !== "undefined") {
        window.localStorage.removeItem(ACTIVE_BRANCH_SELECTION_STORAGE_KEY);
      }
    },
  },
  extraReducers: (builder) => {
    builder
      .addCase(fetchMyBranches.pending, (state) => {
        state.loading = true;
        state.error = null;
      })
      .addCase(fetchMyBranches.fulfilled, (state, action) => {
        state.availableBranches = action.payload;
        state.loaded = true;
        state.loading = false;
        state.error = null;
      })
      .addCase(fetchMyBranches.rejected, (state, action) => {
        state.loading = false;
        state.error = action.payload ?? "Failed to load branches.";
      });
  },
});

export const { setBranchSelection, restoreBranchSelection, clearBranchSelection } =
  branchSlice.actions;

export default branchSlice.reducer;