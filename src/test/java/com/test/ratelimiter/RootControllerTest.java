package com.test.ratelimiter;

import com.test.ratelimiter.userlimiter.RequestLimiterCacheService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class RootControllerTest {

    @Autowired
    private TestRestTemplate template;
    @Autowired
    RequestLimiterCacheService requestCacheService;

    @BeforeEach
    private void init() {
        requestCacheService.clearCache();
    }

    @Test
    public void test_returnSuccess() {
        ResponseEntity<String> response1 = template.getForEntity("/", String.class);
        assertThat(response1.getBody()).isEqualTo("Success request");
    }

    @Test
    public void test_thresholdReached_successively() {
        ResponseEntity<String> response1 = template.getForEntity("/", String.class);
        assertThat(response1.getBody()).isEqualTo("Success request");

        ResponseEntity<String> response2 = template.getForEntity("/", String.class);
        assertThat(response2.getBody()).isEqualTo("Success request");

        ResponseEntity<String> response3 = template.getForEntity("/", String.class);
        assertThat(response3.getStatusCode()).isEqualTo(HttpStatus.BAD_GATEWAY);
        assertThat(response3.getBody()).isEqualTo("Rate limit reached. Request later");
    }

    @Test
    public void test_thresholdReached_parallel() throws InterruptedException, ExecutionException {
        ExecutorService executorService = Executors.newFixedThreadPool(5);
        CountDownLatch startLatch = new CountDownLatch(5);
        List<Future<String>> responseMessages = new ArrayList<>(5);
        for (int threadNo = 0; threadNo < 5; threadNo++) {
            responseMessages.add(executorService.submit(() -> new LatchedThread(startLatch, template).call()));
            startLatch.countDown();
        }

        int countSuccess = 0;
        int countRejected = 0;
        for (Future<String> responseMessage : responseMessages) {
            while (!responseMessage.isDone()) {
                Thread.sleep(100);
            }
            String response = responseMessage.get();
            if (response.contains("Success request")) countSuccess++;
            else if (response.contains("Rate limit reached")) countRejected++;
        }
        assertThat(countSuccess).isEqualTo(2);
        assertThat(countRejected).isEqualTo(3);
    }

    @Disabled //check only manually (not to spend time of build)
    @Test
    public void checkTimeout() throws InterruptedException {
        ResponseEntity<String> response1 = template.getForEntity("/", String.class);
        assertThat(response1.getBody()).isEqualTo("Success request");

        ResponseEntity<String> response2 = template.getForEntity("/", String.class);
        assertThat(response2.getBody()).isEqualTo("Success request");

        ResponseEntity<String> response3 = template.getForEntity("/", String.class);
        assertThat(response3.getStatusCode()).isEqualTo(HttpStatus.BAD_GATEWAY);
        assertThat(response3.getBody()).isEqualTo("Rate limit reached. Request later");

        Thread.sleep(60000);
        ResponseEntity<String> response4 = template.getForEntity("/", String.class);
        assertThat(response4.getBody()).isEqualTo("Success request");
    }

    static class LatchedThread implements Callable<String> {
        private final CountDownLatch startLatch;
        private final TestRestTemplate template;

        public LatchedThread(CountDownLatch startLatch, TestRestTemplate template) {
            this.startLatch = startLatch;
            this.template = template;
        }

        @Override
        public String call() throws Exception {
            startLatch.await();
            ResponseEntity<String> response = template.getForEntity("/", String.class);
            return response.getBody();
        }
    }
}
