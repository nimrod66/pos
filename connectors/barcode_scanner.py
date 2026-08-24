"""
Barcode scanner input handler
Most scanners work as keyboard wedge — input goes directly to the focused field.
This module handles serial/USB scanner input for background scanning.
"""
import logging
import threading

import serial

logger = logging.getLogger(__name__)


class BarcodeScanner:
    def __init__(self, config=None):
        self.config = config or {}
        self.mode = self.config.get("mode", "keyboard_wedge")
        self.com_port = self.config.get("com_port", "COM2")
        self.baud_rate = int(self.config.get("baud_rate", 9600))
        self._last_barcode = None
        self._listener = None
        self._running = False
        self._connected = self.mode == "keyboard_wedge"
        if self.mode == "serial":
            self.start_listener()

    def status(self):
        return {
            "type": self.mode,
            "ready": self._connected,
            "com_port": self.com_port if self.mode == "serial" else None,
            "last_barcode": self._last_barcode
        }

    def last_scanned(self):
        barcode = self._last_barcode
        self._last_barcode = None
        return barcode

    def set_barcode(self, barcode):
        """Called by HTTP endpoint when the frontend sends a barcode"""
        self._last_barcode = barcode
        logger.info(f"Barcode captured: {barcode}")

    def start_listener(self):
        """Listen for newline-terminated barcodes from a serial scanner."""
        def _listen():
            self._running = True
            while self._running:
                try:
                    with serial.Serial(
                        self.com_port,
                        self.baud_rate,
                        timeout=0.5,
                    ) as scanner_port:
                        self._connected = True
                        logger.info(
                            "Listening for scanner input on %s at %s baud",
                            self.com_port,
                            self.baud_rate,
                        )
                        while self._running:
                            barcode = scanner_port.readline().decode(
                                "ascii",
                                errors="ignore",
                            ).strip()
                            if barcode:
                                self.set_barcode(barcode)
                except serial.SerialException as error:
                    self._connected = False
                    logger.warning("Scanner %s unavailable: %s", self.com_port, error)
                    threading.Event().wait(2)
                except Exception as error:
                    self._connected = False
                    logger.error("Scanner listener failed: %s", error)
                    threading.Event().wait(2)

        self._listener = threading.Thread(target=_listen, daemon=True)
        self._listener.start()

    def stop(self):
        self._running = False
        self._connected = False
