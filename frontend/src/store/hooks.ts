import { useDispatch, useSelector } from "react-redux";
import type { TypedUseSelectorHook } from "react-redux";
import type { RootState, AppDispatch } from "./index";

/**
 * Typed versions of React Redux hooks.
 *
 * Use these throughout the app instead of the untyped versions from
 * `react-redux` to get full type safety for state and dispatch.
 */
export const useAppDispatch = useDispatch as () => AppDispatch;
export const useAppSelector: TypedUseSelectorHook<RootState> = useSelector;
