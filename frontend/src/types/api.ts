/**
 * Shared API response types.
 *
 * These were previously copy-pasted into every service file (`category`,
 * `department`, `branch`, `user`, `role`), all five byte-identical. They live here
 * now so paginated endpoints cannot drift apart from one another.
 *
 * The field names mirror the backend Spring `Page` serialisation exactly, so no
 * mapping layer is needed.
 */

/**
 * Standard Spring Data paginated response envelope.
 *
 * @typeParam T the item type contained in the current page
 */
export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
  first: boolean;
  last: boolean;
  empty: boolean;
}
