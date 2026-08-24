"use client";

import { AlertTriangle, Bell, CheckCircle2, Clock3, PackageMinus, X } from "lucide-react";
import Link from "next/link";
import { useCallback, useEffect, useState } from "react";

import {
  type PharmacyNotification,
  notificationGateway,
} from "@/features/notifications/notification-gateway";

function notificationIcon(type: PharmacyNotification["type"]) {
  if (type === "LOW_STOCK") return PackageMinus;
  if (type === "SHIFT_REMINDER") return Clock3;
  if (type === "SALE_COMPLETED") return CheckCircle2;
  return AlertTriangle;
}

function notificationHref(notification: PharmacyNotification) {
  if (notification.type === "SALE_COMPLETED" && notification.referenceId) {
    return `/sales/${notification.referenceId}`;
  }
  if (notification.type === "SHIFT_REMINDER") return "/shifts/current";
  if (notification.type === "LOW_STOCK" || notification.type === "EXPIRY_WARNING") {
    return "/inventory";
  }
  return null;
}

function relativeTime(value: string) {
  const minutes = Math.max(0, Math.round((Date.now() - new Date(value).getTime()) / 60_000));
  if (minutes < 1) return "Now";
  if (minutes < 60) return `${minutes}m`;
  const hours = Math.floor(minutes / 60);
  if (hours < 24) return `${hours}h`;
  return `${Math.floor(hours / 24)}d`;
}

export function NotificationMenu({ branchId }: { branchId: string }) {
  const [items, setItems] = useState<PharmacyNotification[]>([]);
  const [loading, setLoading] = useState(true);

  const load = useCallback(async () => {
    try {
      setItems(await notificationGateway.list(branchId));
    } catch {
      setItems([]);
    } finally {
      setLoading(false);
    }
  }, [branchId]);

  useEffect(() => {
    const initial = window.setTimeout(() => void load(), 0);
    const interval = window.setInterval(() => void load(), 60_000);
    window.addEventListener("focus", load);
    return () => {
      window.clearTimeout(initial);
      window.clearInterval(interval);
      window.removeEventListener("focus", load);
    };
  }, [load]);

  const unread = items.filter((item) => item.status === "UNREAD").length;

  async function markRead(item: PharmacyNotification) {
    if (item.status !== "UNREAD") return;
    try {
      const updated = await notificationGateway.markRead(item.id);
      setItems((current) => current.map((candidate) =>
        candidate.id === item.id ? updated : candidate,
      ));
    } catch {
      // The next poll restores the authoritative state.
    }
  }

  async function dismiss(id: string) {
    try {
      await notificationGateway.dismiss(id);
      setItems((current) => current.filter((item) => item.id !== id));
    } catch {
      // Keep the notification visible when dismissal fails.
    }
  }

  return (
    <details className="group relative">
      <summary className="relative flex size-10 cursor-pointer list-none items-center justify-center rounded-md text-[var(--text-muted)] hover:bg-[var(--surface-muted)]" aria-label={`${unread} unread notifications`} title="Notifications">
        <Bell aria-hidden="true" size={19} />
        {unread ? <span className="absolute right-1 top-1 flex min-w-4 items-center justify-center rounded-full bg-[var(--danger)] px-1 text-[10px] font-bold leading-4 text-white">{unread > 9 ? "9+" : unread}</span> : null}
      </summary>
      <div className="absolute right-0 mt-2 w-[min(380px,calc(100vw-2rem))] overflow-hidden rounded-md border border-[var(--border)] bg-white shadow-lg">
        <div className="flex items-center justify-between border-b border-[var(--border)] px-4 py-3">
          <h2 className="text-sm font-semibold">Notifications</h2>
          <span className="text-xs text-[var(--text-muted)]">{unread} unread</span>
        </div>
        <div className="max-h-[420px] overflow-y-auto">
          {loading ? <p className="px-4 py-6 text-center text-sm text-[var(--text-muted)]">Loading...</p> : items.length ? items.map((item) => {
            const Icon = notificationIcon(item.type);
            const href = notificationHref(item);
            const content = <><span className="mt-0.5 flex size-8 shrink-0 items-center justify-center rounded-md bg-[var(--brand-soft)] text-[var(--brand-strong)]"><Icon aria-hidden="true" size={16} /></span><span className="min-w-0 flex-1"><span className="flex items-start justify-between gap-3"><span className="text-sm font-semibold">{item.title}</span><span className="shrink-0 text-[10px] text-[var(--text-subtle)]">{relativeTime(item.createdAt)}</span></span><span className="mt-1 block text-xs leading-5 text-[var(--text-muted)]">{item.message}</span></span></>;
            return <div key={item.id} className={`relative border-b border-[var(--border)] last:border-0 ${item.status === "UNREAD" ? "bg-[var(--brand-soft)]/35" : ""}`}>
              {href ? <Link href={href} onClick={() => void markRead(item)} className="flex gap-3 py-3 pl-4 pr-11 hover:bg-[var(--surface-muted)]">{content}</Link> : <button type="button" onClick={() => void markRead(item)} className="flex w-full gap-3 py-3 pl-4 pr-11 text-left hover:bg-[var(--surface-muted)]">{content}</button>}
              <button type="button" title="Dismiss notification" aria-label={`Dismiss ${item.title}`} onClick={() => void dismiss(item.id)} className="absolute right-2 top-2 flex size-8 items-center justify-center rounded-md text-[var(--text-subtle)] hover:bg-white hover:text-[var(--text)]"><X aria-hidden="true" size={14} /></button>
            </div>;
          }) : <p className="px-4 py-8 text-center text-sm text-[var(--text-muted)]">You are all caught up.</p>}
        </div>
      </div>
    </details>
  );
}
