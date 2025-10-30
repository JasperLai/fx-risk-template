package com.bank.risk.gateway.controller;

import com.bank.risk.engine.RuleRuntime;
import org.springframework.web.bind.annotation.*;

/**
 * 简单的发布控制器：用于规则中心完成入库后，通知风控侧失效并预热缓存。
 */
@RestController
@RequestMapping("/api/rule")
public class RuleAdminController {
  private final RuleRuntime runtime;

  public RuleAdminController(RuleRuntime runtime) {
    this.runtime = runtime;
  }

  @PostMapping("/invalidate")
  public String invalidate(@RequestParam String strategyId) {
    runtime.invalidate(strategyId);
    return "OK";
  }

  @PostMapping("/preload")
  public String preload(@RequestParam String strategyId) {
    boolean ok = runtime.preload(strategyId);
    return ok ? "OK" : "FAIL";
  }
}


