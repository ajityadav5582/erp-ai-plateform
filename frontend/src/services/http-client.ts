import axios from "axios";
import { getEnv } from "@/config/env";

/**
 * Shared Axios instance for the ERP AI Platform API.
 *
 * - Base URL and timeout are sourced from validated environment config.
 * - JSON content type is set by default.
 * - Auth interceptors are attached in `services/interceptors.ts` (wired up
 *   from `services/api.ts` at module load) so this instance stays free of
 *   any Redux/React dependencies.
 */
export const axiosInstance = axios.create({
  baseURL: getEnv().API_BASE_URL,
  timeout: getEnv().API_TIMEOUT_MS,
  headers: {
    "Content-Type": "application/json",
  },
});
