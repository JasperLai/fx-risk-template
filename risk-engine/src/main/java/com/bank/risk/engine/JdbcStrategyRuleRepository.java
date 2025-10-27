package com.bank.risk.engine;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

/**
 * 基于 MySQL 的策略规则仓库：按 strategyId 读取最新有效的 DRL 文本。
 * 表结构建议：strategy_rules(strategy_id VARCHAR, version BIGINT, drl TEXT, status VARCHAR, updated_at TIMESTAMP)
 * 读取规则：status='ACTIVE' 且按 version/updated_at 倒序，取最新一条。
 */
public class JdbcStrategyRuleRepository implements StrategyRuleRepository {
  private final DataSource dataSource;

  public JdbcStrategyRuleRepository(DataSource dataSource) {
    this.dataSource = dataSource;
  }

  @Override
  public String getDrl(String strategyId) {
    final String sql = "SELECT drl FROM strategy_rules WHERE strategy_id=? AND status='ACTIVE' ORDER BY version DESC, updated_at DESC LIMIT 1";
    try (Connection c = dataSource.getConnection();
         PreparedStatement ps = c.prepareStatement(sql)) {
      ps.setString(1, strategyId);
      try (ResultSet rs = ps.executeQuery()) {
        if (rs.next()) {
          return rs.getString(1);
        }
      }
      return null;
    } catch (Exception e) {
      throw new RuntimeException("load drl failed for strategyId=" + strategyId, e);
    }
  }
}


