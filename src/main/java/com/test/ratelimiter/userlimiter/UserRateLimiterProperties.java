package com.test.ratelimiter.userlimiter;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@Configuration
@PropertySource("classpath:application.properties")
public class UserRateLimiterProperties {
    @Value( "${rate-limit.user.minutes}" )
    private Integer minutes;

    @Value( "${rate-limit.user.threshold-request-amount}" )
    private Integer thresholdRequestAmount;

    public Integer getMinutes() {
        return minutes;
    }

    public Integer getThresholdRequestAmount() {
        return thresholdRequestAmount;
    }
}
