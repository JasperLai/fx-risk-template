package com.bank.risk.common.dto;
import java.util.List; import java.util.Map;
public record RiskResponse(Decision decision, String decisionId, List<String> reasons, Map<String,Object> debug) {}
