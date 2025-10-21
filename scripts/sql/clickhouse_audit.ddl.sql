CREATE TABLE IF NOT EXISTS rc_audit_ck (
  decision_id String,
  policy_version String,
  req_type LowCardinality(String),
  symbol LowCardinality(String),
  result LowCardinality(String),
  reasons String,
  features String,
  latency_ms UInt32,
  ts DateTime
) ENGINE = MergeTree ORDER BY (ts, symbol);
