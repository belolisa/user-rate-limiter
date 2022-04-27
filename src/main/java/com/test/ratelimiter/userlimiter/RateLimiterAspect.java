package com.test.ratelimiter.userlimiter;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Aspect checks if user has reached threshold of http request (or even service calls) in time period.
 * If reached throws RequestLimitReachedException
 *
 * Aspect works for methods annotated by @UserRateLimit and have request parameter either HttpServletRequest.class
 * or a String matches IP mask
 */
@Aspect
@Component
public class RateLimiterAspect {

    private static final String IP_MASK = "^[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}$";
    private static final String IP_ARGUMENT_MAME = "userIP";

    private final RequestLimiterCacheService cacheService;
    private final UserRateLimiterProperties rateLimiterProperties;

    public RateLimiterAspect(@Autowired RequestLimiterCacheService cacheService,
                             @Autowired UserRateLimiterProperties rateLimiterProperties) {
        this.cacheService = cacheService;
        this.rateLimiterProperties = rateLimiterProperties;
    }

    @PostConstruct
    public void init() {
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        scheduler.scheduleAtFixedRate(cacheService::clearCache,
                rateLimiterProperties.getMinutes(),
                rateLimiterProperties.getMinutes(),
                TimeUnit.MINUTES);
    }

    @Before("@annotation(UserRateLimit)")
    public void checkAmountOfRequestByUser(JoinPoint joinPoint) {
        MethodSignature methodSig = (MethodSignature) joinPoint.getSignature();
        Object[] args = joinPoint.getArgs();

        if (Arrays.asList(methodSig.getParameterNames()).contains(IP_ARGUMENT_MAME)) {
            for (Object arg : args) {
                if (arg instanceof String && ((String) arg).matches(IP_MASK)) {
                    if (!cacheService.addElement((String) arg, rateLimiterProperties.getThresholdRequestAmount())) {
                        throw new RequestLimitReachedException();
                    }
                }
            }
        } else if (Arrays.asList(methodSig.getParameterTypes()).contains(HttpServletRequest.class)) {
            for (Object arg : args) {
                if (arg instanceof HttpServletRequest) {
                    if (!cacheService.addElement(((HttpServletRequest) arg).getRemoteAddr(), rateLimiterProperties.getThresholdRequestAmount())) {
                        throw new RequestLimitReachedException();
                    }
                }
            }
        }
    }
}