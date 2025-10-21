package com.bank.risk.engine;

import com.bank.risk.common.dto.*;
import com.bank.risk.feature.FeatureService;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;
import java.util.*;

public class RiskService {
  private final FeatureService feat;
  private final PolicyRepository repo;
  private final KieContainer kc;
  private Map<String, Object> cfg;

  public RiskService(FeatureService feat, PolicyRepository repo) {
    this.feat = feat;
    this.repo = repo;
    this.kc = DroolsConfig.container();
    this.cfg = repo.load("default");
  }

  public RiskResponse decide(FxRiskReq req) {
    Optional<RiskResponse> quick = new FastLayer(feat, cfg).check(req);
    if (quick.isPresent())
      return quick.get();
    KieSession ks = kc.newKieSession("fxSession");
    ks.insert(req);
    ks.insert(feat);
    ks.fireAllRules();
    ks.dispose();
    return new RiskResponse(Decision.ALLOW, java.util.UUID.randomUUID().toString(), List.of(), Map.of());
  }
}
