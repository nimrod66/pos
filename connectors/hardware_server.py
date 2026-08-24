"""Local-only HTTP bridge between Pharmacy POS and terminal peripherals."""

import json
import logging
import os
from copy import deepcopy

from flask import Flask, jsonify, request

from barcode_scanner import BarcodeScanner
from cash_drawer import CashDrawer
from customer_display import CustomerDisplay
from escpos_printer import EscposPrinter


app = Flask(__name__)
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

DEFAULT_CONFIG = {
    "printer": {
        "type": "network",
        "host": "192.168.1.100",
        "port": 9100,
        "com_port": "COM3",
        "baud_rate": 9600,
        "vendor_id": 0x04B8,
        "product_id": 0x0E28,
        "width": 42,
    },
    "scanner": {
        "mode": "keyboard_wedge",
        "com_port": "COM2",
        "baud_rate": 9600,
    },
    "cash_drawer": {
        "enabled": False,
        "mode": "printer",
        "com_port": "COM4",
        "pin": 2,
    },
    "display": {
        "enabled": False,
        "com_port": "COM5",
        "baud_rate": 9600,
        "lines": 2,
        "columns": 20,
    },
}

allowed_origins = {
    origin.strip()
    for origin in os.environ.get(
        "POS_CONNECTOR_ALLOWED_ORIGINS",
        "http://localhost:3000,http://127.0.0.1:3000",
    ).split(",")
    if origin.strip()
}
config_file = os.environ.get("POS_HARDWARE_CONFIG", "hardware_config.json")


def _int_value(value, name, minimum, maximum):
    try:
        parsed = int(value, 0) if isinstance(value, str) else int(value)
    except (TypeError, ValueError) as error:
        raise ValueError(f"{name} must be a number") from error
    if parsed < minimum or parsed > maximum:
        raise ValueError(f"{name} must be between {minimum} and {maximum}")
    return parsed


def _text_value(value, name, maximum=255):
    parsed = str(value or "").strip()
    if not parsed or len(parsed) > maximum:
        raise ValueError(
            f"{name} is required and must be at most {maximum} characters"
        )
    return parsed


def normalize_config(candidate):
    if not isinstance(candidate, dict):
        raise ValueError("Configuration must be a JSON object")

    merged = deepcopy(DEFAULT_CONFIG)
    for section in merged:
        supplied = candidate.get(section, {})
        if supplied is not None and not isinstance(supplied, dict):
            raise ValueError(f"{section} must be a JSON object")
        merged[section].update(supplied or {})

    printer_config = merged["printer"]
    if printer_config["type"] not in {"network", "serial", "usb"}:
        raise ValueError("printer.type must be network, serial, or usb")
    printer_config["host"] = _text_value(
        printer_config["host"], "printer.host"
    )
    printer_config["port"] = _int_value(
        printer_config["port"], "printer.port", 1, 65535
    )
    printer_config["com_port"] = _text_value(
        printer_config["com_port"], "printer.com_port", 32
    )
    printer_config["baud_rate"] = _int_value(
        printer_config["baud_rate"], "printer.baud_rate", 300, 921600
    )
    printer_config["vendor_id"] = _int_value(
        printer_config["vendor_id"], "printer.vendor_id", 0, 65535
    )
    printer_config["product_id"] = _int_value(
        printer_config["product_id"], "printer.product_id", 0, 65535
    )
    printer_config["width"] = _int_value(
        printer_config["width"], "printer.width", 24, 80
    )

    scanner_config = merged["scanner"]
    if scanner_config["mode"] not in {"keyboard_wedge", "serial"}:
        raise ValueError("scanner.mode must be keyboard_wedge or serial")
    scanner_config["com_port"] = _text_value(
        scanner_config["com_port"], "scanner.com_port", 32
    )
    scanner_config["baud_rate"] = _int_value(
        scanner_config["baud_rate"], "scanner.baud_rate", 300, 921600
    )

    drawer_config = merged["cash_drawer"]
    drawer_config["enabled"] = bool(drawer_config["enabled"])
    if drawer_config["mode"] not in {"printer", "serial"}:
        raise ValueError("cash_drawer.mode must be printer or serial")
    drawer_config["com_port"] = _text_value(
        drawer_config["com_port"], "cash_drawer.com_port", 32
    )
    drawer_config["pin"] = _int_value(
        drawer_config["pin"], "cash_drawer.pin", 0, 255
    )

    display_config = merged["display"]
    display_config["enabled"] = bool(display_config["enabled"])
    display_config["com_port"] = _text_value(
        display_config["com_port"], "display.com_port", 32
    )
    display_config["baud_rate"] = _int_value(
        display_config["baud_rate"], "display.baud_rate", 300, 921600
    )
    display_config["lines"] = _int_value(
        display_config["lines"], "display.lines", 1, 4
    )
    display_config["columns"] = _int_value(
        display_config["columns"], "display.columns", 8, 80
    )
    return merged


def load_config():
    if not os.path.exists(config_file):
        return normalize_config({})
    try:
        with open(config_file, encoding="utf-8") as source:
            return normalize_config(json.load(source))
    except Exception as error:
        logger.error("Invalid hardware configuration at %s: %s", config_file, error)
        return normalize_config({})


def save_config(value):
    directory = os.path.dirname(os.path.abspath(config_file))
    os.makedirs(directory, exist_ok=True)
    temporary_path = f"{config_file}.tmp"
    with open(temporary_path, "w", encoding="utf-8") as target:
        json.dump(value, target, indent=2)
        target.write("\n")
    os.replace(temporary_path, config_file)


def configure_devices(value):
    global config, printer, scanner, cash_drawer, display
    previous_scanner = globals().get("scanner")
    if previous_scanner is not None:
        previous_scanner.stop()
    config = value
    printer = EscposPrinter(config["printer"])
    scanner = BarcodeScanner(config["scanner"])
    cash_drawer = CashDrawer(config["cash_drawer"], printer)
    display = CustomerDisplay(config["display"])


configure_devices(load_config())


@app.after_request
def add_cors_headers(response):
    origin = request.headers.get("Origin")
    if origin in allowed_origins:
        response.headers["Access-Control-Allow-Origin"] = origin
        response.headers["Vary"] = "Origin"
        response.headers["Access-Control-Allow-Headers"] = "Content-Type"
        response.headers["Access-Control-Allow-Methods"] = "GET,POST,PUT,OPTIONS"
    return response


@app.route("/health")
def health():
    return jsonify({
        "status": "ok",
        "printer": printer.status(),
        "scanner": scanner.status(),
        "cash_drawer": cash_drawer.status(),
        "display": display.status(),
    })


@app.route("/config", methods=["GET", "PUT"])
def connector_config():
    if request.method == "GET":
        return jsonify(config)
    try:
        updated = normalize_config(request.get_json(silent=True))
        save_config(updated)
        configure_devices(updated)
        return jsonify(updated)
    except ValueError as error:
        return jsonify({"success": False, "error": str(error)}), 400


@app.route("/print", methods=["POST"])
def print_receipt():
    data = request.get_json(silent=True)
    if not data or "receipt" not in data:
        return jsonify({"success": False, "error": "Missing receipt data"}), 400
    try:
        printer.print_raw(data["receipt"])
        return jsonify({"success": True})
    except Exception as error:
        logger.error("Print error: %s", error)
        return jsonify({"success": False, "error": str(error)}), 500


@app.route("/print/preview", methods=["POST"])
def print_preview():
    data = request.get_json(silent=True)
    if not data or "receipt" not in data:
        return jsonify({"success": False, "error": "Missing receipt data"}), 400
    return jsonify({"success": True, "lines": data["receipt"].split("\n")})


@app.route("/cash-drawer/open", methods=["POST"])
def open_cash_drawer():
    try:
        cash_drawer.open()
        return jsonify({"success": True})
    except Exception as error:
        return jsonify({"success": False, "error": str(error)}), 500


@app.route("/display/show", methods=["POST"])
def show_display():
    data = request.get_json(silent=True)
    if not data or "line1" not in data:
        return jsonify({"success": False, "error": "Missing display text"}), 400
    try:
        display.show(data["line1"], data.get("line2", ""))
        return jsonify({"success": True})
    except Exception as error:
        return jsonify({"success": False, "error": str(error)}), 500


@app.route("/display/clear", methods=["POST"])
def clear_display():
    try:
        display.clear()
        return jsonify({"success": True})
    except Exception as error:
        return jsonify({"success": False, "error": str(error)}), 500


@app.route("/scanner/status")
def scanner_status():
    return jsonify(scanner.status())


@app.route("/scanner/last")
def scanner_last():
    return jsonify({"barcode": scanner.last_scanned()})


@app.route("/scanner/capture", methods=["POST"])
def scanner_capture():
    data = request.get_json(silent=True)
    barcode = str((data or {}).get("barcode", "")).strip()
    if not barcode:
        return jsonify({"success": False, "error": "Missing barcode"}), 400
    scanner.set_barcode(barcode)
    return jsonify({"success": True})


if __name__ == "__main__":
    logger.info("Starting POS Hardware Connector on port 9100")
    app.run(host="127.0.0.1", port=9100, debug=False)
