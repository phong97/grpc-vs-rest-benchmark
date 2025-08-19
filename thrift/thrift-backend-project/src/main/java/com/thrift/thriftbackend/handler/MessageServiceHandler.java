package com.thrift.thriftbackend.handler;

import com.thrift.thriftbackend.Message;
import com.thrift.thriftbackend.MessageResponse;
import com.thrift.thriftbackend.MessageService;
import com.thrift.thriftbackend.Quota;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.apache.thrift.TException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Random;
import java.util.UUID;

/**
 *
 * @author phonghv
 */
@Service
public class MessageServiceHandler implements MessageService.Iface {

    private static final Logger logger = LoggerFactory.getLogger(MessageServiceHandler.class);
    private final Random random = new Random();
    private final Counter sendMessageCounter;
    private final Counter sendMessageNoDelayCounter;

    public MessageServiceHandler(MeterRegistry meterRegistry) {
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
    public MessageResponse sendMessage(Message message) throws TException {
        try {
            simulateDelay();
            String msgId = UUID.randomUUID().toString().substring(0, 20);
            long now = System.currentTimeMillis();

            return new MessageResponse()
                    .setError(0)
                    .setMessage("Success")
                    .setMsgId(msgId)
                    .setSendTime(now)
                    .setSendingMode("1")
                    .setQuota(new Quota()
                            .setDailyQuota("500")
                            .setRemainingQuota("499"));
        } catch (Exception e) {
            logger.error("Error processing sendMessage request: {}", e.getMessage(), e);
            return new MessageResponse(1, "Error processing request: " + e.getMessage());
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
    public MessageResponse sendMessageNoRandomDelay(Message message) throws TException {
        try {
            // delay for 100 milliseconds
            Thread.sleep(100);

            String msgId = UUID.randomUUID().toString().substring(0, 20);
            long now = System.currentTimeMillis();

            return new MessageResponse()
                    .setError(0)
                    .setMessage("Success")
                    .setMsgId(msgId)
                    .setSendTime(now)
                    .setSendingMode("1")
                    .setQuota(new Quota()
                            .setDailyQuota("500")
                            .setRemainingQuota("499"));
        } catch (Exception e) {
            logger.error("Error processing sendMessageNoRandomDelay request: {}", e.getMessage(), e);
            return new MessageResponse(1, "Error processing request: " + e.getMessage());
        } finally {
            sendMessageNoDelayCounter.increment();
        }
    }
}
