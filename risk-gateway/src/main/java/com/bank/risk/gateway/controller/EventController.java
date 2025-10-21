package com.bank.risk.gateway.controller;
import org.springframework.web.bind.annotation.*; import redis.clients.jedis.JedisPooled;
import java.math.BigDecimal; import java.util.Map;
@RestController @RequestMapping("/api/event")
public class EventController {
  private final JedisPooled jedis; public EventController(JedisPooled jedis){ this.jedis=jedis; }
  @PostMapping("/trade")
  public Map<String,String> onTrade(@RequestBody Map<String,Object> ev){
    String book=(String)ev.getOrDefault("book","bookA");
    String symbol=(String)ev.get("symbol");
    String tenor=(String)ev.getOrDefault("tenor","SPOT");
    BigDecimal qty=new BigDecimal(ev.getOrDefault("qty","0").toString());
    jedis.incrByFloat("expo:"+book+":"+symbol+":"+tenor, qty.doubleValue());
    jedis.incrByFloat("pnl:today:deskDefault", -10.0);
    return Map.of("status","ok");
  }
  @PostMapping("/cancel")
  public Map<String,String> onCancel(@RequestBody Map<String,Object> ev){
    String trader=(String)ev.get("trader");
    jedis.incr("cancel:"+trader+":10:num");
    jedis.incr("order:"+trader+":10:den");
    return Map.of("status","ok");
  }
}
