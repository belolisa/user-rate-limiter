package com.test.ratelimiter;

import com.test.ratelimiter.userlimiter.UserRateLimit;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;

@RestController
public class RootController {

    @UserRateLimit
    @GetMapping("/")
    public String getSuccess(HttpServletRequest request) {
        return "Success request";
    }

}

