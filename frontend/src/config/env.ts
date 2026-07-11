import { z } from "zod";

/**
 * Runtime environment variable validation.
 *
 * Validates and strongly types `process.env` using Zod. Public (browser-exposed)
 * variables are prefixed with `NEXT_PUBLIC_`. Server-only variables are never
 * sent to the client. Validation runs once at module load so misconfiguration
 * fails fast during build/startup instead of at runtime.
 */

const envSchema = z.object({
  // Public
  NEXT_PUBLIC_APP_URL: z.string().url().default("http://localhost:3000"),
  NEXT_PUBLIC_ENABLE_DEBUG_LOGGING: z
    .enum(["true", "false"])
    .default("false")
    .transform((value) => value === "true"),
  NEXT_PUBLIC_ANALYTICS_KEY: z.string().optional(),

  // Server-only
  NODE_ENV: z
    .enum(["development", "test", "staging", "production"])
    .default("development"),
  API_BASE_URL: z.string().url().default("http://localhost:8080/api/v1"),
  API_TIMEOUT_MS: z.coerce.number().int().positive().default(30000),
});

export type Env = z.infer<typeof envSchema>;

let cachedEnv: Env | null = null;

/**
 * Parses and returns the validated environment. The result is memoized for the
 * lifetime of the process. Throws a descriptive error if validation fails.
 */
export function getEnv(): Env {
  if (cachedEnv) {
    return cachedEnv;
  }

  const parsed = envSchema.safeParse(process.env);

  if (!parsed.success) {
    const issues = parsed.error.issues
      .map((issue) => `  - ${issue.path.join(".") || "(root)"}: ${issue.message}`)
      .join("\n");
    throw new Error(`Invalid environment configuration:\n${issues}`);
  }

  cachedEnv = parsed.data;
  return cachedEnv;
}

/** Convenience flag for client components (safe to import anywhere). */
export const isDebugLoggingEnabled = (): boolean =>
  getEnv().NEXT_PUBLIC_ENABLE_DEBUG_LOGGING;
