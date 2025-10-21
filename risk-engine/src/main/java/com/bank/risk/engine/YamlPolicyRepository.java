package com.bank.risk.engine;

import org.yaml.snakeyaml.Yaml;
import java.io.InputStream;
import java.util.Map;

public class YamlPolicyRepository implements PolicyRepository {
  @Override
  public Map<String, Object> load(String grayTag) {
    try (InputStream is = getClass().getResourceAsStream("/policies/fx-policy.yaml")) {
      return new Yaml().load(is);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}