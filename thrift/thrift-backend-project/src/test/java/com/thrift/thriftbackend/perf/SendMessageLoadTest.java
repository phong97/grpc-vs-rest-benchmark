package com.thrift.thriftbackend.perf;

import com.thrift.thriftbackend.Message;
import com.thrift.thriftbackend.MessageResponse;
import com.thrift.thriftbackend.client.MessageServiceClient;
import org.apache.thrift.TException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

/**
 *
 * @author phonghv
 */
@SpringBootTest(properties = "thrift.server.enabled=false")
@ExtendWith(SpringExtension.class)
public class SendMessageLoadTest {

    private static final Logger logger = org.slf4j.LoggerFactory.getLogger(SendMessageLoadTest.class);

    @Autowired
    private MessageServiceClient messageServiceClient;

    @Test
    public void test_sendMessageNoDelay() throws TException {
        Message message = new Message();
        message.setPhone("84987654321")
                .setTemplateId("7895417a7d3f9461cd2e")
                .setTrackingId("tracking_id")
                .setTemplateData(Map.of("ky", "1", "thang", "4/2020",
                        "start_date", "20/03/2020",
                        "end_date", "20/04/2020",
                        "customer", "Nguyễn Thị Hoàng Anh",
                        "cid", "PE010299485",
                        "address", "VNG Campus, TP.HCM",
                        "amount", "100",
                        "total", "100000"));
        try {
            MessageResponse messageResponse = messageServiceClient.getClient().client().sendMessageNoRandomDelay(message);
            System.out.println("Response: " + messageResponse);
        } catch (Throwable ex) {
            logger.error(ex.getMessage(), ex);
        }
    }

    @Test
    public void loadTest_sendMessageNoDelay() throws InterruptedException {
        int numThreads = 100;
        int totalMessages = 1_000_000;
        int messagesPerThread = totalMessages / numThreads;
        long startTime = System.currentTimeMillis();

        ExecutorService executorService = Executors.newFixedThreadPool(numThreads);

        List<Future<?>> futures = new ArrayList<>();
        for (int i = 0; i < totalMessages; i++) {
            futures.add(executorService.submit(() -> {
                try {
                    sendMessageNoDelay();
                } catch (Exception ex) {
                    logger.error(ex.getMessage(), ex);
                }
            }));
        }

        logger.info("Starting load test with {} threads, each sending {} messages", numThreads, messagesPerThread);

        AtomicInteger threadIndex = new AtomicInteger();
        futures.forEach(future -> {
            try {
                logger.info("Waiting for future task to complete for thread {}", threadIndex.getAndIncrement());
                future.get();
            } catch (Exception e) {
                logger.error("Error in future task: {}", e.getMessage(), e);
            }
        });
        executorService.shutdownNow();

        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        logger.info("Load test completed: {} threads, {} messages, duration: {} ms", numThreads, totalMessages, duration);
        double messagesPerSecond = (totalMessages * 1000.0) / duration;
        logger.info("Messages per second: {}", messagesPerSecond);
    }

    private void test_sendMessageNoDelay(ExecutorService executor, int totalMessages) {
        List<Future<?>> futures = new ArrayList<>();
        for (int i = 0; i < totalMessages; i++) {
            futures.add(executor.submit(() -> {
                try {
                    sendMessageNoDelay();
                } catch (Exception ex) {
                    logger.error("Error sending message: {}", ex.getMessage(), ex);
                }
            }));
        }

        futures.forEach(future -> {
            try {
                future.get(); // Wait for each task to complete
            } catch (Exception e) {
                logger.error("Error in future task: {}", e.getMessage(), e);
            }
        });
        logger.info("All messages sent in this batch");
    }

    private void sendMessageNoDelay() {
        MessageServiceClient.ClientConnection clientConnection = null;
        try {
            clientConnection = messageServiceClient.getClient();
        } catch (InterruptedException e) {
            logger.error("Error getting client connection: {}", e.getMessage(), e);
            return;
        }

        if (clientConnection == null) {
            logger.error("Failed to get client connection");
            return;
        }

        try {
            Message message = new Message();
            message.setPhone("84987654321")
                    .setTemplateId("7895417a7d3f9461cd2e")
                    .setTrackingId("tracking_id")
                    .setTemplateData(Map.of("ky", "1", "thang", "4/2020",
                            "start_date", "20/03/2020",
                            "end_date", "20/04/2020",
                            "customer", "Nguyễn Thị Hoàng Anh",
                            "cid", "PE010299485",
                            "address", "VNG Campus, TP.HCM",
                            "amount", "100",
                            "total", "100000"));
            MessageResponse messageResponse = clientConnection.client().sendMessageNoRandomDelay(message);
//            logger.info("Response: {}", messageResponse);
            messageServiceClient.releaseConnection(clientConnection);
        } catch (TException e) {
            logger.error("Error sending message: {}", e.getMessage(), e);
            messageServiceClient.invalidateConnection(clientConnection);
        }
    }
}
