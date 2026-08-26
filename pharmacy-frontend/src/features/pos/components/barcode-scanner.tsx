"use client";

import { useCallback, useEffect, useRef, useState } from "react";
import { Camera, X, Zap, ZapOff } from "lucide-react";

import { PrimaryButton } from "@/components/ui/buttons";
import { FormError } from "@/components/ui/form-controls";
import { cn } from "@/lib/cn";

interface BarcodeScannerProps {
  onDetected: (barcode: string) => void;
  onClose: () => void;
}

export function BarcodeScanner({ onDetected, onClose }: BarcodeScannerProps) {
  const videoRef = useRef<HTMLVideoElement>(null);
  const controlsRef = useRef<{ stop: () => void } | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [torchOn, setTorchOn] = useState(false);
  const [torchSupported, setTorchSupported] = useState(false);
  const streamRef = useRef<MediaStream | null>(null);
  const lastScanRef = useRef<{ text: string; at: number }>({ text: "", at: 0 });

  const startCamera = useCallback(async () => {
    try {
      const media = await navigator.mediaDevices.getUserMedia({
        video: { facingMode: "environment", width: { ideal: 1280 }, height: { ideal: 720 } },
      });
      streamRef.current = media;
      if (videoRef.current) {
        videoRef.current.srcObject = media;
        // Check torch support
        const track = media.getVideoTracks()[0];
        const caps = track.getCapabilities?.() as
          & Record<string, unknown>
          & { torch?: boolean };
        setTorchSupported(Boolean(caps?.torch));
      }

      const { BrowserMultiFormatReader } = await import("@zxing/browser");
      const reader = new BrowserMultiFormatReader();
      const controls = await reader.decodeFromVideoElement(videoRef.current!, (result) => {
        if (!result) return;
        const text = result.getText();
        const now = Date.now();
        // Debounce: same barcode within 2s is a duplicate read
        if (text === lastScanRef.current.text && now - lastScanRef.current.at < 2000) return;
        lastScanRef.current = { text, at: now };
        controls.stop();
        onDetected(text);
      });
      controlsRef.current = controls;
    } catch (err) {
      setError(
        err instanceof Error && err.name === "NotAllowedError"
          ? "Camera access denied. Allow camera in your browser settings."
          : "Camera could not be started on this device.",
      );
    }
  }, [onDetected]);

  useEffect(() => {
    let cancelled = false;
    const timer = window.setTimeout(() => {
      if (cancelled) return;
      void startCamera();
    }, 0);
    return () => {
      cancelled = true;
      window.clearTimeout(timer);
      controlsRef.current?.stop();
      streamRef.current?.getTracks().forEach((track) => track.stop());
    };
  }, [startCamera]);

  async function toggleTorch() {
    if (!streamRef.current) return;
    const track = streamRef.current.getVideoTracks()[0];
    try {
      await track.applyConstraints({ advanced: [{ torch: !torchOn }] });
      setTorchOn((prev) => !prev);
    } catch {
      /* torch not supported after all */
    }
  }

  return (
    <div className="fixed inset-0 z-[60] flex flex-col bg-black">
      <div className="flex items-center justify-between px-4 py-3 text-white">
        <span className="text-sm font-semibold">Scan barcode</span>
        <div className="flex gap-2">
          {torchSupported ? (
            <button
              type="button"
              onClick={() => void toggleTorch()}
              aria-label={torchOn ? "Turn off flash" : "Turn on flash"}
              className={cn(
                "flex size-10 items-center justify-center rounded-full",
                torchOn ? "bg-yellow-400 text-black" : "bg-white/20 text-white",
              )}
            >
              {torchOn ? <ZapOff size={18} /> : <Zap size={18} />}
            </button>
          ) : null}
          <button
            type="button"
            onClick={onClose}
            aria-label="Close scanner"
            className="flex size-10 items-center justify-center rounded-full bg-white/20 text-white"
          >
            <X size={20} />
          </button>
        </div>
      </div>
      <div className="relative flex-1 overflow-hidden">
        <video
          ref={videoRef}
          autoPlay
          playsInline
          muted
          className="h-full w-full object-cover"
        />
        {/* Scan guide overlay */}
        <div className="pointer-events-none absolute inset-0 flex items-center justify-center">
          <div className="h-48 w-72 rounded-lg border-2 border-white/60 shadow-[0_0_0_9999px_rgba(0,0,0,0.35)]" />
        </div>
        <p className="absolute bottom-6 left-0 right-0 text-center text-xs text-white/70">
          Point the camera at a product barcode
        </p>
      </div>
      {error ? (
        <div className="p-4">
          <FormError message={error} />
          <PrimaryButton type="button" className="mt-3 w-full" onClick={onClose}>
            <Camera aria-hidden="true" size={16} /> Close scanner
          </PrimaryButton>
        </div>
      ) : null}
    </div>
  );
}
