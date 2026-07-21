"""
Barcode scanner input handler
Most scanners work as keyboard wedge — input goes directly to the focused field.
This module handles serial/USB scanner input for background scanning.
"""
import logging
import threading

logger = logging.getLogger(__name__)


class BarcodeScanner:
    def __init__(self, config=None):
        self.config = config or {}
        self._last_barcode = None
        self._listener = None
        self._running = False

    def status(self):
        return {
            "type": "keyboard_wedge",
            "ready": True,
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

    def start_listener(self, device_path="/dev/hidraw0"):
        """Listen for raw HID scanner input (optional, for serial scanners)"""
        def _listen():
            self._running = True
            try:
                with open(device_path, "rb") as f:
                    while self._running:
                        char = f.read(1)
                        logger.debug(f"Scanner raw: {char}")
            except FileNotFoundError:
                logger.warning(f"Scanner device {device_path} not found — keyboard wedge mode")
            except Exception as e:
                logger.error(f"Scanner error: {e}")

        self._listener = threading.Thread(target=_listen, daemon=True)
        self._listener.start()

    def stop(self):
        self._running = False
