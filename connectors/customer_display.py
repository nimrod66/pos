"""
Customer-facing pole display (LCD/VFD)
Typically 2 lines x 20 characters.
Connected via COM/Serial/USB.
"""
import logging
import serial

logger = logging.getLogger(__name__)


class CustomerDisplay:
    def __init__(self, config=None):
        self.config = config or {}
        self.enabled = self.config.get("enabled", False)
        self.com_port = self.config.get("com_port", "COM5")
        self.baud_rate = self.config.get("baud_rate", 9600)
        self.lines = self.config.get("lines", 2)
        self.columns = self.config.get("columns", 20)

    def status(self):
        return {
            "enabled": self.enabled,
            "com_port": self.com_port,
            "lines": self.lines,
            "columns": self.columns,
            "connected": self._test_connection() if self.enabled else False,
        }

    def _test_connection(self):
        try:
            with serial.Serial(self.com_port, self.baud_rate, timeout=1):
                return True
        except Exception:
            return False

    def show(self, line1, line2=""):
        if not self.enabled:
            logger.debug(f"Display disabled — would show: '{line1}' / '{line2}'")
            return

        line1 = self._pad(line1)
        line2 = self._pad(line2)

        try:
            with serial.Serial(self.com_port, self.baud_rate, timeout=1) as ser:
                ser.write(b"\x0C")
                ser.write(line1.encode("ascii", errors="replace"))
                if self.lines >= 2:
                    ser.write(b"\x0D\x0A")
                    ser.write(line2.encode("ascii", errors="replace"))
                ser.flush()
            logger.info(f"Display: '{line1}' / '{line2}'")
        except Exception as e:
            logger.error(f"Display error: {e}")
            raise

    def clear(self):
        if not self.enabled:
            return
        try:
            with serial.Serial(self.com_port, self.baud_rate, timeout=1) as ser:
                ser.write(b"\x0C")
            logger.info("Display cleared")
        except Exception as e:
            logger.error(f"Display clear error: {e}")
            raise

    def _pad(self, text):
        text = text or ""
        if len(text) > self.columns:
            return text[:self.columns]
        return text.ljust(self.columns)
