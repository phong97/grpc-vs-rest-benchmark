# gRPC vs REST Performance Benchmark

A comprehensive performance benchmarking project comparing gRPC & REST API & APACHE THRIFT implementations using Spring Boot with Java 21 Virtual Threads.

Presentation slide: https://docs.google.com/presentation/d/1Di1Pb4XVcBCwhuM6yNRgIOMptYCn3Wzk6w6dABqvCYc/edit?usp=sharing

## 🎯 Project Overview

This project implements identical messaging API functionality using both gRPC and REST protocols to compare:
- **Performance**: Throughput, latency, and resource usage
- **Scalability**: Behavior under different load conditions  
- **Architecture**: Protocol efficiency and implementation complexity

## 🏗️ Architecture

### REST Backend
- Framework: Spring Boot 3.5.4 with Jetty (Tomcat excluded)
- Concurrency: Java 21 Virtual Threads via custom Jetty thread pool configuration
- Load Balancing: Nginx reverse proxy with 2 backend instances
- Endpoints:
  - POST /zalo/send-message (random latency distribution)
  - POST /zalo/send-message-no-random-delay (near constant latency ~10ms + processing)

### gRPC Backend  
- Framework: Spring Boot 3.5.4 with Spring gRPC
- Protocol: gRPC (HTTP/2) + Envoy (as both gRPC LB & JSON -> gRPC transcoder)
- Service: MessageService (sendMessage, sendMessageNoRandomDelay)
- Concurrency: Java 21 Virtual Threads (JVM level usage for request handling)
- Gateways:
  - Envoy gRPC load balancer (port 9091)
  - Envoy JSON-to-gRPC transcoder (port 8081) exposing same REST-like paths mapped to gRPC via google.api.http annotations

### Thrift Backend (Experimental)
- Framework: Spring Boot 3.5.4 + embedded Thrift server (TThreadedSelectorServer)
- Protocol: Thrift binary protocol over framed, non-blocking transport
- Concurrency: Virtual Thread backed executor (custom ThreadPoolExecutor using Thread.ofVirtual())
- Load Balancing: Nginx (2 backend instances) similar to REST pattern
- Endpoints: Thrift service (MessageService) methods:
  - sendMessage(Message) -> MessageResponse (random latency)
  - sendMessageNoRandomDelay(Message) -> MessageResponse (baseline latency)
- Metrics: Same Micrometer counters & Prometheus integration

## 📁 Project Structure

```
grpc-vs-rest-benchmark/
├── grpc/
│   ├── docker-compose.yml
│   ├── envoy/
│   │   ├── envoy_grpc.yaml                 # gRPC LB
│   │   ├── envoy_json_to_grpc.yaml         # JSON -> gRPC transcoder
│   │   └── message.pd                      # Proto descriptor set
│   ├── grpc-backend-project/
│   │   ├── build.gradle
│   │   ├── Dockerfile
│   │   ├── src/main/proto/message.proto
│   │   └── src/main/java/... (service impl)
│   ├── monitoring/ (Prometheus, Grafana)
├── rest/
│   ├── docker-compose.yml
│   ├── nginx/nginx.conf                    # LB & access log
│   ├── rest-backend-project/
│   │   ├── build.gradle
│   │   ├── Dockerfile
│   │   ├── src/main/java/... (controller)
│   │   └── src/main/resources/application.properties
├── thrift/
│   ├── docker-compose.yml                  # 2 Thrift instances + nginx + monitoring
│   ├── thrift-backend-project/
│   │   ├── build.gradle
│   │   ├── src/main/thrift/message.thrift  # Thrift IDL
│   │   ├── src/main/java/... (handler, server config)
│   │   └── thrift-gen/ (bundled thrift compiler)
├── README.md
└── monitor-results/
```

## 🚀 Quick Start

1. **Clone the repository**
   ```bash
   git clone https://github.com/phong97/grpc-vs-rest-vs-thrift-benchmark
   cd grpc-vs-rest-benchmark
   ```

2. **Build the backend services** (produces fat jars `rest-backend.jar` & `grpc-backend.jar`)
   ```bash
   cd rest/rest-backend-project
   ./gradlew build
   cd ../../grpc/grpc-backend-project
   ./gradlew build
   ```

3. **Start the services with Docker Compose** (each stack provides 2 instances + LB/proxy + monitoring)
   - For REST:
     ```bash
     cd rest
     docker-compose up --build
     ```
   - For gRPC:
     ```bash
     cd grpc
     docker-compose up --build
     ```

4. **Access monitoring dashboards**
- REST stack: Prometheus (9090), Grafana (3000), Telegraf exporter (9273), Nginx access log (mounted)
- gRPC stack: Prometheus (9090), Grafana (3000), Envoy admin (9901 & 9911), JSON gateway (8081), gRPC gateway (9091)
- Thrift stack: Prometheus (9090), Grafana (3000), Nginx LB (9091) + Telegraf (9273)
- Dashboards: JVM, Spring Boot, Envoy, System metrics auto-provisioned

### Prerequisites
- **Java 21+** (for Virtual Threads support)
- **Docker & Docker Compose** (for containerized deployment)
- **Gradle** (wrapper included)

### REST Backend Setup

1. **Single Instance**:
```bash
cd rest-backend/rest-backend-project
./gradlew bootRun
# Service runs on http://localhost:8080
```

2. **Load Balanced (Recommended)**:
```bash
cd rest-backend
docker compose up --build
# Service runs on http://localhost:8081 (nginx proxy)
```

### gRPC Backend Setup

```bash
cd grpc-backend/grpc-backend-project
./gradlew bootRun
# gRPC service runs on default port
```

### Thrift Backend Setup (Experimental)

```bash
cd thrift/thrift-backend-project
./gradlew build
./gradlew bootRun
# Thrift server (binary protocol) listening default host:port (localhost:9091) unless overridden
```

Docker Compose (2 instances + nginx + monitoring):
```bash
cd thrift
docker-compose up --build
```

Client example pseudo-code (Java):
```java
TSocket socket = new TSocket(host, 9091, 3000);
TTransport transport = new TFramedTransport(socket);
transport.open();
TProtocol proto = new TBinaryProtocol(transport);
MessageService.Client client = new MessageService.Client(proto);
Message msg = new Message().setPhone("84987654321").setTemplateId("7895417a7d3f9461cd2e");
MessageResponse resp = client.sendMessage(msg);
```

## 📊 Monitoring & Metrics

- **Spring Boot Actuator**: Exposes metrics at `/actuator/prometheus` for Prometheus scraping.
- **Prometheus**: Configured to collect metrics from both Envoy and Spring Boot backends.
- **Grafana**: Visualizes performance, JVM, and application metrics.

## 📝 API Documentation

### REST & JSON-gRPC Transcoded API

Endpoints (both served by REST backend via Nginx and by Envoy JSON->gRPC gateway):
- POST /zalo/send-message
- POST /zalo/send-message-no-random-delay

Sample Request Body (the realistic provided example):
```json
{
  "phone": "84987654321",
  "template_id": "7895417a7d3f9461cd2e",
  "template_data": {
    "ky": "1",
    "thang": "4/2020",
    "start_date": "20/03/2020",
    "end_date": "20/04/2020",
    "customer": "Nguyễn Thị Hoàng Anh",
    "cid": "PE010299485",
    "address": "VNG Campus, TP.HCM",
    "amount": "100",
    "total": "100000"
  },
  "tracking_id": "tracking_id"
}
```

Typical Response Body:
```json
{
  "error": 0,
  "message": "Success",
  "data": {
    "msg_id": "<20-char-id>",
    "sent_time": 1710000000000,
    "sending_mode": "1",
    "quota": { "dailyQuota": "500", "remainingQuota": "499" }
  }
}
```

Example cURL (REST via Nginx or Envoy JSON gateway on 8081):
```bash
curl -X POST http://localhost:8081/zalo/send-message \
  -H 'Content-Type: application/json' \
  -d '{
  "phone": "84987654321",
  "template_id": "7895417a7d3f9461cd2e",
  "template_data": {
    "ky": "1",
    "thang": "4/2020",
    "start_date": "20/03/2020",
    "end_date": "20/04/2020",
    "customer": "Nguyễn Thị Hoàng Anh",
    "cid": "PE010299485",
    "address": "VNG Campus, TP.HCM",
    "amount": "100",
    "total": "100000"
  },
  "tracking_id": "tracking_id"
}'
```

### gRPC API

**gRPC Service**: MessageService

Methods:
- rpc sendMessage(MessageRequest) returns (MessageResponse)
- rpc sendMessageNoRandomDelay(MessageRequest) returns (MessageResponse)

Proto (excerpt):
```protobuf
service MessageService {
  rpc sendMessage(MessageRequest) returns (MessageResponse) {
    option (google.api.http) = { post: "/zalo/send-message" body: "*" };
  }
  rpc sendMessageNoRandomDelay(MessageRequest) returns (MessageResponse) {
    option (google.api.http) = { post: "/zalo/send-message-no-random-delay" body: "*" };
  }
}
```

### Thrift API (IDL Excerpt)
```thrift
namespace java com.thrift.thriftbackend

struct Message {
  1: required string phone,
  2: required string templateId,
  3: optional map<string,string> templateData,
  4: optional string trackingId
}

struct Quota {
  1: optional string dailyQuota,
  2: optional string remainingQuota
}

struct MessageResponse {
  1: required i32 error,
  2: required string message,
  3: optional string msgId,
  4: optional i64 sendTime,
  5: optional string sendingMode,
  6: optional Quota quota
}

service MessageService {
  MessageResponse sendMessage(1: Message message)
  MessageResponse sendMessageNoRandomDelay(1: Message message)
}
```

## ⚡ Performance Features

### Latency Simulation
Both implementations share identical probabilistic latency (applied only on sendMessage / sendMessage RPC):
- 95%: 100–500 ms
- 3%: 500–1000 ms  
- 0.05%: 1000–3000 ms
- 0.005%: 3000–5000 ms

The /send-message-no-random-delay endpoint/RPC keeps latency near constant (~10 ms sleep + processing) to provide a control baseline.

### Virtual Threads Configuration
Java 21 Virtual Threads are enabled for maximum concurrent request handling:

```java
@Configuration
public class VirtualThreadConfig {
    @Bean
    public WebServerFactoryCustomizer<JettyServletWebServerFactory> customizer() {
        return factory -> {
            QueuedThreadPool threadPool = new QueuedThreadPool();
            threadPool.setVirtualThreadsExecutor(
                Executors.newVirtualThreadPerTaskExecutor()
            );
            factory.setThreadPool(threadPool);
        };
    }
}
```

## 🛠️ Development

### Building Projects

**REST Backend**:
```bash
cd rest/rest-backend-project
./gradlew clean build -x test
```

**gRPC Backend**:
```bash
cd grpc/grpc-backend-project  
./gradlew clean build -x test
```

**Thrift Backend**:
```bash
cd thrift/thrift-backend-project  
./gradlew clean build -x test
```

### Docker Run
```bash
docker compose up --build
```

## 🧪 Benchmarking

- Presentation slide: https://docs.google.com/presentation/d/1Di1Pb4XVcBCwhuM6yNRgIOMptYCn3Wzk6w6dABqvCYc/edit?usp=sharing
- Monitor result: https://github.com/phong97/grpc-vs-rest-vs-thrift-benchmark/tree/master/monitor-results

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit changes (`git commit -m 'Add amazing feature'`)
4. Push to branch (`git push origin feature/amazing-feature`)  
5. Open a Pull Request

## 📚 References

- [Spring Boot Documentation](https://docs.spring.io/spring-boot/docs/current/reference/html/)
- [gRPC Java](https://grpc.io/docs/languages/java/)
- [Apache Thrift](https://thrift.apache.org/)
- [Prometheus](https://prometheus.io/)
- [Grafana](https://grafana.com/)
- [Envoy](https://www.envoyproxy.io/docs/envoy/v1.35.1/)
- [Nginx](https://nginx.org/en/docs/index.html)
