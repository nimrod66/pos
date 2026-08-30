"use client";

import { useEffect, useMemo, useState } from "react";
import { ChevronLeft, ChevronRight } from "lucide-react";

/**
 * Client-side pagination for table data that is already fully loaded.
 * Keeps page state, clamps when the dataset shrinks, and exposes slices.
 */
export function usePagination<T>(rows: T[], pageSize = 25) {
  const [page, setPage] = useState(1);
  const total = rows.length;
  const pageCount = Math.max(1, Math.ceil(total / pageSize));

  useEffect(() => {
    if (page > pageCount) setPage(pageCount);
  }, [page, pageCount]);

  const pageRows = useMemo(() => {
    const start = (page - 1) * pageSize;
    return rows.slice(start, start + pageSize);
  }, [rows, page, pageSize]);

  return { page, setPage, pageCount, total, pageRows, pageSize };
}

interface PaginationControlsProps {
  page: number;
  pageCount: number;
  total: number;
  pageSize: number;
  onPage(page: number): void;
}

export function PaginationControls({
  page,
  pageCount,
  total,
  pageSize,
  onPage,
}: PaginationControlsProps) {
  if (total === 0) return null;
  const from = (page - 1) * pageSize + 1;
  const to = Math.min(total, page * pageSize);
  return (
    <div className="flex items-center justify-between border-t border-[var(--border)] px-4 py-3 text-xs text-[var(--text-muted)]">
      <span>
        Showing {from}–{to} of {total}
      </span>
      {pageCount > 1 ? (
        <div className="flex items-center gap-1">
          <button
            type="button"
            aria-label="Previous page"
            disabled={page <= 1}
            onClick={() => onPage(page - 1)}
            className="flex size-8 items-center justify-center rounded-md border border-[var(--border)] disabled:opacity-40 hover:bg-[var(--surface-muted)]"
          >
            <ChevronLeft aria-hidden="true" size={15} />
          </button>
          <span className="px-2 font-semibold text-[var(--text)]">
            {page} / {pageCount}
          </span>
          <button
            type="button"
            aria-label="Next page"
            disabled={page >= pageCount}
            onClick={() => onPage(page + 1)}
            className="flex size-8 items-center justify-center rounded-md border border-[var(--border)] disabled:opacity-40 hover:bg-[var(--surface-muted)]"
          >
            <ChevronRight aria-hidden="true" size={15} />
          </button>
        </div>
      ) : null}
    </div>
  );
}
