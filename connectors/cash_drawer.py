"""
Cash drawer control
Most cash drawers connect to the receipt printer's DK port.
Opening is triggered by ESC/POS command sent to the printer.
Standalone USB drawers are also supported.
"""
import logging
import serial
import socket

logger = logging.getLogger(__name__)


class CashDrawer:
    def __init__(self, config=None):
        self.config = config or {}
        self.mode = self.config.get("mode", "printer")
        self.printer_host = self.config.get("printer_host", "192.168.1.100")
        self.printer_port = self.config.get("printer_port", 9100)
        self.pin = self.config.get("pin", 2)

    def status(self):
        return {
            "mode": self.mode,
            "ready": True
        }

    def open(self):
        if self.mode == "printer":
            self._open_via_printer()
        elif self.mode == "serial":
            self._open_via_serial()
        else:
            logger.info(f"Cash drawer: mode={self.mode}, no action taken")

    def _open_via_printer(self):
        try:
            s = socket.socket()
            s.settimeout(2)
            s.connect((self.printer_host, self.printer_port))
            s.send(bytes([0x1b, 0x70, self.pin, 50, 50]))
            s.close()
            logger.info("Cash drawer opened via printer")
        except Exception as e:
            logger.error(f"Failed to open cash drawer via printer: {e}")

    def _open_via_serial(self):
        try:
            com_port = self.config.get("com_port", "COM4")
            with serial.Serial(com_port, 9600, timeout=1) as ser:
                ser.write(b"\x1b\x70\x00\x19\xfa")
                ser.flush()
            logger.info(f"Cash drawer opened via {com_port}")
        except Exception as e:
            logger.error(f"Failed to open cash drawer via serial: {e}")
