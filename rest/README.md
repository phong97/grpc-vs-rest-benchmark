# REST Backend Project

This is a Spring Boot REST backend service, designed for benchmarking and comparison with gRPC-based services. It provides mock endpoints and is containerized for easy deployment.

## Features
- RESTful API endpoints
- Mock Zalo API controller
- Virtual thread configuration for performance
- Docker and Docker Compose support

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

Available endpoints:

- `POST /zalo/message/template` — Send a template message (mock Zalo API)

### Sample cURL Test

Test the mock endpoint with:

```sh
curl -X POST http://localhost:8081/zalo/send-message \
  -H "Content-Type: application/json" \
  -d '{"phone": "0123456789", "template_id": "abc123", "params": {"name": "John"}}'
```

## Configuration

Application properties can be set in `src/main/resources/application.properties`.

## Testing

Run tests with:

```sh
./gradlew test
```

## License

MIT (or specify your license)
