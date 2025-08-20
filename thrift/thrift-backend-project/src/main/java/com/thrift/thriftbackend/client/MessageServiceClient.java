package com.thrift.thriftbackend.client;

import com.thrift.thriftbackend.MessageService;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.TFramedTransport;
import org.apache.thrift.transport.TSocket;
import org.apache.thrift.transport.TTransport;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.concurrent.*;

/**
 *
 * @author phonghv
 */
@Service
public class MessageServiceClient {

    private static final int DEFAULT_MAX_ACTIVE = 10;

    private final BlockingQueue<ClientConnection> pool = new LinkedBlockingQueue<>();

    @Value("${thrift.server.host:localhost}")
    private String host;

    @Value("${thrift.server.port:9090}")
    private int port;

    @Value("${thrift.client.pool.maxSize:1000}")
    private int maxPoolSize;

    @Value("${thrift.client.pool.minIdle:0}")
    private int minIdle;

    @Value("${thrift.client.pool.maxIdle:8}")
    private int maxIdle;

    @Value("${thrift.client.pool.borrowTimeoutMillis:10000}")
    private int borrowTimeoutMillis;

    // request timeout in milliseconds
    @Value("${thrift.client.requestTimeoutMillis:3000}")
    private int requestTimeoutMillis;

    private Semaphore permits;
    private ScheduledExecutorService housekeeper;

    @PostConstruct
    void init() {
        applyConfigGuards();

        this.permits = new Semaphore(maxPoolSize, true);

        if (minIdle > 0) {
            this.housekeeper = Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "thrift-client-pool-housekeeper");
                t.setDaemon(true);
                return t;
            });
            ensureMinIdle();
            housekeeper.scheduleAtFixedRate(this::safeEnsureMinIdle, 1, 1, TimeUnit.SECONDS);
        }
    }

    @PreDestroy
    void shutdown() {
        if (housekeeper != null) {
            housekeeper.shutdownNow();
        }
        ClientConnection conn;
        while ((conn = pool.poll()) != null) {
            closeQuietly(conn);
            permits.release();
        }
    }

    public ClientConnection getClient() throws InterruptedException {
        ClientConnection conn = pool.poll(borrowTimeoutMillis, TimeUnit.MILLISECONDS);
        if (conn != null) {
            if (isOpen(conn)) {
                return conn;
            }
            try {
                conn.transport.open();
                return conn;
            } catch (Exception e) {
                invalidateConnection(conn);
            }
        }

        try {
            return createNewConnection();
        } catch (Exception e) {
            throw new RuntimeException("Failed to create Thrift client", e);
        }

//        try {
//            if (!permits.tryAcquire(borrowTimeoutMillis, TimeUnit.MILLISECONDS)) {
//                throw new RuntimeException("Timeout acquiring connection from pool");
//            }
//        } catch (InterruptedException ie) {
//            Thread.currentThread().interrupt();
//            throw new RuntimeException("Interrupted while waiting for connection", ie);
//        }
//
//        try {
//            return createNewConnection();
//        } catch (Exception e) {
//            permits.release();
//            throw new RuntimeException("Failed to create Thrift client", e);
//        }
    }

    public void releaseConnection(ClientConnection conn) {
        if (conn == null) return;
        if (isOpen(conn)) {
            if (!pool.offer(conn)) {
                invalidateConnection(conn);
            }
        } else {
            invalidateConnection(conn);
        }
    }

    public void invalidateConnection(ClientConnection conn) {
        if (conn == null) return;
        closeQuietly(conn);
        permits.release();
    }

    private ClientConnection createNewConnection() throws Exception {
        TSocket socket = new TSocket(host, port);
        socket.setTimeout(requestTimeoutMillis);

        TFramedTransport transport = new TFramedTransport(socket);
        TProtocol protocol = new TBinaryProtocol(transport);
        MessageService.Client client = new MessageService.Client(protocol);
        transport.open();
        return new ClientConnection(client, transport);
    }

    private boolean isOpen(ClientConnection conn) {
        return conn.transport != null && conn.transport.isOpen();
    }

    private void closeQuietly(ClientConnection conn) {
        try {
            if (conn.transport != null && conn.transport.isOpen()) {
                conn.transport.close();
            }
        } catch (Exception ignored) {
        }
    }

    private void ensureMinIdle() {
        while (pool.size() < Math.min(minIdle, maxIdle) && permits.tryAcquire()) {
            try {
                ClientConnection conn = createNewConnection();
                if (!pool.offer(conn)) {
                    closeQuietly(conn);
                    permits.release();
                    break;
                }
            } catch (Exception e) {
                permits.release();
                break;
            }
        }
    }

    private void safeEnsureMinIdle() {
        try {
            ensureMinIdle();
        } catch (Throwable ignored) {
        }
    }

    private void applyConfigGuards() {
        if (maxIdle > maxPoolSize) maxIdle = maxPoolSize;
        if (minIdle > maxIdle) minIdle = maxIdle;
        if (minIdle < 0) minIdle = 0;
        if (maxPoolSize < 1) maxPoolSize = DEFAULT_MAX_ACTIVE;
    }

    public record ClientConnection(MessageService.Client client, TTransport transport) {
    }
}