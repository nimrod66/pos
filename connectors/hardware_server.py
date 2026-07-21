"""
Pharmacy POS — Hardware Connector Service
Runs on the POS terminal locally (localhost:9100).
Bridges the web frontend to physical hardware devices.

Usage: python hardware_server.py
"""
import logging
import json
import os
from flask import Flask, request, jsonify
from escpos_printer import EscposPrinter
from barcode_scanner import BarcodeScanner
from cash_drawer import CashDrawer
from customer_display import CustomerDisplay

app = Flask(__name__)
logging.basicConfig(level=logging.INFO)

config_file = os.environ.get("POS_HARDWARE_CONFIG", "hardware_config.json")
config = {}
if os.path.exists(config_file):
    with open(config_file) as f:
        config = json.load(f)

printer = EscposPrinter(config.get("printer", {}))
scanner = BarcodeScanner(config.get("scanner", {}))
cash_drawer = CashDrawer(config.get("cash_drawer", {}))
display = CustomerDisplay(config.get("display", {}))

@app.route("/health")
def health():
    return jsonify({
        "status": "ok",
        "printer": printer.status(),
        "scanner": scanner.status(),
        "cash_drawer": cash_drawer.status(),
        "display": display.status()
    })

@app.route("/print", methods=["POST"])
def print_receipt():
    data = request.get_json()
    if not data or "receipt" not in data:
        return jsonify({"success": False, "error": "Missing receipt data"}), 400
    try:
        printer.print_raw(data["receipt"])
        return jsonify({"success": True})
    except Exception as e:
        logging.error(f"Print error: {e}")
        return jsonify({"success": False, "error": str(e)}), 500

@app.route("/print/preview", methods=["POST"])
def print_preview():
    """Returns printable lines for preview without hardware"""
    data = request.get_json()
    if not data or "receipt" not in data:
        return jsonify({"success": False, "error": "Missing receipt data"}), 400
    lines = data["receipt"].split("\n")
    return jsonify({"success": True, "lines": lines})

@app.route("/cash-drawer/open", methods=["POST"])
def open_cash_drawer():
    try:
        cash_drawer.open()
        return jsonify({"success": True})
    except Exception as e:
        return jsonify({"success": False, "error": str(e)}), 500

@app.route("/display/show", methods=["POST"])
def show_display():
    data = request.get_json()
    if not data or "line1" not in data:
        return jsonify({"success": False, "error": "Missing display text"}), 400
    try:
        display.show(data["line1"], data.get("line2", ""))
        return jsonify({"success": True})
    except Exception as e:
        return jsonify({"success": False, "error": str(e)}), 500

@app.route("/display/clear", methods=["POST"])
def clear_display():
    try:
        display.clear()
        return jsonify({"success": True})
    except Exception as e:
        return jsonify({"success": False, "error": str(e)}), 500

@app.route("/scanner/status", methods=["GET"])
def scanner_status():
    return jsonify(scanner.status())

@app.route("/scanner/last", methods=["GET"])
def scanner_last():
    return jsonify({"barcode": scanner.last_scanned()})

if __name__ == "__main__":
    logging.info("Starting POS Hardware Connector on port 9100")
    app.run(host="127.0.0.1", port=9100, debug=False)
