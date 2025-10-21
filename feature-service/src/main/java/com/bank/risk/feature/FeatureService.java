package com.bank.risk.feature;
import java.math.BigDecimal; import java.time.Duration;
public interface FeatureService {
  BigDecimal mid(String symbol);
  int winCount(String key, Duration win);
  BigDecimal cancelRate(String subject, Duration win);
  BigDecimal pnlToday(String scope);
  BigDecimal pnlArb(String scope, int seconds);
  BigDecimal netExposureUsd(String symbol, String tenor, String book);
  BigDecimal hedgeSlippage(String symbol, String waveId);
}
