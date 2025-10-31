package com.bank.risk.engine;

import com.bank.risk.common.dto.*;
import com.bank.risk.feature.FeatureService;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class RiskServiceTest {

  private static final String DRL = "" +
      "package rules;\n" +
      "import com.bank.risk.common.dto.*;\n" +
      "import com.bank.risk.common.model.RuleHit;\n" +
      "global java.util.List hits;\n" +
      "global java.util.concurrent.atomic.AtomicReference decisionRef;\n" +
      "rule \"R_SVC_TEST\" salience 90 agenda-group \"s_sid1\"\n" +
      "when\n" +
      "  $ctx: com.bank.risk.common.dto.FxRiskReq( symbol == \"EURUSD\", type == OrderType.ORDER )\n" +
      "then\n" +
      "  hits.add(new com.bank.risk.common.model.RuleHit(\"R_SVC_TEST\", \"ok\"));\n" +
      "  if (((Decision)decisionRef.get()).compareTo(Decision.DENY) < 0) decisionRef.set(Decision.DENY);\n" +
      "end\n";

  @Test
  public void testDecide_WithCachedStrategy() {
    // 准备 runtime 缓存
    InMemoryStrategyRuleRepository repo = new InMemoryStrategyRuleRepository();
    repo.putDrl("sid1", DRL);
    RuleRuntime runtime = new RuleRuntime(repo);
    assertTrue(runtime.preload("sid1"));

    // 构造服务
    FeatureService feat = dummyFeature();
    RiskService svc = new RiskService(feat, new YamlPolicyRepository(), runtime);

    FxRiskReq req = new FxRiskReq(
        OrderType.ORDER, "FX1", "alice", "bookA", "EURUSD", "SPOT", "BUY",
        new BigDecimal("100000"), new BigDecimal("1.1000"), new BigDecimal("1.1002"),
        Map.of("strategyId", "sid1")
    );

    RiskResponse resp = svc.decide(req);
    assertEquals(Decision.DENY, resp.decision());
    assertTrue(resp.reasons().contains("R_SVC_TEST"));
  }

  @Test
  public void testDecide_FallbackToDefaultSession() {
    // 无缓存策略，走默认 fxSession；利用默认 DRL 的 per-order 限额触发 REVIEW
    FeatureService feat = dummyFeature();
    RiskService svc = new RiskService(feat, new YamlPolicyRepository());

    FxRiskReq req = new FxRiskReq(
        OrderType.ORDER, "FX1", "alice", "bookA", "EURUSD", "SPOT", "BUY",
        new BigDecimal("9000000"), new BigDecimal("1.1000"), new BigDecimal("1.1002"),
        Map.of()
    );

    RiskResponse resp = svc.decide(req);
    assertTrue(resp.decision() == Decision.REVIEW || resp.decision() == Decision.DENY || resp.decision() == Decision.LOCK);
    assertNotNull(resp.decisionId());
  }

  private FeatureService dummyFeature() {
    return new FeatureService() {
      public BigDecimal mid(String symbol) { return new BigDecimal("1.1001"); }
      public int winCount(String key, java.time.Duration win) { return 0; }
      public BigDecimal cancelRate(String subject, java.time.Duration win) { return BigDecimal.ZERO; }
      public BigDecimal pnlToday(String scope) { return BigDecimal.ZERO; }
      public BigDecimal pnlArb(String scope, int seconds) { return BigDecimal.ZERO; }
      public BigDecimal netExposureUsd(String symbol, String tenor, String book) { return BigDecimal.ZERO; }
      public BigDecimal hedgeSlippage(String symbol, String waveId) { return BigDecimal.ZERO; }
    };
  }
}


