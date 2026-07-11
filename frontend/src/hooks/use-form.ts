"use client";

import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import type { ZodSchema } from "zod";

/**
 * Generic form hook that integrates React Hook Form with Zod validation.
 *
 * @param schema - Zod schema for validation
 * @param defaultValues - Default form values
 * @param options - Additional react-hook-form options
 *
 * @example
 * ```tsx
 * const schema = z.object({ email: z.string().email(), password: z.string().min(8) });
 * const { register, handleSubmit, formState: { errors } } = useFormWithZod<LoginForm>({
 *   schema,
 *   defaultValues: { email: "", password: "" },
 * });
 * ```
 */
export function useFormWithZod<T extends Record<string, unknown>>(
  schema: ZodSchema<T>,
  defaultValues: T,
  options?: Omit<Parameters<typeof useForm<T>>[0], "resolver" | "defaultValues">
) {
  return useForm<T>({
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    resolver: zodResolver(schema as any),
    defaultValues,
    mode: "onBlur",
    ...options,
  } as Parameters<typeof useForm<T>>[0]);
}
