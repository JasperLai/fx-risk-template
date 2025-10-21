package com.bank.risk.engine;

import com.bank.risk.common.dto.*;
import com.bank.risk.feature.FeatureService;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.*;

public class FastLayer {
  private final FeatureService feat;
  private final Map<String, Object> cfg;

  public FastLayer(FeatureService feat, Map<String, Object> cfg) {
    this.feat = feat;
    this.cfg = cfg;
  }

  public Optional<RiskResponse> check(FxRiskReq req) {
    Optional<RiskResponse> wl = applyWhitelistBypass(req);
    if (wl.isPresent()) return wl;

    Optional<RiskResponse> throttle = applyOrderThrottle(req);
    if (throttle.isPresent()) return throttle;

    Optional<RiskResponse> cancelRate = applyCancelRate(req);
    if (cancelRate.isPresent()) return cancelRate;

    Optional<RiskResponse> price = applyPriceRules(req);
    if (price.isPresent()) return price;

    return Optional.empty();
  }

  private Optional<RiskResponse> applyWhitelistBypass(FxRiskReq req) {
    if (isWhitelisted(req)) {
      return Optional.of(resp(Decision.ALLOW, List.of("R_WL_BYPASS"), Map.of("wl", true)));
    }
    return Optional.empty();
  }

  private Optional<RiskResponse> applyOrderThrottle(FxRiskReq req) {
    if (!OrderType.ORDER.equals(req.type())) return Optional.empty();
    int cap = intCfg(5, "throttle", "qps", "perTraderPer5s");
    int windowSec = 5; // 与 perTraderPer5s 语义绑定
    int cnt = feat.winCount("ord:" + req.trader(), Duration.ofSeconds(windowSec));
    if (cnt > cap) {
      return Optional.of(resp(Decision.DENY, List.of("R_THROTTLE"), Map.of("cnt", cnt, "cap", cap)));
    }
    return Optional.empty();
  }

  private Optional<RiskResponse> applyCancelRate(FxRiskReq req) {
    int winSec = intCfg(10, "throttle", "cancel", "windowSec");
    BigDecimal maxRate = decimalCfg(new BigDecimal("0.7"), "throttle", "cancel", "maxRate");
    BigDecimal rate = feat.cancelRate(req.trader(), Duration.ofSeconds(winSec));
    if (rate.compareTo(maxRate) > 0) {
      return Optional.of(resp(Decision.REVIEW, List.of("R_CANCEL_RATE"), Map.of("rate", rate, "maxRate", maxRate)));
    }
    return Optional.empty();
  }

  private Optional<RiskResponse> applyPriceRules(FxRiskReq req) {
    BigDecimal mid = feat.mid(req.symbol());
    if (mid == null || req.ourBid() == null || req.ourAsk() == null) return Optional.empty();

    int maxDevPipsDefault = req.symbol().endsWith("JPY") ? 12 : 10;
    int maxDevPips = intCfg(maxDevPipsDefault, "policy", "symbols", req.symbol(), "maxDevPips");
    if (tooFar(req.symbol(), mid, req.ourBid(), maxDevPips) || tooFar(req.symbol(), mid, req.ourAsk(), maxDevPips)) {
      return Optional.of(resp(Decision.DENY, List.of("R_PRICE_BAND"), Map.of("mid", mid, "maxDevPips", maxDevPips)));
    }

    int minSpreadPips = intCfg(1, "policy", "symbols", req.symbol(), "minSpreadPips");
    int maxSpreadPips = intCfg(30, "policy", "symbols", req.symbol(), "maxSpreadPips");
    BigDecimal sp = req.ourAsk().subtract(req.ourBid());
    int spPips = pips(req.symbol(), sp);
    if (spPips < minSpreadPips || spPips > maxSpreadPips) {
      return Optional.of(resp(Decision.REVIEW, List.of("R_SPREAD_RANGE"), Map.of("spreadPips", spPips, "range", minSpreadPips + "-" + maxSpreadPips)));
    }

    return Optional.empty();
  }

  private boolean isWhitelisted(FxRiskReq req) {
    Object wl = cfg == null ? null : cfg.get("whitelist");
    if (wl instanceof java.util.List<?> list) {
      for (Object o : list) {
        if (o instanceof Map<?,?> m) {
          Object scope = m.get("scope");
          Object subject = m.get("subject");
          if ("TRADER".equals(scope) && req.trader() != null && req.trader().equals(subject)) {
            return true;
          }
        }
      }
    }
    return false;
  }

  private RiskResponse resp(Decision d, List<String> reasons, Map<String, Object> dbg) {
    return new RiskResponse(d, java.util.UUID.randomUUID().toString(), reasons, dbg);
  }

  private boolean tooFar(String symbol, BigDecimal mid, BigDecimal px, int maxDevPips) {
    return Math.abs(px.subtract(mid).doubleValue()) > fromPips(symbol, maxDevPips).doubleValue();
  }

  private BigDecimal fromPips(String symbol, int p) {
    return symbol.endsWith("JPY") ? new BigDecimal(p).movePointLeft(2) : new BigDecimal(p).movePointLeft(4);
  }

  private int pips(String symbol, BigDecimal d) {
    BigDecimal abs = d.abs();
    return symbol.endsWith("JPY") ? abs.movePointRight(2).intValue() : abs.movePointRight(4).intValue();
  }

  private int intCfg(int def, String... path) {
    Object v = get(cfg, path);
    if (v instanceof Number n) return n.intValue();
    if (v instanceof String s) {
      try { return Integer.parseInt(s); } catch (Exception ignored) {}
    }
    return def;
  }

  private BigDecimal decimalCfg(BigDecimal def, String... path) {
    Object v = get(cfg, path);
    if (v instanceof Number n) return new BigDecimal(n.toString());
    if (v instanceof String s) {
      try { return new BigDecimal(s); } catch (Exception ignored) {}
    }
    return def;
  }

  @SuppressWarnings("unchecked")
  private Object get(Map<String, Object> root, String... path) {
    if (root == null || path == null) return null;
    Object cur = root;
    for (String p : path) {
      if (!(cur instanceof Map)) return null;
      cur = ((Map<String, Object>) cur).get(p);
      if (cur == null) return null;
    }
    return cur;
  }
}
