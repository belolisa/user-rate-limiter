package com.test.ratelimiter.userlimiter;

/**
 * Service of manage local cache of current calls by user ip.
 */
public interface RequestLimiterCacheService {
    /**
     *  Add element to local cache if threshold not reached
     * 
     * @param userIP ip of user made a request
     * @param thresholdRequestNumber threshold number
     * @return true if added, false if cache is full
     */
    boolean addElement(String userIP, Integer thresholdRequestNumber);

    /**
     * Clears all cache.
     */
    void clearCache();
}
