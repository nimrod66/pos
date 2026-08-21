"use client";

import {
  Pencil,
  Plus,
  UserCheck,
  UserPlus,
  Users,
  UserX,
  X,
} from "lucide-react";
import { useState } from "react";

import { PrimaryButton, SecondaryButton } from "@/components/ui/buttons";
import { Field, FormError, Input } from "@/components/ui/form-controls";
import { PageHeader } from "@/components/ui/page-header";
import { StatusBadge } from "@/components/ui/status-badge";
import { AccessRestricted } from "@/features/auth/components/access-restricted";
import {
  PERMISSIONS,
  ROLE_DEFINITIONS,
  roleLabel,
} from "@/features/auth/access-control";
import { usePermission } from "@/features/auth/hooks/use-permission";
import { useAuthStore } from "@/features/auth/store/auth-store";
import {
  getWorkspaceErrorMessage,
  useWorkspaceQuery,
  workspaceGateway,
} from "@/features/workspace/gateway/workspace-gateway";
import type {
  StaffInput,
  StaffRole,
  StaffUser,
} from "@/features/workspace/types";

export function StaffPage() {
  const staff = useWorkspaceQuery((state) => state.staff);
  const canManageStaff = usePermission(PERMISSIONS.USER_MANAGE);
  const currentUsername = useAuthStore(
    (state) => state.session?.user.username ?? "",
  );
  const [showForm, setShowForm] = useState(false);
  const [editingId, setEditingId] = useState<string | null>(null);
  const [displayName, setDisplayName] = useState("");
  const [username, setUsername] = useState("");
  const [phoneNumber, setPhoneNumber] = useState("");
  const [jobTitle, setJobTitle] = useState("");
  const [password, setPassword] = useState("");
  const [roles, setRoles] = useState<StaffRole[]>(["CASHIER"]);
  const [error, setError] = useState<string | null>(null);
  const [busyStaffId, setBusyStaffId] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const activeOwnerCount = staff.filter(
    (user) => user.status === "ACTIVE" && user.roles.includes("OWNER"),
  ).length;

  function resetForm() {
    setDisplayName("");
    setUsername("");
    setPhoneNumber("");
    setJobTitle("");
    setPassword("");
    setRoles(["CASHIER"]);
    setEditingId(null);
    setError(null);
  }

  function openCreateForm() {
    resetForm();
    setShowForm(true);
  }

  function openEditForm(user: StaffUser) {
    setDisplayName(user.displayName);
    setUsername(user.username);
    setPhoneNumber(user.phoneNumber);
    setJobTitle(user.jobTitle);
    setRoles(user.roles);
    setEditingId(user.id);
    setError(null);
    setShowForm(true);
    window.scrollTo({ top: 0, behavior: "smooth" });
  }

  function closeForm() {
    setShowForm(false);
    resetForm();
  }

  function toggleRole(role: StaffRole) {
    setRoles((current) =>
      current.includes(role)
        ? current.filter((candidate) => candidate !== role)
        : [...current, role],
    );
  }

  async function handleSubmit(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setError(null);
    if (!canManageStaff) {
      setError("Your active roles do not permit staff changes.");
      return;
    }
    if (
      displayName.trim().length < 2 ||
      username.trim().length < 3 ||
      jobTitle.trim().length < 2
    ) {
      setError("Enter a full name, job title, and valid staff email address.");
      return;
    }
    const normalizedPhone = phoneNumber.replace(/[\s-]/g, "");
    if (!/^\+?\d{10,15}$/.test(normalizedPhone)) {
      setError("Enter a valid phone number with 10 to 15 digits.");
      return;
    }
    if (roles.length === 0) {
      setError("Assign at least one access role.");
      return;
    }
    if (!editingId && password.length < 8) {
      setError("Enter a temporary password with at least 8 characters.");
      return;
    }

    setSubmitting(true);
    try {
      const input: StaffInput = {
        displayName: displayName.trim(),
        username: username.trim(),
        phoneNumber: phoneNumber.trim(),
        jobTitle: jobTitle.trim(),
        password: editingId ? undefined : password,
        roles,
      };
      if (editingId) {
        await workspaceGateway.updateStaff(editingId, input);
      } else {
        await workspaceGateway.addStaff(input);
      }
      closeForm();
    } catch (caught) {
      setError(
        getWorkspaceErrorMessage(
          caught,
          `The staff member could not be ${editingId ? "updated" : "added"}.`,
        ),
      );
    } finally {
      setSubmitting(false);
    }
  }

  async function handleStatusChange(user: StaffUser) {
    if (!canManageStaff || busyStaffId) return;
    setError(null);
    setBusyStaffId(user.id);
    try {
      await workspaceGateway.setStaffStatus(
        user.id,
        user.status === "ACTIVE" ? "DISABLED" : "ACTIVE",
      );
    } catch (caught) {
      setError(
        getWorkspaceErrorMessage(
          caught,
          "The staff status could not be changed.",
        ),
      );
    } finally {
      setBusyStaffId(null);
    }
  }

  if (!canManageStaff) {
    return <AccessRestricted />;
  }

  return (
    <div>
      <PageHeader
        title="Staff"
        description="Unique staff accounts for the active pharmacy branch."
        actions={
          <SecondaryButton
            type="button"
            onClick={showForm ? closeForm : openCreateForm}
          >
            {showForm ? (
              <X aria-hidden="true" size={17} />
            ) : (
              <Plus aria-hidden="true" size={17} />
            )}
            {showForm ? "Close" : "Add staff"}
          </SecondaryButton>
        }
      />

      {showForm ? (
        <form
          onSubmit={handleSubmit}
          className="mb-6 rounded-md border border-[var(--border)] bg-white p-4 sm:p-6"
        >
          <div className="flex items-center gap-2">
            <UserPlus
              aria-hidden="true"
              className="text-[var(--brand)]"
              size={18}
            />
            <h2 className="text-base font-semibold">
              {editingId ? "Edit staff member" : "New staff member"}
            </h2>
          </div>

          <div className="mt-4 grid gap-4 md:grid-cols-2 xl:grid-cols-5">
            <Field label="Full name" required>
              <Input
                autoFocus
                value={displayName}
                onChange={(event) => setDisplayName(event.target.value)}
              />
            </Field>
            <Field label="Email address" required>
              <Input
                autoCapitalize="none"
                type="email"
                value={username}
                onChange={(event) => setUsername(event.target.value)}
              />
            </Field>
            <Field label="Phone number" required>
              <Input
                autoComplete="tel"
                inputMode="tel"
                placeholder="e.g. 0712345678"
                type="tel"
                value={phoneNumber}
                onChange={(event) => setPhoneNumber(event.target.value)}
              />
            </Field>
            <Field label="Job title" required>
              <Input
                placeholder="e.g. Pharmacy technician"
                value={jobTitle}
                onChange={(event) => setJobTitle(event.target.value)}
              />
            </Field>
            {!editingId ? (
              <Field label="Temporary password" required>
                <Input
                  autoComplete="new-password"
                  minLength={8}
                  type="password"
                  value={password}
                  onChange={(event) => setPassword(event.target.value)}
                />
              </Field>
            ) : null}
          </div>

          <fieldset className="mt-5 border-t border-[var(--border)] pt-4">
            <legend className="pr-3 text-sm font-semibold">Access roles</legend>
            <div className="mt-3 grid gap-2 md:grid-cols-2 xl:grid-cols-3">
              {ROLE_DEFINITIONS.map((role) => (
                <label
                  key={role.code}
                  className="flex min-h-20 cursor-pointer items-start gap-3 rounded-md border border-[var(--border)] p-3 hover:bg-[var(--surface-muted)]"
                >
                  <input
                    type="checkbox"
                    checked={roles.includes(role.code)}
                    onChange={() => toggleRole(role.code)}
                    className="mt-0.5 size-4 accent-[var(--brand)]"
                  />
                  <span>
                    <span className="block text-sm font-semibold">{role.label}</span>
                    <span className="mt-1 block text-xs text-[var(--text-muted)]">
                      {role.description}
                    </span>
                  </span>
                </label>
              ))}
            </div>
          </fieldset>

          <div className="mt-4">
            <FormError message={error} />
          </div>
          <div className="mt-4 flex justify-end">
            <PrimaryButton type="submit" disabled={submitting}>
              {editingId ? (
                <Pencil aria-hidden="true" size={17} />
              ) : (
                <UserPlus aria-hidden="true" size={17} />
              )}
              {submitting
                ? "Saving..."
                : editingId
                  ? "Save staff member"
                  : "Add staff member"}
            </PrimaryButton>
          </div>
        </form>
      ) : null}

      {!showForm ? <FormError message={error} /> : null}

      <section
        className={`${!showForm && error ? "mt-4 " : ""}rounded-md border border-[var(--border)] bg-white`}
      >
        <div className="border-b border-[var(--border)] px-4 py-3.5">
          <h2 className="flex items-center gap-2 text-sm font-semibold">
            <Users aria-hidden="true" size={17} />
            Team directory
          </h2>
        </div>
        <div className="overflow-x-auto">
          <table className="w-full min-w-[1020px] text-left text-sm">
            <thead className="bg-[var(--surface-muted)] text-xs uppercase text-[var(--text-muted)]">
              <tr>
                <th className="px-4 py-3 font-semibold">Staff member</th>
                <th className="px-4 py-3 font-semibold">Email</th>
                <th className="px-4 py-3 font-semibold">Phone</th>
                <th className="px-4 py-3 font-semibold">Job title</th>
                <th className="px-4 py-3 font-semibold">Access roles</th>
                <th className="px-4 py-3 font-semibold">Status</th>
                <th className="px-4 py-3 text-right font-semibold">Actions</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-[var(--border)]">
              {staff.map((user) => (
                <tr key={user.id}>
                  <td className="px-4 py-3.5 font-semibold">
                    {user.displayName}
                  </td>
                  <td className="px-4 py-3.5 font-mono text-xs">
                    {user.username}
                  </td>
                  <td className="px-4 py-3.5 text-[var(--text-muted)]">
                    {user.phoneNumber || "Not set"}
                  </td>
                  <td className="px-4 py-3.5 text-[var(--text-muted)]">
                    {user.jobTitle}
                  </td>
                  <td className="px-4 py-3.5">
                    <div className="flex flex-wrap gap-1.5">
                      {user.roles.map((role) => (
                        <span
                          key={role}
                          className="rounded-md bg-[var(--surface-muted)] px-2 py-1 text-xs font-medium"
                        >
                          {roleLabel(role)}
                        </span>
                      ))}
                    </div>
                  </td>
                  <td className="px-4 py-3.5">
                    <StatusBadge
                      tone={user.status === "ACTIVE" ? "success" : "neutral"}
                    >
                      {user.status === "ACTIVE" ? "Active" : "Disabled"}
                    </StatusBadge>
                  </td>
                  <td className="px-4 py-3.5">
                    <div className="flex justify-end gap-1">
                      <button
                        type="button"
                        title={
                          user.username.toLowerCase() ===
                          currentUsername.toLowerCase()
                            ? "You cannot edit your signed-in account"
                            : `Edit ${user.displayName}`
                        }
                        aria-label={`Edit ${user.displayName}`}
                        disabled={
                          user.username.toLowerCase() ===
                          currentUsername.toLowerCase()
                        }
                        onClick={() => openEditForm(user)}
                        className="flex size-9 items-center justify-center rounded-md text-[var(--text-muted)] hover:bg-[var(--surface-muted)] hover:text-[var(--text)] disabled:cursor-not-allowed disabled:opacity-35"
                      >
                        <Pencil aria-hidden="true" size={16} />
                      </button>
                      <button
                        type="button"
                        title={
                          user.status === "DISABLED"
                            ? `Enable ${user.displayName}`
                            : user.username.toLowerCase() ===
                                currentUsername.toLowerCase()
                              ? "You cannot disable your signed-in account"
                              : user.roles.includes("OWNER") &&
                                  activeOwnerCount === 1
                                ? "At least one owner must remain active"
                                : `Disable ${user.displayName}`
                        }
                        aria-label={
                          user.status === "DISABLED"
                            ? `Enable ${user.displayName}`
                            : `Disable ${user.displayName}`
                        }
                        disabled={
                          busyStaffId === user.id ||
                          (user.status === "ACTIVE" &&
                            (user.username.toLowerCase() ===
                              currentUsername.toLowerCase() ||
                              (user.roles.includes("OWNER") &&
                                activeOwnerCount === 1)))
                        }
                        onClick={() => void handleStatusChange(user)}
                        className="flex size-9 items-center justify-center rounded-md text-[var(--text-muted)] hover:bg-[var(--surface-muted)] hover:text-[var(--text)] disabled:cursor-not-allowed disabled:opacity-35"
                      >
                        {user.status === "ACTIVE" ? (
                          <UserX aria-hidden="true" size={17} />
                        ) : (
                          <UserCheck aria-hidden="true" size={17} />
                        )}
                      </button>
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </section>
    </div>
  );
}
