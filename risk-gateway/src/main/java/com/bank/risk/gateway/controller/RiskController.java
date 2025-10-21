package com.bank.risk.gateway.controller;

import com.bank.risk.common.dto.*;
import com.bank.risk.engine.*;
import com.bank.risk.feature.*;
import org.springframework.web.bind.annotation.*;
import redis.clients.jedis.JedisPooled;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

@RestController
@RequestMapping("/api/risk")
public class RiskController {
  private final RiskService risk;

  public RiskController(JedisPooled jedis) throws IOException {
    String script = new String(Files.readAllBytes(Paths.get("feature-service/src/main/resources/lua/win_incr.lua")));
    FeatureService feat = new RedisFeatureService(jedis, jedis.scriptLoad(script));
    risk = new RiskService(feat, new YamlPolicyRepository());
  }

  @PostMapping("/check")
  public RiskResponse check(@RequestBody FxRiskReq req) {
    return risk.decide(req);
  }
}