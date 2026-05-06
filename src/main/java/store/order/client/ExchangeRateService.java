package store.order.client;

import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

/**
 * Wrapper around the Feign {@link ExchangeClient} that adds Redis caching.
 *
 * <p>{@code @Cacheable} cannot be applied directly to a Feign interface — Spring Cache
 * needs a concrete bean to wrap. This service is the indirection layer.</p>
 *
 * <p>The cache key is intentionally restricted to {@code from}+{@code to}: the rate
 * returned by AwesomeAPI does not depend on which user is asking. Including
 * {@code idAccount} would partition the cache per user, defeating the bottleneck.</p>
 */
@Service
@RequiredArgsConstructor
public class ExchangeRateService {

    private final ExchangeClient client;

    @Cacheable(value = "exchange-rates", key = "#from + '-' + #to")
    public ExchangeResponse getRate(String from, String to, String idAccount) {
        return client.getRate(from, to, idAccount);
    }
}
