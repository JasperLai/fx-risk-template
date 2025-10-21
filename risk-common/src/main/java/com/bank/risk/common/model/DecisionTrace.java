package com.bank.risk.common.model;

import java.util.List;
import java.util.Map;

public record DecisionTrace(String decisionId, String policyVersion, String symbol, String type, String result,
    List<RuleHit> hits, Map<String, Object> features, long latencyMs) {
}
