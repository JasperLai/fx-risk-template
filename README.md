# FX Risk Template (YAML + Drools + Java Skeleton)

Minimal runnable skeleton for FX quoting & hedging risk controls with:
- Fast Layer (throttle/whitelist/price band)
- Main Layer (Drools rules: price/ratio/per-order/exposure/PNL/hedge)
- Feature cache (Redis + Lua)
- Audit DDL (MySQL/ClickHouse)

See `risk-engine/src/main/resources/policies/fx-policy.yaml` for EURUSD/EURJPY and 1W/1M examples.

## API 文档（网关）

### 请求/响应模型
- 请求 `FxRiskReq` 字段：`type, desk, trader, book, symbol, tenor, side, qty, ourBid, ourAsk, attrs`
- 响应 `RiskResponse` 字段：`decision(ALLOW/REVIEW/DENY/LOCK), decisionId, reasons[], debug{}`

### Endpoints
- `POST /api/risk/check`：事前校验（报价/下单）。
  - 请求示例：
  ```json
  {
    "type": "QUOTE",
    "desk": "FX1",
    "trader": "alice",
    "book": "bookA",
    "symbol": "EURUSD",
    "tenor": "SPOT",
    "side": "BUY",
    "qty": 1000000,
    "ourBid": 1.0960,
    "ourAsk": 1.0972,
    "attrs": {}
  }
  ```
  - 响应示例：
  ```json
  {
    "decision": "ALLOW",
    "decisionId": "rc-20250101-000001",
    "reasons": ["R_PRICE_BAND"],
    "debug": {"latencyMs": 2}
  }
  ```

- `POST /api/event/trade`：成交事件写入（更新敞口/PNL）。
  - 请求示例：
  ```json
  { "book":"bookA", "symbol":"EURUSD", "tenor":"SPOT", "qty": 1000000 }
  ```
  - 响应：`{"status":"ok"}`

- `POST /api/event/cancel`：撤单事件（更新撤单率）。
  - 请求示例：
  ```json
  { "trader": "alice" }
  ```
  - 响应：`{"status":"ok"}`

> 端口：默认 `8088`，见 `risk-gateway/src/main/resources/application.yml`。

## 本地运行
```bash
# 0) 启动本机 Redis（或使用已有实例）
redis-server --daemonize yes

# 1) 构建
mvn -q -DskipTests package

# 2) 启动网关（包含特征与引擎依赖）
java -jar risk-gateway/target/risk-gateway-0.1.0.jar

# 或使用脚本
bash scripts/run-local.sh
```

## 示例 curl
```bash
# 事前校验
curl -s http://localhost:8088/api/risk/check \
  -H 'Content-Type: application/json' \
  -d '{
    "type":"QUOTE","desk":"FX1","trader":"alice","book":"bookA",
    "symbol":"EURUSD","tenor":"SPOT","side":"BUY","qty":1000000,
    "ourBid":1.0960,"ourAsk":1.0972,"attrs":{}
  }'

# 成交事件（影响敞口/PNL）
curl -s http://localhost:8088/api/event/trade \
  -H 'Content-Type: application/json' \
  -d '{"book":"bookA","symbol":"EURUSD","tenor":"SPOT","qty":1000000}'

# 撤单事件（影响撤单率）
curl -s http://localhost:8088/api/event/cancel \
  -H 'Content-Type: application/json' \
  -d '{"trader":"alice"}'
```

## 最小可运行联调指南
1. 启动 Redis 与服务（见“本地运行”）。
2. 发送若干 `trade` 与 `cancel` 事件，积累 `expo:*`、`pnl:*`、`cancel:*` 等特征：
   ```bash
   curl -s localhost:8088/api/event/trade  -H 'Content-Type: application/json' -d '{"book":"bookA","symbol":"EURUSD","tenor":"SPOT","qty":1000000}'
   curl -s localhost:8088/api/event/cancel -H 'Content-Type: application/json' -d '{"trader":"alice"}'
   ```
3. 调用 `/api/risk/check` 发起一次评估，观察 `reasons` 与 `debug` 字段；`decision` 受 `rules/fx_rules.drl` 与 `policies/fx-policy.yaml` 影响。
4. 如需调整策略参数，在 `risk-engine/src/main/resources/policies/fx-policy.yaml` 中修改（示例包含 EURUSD/EURJPY 与 SPOT/1W/1M），重启服务生效。
5. 审计库建表可使用 `scripts/sql/mysql_audit.ddl.sql` 或 `scripts/sql/clickhouse_audit.ddl.sql`。
