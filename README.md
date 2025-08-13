# gRPC vs REST Performance Benchmark

A comprehensive performance benchmarking project comparing gRPC and REST API implementations using Spring Boot with Java 21 Virtual Threads.

## 🎯 Project Overview

This project implements identical messaging API functionality using both gRPC and REST protocols to compare:
- **Performance**: Throughput, latency, and resource usage
- **Scalability**: Behavior under different load conditions  
- **Architecture**: Protocol efficiency and implementation complexity

## 🏗️ Architecture

### REST Backend
- **Framework**: Spring Boot 3.5.4 with Jetty (excluding Tomcat)
- **Concurrency**: Java 21 Virtual Threads via custom configuration
- **Load Balancing**: Nginx reverse proxy with 2 backend instances
- **API**: Mock Zalo messaging service with realistic latency simulation

### gRPC Backend  
- **Framework**: Spring Boot 3.5.4 with Spring gRPC
- **Protocol**: gRPC with Protocol Buffers
- **Service**: MessageService implementing template message sending
- **Concurrency**: Java 21 Virtual Threads support

## 📁 Project Structure

```
grpc-vs-rest-benchmark/
├── grpc/
│   ├── docker-compose.yml
│   ├── envoy/
│   │   └── envoy.yaml
│   ├── grpc-backend-project/
│   │   ├── build.gradle
│   │   ├── Dockerfile
│   │   ├── src/
│   │   │   ├── main/
│   │   │   │   ├── java/com/
│   │   │   │   ├── proto/message.proto
│   │   │   │   └── resources/application.properties
│   │   │   └── test/java/com/
│   ├── monitoring/
│   │   ├── prometheus.yml
│   │   └── grafana/
│   │       ├── datasources/
│   │       └── dashboards/
├── rest/
│   ├── docker-compose.yml
│   ├── nginx/
│   │   └── nginx.conf
│   ├── rest-backend-project/
│   │   ├── build.gradle
│   │   ├── Dockerfile
│   │   ├── src/
│   │   │   ├── main/
│   │   │   │   ├── java/com/
│   │   │   │   └── resources/application.properties
│   │   │   └── test/java/com/
├── thrift/
│   └── thrift-backend-project/
│       ├── build.gradle
│       ├── src/
│       │   ├── main/
│       │   │   ├── java/com/
│       │   │   └── resources/application.properties
│       │   └── test/java/com/
├── README.md
└── note.txt
```

## 🚀 Quick Start

1. **Clone the repository**
   ```bash
   git clone https://github.com/your-username/grpc-vs-rest-benchmark.git
   cd grpc-vs-rest-benchmark
   ```

2. **Build the backend services**
   ```bash
   cd rest/rest-backend-project
   ./gradlew build
   cd ../../grpc/grpc-backend-project
   ./gradlew build
   ```

3. **Start the services with Docker Compose**
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
   - Prometheus and Grafana are available in the `grpc/monitoring` directory.
   - Grafana dashboards for system, JVM, and Spring Boot metrics are pre-configured.

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

## 📊 Monitoring & Metrics

- **Spring Boot Actuator**: Exposes metrics at `/actuator/prometheus` for Prometheus scraping.
- **Prometheus**: Configured to collect metrics from both Envoy and Spring Boot backends.
- **Grafana**: Visualizes performance, JVM, and application metrics.

## 📝 API Documentation

### REST API

**Endpoint**: `POST /zalo/message/template`

**Request Body**:
```json
{
  "phone": "0123456789",
  "template_id": "template_001", 
  "template_data": {
    "name": "John Doe",
    "amount": "100,000 VND"
  },
  "tracking_id": "track_123"
}
```

**Response**:
```json
{
  "error": 0,
  "message": "Success",
  "data": {
    "msg_id": "msg_12345abcde67890f",
    "sent_time": 1654072800000,
    "sending_mode": "1",
    "quota": {
      "dailyQuota": "500",
      "remainingQuota": "499"
    }
  }
}
```

**cURL Example**:
```bash
curl -X POST http://localhost:8081/zalo/message/template \
  -H "Content-Type: application/json" \
  -d '{
    "phone": "0123456789",
    "template_id": "welcome_template",
    "template_data": {"name": "Alice"},
    "tracking_id": "test_001"
  }'
```

### gRPC API

**Service**: `MessageService`  
**Method**: `SendTemplate`

**Proto Definition**:
```protobuf
service MessageService {
  rpc SendTemplate (MessageRequest) returns (MessageResponse);
}

message MessageRequest {
  string phone = 1;
  string template_id = 2;
  map<string, string> template_data = 3;
  string tracking_id = 4;
}

message MessageResponse {
  int32 error = 1;
  string message = 2;
  string msg_id = 3;
  int64 sent_time = 4;
  string sending_mode = 5;
  Quota quota = 6;
  
  message Quota {
    string daily_quota = 1;
    string remaining_quota = 2;
  }
}
```

## ⚡ Performance Features

### Latency Simulation
Both implementations include realistic latency patterns:
- **95%** of requests: 100-500ms (normal)
- **3%** of requests: 500-1000ms (slower)  
- **0.05%** of requests: 1000-3000ms (slow)
- **0.005%** of requests: 3000-5000ms (very slow)

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

## 🧪 Benchmarking

### Load Testing Tools
Recommended tools for performance testing:
- **Apache Bench (ab)**: Simple HTTP load testing
- **wrk**: Modern HTTP benchmarking tool  
- **JMeter**: GUI-based comprehensive testing
- **ghz**: gRPC-specific load testing tool

### Sample Load Test Commands

**REST API with wrk**:
```bash
wrk -t12 -c400 -d30s -s script.lua http://localhost:8081/zalo/message/template
```

**gRPC with ghz**:
```bash
ghz --insecure \
  --proto ./grpc-backend/grpc-backend-project/src/main/proto/message.proto \
  --call MessageService.SendTemplate \
  -d '{"phone":"0123456789","template_id":"test","template_data":{"name":"Test"},"tracking_id":"load_test"}' \
  --total=10000 \
  --concurrency=50 \
  localhost:9090
```

### Key Metrics to Compare
- **Requests per second (RPS)**
- **95th percentile latency**  
- **Memory usage**
- **CPU utilization**
- **Network bandwidth**
- **Connection overhead**

## 🔧 Configuration

### REST Backend Config
```properties
# rest-backend-project/src/main/resources/application.properties
spring.application.name=rest-backend-project
server.port=8080
```

### gRPC Backend Config  
```properties
# grpc-backend-project/src/main/resources/application.properties
spring.application.name=grpc-backend
```

### Docker Compose for REST Load Balancing
```yaml
services:
  backend1:
    build: ./rest-backend-project
    ports: ["8080"]
  backend2:  
    build: ./rest-backend-project
    ports: ["8080"]
  nginx:
    image: nginx:latest
    volumes:
      - ./nginx/nginx.conf:/etc/nginx/nginx.conf:ro
    ports:
      - "8081:8081"
    depends_on: [backend1, backend2]
```

## 🏆 Expected Benchmark Results

**Typical performance characteristics**:

| Metric | REST | gRPC | Winner |
|--------|------|------|---------|
| Serialization Speed | JSON (slower) | Protobuf (faster) | gRPC |
| Payload Size | Larger (JSON) | Smaller (binary) | gRPC |  
| HTTP/2 Features | Limited | Full support | gRPC |
| Browser Support | Excellent | Limited | REST |
| Debugging | Easy (human readable) | Harder (binary) | REST |
| Ecosystem | Mature | Growing | REST |

## 🛠️ Development

### Building Projects

**REST Backend**:
```bash
cd rest-backend/rest-backend-project
./gradlew clean build
```

**gRPC Backend**:
```bash
cd grpc-backend/grpc-backend-project  
./gradlew clean build
```

### Running Tests
```bash
./gradlew test
```

### Docker Build
```bash
# REST
docker build -t rest-backend ./rest-backend/rest-backend-project

# gRPC  
docker build -t grpc-backend ./grpc-backend/grpc-backend-project
```

## 🚧 Development Status

### ✅ Completed
- [x] REST backend with Virtual Threads
- [x] Mock Zalo API implementation  
- [x] Docker containerization
- [x] Nginx load balancing
- [x] Realistic latency simulation
- [x] gRPC proto definition

### 🔄 In Progress  
- [ ] gRPC service implementation
- [ ] gRPC Docker configuration
- [ ] Performance testing scripts
- [ ] Benchmark result analysis tools

### 📋 Todo
- [ ] Monitoring and metrics collection
- [ ] Database integration for realistic workloads
- [ ] CI/CD pipeline for automated benchmarking
- [ ] Grafana dashboards for result visualization

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit changes (`git commit -m 'Add amazing feature'`)
4. Push to branch (`git push origin feature/amazing-feature`)  
5. Open a Pull Request

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 📚 References

- [Spring Boot Documentation](https://docs.spring.io/spring-boot/docs/current/reference/html/)
- [gRPC Java](https://grpc.io/docs/languages/java/)
- [Prometheus](https://prometheus.io/)
- [Grafana](https://grafana.com/)
