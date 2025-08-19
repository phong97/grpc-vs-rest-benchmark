package com.thrift.thriftbackend.config;

import com.thrift.thriftbackend.MessageService;
import com.thrift.thriftbackend.handler.MessageServiceHandler;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.server.TThreadedSelectorServer;
import org.apache.thrift.transport.TFramedTransport;
import org.apache.thrift.transport.TNonblockingServerSocket;
import org.apache.thrift.transport.TTransportException;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;

import java.net.InetSocketAddress;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;

/**
 *
 * @author phonghv
 */
@Configuration
@ConditionalOnProperty(value = "thrift.server.enabled", havingValue = "true", matchIfMissing = true)
public class AppRunner implements ApplicationRunner {

    private static final Logger logger = org.slf4j.LoggerFactory.getLogger(AppRunner.class);

    private final MessageServiceHandler messageServiceHandler;

    @Value("${thrift.server.host:localhost}")
    private String host;

    @Value("${thrift.server.port:9090}")
    private int port;

    public AppRunner(MessageServiceHandler messageServiceHandler) {
        this.messageServiceHandler = messageServiceHandler;
    }


    @Override
    public void run(ApplicationArguments args) throws TTransportException, InterruptedException {
        MessageService.Processor<MessageService.Iface> processor = new MessageService.Processor<>(messageServiceHandler);

        TNonblockingServerSocket socket = new TNonblockingServerSocket(new InetSocketAddress(this.host, this.port));
        TThreadedSelectorServer.Args serverArgs = new TThreadedSelectorServer.Args(socket);

        // configure executor service with virtual threads
        ThreadFactory virtualThreadFactory = Thread.ofVirtual().factory();
        ThreadPoolExecutor executorService = new ThreadPoolExecutor(
                16, // corePoolSize
                32, // maximumPoolSize
                serverArgs.getStopTimeoutVal(), // keepAliveTime
                serverArgs.getStopTimeoutUnit(), // đơn vị thời gian
                new LinkedBlockingQueue<>(), // hàng đợi
                virtualThreadFactory, // Sử dụng virtual threads
                new ThreadPoolExecutor.AbortPolicy()
        );
        serverArgs.executorService(executorService);

        serverArgs.processor(processor);

        serverArgs.transportFactory(new TFramedTransport.Factory());
        serverArgs.protocolFactory(new TBinaryProtocol.Factory());
        serverArgs.acceptQueueSizePerThread(32);
        serverArgs.selectorThreads(4);

        TThreadedSelectorServer server = new TThreadedSelectorServer(serverArgs);

        Thread serverThread = new Thread(server::serve, "thrift-test-server");
        serverThread.setDaemon(true);
        serverThread.start();
        Thread.sleep(200);
        logger.info("Thrift server is running on {}:{}", this.host, this.port);
    }
}
