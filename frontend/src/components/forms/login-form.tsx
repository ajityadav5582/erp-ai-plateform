"use client";

import { useState } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { AlertTriangle, CheckCircle2, Eye, EyeOff, Loader2 } from "lucide-react";
import { useAuth } from "@/hooks/use-auth";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Checkbox } from "@/components/ui/checkbox";
import { Form, FormControl, FormField, FormItem, FormLabel, FormMessage } from "@/components/ui/form";
import { getApiErrorMessage, isApiErrorCode } from "@/services/error-handler";
import { useRouter } from "next/navigation";
import { RegisterForm } from "@/components/forms/register-form";

const loginSchema = z.object({
  tenantId: z.coerce.number().int("Tenant ID must be a whole number").optional(),
  username: z.string().min(1, "Username or email is required").max(255),
  password: z.string().min(1, "Password is required").max(1024),
  rememberMe: z.boolean(),
});

type LoginFormValues = z.infer<typeof loginSchema>;

export interface AuthFormProps {
  defaultTab?: "login" | "onboard" | "join";
}

export function AuthForm(_props: AuthFormProps) {
  const router = useRouter();
  const { login } = useAuth();
  const [mode, setMode] = useState<"login" | "register">("login");
  const [showPassword, setShowPassword] = useState(false);
  const [apiError, setApiError] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [registeredEmail, setRegisteredEmail] = useState<string | null>(null);

  const form = useForm<LoginFormValues>({
    resolver: zodResolver(loginSchema),
    defaultValues: { tenantId: undefined, username: "", password: "", rememberMe: false },
  });

  const onSubmit = async ({ rememberMe, ...credentials }: LoginFormValues) => {
    setApiError(null);
    setIsSubmitting(true);
    try {
      await login(credentials, { rememberMe });
      router.push("/companies");
      router.refresh();
    } catch (err) {
      setApiError(getApiErrorMessage(err));
    } finally {
      setIsSubmitting(false);
    }
  };

  const isAccountLocked = apiError != null && isApiErrorCode(apiError, "ACCOUNT_LOCKED");

  const showLogin = () => {
    setMode("login");
    setApiError(null);
  };

  if (mode === "register") {
    return (
      <div className="w-full max-w-md space-y-5">
        <div className="space-y-1 text-center sm:text-left">
          <h2 className="text-xl font-semibold tracking-tight">Create your account</h2>
          <p className="text-sm text-muted-foreground">Enter your details to get started. You can add your companies after signing in.</p>
        </div>
        <RegisterForm
          onSuccess={(email) => {
            form.setValue("username", email, { shouldValidate: true });
            setRegisteredEmail(email);
            setApiError(null);
            setMode("login");
          }}
        />
        <p className="text-center text-sm text-muted-foreground">
          Already have an account? <button type="button" onClick={showLogin} className="font-medium text-primary underline-offset-4 hover:underline">Sign in</button>
        </p>
      </div>
    );
  }

  return (
    <div className="w-full max-w-md space-y-4">
      {registeredEmail && (
        <div role="status" className="flex gap-2 rounded-md border border-emerald-500/30 bg-emerald-500/5 px-4 py-3 text-sm text-emerald-700 dark:text-emerald-300">
          <CheckCircle2 className="mt-0.5 size-4 shrink-0" />
          <p>Account created for <span className="font-medium">{registeredEmail}</span>. Sign in to continue.</p>
        </div>
      )}
      {apiError && (
        <div role="alert" className={`mb-4 rounded-md border px-4 py-3 text-sm ${isAccountLocked ? "border-amber-500/50 bg-amber-50 text-amber-800 dark:bg-amber-950/30 dark:text-amber-200" : "border-destructive/50 bg-destructive/10 text-destructive"}`}>
          <div className="flex items-start gap-2">
            {isAccountLocked && <AlertTriangle className="mt-0.5 size-4 shrink-0" />}
            <div>
              <p className="font-medium">{apiError}</p>
              {isAccountLocked && <p className="mt-1 text-xs opacity-80">If you need immediate access, contact your administrator or use the password reset option.</p>}
            </div>
          </div>
        </div>
      )}

      <Form {...form}>
        <form onSubmit={form.handleSubmit(onSubmit)} className="space-y-4">
          <FormField control={form.control} name="tenantId" render={({ field }) => (
            <FormItem>
              <FormLabel>Tenant ID (optional)</FormLabel>
              <FormControl>
                <Input type="number" placeholder="Leave empty to search all tenants" autoComplete="off" disabled={isSubmitting} {...field} value={field.value ?? ""} onChange={(event) => field.onChange(event.target.value === "" ? undefined : Number(event.target.value))} />
              </FormControl>
              <FormMessage />
            </FormItem>
          )} />

          <FormField control={form.control} name="username" render={({ field }) => (
            <FormItem>
              <FormLabel>Username or Email *</FormLabel>
              <FormControl><Input type="text" placeholder="you@example.com" autoComplete="username" disabled={isSubmitting} {...field} /></FormControl>
              <FormMessage />
            </FormItem>
          )} />

          <FormField control={form.control} name="password" render={({ field }) => (
            <FormItem>
              <FormLabel>Password</FormLabel>
              <FormControl>
                <div className="relative">
                  <Input type={showPassword ? "text" : "password"} placeholder="Enter your password" autoComplete="current-password" disabled={isSubmitting} className="pr-10" {...field} />
                  <Button type="button" variant="ghost" size="icon-sm" className="absolute right-1 top-1/2 -translate-y-1/2" onClick={() => setShowPassword((visible) => !visible)}>
                    {showPassword ? <EyeOff className="size-4" /> : <Eye className="size-4" />}
                  </Button>
                </div>
              </FormControl>
              <FormMessage />
            </FormItem>
          )} />

          <div className="flex items-center justify-between">
            <FormField control={form.control} name="rememberMe" render={({ field }) => (
              <FormItem className="flex flex-row items-center gap-2 space-y-0">
                <FormControl><Checkbox checked={field.value} onCheckedChange={(checked) => field.onChange(checked === true)} disabled={isSubmitting} /></FormControl>
                <FormLabel className="cursor-pointer text-sm font-normal">Remember me</FormLabel>
              </FormItem>
            )} />
            <Button type="button" variant="link" size="sm" className="px-0 text-sm" disabled={isSubmitting} onClick={() => router.push("/forgot-password")}>Forgot password?</Button>
          </div>

          <Button type="submit" className="mt-2 w-full" disabled={isSubmitting}>
            {isSubmitting && <Loader2 className="mr-2 size-4 animate-spin" />}
            {isSubmitting ? "Signing in..." : "Sign in"}
          </Button>
        </form>
      </Form>

      <p className="text-center text-sm text-muted-foreground">
        Don&apos;t have an account? <button type="button" onClick={() => { setApiError(null); setMode("register"); }} className="font-medium text-primary underline-offset-4 hover:underline">Create one</button>
      </p>
    </div>
  );
}

/** Backwards-compatible export for existing imports */
export function LoginForm() {
  return <AuthForm defaultTab="login" />;
}
