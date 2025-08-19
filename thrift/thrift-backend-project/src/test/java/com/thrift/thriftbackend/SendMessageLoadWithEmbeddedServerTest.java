package com.thrift.thriftbackend;

import com.thrift.thriftbackend.client.MessageServiceClient;
import org.apache.thrift.TException;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.server.TServer;
import org.apache.thrift.server.TThreadPoolServer;
import org.apache.thrift.transport.TFramedTransport;
import org.apache.thrift.transport.TServerSocket;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.net.InetSocketAddress;

@SpringBootTest(properties = {
        "thrift.server.enabled=false",    // prevent Spring server
        "thrift.server.host=localhost",
        "thrift.server.port=9090"
})
class SendMessageLoadWithEmbeddedServerTest {

    private static TServer server;
    private static Thread serverThread;

    @BeforeAll
    static void startServer() throws Exception {
        MessageService.Iface handler = new MessageService.Iface() {
            @Override
            public MessageResponse sendMessage(Message message) throws TException {
                return null;
            }

            @Override
            public MessageResponse sendMessageNoRandomDelay(Message message) {
                return new MessageResponse().setError(0).setMessage("success");
            }
        };
        MessageService.Processor<MessageService.Iface> processor = new MessageService.Processor<>(handler);
        TServerSocket socket = new TServerSocket(new InetSocketAddress("localhost", 9090));
        TThreadPoolServer.Args args = new TThreadPoolServer.Args(socket)
                .processor(processor)
                .protocolFactory(new TBinaryProtocol.Factory())
                .transportFactory(new TFramedTransport.Factory());

        server = new TThreadPoolServer(args);
        serverThread = new Thread(server::serve, "thrift-test-server");
        serverThread.setDaemon(true);
        serverThread.start();
        // crude wait for startup
        Thread.sleep(200);
    }

    @AfterAll
    static void stopServer() {
        if (server != null) server.stop();
    }

    @Autowired
    MessageServiceClient client;

    @Test
    void test_roundTrip() throws TException, InterruptedException {
        MessageServiceClient.ClientConnection conn = client.getClient();
        try {
            Message msg = new Message().setPhone("123").setTemplateId("1234");
            MessageResponse resp = conn.client().sendMessageNoRandomDelay(msg);
            System.out.println(resp);
            Assertions.assertNotNull(resp);
        } finally {
            client.releaseConnection(conn);
        }
    }
}