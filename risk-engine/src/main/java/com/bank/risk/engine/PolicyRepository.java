package com.bank.risk.engine;

import java.util.Map;

public interface PolicyRepository {
    Map<String, Object> load(String grayTag);
}