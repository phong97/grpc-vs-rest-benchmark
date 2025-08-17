package com.demo.rest.controller;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

/**
 *
 * @author phonghv
 */
@RestController
@RequestMapping("/zalo")
public class ZaloMockController {
    private final Random random = new Random();

    private final Counter sendMessageCounter;
    private final Counter sendMessageNoDelayCounter;

    public ZaloMockController(MeterRegistry meterRegistry) {
        this.sendMessageCounter = Counter.builder("message_service_send_total")
                .description("Số lần gọi sendMessage()")
                .tag("method", "sendMessage")
                .register(meterRegistry);

        this.sendMessageNoDelayCounter = Counter.builder("message_service_send_no_delay_total")
                .description("Số lần gọi sendMessageNoRandomDelay()")
                .tag("method", "sendMessageNoRandomDelay")
                .register(meterRegistry);
    }

    @PostMapping("/send-message")
    public ResponseEntity<Map<String, Object>> sendMessage(@RequestBody Map<String, Object> request) throws InterruptedException, UnknownHostException {
        simulateDelay();
        String hostname = InetAddress.getLocalHost().getHostName();
        System.out.println("Handled by instance: " + hostname + ", Thread: " + Thread.currentThread());

        Map<String, Object> response = Map.of(
                "error", 0,
                "message", "Success",
                "data", Map.of(
                        "msg_id", UUID.randomUUID().toString().substring(0, 20),
                        "sent_time", System.currentTimeMillis(),
                        "sending_mode", "1",
                        "quota", Map.of("dailyQuota", "500", "remainingQuota", "499")
                )
        );

        sendMessageCounter.increment();
        return ResponseEntity.ok(response);
    }

    @PostMapping("/send-message-no-random-delay")
    public ResponseEntity<Map<String, Object>> sendMessageNoRandomDelay(@RequestBody Map<String, Object> request) throws UnknownHostException, InterruptedException {
        String hostname = InetAddress.getLocalHost().getHostName();
        System.out.println("Handled by instance: " + hostname + ", Thread: " + Thread.currentThread());

        Thread.sleep(100);

        Map<String, Object> response = Map.of(
                "error", 0,
                "message", "Success",
                "data", Map.of(
                        "msg_id", UUID.randomUUID().toString().substring(0, 20),
                        "sent_time", System.currentTimeMillis(),
                        "sending_mode", "1",
                        "quota", Map.of("dailyQuota", "500", "remainingQuota", "499")
                )
        );

        sendMessageNoDelayCounter.increment();
        return ResponseEntity.ok(response);
    }

    private void simulateDelay() throws InterruptedException {
        double p = random.nextDouble();
        long delay;
        if (p < 0.95) delay = 100 + random.nextInt(400);
        else if (p < 0.98) delay = 500 + random.nextInt(500);
        else if (p < 0.9995) delay = 1000 + random.nextInt(2000);
        else delay = 3001 + random.nextInt(2000);
        Thread.sleep(delay);
    }
}
