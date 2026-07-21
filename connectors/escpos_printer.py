"""
ESC/POS Printer driver
Supports USB, Network (LAN/WiFi), and Serial (COM) printers.
Tested with: Epson TM-T20, TM-T88, Xprinter XP-58, generic ESC/POS
"""
import logging
import socket
import usb.core
import usb.util
import serial
import time

logger = logging.getLogger(__name__)


class EscposPrinter:
    def __init__(self, config=None):
        self.config = config or {}
        self.connection_type = self.config.get("type", "network")
        self.host = self.config.get("host", "192.168.1.100")
        self.port = self.config.get("port", 9100)
        self.com_port = self.config.get("com_port", "COM3")
        self.baud_rate = self.config.get("baud_rate", 9600)
        self.vendor_id = self.config.get("vendor_id", 0x04b8)
        self.product_id = self.config.get("product_id", 0x0e28)
        self.width = self.config.get("width", 42)

    def status(self):
        return {
            "connected": self._test_connection(),
            "type": self.connection_type,
            "width": self.width
        }

    def _test_connection(self):
        try:
            if self.connection_type == "network":
                s = socket.socket()
                s.settimeout(1)
                s.connect((self.host, self.port))
                s.close()
                return True
            elif self.connection_type == "serial":
                s = serial.Serial(self.com_port, self.baud_rate, timeout=1)
                s.close()
                return True
        except:
            return False
        return False

    def print_raw(self, data):
        """Send raw ESC/POS bytes to printer"""
        if isinstance(data, str):
            data = data.encode("utf-8", errors="replace")

        if self.connection_type == "network":
            self._print_network(data)
        elif self.connection_type == "serial":
            self._print_serial(data)
        elif self.connection_type == "usb":
            self._print_usb(data)
        else:
            logger.warning(f"Printer: unknown connection type '{self.connection_type}', data logged only")
            logger.info(f"Would print: {data[:200]}...")

    def _print_network(self, data):
        s = socket.socket()
        s.settimeout(5)
        try:
            s.connect((self.host, self.port))
            s.send(data)
            logger.info(f"Sent {len(data)} bytes to printer {self.host}:{self.port}")
        finally:
            s.close()

    def _print_serial(self, data):
        with serial.Serial(self.com_port, self.baud_rate, timeout=5) as ser:
            ser.write(data)
            ser.flush()
            logger.info(f"Sent {len(data)} bytes to printer on {self.com_port}")

    def _print_usb(self, data):
        dev = usb.core.find(idVendor=self.vendor_id, idProduct=self.product_id)
        if dev is None:
            raise RuntimeError(f"USB printer not found (VID={hex(self.vendor_id)}, PID={hex(self.product_id)})")
        if dev.is_kernel_driver_active(0):
            dev.detach_kernel_driver(0)
        dev.set_configuration()
        endpoint = dev[0][(0, 0)][0]
        endpoint.write(data)
        logger.info(f"Sent {len(data)} bytes to USB printer")

    def cut_paper(self):
        self.print_raw(b'\x1d\x56\x42\x00')

    def open_cash_drawer(self, pin=2):
        self.print_raw(bytes([0x1b, 0x70, pin, 50, 50]))
