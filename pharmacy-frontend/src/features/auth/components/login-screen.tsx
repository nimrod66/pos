"use client";

import { zodResolver } from "@hookform/resolvers/zod";
import {
  Boxes,
  Eye,
  EyeOff,
  LoaderCircle,
  LogIn,
  ReceiptText,
  ScanLine,
  ShieldCheck,
} from "lucide-react";
import { useRouter } from "next/navigation";
import { useEffect, useState } from "react";
import { useForm } from "react-hook-form";
import { z } from "zod";

import { BrandMark } from "@/components/brand-mark";
import { Select } from "@/components/ui/form-controls";
import { homePathForPermissions } from "@/features/auth/access-control";
import {
  DEMO_ACCOUNTS,
  findDemoAccount,
} from "@/features/auth/lib/demo-accounts";
import { useAuthStore } from "@/features/auth/store/auth-store";
import {
  DEMO_ACCOUNTS_VISIBLE,
  DEMO_AUTH_ENABLED,
} from "@/lib/api-config";

const identityLabel = "Email address";
const identityError = "Enter your staff email address.";
const showDemoAccounts = DEMO_AUTH_ENABLED || DEMO_ACCOUNTS_VISIBLE;

const loginSchema = z.object({
  username: z.string().trim().min(3, identityError),
  password: z.string().min(8, "Password must contain at least 8 characters."),
});

type LoginInput = z.infer<typeof loginSchema>;

const visualIcons = [ScanLine, Boxes, ReceiptText, ShieldCheck];

export function LoginScreen() {
  const router = useRouter();
  const [showPassword, setShowPassword] = useState(false);
  const [previewUsername, setPreviewUsername] = useState(
    DEMO_AUTH_ENABLED ? DEMO_ACCOUNTS[0].username : "",
  );
  const signIn = useAuthStore((state) => state.signIn);
  const session = useAuthStore((state) => state.session);
  const status = useAuthStore((state) => state.status);
  const sessionExpired = useAuthStore((state) => state.expired);
  const {
    formState: { errors, isSubmitting },
    handleSubmit,
    register,
    reset,
    setError,
  } = useForm<LoginInput>({
    defaultValues: DEMO_AUTH_ENABLED
      ? {
          username: DEMO_ACCOUNTS[0].username,
          password: DEMO_ACCOUNTS[0].password,
        }
      : { username: "", password: "" },
    resolver: zodResolver(loginSchema),
  });

  useEffect(() => {
    if (status === "authenticated" && session) {
      router.replace(homePathForPermissions(session.user.permissions));
    }
  }, [router, session, status]);

  async function submit(credentials: LoginInput) {
    try {
      const authenticatedSession = await signIn(credentials);
      router.replace(
        homePathForPermissions(authenticatedSession.user.permissions),
      );
    } catch (error) {
      setError("root", {
        message:
          error instanceof Error
            ? error.message
            : "Sign in could not be completed.",
      });
    }
  }

  function selectPreviewAccount(username: string) {
    const account = findDemoAccount(username);
    if (!account) return;
    setPreviewUsername(account.username);
    reset({ username: account.username, password: account.password });
  }

  const disabled = isSubmitting || status === "checking";

  return (
    <main className="grid min-h-screen bg-white lg:grid-cols-[minmax(28rem,1fr)_minmax(30rem,42rem)]">
      <section className="flex min-h-screen items-center px-5 py-10 sm:px-10 lg:px-16">
        <div className="mx-auto w-full max-w-md">
          <div className="mb-10 flex items-center gap-3 lg:hidden">
            <BrandMark />
            <div>
              <p className="text-sm font-semibold">Pharmacy POS</p>
              <p className="text-xs text-[var(--text-muted)]">Staff workspace</p>
            </div>
          </div>

          <div className="mb-8">
            <p className="mb-2 text-xs font-semibold uppercase text-[var(--brand)]">
              Secure staff access
            </p>
            <h1 className="text-3xl font-semibold">Sign in</h1>
            <p className="mt-2 text-sm text-[var(--text-muted)]">
              Continue to the active pharmacy workspace.
            </p>
          </div>

          <form className="space-y-5" onSubmit={handleSubmit(submit)} noValidate>
            {sessionExpired ? (
              <div
                role="status"
                className="rounded-md bg-[var(--warning-soft)] px-3 py-2.5 text-sm text-[var(--warning)]"
              >
                Your session expired. Sign in again to continue — any saved till
                draft is still here.
              </div>
            ) : null}
            {showDemoAccounts ? (
              <div>
                <label
                  className="mb-2 block text-sm font-medium"
                  htmlFor="preview-account"
                >
                  Demo account
                </label>
                <Select
                  id="preview-account"
                  value={previewUsername}
                  onChange={(event) => selectPreviewAccount(event.target.value)}
                >
                  {!DEMO_AUTH_ENABLED ? (
                    <option value="">Choose an account</option>
                  ) : null}
                  {DEMO_ACCOUNTS.map((account) => (
                    <option key={account.username} value={account.username}>
                      {account.label}
                    </option>
                  ))}
                </Select>
              </div>
            ) : null}
            <div>
              <label className="mb-2 block text-sm font-medium" htmlFor="username">
                {identityLabel}
              </label>
              <input
                id="username"
                type="email"
                autoComplete="username"
                aria-invalid={Boolean(errors.username)}
                aria-describedby={errors.username ? "username-error" : undefined}
                className="h-11 w-full rounded-md border border-[var(--border-strong)] bg-white px-3 text-sm outline-none transition-colors placeholder:text-[var(--text-subtle)] focus:border-[var(--brand)] focus:ring-3 focus:ring-[var(--brand-ring)]"
                placeholder="name@pharmacy.com"
                {...register("username")}
              />
              {errors.username ? (
                <p id="username-error" className="mt-1.5 text-xs text-[var(--danger)]">
                  {errors.username.message}
                </p>
              ) : null}
            </div>

            <div>
              <label className="mb-2 block text-sm font-medium" htmlFor="password">
                Password
              </label>
              <div className="relative">
                <input
                  id="password"
                  type={showPassword ? "text" : "password"}
                  autoComplete="current-password"
                  aria-invalid={Boolean(errors.password)}
                  aria-describedby={errors.password ? "password-error" : undefined}
                  className="h-11 w-full rounded-md border border-[var(--border-strong)] bg-white px-3 pr-11 text-sm outline-none transition-colors placeholder:text-[var(--text-subtle)] focus:border-[var(--brand)] focus:ring-3 focus:ring-[var(--brand-ring)]"
                  placeholder="Password"
                  {...register("password")}
                />
                <button
                  type="button"
                  aria-label={showPassword ? "Hide password" : "Show password"}
                  title={showPassword ? "Hide password" : "Show password"}
                  className="absolute right-1 top-1 flex size-9 items-center justify-center rounded-md text-[var(--text-muted)] hover:bg-[var(--surface-muted)] hover:text-[var(--text)]"
                  onClick={() => setShowPassword((visible) => !visible)}
                >
                  {showPassword ? (
                    <EyeOff aria-hidden="true" size={17} />
                  ) : (
                    <Eye aria-hidden="true" size={17} />
                  )}
                </button>
              </div>
              {errors.password ? (
                <p id="password-error" className="mt-1.5 text-xs text-[var(--danger)]">
                  {errors.password.message}
                </p>
              ) : null}
            </div>

            {errors.root ? (
              <div
                role="alert"
                className="rounded-md border border-[var(--danger-border)] bg-[var(--danger-soft)] px-3 py-2.5 text-sm text-[var(--danger)]"
              >
                {errors.root.message}
              </div>
            ) : null}

            <button
              type="submit"
              disabled={disabled}
              className="flex h-11 w-full items-center justify-center gap-2 rounded-md bg-[var(--brand)] px-4 text-sm font-semibold text-white transition-colors hover:bg-[var(--brand-strong)] focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-[var(--brand)] disabled:cursor-wait disabled:opacity-60"
            >
              {disabled ? (
                <LoaderCircle aria-hidden="true" className="animate-spin" size={17} />
              ) : (
                <LogIn aria-hidden="true" size={17} />
              )}
              {status === "checking" ? "Checking session" : "Sign in"}
            </button>
          </form>

          <p className="mt-8 text-xs text-[var(--text-subtle)]">
            Access is limited to authorized pharmacy staff.
          </p>
        </div>
      </section>

      <section className="relative hidden min-h-screen overflow-hidden bg-[var(--brand-deep)] px-12 py-12 text-white lg:flex lg:flex-col">
        <div className="flex items-center gap-3">
          <BrandMark inverse />
          <div>
            <p className="text-sm font-semibold">Pharmacy POS</p>
            <p className="text-xs text-white/65">Main branch workspace</p>
          </div>
        </div>

        <div className="my-auto max-w-lg">
          <p className="mb-3 text-xs font-semibold uppercase text-[#8ee8d9]">
            {DEMO_AUTH_ENABLED ? "Local preview" : "Local pharmacy node"}
          </p>
          <h2 className="max-w-md text-4xl font-semibold leading-tight">
            Daily pharmacy operations in one focused workspace.
          </h2>

          <div className="mt-10 grid w-full max-w-md grid-cols-2 border border-white/20">
            {visualIcons.map((Icon, index) => (
              <div
                key={index}
                className={`flex h-28 items-center justify-center text-[#a9eee2] ${
                  index % 2 === 1 ? "border-l border-white/20" : ""
                } ${index > 1 ? "border-t border-white/20" : ""}`}
              >
                <Icon aria-hidden="true" size={30} strokeWidth={1.7} />
              </div>
            ))}
          </div>
        </div>

        <div className="flex items-center gap-3 text-xs text-white/60">
          <span>KES</span>
          <span aria-hidden="true">/</span>
          <span>Africa/Nairobi</span>
          <span aria-hidden="true">/</span>
          <span>/api/v1</span>
        </div>
      </section>
    </main>
  );
}
