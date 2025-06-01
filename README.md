# Java Web Server

A simple Java web server supporting both single-threaded non-blocking I/O and multi-threaded blocking I/O modes.

## Features

- **SingleThreadedNonBlockingIOServer**: Uses Java NIO for efficient single-threaded event-driven networking.
- **MultiThreadedBlockingIOServer**: Uses classic blocking I/O with a thread pool for concurrency.
- HTTP/1.1 support with basic keep-alive handling.
- Connection management and cleanup.
- Configurable via `ServerConfig`.

## Requirements

- Java 17 or later
- Maven

## Getting Started

### 1. Clone the Repository

```bash
git clone https://github.com/yourusername/java-web-server.git
cd java-web-server
```

### 2. Build

If using Maven:

```bash
mvn clean package
```

### 3. Configuration

Edit `application.properties`:

```java
server.port=1024
server.host=127.0.0.1
server.noThreads=15
server.sockerscheduler=[SingleThreadedNonBlockingIOServer|MultiThreadedBlockingIOServer]
```

### 4. Running the Server

#### SingleThreadedNonBlockingIOServer

```java
ServerConfig config = new ServerConfig();
WebServer server = new SingleThreadedNonBlockingIOServer(config);
server.start();
```

#### MultiThreadedBlockingIOServer

```java
ServerConfig config = new ServerConfig();
WebServer server = new MultiThreadedBlockingIOServer(config);
server.start();
```

## Usage

- Send HTTP requests using Postman, curl, or JMeter.
- The server will handle multiple concurrent connections.
- For best results with the non-blocking server, ensure clients either reuse connections or the server closes connections after each response.

## Notes

- **SingleThreadedNonBlockingIOServer**: Designed for efficiency, but ensure clients either reuse connections or the server closes connections after each response.
- **MultiThreadedBlockingIOServer**: Each connection is handled by a thread from the pool. Suitable for workloads with moderate concurrency.
- Load tested using jMeter and postman, and the server can handle up to 100-200 concurrent users with as errors less than 1% of requests. **MultiThreadedBlockingIOServer** works best because connections are managed more gracefully using java blocking socket IO and thread per connection using thread pooling.

## License

MIT License