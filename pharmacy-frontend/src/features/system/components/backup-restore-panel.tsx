"use client";

import { useCallback, useEffect, useRef, useState } from "react";
import { Download, Loader2, RefreshCw, Trash2, Upload } from "lucide-react";

import { PrimaryButton, SecondaryButton } from "@/components/ui/buttons";
import { apiRequest } from "@/lib/api-client";
import { getCsrfHeaderName, getCsrfToken } from "@/lib/csrf-token";
import { getApiBaseUrl } from "@/lib/api-config";

interface BackupEntry {
  filename: string;
  sizeBytes: number;
  createdAt: string;
  status: string;
}

function formatSize(bytes: number): string {
  if (bytes < 1024) return bytes + " B";
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + " KB";
  return (bytes / (1024 * 1024)).toFixed(1) + " MB";
}

function timeAgo(dateStr: string): string {
  const diff = Date.now() - new Date(dateStr).getTime();
  const mins = Math.floor(diff / 60000);
  if (mins < 1) return "just now";
  if (mins < 60) return mins + "m ago";
  const hours = Math.floor(mins / 60);
  if (hours < 24) return hours + "h ago";
  const days = Math.floor(hours / 24);
  return days + "d ago";
}

export function BackupRestorePanel() {
  const [backups, setBackups] = useState<BackupEntry[]>([]);
  const [loading, setLoading] = useState(true);
  const [creating, setCreating] = useState(false);
  const [restoring, setRestoring] = useState(false);
  const [restoreConfirm, setRestoreConfirm] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);
  const fileInputRef = useRef<HTMLInputElement>(null);

  const loadBackups = useCallback(async () => {
    try {
      const res = await apiRequest<BackupEntry[]>("/system/backup/list", { cache: "no-store" });
      setBackups(res.data ?? []);
    } catch {
      setBackups([]);
    }
  }, []);

  useEffect(() => {
    setLoading(true);
    loadBackups().finally(() => setLoading(false));
  }, [loadBackups]);

  async function handleCreateBackup() {
    setCreating(true);
    setError(null);
    setSuccess(null);
    try {
      const res = await apiRequest<BackupEntry>("/system/backup", { method: "POST" });
      setSuccess("Backup created: " + res.data.filename);
      await loadBackups();
    } catch (e: unknown) {
      const msg = e instanceof Error ? e.message : "Backup failed";
      setError(msg);
    } finally {
      setCreating(false);
    }
  }

  function handleDownload(filename: string) {
    const link = document.createElement("a");
    link.href = `${getApiBaseUrl()}/system/backup/download/${encodeURIComponent(filename)}`;
    link.download = filename;
    link.click();
  }

  async function handleRestore() {
    const file = fileInputRef.current?.files?.[0];
    if (!file) {
      setError("Choose a .dump file first.");
      return;
    }
    if (restoreConfirm !== "RESTORE") {
      setError("Type RESTORE to confirm.");
      return;
    }
    setRestoring(true);
    setError(null);
    setSuccess(null);
    try {
      const formData = new FormData();
      formData.append("file", file);
      formData.append("confirm", "true");
      const csrfHeader = getCsrfHeaderName();
      const csrfValue = getCsrfToken();
      const headers: Record<string, string> = {};
      if (csrfHeader && csrfValue) headers[csrfHeader] = csrfValue;
      const res = await fetch(`${getApiBaseUrl()}/system/backup/restore`, {
        method: "POST",
        body: formData,
        credentials: "include",
        headers,
      });
      if (!res.ok) {
        const body = await res.json().catch(() => null);
        throw new Error(body?.message || "Restore failed");
      }
      setSuccess("Restore complete. The page will reload.");
      setTimeout(() => window.location.reload(), 2000);
    } catch (e: unknown) {
      const msg = e instanceof Error ? e.message : "Restore failed";
      setError(msg);
    } finally {
      setRestoring(false);
    }
  }

  async function handleDelete(filename: string) {
    try {
      await apiRequest(`/system/backup/${encodeURIComponent(filename)}`, { method: "DELETE" });
      await loadBackups();
    } catch {
      setError("Failed to delete backup.");
    }
  }

  return (
    <section className="rounded-md border border-[var(--border)] bg-white p-5">
      <h2 className="mb-4 text-sm font-semibold">Backup & Restore</h2>

      {error && (
        <div className="mb-3 rounded bg-[var(--danger-soft)] px-3 py-2 text-xs text-[var(--danger)]">
          {error}
        </div>
      )}
      {success && (
        <div className="mb-3 rounded bg-emerald-50 px-3 py-2 text-xs text-emerald-700">
          {success}
        </div>
      )}

      <div className="mb-4 flex items-center gap-3">
        <PrimaryButton
          type="button"
          onClick={handleCreateBackup}
          disabled={creating || restoring}
          className="inline-flex items-center gap-2"
        >
          {creating ? <Loader2 size={14} className="animate-spin" /> : <Download size={14} />}
          {creating ? "Creating..." : "Create backup now"}
        </PrimaryButton>
        <SecondaryButton
          type="button"
          onClick={() => { setLoading(true); loadBackups().finally(() => setLoading(false)); }}
          disabled={loading}
          className="inline-flex items-center gap-2"
        >
          <RefreshCw size={14} className={loading ? "animate-spin" : ""} />
          Refresh
        </SecondaryButton>
      </div>

      {loading ? (
        <p className="text-xs text-[var(--text-muted)]">Loading backups...</p>
      ) : backups.length === 0 ? (
        <p className="text-xs text-[var(--text-muted)]">No backups found.</p>
      ) : (
        <div className="mb-5 space-y-2">
          {backups.map((b) => (
            <div key={b.filename} className="flex items-center justify-between rounded border border-[var(--border)] px-3 py-2 text-sm">
              <div className="min-w-0 flex-1">
                <p className="truncate font-mono text-xs">{b.filename}</p>
                <p className="text-xs text-[var(--text-muted)]">
                  {formatSize(b.sizeBytes)} &middot; {timeAgo(b.createdAt)}
                </p>
              </div>
              <div className="flex items-center gap-2">
                <button
                  type="button"
                  onClick={() => handleDownload(b.filename)}
                  className="inline-flex items-center gap-1 rounded border border-[var(--border)] px-2 py-1 text-xs hover:bg-[var(--surface-muted)]"
                >
                  <Download size={12} /> Download
                </button>
                <button
                  type="button"
                  onClick={() => handleDelete(b.filename)}
                  className="inline-flex items-center gap-1 rounded border border-[var(--border)] px-2 py-1 text-xs text-[var(--danger)] hover:bg-red-50"
                >
                  <Trash2 size={12} />
                </button>
              </div>
            </div>
          ))}
        </div>
      )}

      <div className="border-t border-[var(--border)] pt-4">
        <h3 className="mb-2 text-xs font-semibold text-[var(--danger)]">Restore from backup</h3>
        <p className="mb-3 text-xs text-[var(--text-muted)]">
          This will REPLACE the current database. A safety backup is created automatically before restore.
        </p>
        <div className="flex flex-col gap-3 sm:flex-row sm:items-end">
          <label className="flex-1 text-xs">
            <span className="mb-1 block text-[var(--text-muted)]">Select .dump file</span>
            <input
              ref={fileInputRef}
              type="file"
              accept=".dump"
              className="block w-full rounded border border-[var(--border)] px-2 py-1.5 text-xs"
            />
          </label>
          <label className="w-full text-xs sm:w-48">
            <span className="mb-1 block text-[var(--text-muted)]">Type RESTORE to confirm</span>
            <input
              type="text"
              value={restoreConfirm}
              onChange={(e) => setRestoreConfirm(e.target.value)}
              placeholder="RESTORE"
              className="block w-full rounded border border-[var(--border)] px-2 py-1.5 font-mono text-xs"
            />
          </label>
          <PrimaryButton
            type="button"
            onClick={handleRestore}
            disabled={restoring || creating || restoreConfirm !== "RESTORE"}
            className="inline-flex items-center gap-2 self-end"
          >
            {restoring ? <Loader2 size={14} className="animate-spin" /> : <Upload size={14} />}
            {restoring ? "Restoring..." : "Restore"}
          </PrimaryButton>
        </div>
      </div>
    </section>
  );
}
