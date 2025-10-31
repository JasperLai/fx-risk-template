package com.bank.risk.engine;

import com.bank.risk.common.dto.*;
import com.bank.risk.feature.FeatureService;
import org.junit.jupiter.api.Test;
import org.kie.api.runtime.KieSession;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

public class RuleRuntimeTest {

  private static final String DRL = "" +
      "package rules;\n" +
      "import com.bank.risk.common.dto.*;\n" +
      "import com.bank.risk.common.model.RuleHit;\n" +
      "global java.util.List hits;\n" +
      "global java.util.concurrent.atomic.AtomicReference decisionRef;\n" +
      "rule \"R_TEST\" salience 100 agenda-group \"s_sUT\"\n" +
      "when\n" +
      "  $ctx: com.bank.risk.common.dto.FxRiskReq( symbol == \"EURUSD\" )\n" +
      "then\n" +
      "  hits.add(new com.bank.risk.common.model.RuleHit(\"R_TEST\", \"ok\"));\n" +
      "  if (((Decision)decisionRef.get()).compareTo(Decision.REVIEW) < 0) decisionRef.set(Decision.REVIEW);\n" +
      "end\n";

  @Test
  public void testPreloadAndCachedSession() {
    InMemoryStrategyRuleRepository repo = new InMemoryStrategyRuleRepository();
    repo.putDrl("sUT", DRL);
    RuleRuntime rr = new RuleRuntime(repo);

    assertTrue(rr.preload("sUT"));

    KieSession ks = rr.getCachedSession("sUT", "s_sUT");
    assertNotNull(ks);

    try {
      List<com.bank.risk.common.model.RuleHit> hits = new ArrayList<>();
      AtomicReference<Decision> decisionRef = new AtomicReference<>(Decision.ALLOW);
      ks.setGlobal("hits", hits);
      ks.setGlobal("decisionRef", decisionRef);

      FxRiskReq req = new FxRiskReq(OrderType.ORDER, "FX1", "alice", "bookA", "EURUSD", "SPOT", "BUY",
          new BigDecimal("100000"), new BigDecimal("1.1000"), new BigDecimal("1.1002"), java.util.Map.of());
      ks.insert(req);
      ks.insert(dummyFeature());

      ks.fireAllRules();

      assertEquals(1, hits.size());
      assertEquals("R_TEST", hits.get(0).ruleId());
      assertEquals(Decision.REVIEW, decisionRef.get());
    } finally {
      ks.dispose();
    }
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


