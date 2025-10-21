package com.bank.risk.common.dto;
import java.math.BigDecimal; import java.util.Map;
public record FxRiskReq(
  String type, String desk, String trader, String book,
  String symbol, String tenor, String side, BigDecimal qty,
  BigDecimal ourBid, BigDecimal ourAsk, Map<String,Object> attrs
) {}
