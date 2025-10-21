# FX Risk Template (YAML + Drools + Java Skeleton)

Minimal runnable skeleton for FX quoting & hedging risk controls with:
- Fast Layer (throttle/whitelist/price band)
- Main Layer (Drools rules: price/ratio/per-order/exposure/PNL/hedge)
- Feature cache (Redis + Lua)
- Audit DDL (MySQL/ClickHouse)

See `risk-engine/src/main/resources/policies/fx-policy.yaml` for EURUSD/EURJPY and 1W/1M examples.
