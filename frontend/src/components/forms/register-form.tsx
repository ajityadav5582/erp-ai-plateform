"use client";

import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { Loader2 } from "lucide-react";
import { useOnboardMutation } from "@/services/auth.service";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Form, FormControl, FormField, FormItem, FormLabel, FormMessage } from "@/components/ui/form";
import { getApiErrorMessage } from "@/services/error-handler";
import { toast } from "sonner";
import { useState } from "react";

const signupSchema = z.object({
  firstName: z.string().trim().min(1, "First name is required").max(100),
  lastName: z.string().trim().min(1, "Last name is required").max(100),
  email: z.string().trim().email("Enter a valid email address").max(100),
  mobile: z.string().trim().min(1, "Mobile or phone is required").max(20),
  password: z.string().min(6, "Password must be at least 6 characters").max(100),
  confirmPassword: z.string().min(1, "Confirm your password"),
}).refine((data) => data.password === data.confirmPassword, {
  message: "Passwords do not match",
  path: ["confirmPassword"],
});

type SignupValues = z.infer<typeof signupSchema>;

export function RegisterForm({ onSuccess }: { onSuccess: (email: string) => void }) {
  const [error, setError] = useState<string | null>(null);
  const [signup, { isLoading }] = useOnboardMutation();
  const form = useForm<SignupValues>({
    resolver: zodResolver(signupSchema),
    defaultValues: { firstName: "", lastName: "", email: "", mobile: "", password: "", confirmPassword: "" },
  });

  const onSubmit = async ({ confirmPassword, ...details }: SignupValues) => {
    setError(null);
    try {
      await signup({ ...details, confirmPassword }).unwrap();
      toast.success("Account created successfully", { description: "You can now log in with your email and password." });
      onSuccess(details.email);
    } catch (err) {
      setError(getApiErrorMessage(err));
    }
  };

  return (
    <Form {...form}>
      <form onSubmit={form.handleSubmit(onSubmit)} className="space-y-4">
        {error && <div role="alert" className="rounded-md border border-destructive/50 bg-destructive/10 px-4 py-3 text-sm text-destructive">{error}</div>}
        <FormField control={form.control} name="firstName" render={({ field }) => <FormItem><FormLabel>First Name</FormLabel><FormControl><Input autoComplete="given-name" placeholder="John" disabled={isLoading} {...field} /></FormControl><FormMessage /></FormItem>} />
        <FormField control={form.control} name="lastName" render={({ field }) => <FormItem><FormLabel>Last Name</FormLabel><FormControl><Input autoComplete="family-name" placeholder="Smith" disabled={isLoading} {...field} /></FormControl><FormMessage /></FormItem>} />
        <FormField control={form.control} name="email" render={({ field }) => <FormItem><FormLabel>Email</FormLabel><FormControl><Input type="email" autoComplete="email" placeholder="john@example.com" disabled={isLoading} {...field} /></FormControl><FormMessage /></FormItem>} />
        <FormField control={form.control} name="mobile" render={({ field }) => <FormItem><FormLabel>Mobile/Phone</FormLabel><FormControl><Input type="tel" autoComplete="tel" placeholder="+9779812345678" disabled={isLoading} {...field} /></FormControl><FormMessage /></FormItem>} />
        <FormField control={form.control} name="password" render={({ field }) => <FormItem><FormLabel>Password</FormLabel><FormControl><Input type="password" autoComplete="new-password" disabled={isLoading} {...field} /></FormControl><FormMessage /></FormItem>} />
        <FormField control={form.control} name="confirmPassword" render={({ field }) => <FormItem><FormLabel>Confirm Password</FormLabel><FormControl><Input type="password" autoComplete="new-password" disabled={isLoading} {...field} /></FormControl><FormMessage /></FormItem>} />
        <Button type="submit" className="w-full" disabled={isLoading}>{isLoading && <Loader2 className="mr-2 size-4 animate-spin" />}{isLoading ? "Creating Account…" : "Create Account"}</Button>
      </form>
    </Form>
  );
}
