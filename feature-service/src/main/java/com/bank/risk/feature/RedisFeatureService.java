package com.bank.risk.feature;

import redis.clients.jedis.JedisPooled;
import java.math.BigDecimal;
import java.time.Duration;

public class RedisFeatureService implements FeatureService {
  private final JedisPooled jedis;
  private final String luaWinIncrSha;

  public RedisFeatureService(JedisPooled jedis, String luaWinIncrSha) {
    this.jedis = jedis;
    this.luaWinIncrSha = luaWinIncrSha;
  }

  @Override
  public BigDecimal mid(String symbol) {
    String v = jedis.get("mid:" + symbol);
    return v == null ? null : new BigDecimal(v);
  }

  @Override
  public int winCount(String key, Duration win) {
    long now = System.currentTimeMillis() / 1000;
    Object r = jedis.evalsha(luaWinIncrSha, 1, ("win:" + key), String.valueOf(now), String.valueOf(win.getSeconds()));
    return Integer.parseInt(r.toString());
  }

  @Override
  public BigDecimal cancelRate(String subject, Duration win) {
    String num = jedis.get("cancel:" + subject + ":" + win.getSeconds() + ":num");
    String den = jedis.get("order:" + subject + ":" + win.getSeconds() + ":den");
    int n = num == null ? 0 : Integer.parseInt(num), d = den == null ? 0 : Integer.parseInt(den);
    if (d == 0)
      return BigDecimal.ZERO;
    return new BigDecimal(n).divide(new BigDecimal(d), 6, java.math.RoundingMode.HALF_UP);
  }

  @Override
  public BigDecimal pnlToday(String scope) {
    return get("pnl:today:" + scope);
  }

  @Override
  public BigDecimal pnlArb(String scope, int seconds) {
    return get("pnl:arb:" + scope + ":" + seconds);
  }

  @Override
  public BigDecimal netExposureUsd(String symbol, String tenor, String book) {
    return get("expo:" + book + ":" + symbol + ":" + tenor);
  }

  @Override
  public BigDecimal hedgeSlippage(String symbol, String waveId) {
    return get("hedge:slip:" + symbol + ":" + waveId);
  }

  private BigDecimal get(String k) {
    String v = jedis.get(k);
    return v == null ? BigDecimal.ZERO : new BigDecimal(v);
  }
}
