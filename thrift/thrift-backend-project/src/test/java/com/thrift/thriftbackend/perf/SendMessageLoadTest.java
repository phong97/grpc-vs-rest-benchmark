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

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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
        int numThreads = 100; // Number of threads to simulate
        int totalMessages = 10000;
        int messagesPerThread = totalMessages / numThreads;
        long startTime = System.currentTimeMillis();

        ExecutorService vtExec1 = Executors.newVirtualThreadPerTaskExecutor();
        ExecutorService vtExec2 = Executors.newFixedThreadPool(numThreads); // Using a fixed thread pool to slow down the execution

        CompletableFuture<?>[] completableFutures = new CompletableFuture[numThreads];
        for (int i = 0; i < numThreads; i++) {
            final int threadIndex = i;
            completableFutures[i] = CompletableFuture.runAsync(() -> {
                logger.info("Thread {} started", threadIndex);
                long threadStartTime = System.currentTimeMillis();
                try {
                    test_sendMessageNoDelay(vtExec2, messagesPerThread);
                } catch (Exception ex) {
                    logger.error("Error in thread {}: {}", threadIndex, ex.getMessage(), ex);
                }
                long threadEndTime = System.currentTimeMillis();
                long threadDuration = threadEndTime - threadStartTime;
                logger.info("Thread {} completed: {} messages, duration: {} ms", threadIndex, messagesPerThread, threadDuration);
                double messagesPerSecond = (messagesPerThread * 1000.0) / threadDuration;
                logger.info("Thread {} messages per second: {}", threadIndex, messagesPerSecond);
                logger.info("Thread {} finished", threadIndex);
            }, vtExec1);
        }

        CompletableFuture.allOf(completableFutures).join();

        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        logger.info("Load test completed: {} threads, {} messages, duration: {} ms", numThreads, totalMessages, duration);
        double messagesPerSecond = (totalMessages * 1000.0) / duration;
        logger.info("Messages per second: {}", messagesPerSecond);
    }

    private void test_sendMessageNoDelay(ExecutorService executor, int totalMessages) {
        CompletableFuture<?>[] completableFutures = new CompletableFuture[totalMessages];
        for (int i = 0; i < totalMessages; i++) {
            completableFutures[i] = CompletableFuture.runAsync(this::sendMessageNoDelay, executor);
        }

        CompletableFuture.allOf(completableFutures).join();
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
