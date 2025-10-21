package com.bank.risk.common.dto;
import java.math.BigDecimal; import java.util.Map;
/**
 * FX 风控请求。
 *
 * 用于对报价/下单进行事前风控校验，或承载成交/撤单等事件的核心字段。
 *
 * 字段说明：
 * @param type  业务类型，使用枚举 {@link OrderType}（QUOTE/ORDER/TRADE/CANCEL）；规则可能按类型分流。
 * @param desk  交易台/事业部标识，例如 FX1；用于限额、PnL 维度归属。
 * @param trader 交易员标识，例如 alice；用于节流、撤单率、白名单等。
 * @param book  账簿/簿册，例如 bookA；用于净敞口与限额聚合键。
 * @param symbol 交易标的（币对），例如 EURUSD；用于价格/比例/限额规则。
 * @param tenor 期限，例如 SPOT/1W/1M；影响限额、敞口与规则参数选择。
 * @param side  方向，BUY/SELL；与数量共同决定风险敞口符号与校验逻辑。
 * @param qty   数量或名义本金（正数）；用于 per-order 限额与敞口更新。
 * @param ourBid 我方买价；用于价格偏离、价差等价格类规则。
 * @param ourAsk 我方卖价；与 ourBid 一同用于点差与价格带校验。
 * @param attrs 扩展属性（自由键值），例如 clientTier、channel、scenario 等。
 */
public record FxRiskReq(
  OrderType type, String desk, String trader, String book,
  String symbol, String tenor, String side, BigDecimal qty,
  BigDecimal ourBid, BigDecimal ourAsk, Map<String,Object> attrs
) {}
