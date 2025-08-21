package com.grpc.grpcbackend.service;

import com.grpc.grpcbackend.MessageRequest;
import com.grpc.grpcbackend.MessageResponse;
import com.grpc.grpcbackend.MessageServiceGrpc;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.grpc.server.service.GrpcService;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;

import java.util.Random;
import java.util.UUID;

/**
 *
 * @author phonghv
 */
@GrpcService
public class MessageService extends MessageServiceGrpc.MessageServiceImplBase {

    private final Random random = new Random();
    private static final Logger logger = LoggerFactory.getLogger(MessageService.class);

    private final Counter sendMessageCounter;
    private final Counter sendMessageNoDelayCounter;

    public MessageService(MeterRegistry meterRegistry) {
        this.sendMessageCounter = Counter.builder("message_service_send_total")
                .description("Số lần gọi sendMessage()")
                .tag("method", "sendMessage")
                .register(meterRegistry);

        this.sendMessageNoDelayCounter = Counter.builder("message_service_send_no_delay_total")
                .description("Số lần gọi sendMessageNoRandomDelay()")
                .tag("method", "sendMessageNoRandomDelay")
                .register(meterRegistry);
    }


    @Override
    public void sendMessage(MessageRequest request, StreamObserver<MessageResponse> responseObserver) {
        try {
            simulateDelay();
            String msgId = UUID.randomUUID().toString().substring(0, 20);
            long now = System.currentTimeMillis();

            MessageResponse response = MessageResponse.newBuilder()
                    .setError(0)
                    .setMessage("Success")
                    .setMsgId(msgId)
                    .setSentTime(now)
                    .setSendingMode("1")
                    .setQuota(MessageResponse.Quota.newBuilder()
                            .setDailyQuota("500")
                            .setRemainingQuota("499")
                            .build())
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (Exception e) {
            logger.error("Error processing sendMessage request: {}", e.getMessage(), e);
            responseObserver.onError(e);
        } finally {
            sendMessageCounter.increment();
        }
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

    @Override
    public void sendMessageNoRandomDelay(MessageRequest request, StreamObserver<MessageResponse> responseObserver) {
        try {
            // delay for 100 milliseconds
            Thread.sleep(10);

            String msgId = UUID.randomUUID().toString().substring(0, 20);
            long now = System.currentTimeMillis();

            MessageResponse response = MessageResponse.newBuilder()
                    .setError(0)
                    .setMessage("Success")
                    .setMsgId(msgId)
                    .setSentTime(now)
                    .setSendingMode("1")
                    .setQuota(MessageResponse.Quota.newBuilder()
                            .setDailyQuota("500")
                            .setRemainingQuota("499")
                            .build())
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (Exception e) {
            logger.error("Error processing sendMessageNoRandomDelay request: {}", e.getMessage(), e);
            responseObserver.onError(e);
        } finally {
            sendMessageNoDelayCounter.increment();
        }
    }
}

