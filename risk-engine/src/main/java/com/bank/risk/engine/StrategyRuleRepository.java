package com.bank.risk.engine;

/**
 * 提供按 strategyId 获取动态生成的 DRL 文本。
 * 返回 null 表示不存在对应策略，调用方应回退到默认会话。
 */
public interface StrategyRuleRepository {
  String getDrl(String strategyId);
}


