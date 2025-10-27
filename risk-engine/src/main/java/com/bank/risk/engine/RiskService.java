package com.bank.risk.engine;

import com.bank.risk.common.dto.*;
import com.bank.risk.feature.FeatureService;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;
import java.util.concurrent.atomic.AtomicReference;
import java.util.*;

public class RiskService {
  private final FeatureService feat;
  private final KieContainer kc;
  private Map<String, Object> cfg;
  private final RuleRuntime ruleRuntime;

  public RiskService(FeatureService feat, PolicyRepository repo) {
    this.feat = feat;
    this.kc = DroolsConfig.container();
    this.cfg = repo.load("default");
    this.ruleRuntime = new RuleRuntime(new InMemoryStrategyRuleRepository());
  }

  // 可注入的构造器：用于生产环境从数据库加载规则
  public RiskService(FeatureService feat, PolicyRepository repo, RuleRuntime runtime) {
    this.feat = feat;
    this.kc = DroolsConfig.container();
    this.cfg = repo.load("default");
    this.ruleRuntime = runtime;
  }

  public RiskResponse decide(FxRiskReq req) {
    Optional<RiskResponse> quick = new FastLayer(feat, cfg).check(req);
    if (quick.isPresent())
      return quick.get();
    String strategyId = req.attrs()==null? null : String.valueOf(req.attrs().getOrDefault("strategyId", ""));
    KieSession ks = null;
    if (strategyId != null && !strategyId.isEmpty()) {
      ks = ruleRuntime.getSession(strategyId, "s_" + strategyId);
    }
    if (ks == null) {
      ks = kc.newKieSession("fxSession");
    }
    try {
      java.util.List<String> reasons = new java.util.ArrayList<>();
      java.util.List<com.bank.risk.common.model.RuleHit> hits = new java.util.ArrayList<>();
      AtomicReference<Decision> decisionRef = new AtomicReference<>(Decision.ALLOW);

      ks.setGlobal("hits", hits);
      ks.setGlobal("decisionRef", decisionRef);

      ks.insert(req);
      ks.insert(feat);
      ks.fireAllRules();

      for (com.bank.risk.common.model.RuleHit h : hits) {
        reasons.add(h.ruleId());
      }
      Decision finalDecision = decisionRef.get();
      return new RiskResponse(finalDecision, java.util.UUID.randomUUID().toString(), reasons, Map.of("hitCount", hits.size()));
    } finally {
      ks.dispose();
    }
  }
}
