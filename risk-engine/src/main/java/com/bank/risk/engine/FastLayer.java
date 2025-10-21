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
    if (isWhitelisted(req))
      return Optional.of(resp(Decision.ALLOW, List.of("R_WL_BYPASS"), Map.of("wl", true)));
    if ("ORDER".equals(req.type())) {
      int cap = 5; // demo
      int cnt = feat.winCount("ord:" + req.trader(), Duration.ofSeconds(5));
      if (cnt > cap)
        return Optional.of(resp(Decision.DENY, List.of("R_THROTTLE"), Map.of("cnt", cnt)));
    }
    int win = 10;
    BigDecimal rate = feat.cancelRate(req.trader(), Duration.ofSeconds(win));
    if (rate.compareTo(new BigDecimal("0.7")) > 0)
      return Optional.of(resp(Decision.REVIEW, List.of("R_CANCEL_RATE"), Map.of("rate", rate)));
    BigDecimal mid = feat.mid(req.symbol());
    if (mid != null && req.ourBid() != null && req.ourAsk() != null) {
      int maxDevPips = req.symbol().endsWith("JPY") ? 12 : 10;
      if (tooFar(req.symbol(), mid, req.ourBid(), maxDevPips) || tooFar(req.symbol(), mid, req.ourAsk(), maxDevPips))
        return Optional.of(resp(Decision.DENY, List.of("R_PRICE_BAND"), Map.of("mid", mid)));
      BigDecimal sp = req.ourAsk().subtract(req.ourBid());
      int spPips = pips(req.symbol(), sp);
      if (spPips < 1 || spPips > 30)
        return Optional.of(resp(Decision.REVIEW, List.of("R_SPREAD_RANGE"), Map.of("spreadPips", spPips)));
    }
    return Optional.empty();
  }

  private boolean isWhitelisted(FxRiskReq req) {
    return "trader_vip_001".equals(req.trader());
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
}
