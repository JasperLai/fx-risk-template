package com.bank.risk.engine;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryStrategyRuleRepository implements StrategyRuleRepository {
  private final Map<String, String> strategyIdToDrl = new ConcurrentHashMap<>();

  @Override
  public String getDrl(String strategyId) {
    return strategyIdToDrl.get(strategyId);
  }

  public void putDrl(String strategyId, String drl) {
    strategyIdToDrl.put(strategyId, drl);
  }
}


