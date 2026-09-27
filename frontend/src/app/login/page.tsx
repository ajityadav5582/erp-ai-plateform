import { siteConfig } from "@/config/site";
import { AuthForm } from "@/components/forms/login-form";

export default function LoginPage() {
  return (
    <div className="flex min-h-screen flex-col items-center justify-center bg-background px-4 py-12 sm:px-6 lg:px-8">
      <div className="w-full max-w-md space-y-6">
        <div className="text-center">
          <h1 className="text-3xl font-bold tracking-tight text-foreground">
            {siteConfig.name}
          </h1>
          <p className="mt-2 text-sm text-muted-foreground">
            Sign in or create an account to get started
          </p>
        </div>

        <div className="rounded-lg border border-border bg-card p-6 shadow-sm sm:p-8">
          <AuthForm defaultTab="login" />
        </div>

        <p className="text-center text-xs text-muted-foreground">
          By signing in or registering, you agree to our{" "}
          <a href="/terms" className="underline underline-offset-4 hover:text-foreground">
            Terms of Service
          </a>{" "}
          and{" "}
          <a href="/privacy" className="underline underline-offset-4 hover:text-foreground">
            Privacy Policy
          </a>
          .
        </p>
      </div>
    </div>
  );
}
