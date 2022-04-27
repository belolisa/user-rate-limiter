package com.test.ratelimiter;

import com.test.ratelimiter.userlimiter.RequestLimitReachedException;
import com.test.ratelimiter.userlimiter.RequestLimiterCacheService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@SpringBootTest
public class SomeLogicServiceTest {

    @Autowired
    private SomeLogicService service;
    @Autowired
    RequestLimiterCacheService requestCacheService;

    @BeforeEach
    private void init() {
        requestCacheService.clearCache();
    }

    @Test
    public void getSuccess() {
        String result = service.createSomethingForUser("127.0.0.1");
        assertThat(result).isEqualTo("Success");
    }

    @Test
    public void test_requestsFromSeveralIps() throws InterruptedException, ExecutionException {
        ExecutorService executorService1 = Executors.newFixedThreadPool(5);
        ExecutorService executorService2 = Executors.newFixedThreadPool(5);
        CountDownLatch startLatch = new CountDownLatch(10);
        List<Future<String>> responseMessagesFrom1Ip = new ArrayList<>(5);
        for (int threadNo = 0; threadNo < 5; threadNo++) {
            responseMessagesFrom1Ip.add(executorService1.submit(() ->
                    new LatchedThread(startLatch, service, "192.168.0.0").call()));
            startLatch.countDown();
        }
        List<Future<String>> responseMessagesFrom2Ip = new ArrayList<>(5);
        for (int threadNo = 0; threadNo < 5; threadNo++) {
            responseMessagesFrom2Ip.add(executorService2.submit(() ->
                    new LatchedThread(startLatch, service, "172.31.255.255").call()));
            startLatch.countDown();
        }
        int countSuccessFrom1Ip = 0;
        int countRejectedFrom1Ip = 0;
        for (Future<String> responseMessage1 : responseMessagesFrom1Ip) {
            while (!responseMessage1.isDone()) {
                Thread.sleep(100);
            }
            String response = responseMessage1.get();
            if (response.contains("Success")) countSuccessFrom1Ip++;
            else if (response.contains("rate limit reached")) countRejectedFrom1Ip++;
        }

        int countSuccessFrom2Ip = 0;
        int countRejectedFrom2Ip = 0;
        for (Future<String> responseMessage2 : responseMessagesFrom2Ip) {
            while (!responseMessage2.isDone()) {
                Thread.sleep(100);
            }
            String response = responseMessage2.get();
            if (response.contains("Success")) countSuccessFrom2Ip++;
            else if (response.contains("rate limit reached")) countRejectedFrom2Ip++;
        }
        assertThat(countSuccessFrom1Ip).isEqualTo(2);
        assertThat(countRejectedFrom1Ip).isEqualTo(3);
        assertThat(countSuccessFrom2Ip).isEqualTo(2);
        assertThat(countRejectedFrom2Ip).isEqualTo(3);
    }

    static class LatchedThread implements Callable<String> {
        private final CountDownLatch startLatch;
        private final SomeLogicService service;
        private final String ipAddress;

        public LatchedThread(CountDownLatch startLatch,
                             SomeLogicService service,
                             String ipAddress) {
            this.startLatch = startLatch;
            this.service = service;
            this.ipAddress = ipAddress;
        }

        @Override
        public String call() throws Exception {
            try {
                startLatch.await();
                return service.createSomethingForUser(ipAddress);
            } catch (RequestLimitReachedException ex) {
                return "rate limit reached";
            }
        }
    }
}