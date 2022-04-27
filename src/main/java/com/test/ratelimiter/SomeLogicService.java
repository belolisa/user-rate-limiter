package com.test.ratelimiter;

import com.test.ratelimiter.userlimiter.UserRateLimit;
import org.springframework.stereotype.Component;

/**
 * Example class to check @UserRateLimit can work on services as well
 */
@Component
public class SomeLogicService {

    @UserRateLimit
    public String createSomethingForUser(String userIP){
        // do something
        return "Success";
    }
}
