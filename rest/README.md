# REST Backend Project

This is a Spring Boot REST backend service, designed for benchmarking and comparison with gRPC- (and future Thrift-) based services. It provides mock Zalo-like messaging endpoints, realistic latency simulation, Prometheus metrics, and is containerized for easy deployment.

Slide kết quả / presentation: https://docs.google.com/presentation/d/1Di1Pb4XVcBCwhuM6yNRgIOMptYCn3Wzk6w6dABqvCYc/edit?usp=sharing

## Features
- RESTful API endpoints (JSON)
- Two latency profiles: random distribution & near-constant (control)
- Mock Zalo messaging domain model
- Java 21 Virtual Threads via custom Jetty thread pool
- Micrometer + Prometheus metrics (Actuator /actuator/prometheus)
- Nginx load-balancing (2 instances) + access logging
- Docker & Docker Compose orchestration

## Project Structure
```
rest-backend-project/
├── src/
│   ├── main/java/com/demo/rest/
│   │   ├── RestBackendApplication.java
│   │   ├── config/VirtualThreadConfig.java
│   │   └── controller/ZaloMockController.java
│   └── main/resources/
│       └── application.properties
│   └── test/java/com/demo/rest/RestBackendProjectApplicationTests.java
├── build.gradle
├── Dockerfile
└── ...
```

## Prerequisites
- Java 21+
- Gradle (wrapper included)
- Docker (optional, for containerized run)

## Setup & Build

Clone the repository and navigate to the project directory:

```sh
git clone <repo-url>
cd rest-backend-project
```

Build the project using Gradle:

```sh
./gradlew build
```

## Running the Application

### Locally

```sh
./gradlew bootRun
```

The service will start on `http://localhost:8080` by default.

### With Docker

Build and run the Docker container:

```sh
docker build -t rest-backend .
docker run -p 8080:8080 rest-backend
```

Or use Docker Compose (with nginx load balancer):

```sh
docker compose up --build
```

The service will be available on `http://localhost:8081` (nginx load balancer) when using Docker Compose.

## API Endpoints

Available endpoints (exposed directly and via nginx on port 8081):

- POST /zalo/send-message — random latency distribution
- POST /zalo/send-message-no-random-delay — ~10ms fixed sleep (baseline)

### Sample Request Body (realistic)
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

### Sample cURL (nginx LB)
```sh
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

### Typical Response
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

## Configuration

Application properties can be set in `src/main/resources/application.properties`.

Metrics endpoint: `/actuator/prometheus`

Custom counters:
- message_service_send_total (tag method=sendMessage)
- message_service_send_no_delay_total (tag method=sendMessageNoRandomDelay)

## Testing

Run tests with:

```sh
./gradlew test
```

## License

MIT (or specify your license)
