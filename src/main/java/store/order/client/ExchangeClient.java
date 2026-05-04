package store.order.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "exchange-service")
public interface ExchangeClient {

    @GetMapping("/exchanges/{from}/{to}")
    ExchangeResponse getRate(@PathVariable("from") String from,
                             @PathVariable("to") String to,
                             @RequestHeader("id-account") String idAccount);
}
