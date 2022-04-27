package com.test.ratelimiter.userlimiter;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class RequestLimiterCacheServiceImpl implements RequestLimiterCacheService {

    private final Map<String, AtomicInteger> requestNumberByUserCache = new ConcurrentHashMap<>();

    @Override
    public boolean addElement(String userIP, Integer thresholdRequestNumber) {
        if (requestNumberByUserCache.putIfAbsent(userIP, new AtomicInteger(1)) == null) {
            return true;
        }
        AtomicInteger currentRequestNumberForIPAtomic = requestNumberByUserCache.get(userIP);
        int currentNumberValue;
        do {
            currentNumberValue = currentRequestNumberForIPAtomic.get();
            if (currentNumberValue >= thresholdRequestNumber) {
                return false;
            }
        } while (!currentRequestNumberForIPAtomic.compareAndSet(currentNumberValue, currentNumberValue + 1));
        return true;
    }

    @Override
    public void clearCache() {
        requestNumberByUserCache.clear();
    }
}
