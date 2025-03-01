# Proxy System for Cruise Ship Internet Access

This project implements a proxy system for a cruise ship (Royal Caribs) to optimize internet costs by using a single persistent TCP connection. The system comprises two components:

1. **Offshore Proxy (Proxy Server)**: Runs externally, receives requests from the ship proxy, and forwards them to the internet. **Must be started first.**
2. **Ship Proxy (Proxy Client)**: Runs on the ship's local network, accepts HTTP/HTTPS requests from browsers, and forwards them sequentially to the offshore proxy.

The ship proxy is configurable in browser settings (e.g., Chrome) and processes requests one by one, even if multiple requests arrive concurrently.

---