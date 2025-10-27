package com.bank.risk.engine;

import org.kie.api.KieServices;
import org.kie.api.builder.KieBuilder;
import org.kie.api.builder.KieFileSystem;
import org.kie.api.builder.Message;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 动态编译并缓存每个 strategyId 的 KieContainer。
 */
public class RuleRuntime {
  private final StrategyRuleRepository repo;
  private final Map<String, KieContainer> cache = new ConcurrentHashMap<>();

  public RuleRuntime(StrategyRuleRepository repo) {
    this.repo = repo;
  }

  public KieSession getSession(String strategyId, String agendaGroup) {
    KieContainer kc = cache.computeIfAbsent(strategyId, this::compile);
    if (kc == null) return null;
    KieSession ks = kc.newKieSession();
    if (agendaGroup != null && !agendaGroup.isEmpty()) {
      ks.getAgenda().getAgendaGroup(agendaGroup).setFocus();
    }
    return ks;
  }

  public void invalidate(String strategyId) {
    cache.remove(strategyId);
  }

  public boolean preload(String strategyId) {
    try {
      cache.computeIfAbsent(strategyId, this::compile);
      return cache.get(strategyId) != null;
    } catch (Exception e) {
      return false;
    }
  }

  private KieContainer compile(String strategyId) {
    String drl = repo.getDrl(strategyId);
    if (drl == null || drl.isEmpty()) return null;
    KieServices ks = KieServices.Factory.get();
    KieFileSystem kfs = ks.newKieFileSystem();
    String path = "src/main/resources/rules/" + strategyId + ".drl";
    kfs.write(path, drl);
    KieBuilder kb = ks.newKieBuilder(kfs).buildAll();
    if (kb.getResults().hasMessages(Message.Level.ERROR)) {
      throw new IllegalStateException("DRL compile error: " + kb.getResults().toString());
    }
    return ks.newKieContainer(ks.getRepository().getDefaultReleaseId());
  }
}


