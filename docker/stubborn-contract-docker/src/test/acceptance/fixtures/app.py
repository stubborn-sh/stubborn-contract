#!/usr/bin/env python3
"""Minimal HTTP fixture application for the Docker image acceptance test.

The stubborn-contract Docker image generates its tests in EXPLICIT mode, which
means the generated tests fire *real* HTTP requests at APPLICATION_BASE_URL
(see project/build.gradle -> contracts.testMode = "EXPLICIT"). This server is
the target of those requests. It answers every GET with the body the fixture
contract (../contracts/health.yml) expects, so the generated contract test
passes and the inner Gradle build reaches exit 0 and emits the stub jar.

Deliberately dependency-free (stdlib only) so it runs on any CI runner without
setup. Bound to 0.0.0.0 so a `--network host` container can reach it on
localhost.
"""
import json
import sys
from http.server import BaseHTTPRequestHandler, HTTPServer

_BODY = json.dumps({"status": "UP"}).encode("utf-8")


class Handler(BaseHTTPRequestHandler):
    def do_GET(self):  # noqa: N802 (http.server API)
        self.send_response(200)
        self.send_header("Content-Type", "application/json")
        self.send_header("Content-Length", str(len(_BODY)))
        self.end_headers()
        self.wfile.write(_BODY)

    def log_message(self, fmt, *args):
        sys.stderr.write("[fixture-app] " + (fmt % args) + "\n")


def main():
    port = int(sys.argv[1]) if len(sys.argv) > 1 else 8888
    server = HTTPServer(("0.0.0.0", port), Handler)
    sys.stderr.write("[fixture-app] listening on 0.0.0.0:%d\n" % port)
    try:
        server.serve_forever()
    except KeyboardInterrupt:
        pass


if __name__ == "__main__":
    main()
