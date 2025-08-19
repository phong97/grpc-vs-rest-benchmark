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

//    @Test
//    public void test_sendMessageNoDelay() throws TException {
//        Message message = new Message();
//        message.setPhone("84987654321")
//                .setTemplateId("7895417a7d3f9461cd2e")
//                .setTrackingId("tracking_id")
//                .setTemplateData(Map.of("ky", "1", "thang", "4/2020",
//                        "start_date", "20/03/2020",
//                        "end_date", "20/04/2020",
//                        "customer", "Nguyễn Thị Hoàng Anh",
//                        "cid", "PE010299485",
//                        "address", "VNG Campus, TP.HCM",
//                        "amount", "100",
//                        "total", "100000"));
//        try {
//            MessageResponse messageResponse = messageServiceClient.getClient().client().sendMessageNoRandomDelay(message);
//            System.out.println("Response: " + messageResponse);
//        } catch (Throwable ex) {
//            logger.error(ex.getMessage(), ex);
//        }
//    }

    @Test
    public void loadTest_sendMessageNoDelay() {
        int numThreads = 10;
        int totalMessages = 1000;
        long startTime = System.currentTimeMillis();
        test_sendMessageNoDelay(numThreads, totalMessages/2);
        test_sendMessageNoDelay(numThreads, totalMessages/2);
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        logger.info("Load test completed: {} threads, {} messages, duration: {} ms", numThreads, totalMessages, duration);
    }

    private void test_sendMessageNoDelay(int numThreads, int totalMessages) {
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        AtomicInteger messageCounter = new AtomicInteger(0);

        for (int i = 0; i < totalMessages; i++) {
            executor.submit(() -> {
                MessageServiceClient.ClientConnection clientConnection = null;
                try {
                    clientConnection = messageServiceClient.getClient();
                } catch (InterruptedException e) {
                    logger.error("Error getting client connection: {}", e.getMessage(), e);
                    messageCounter.incrementAndGet();
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
                    clientConnection.client().sendMessageNoRandomDelay(message);
                    messageServiceClient.releaseConnection(clientConnection);
                } catch (TException e) {
                    logger.error("Error sending message: {}", e.getMessage(), e);
                    messageServiceClient.invalidateConnection(clientConnection);
                } finally {
                    int currentMessage = messageCounter.incrementAndGet();
                    logger.info("Message sent: {}/{}", currentMessage, totalMessages);
                }
            });
        }

        while (true) {
            int currentMessageDone = messageCounter.get();
            if (currentMessageDone == totalMessages) {
                logger.info("All messages sent successfully: {}", currentMessageDone);
                break;
            }
        }

        executor.shutdown();
    }
}
