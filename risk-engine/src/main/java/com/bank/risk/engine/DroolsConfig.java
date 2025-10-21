package com.bank.risk.engine;

import org.kie.api.KieServices;
import org.kie.api.runtime.KieContainer;

public class DroolsConfig {
  private static final KieContainer KC = KieServices.Factory.get().getKieClasspathContainer();

  public static KieContainer container() {
    return KC;
  }
}
