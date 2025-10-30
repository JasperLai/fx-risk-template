package com.bank.risk.gateway.controller;

import com.bank.risk.common.dto.*;
import com.bank.risk.engine.*;
import com.bank.risk.feature.*;
import org.springframework.web.bind.annotation.*;
import javax.sql.DataSource;
import redis.clients.jedis.JedisPooled;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

@RestController
@RequestMapping("/api/risk")
public class RiskController {
  private final RiskService risk;
  private final RuleRuntime runtime;

  public RiskController(JedisPooled jedis, DataSource dataSource) throws IOException {
    String script = new String(Files.readAllBytes(Paths.get("feature-service/src/main/resources/lua/win_incr.lua")));
    FeatureService feat = new RedisFeatureService(jedis, jedis.scriptLoad(script));
    this.runtime = new RuleRuntime(new JdbcStrategyRuleRepository(dataSource));
    risk = new RiskService(feat, new YamlPolicyRepository(), this.runtime);
  }

  @PostMapping("/check")
  public RiskResponse check(@RequestBody FxRiskReq req) {
    return risk.decide(req);
  }
}