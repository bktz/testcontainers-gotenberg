# Testcontainers Gotenberg - Quarkus Dev Services Extension

A Quarkus extension that provides Dev Services support for [Gotenberg](https://gotenberg.dev/) using Testcontainers. This extension automatically provisions a Gotenberg container during development and testing, making it easy to work with document conversion services.

## Features

- 🚀 Automatic Gotenberg container provisioning in dev and test modes
- 🔄 Shared container support for improved performance across test suites
- ⚙️ Configurable Docker image and port mapping
- 🎯 Zero-configuration by default
- 📝 Automatic injection of `quarkus.gotenberg.url` property

## Installation

Add the extension to your Quarkus project:

```xml
<dependency>
    <groupId>com.digibel</groupId>
    <artifactId>testcontainers-gotenberg</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

## Usage

### Zero Configuration

By default, the extension will automatically start a Gotenberg container when you run your application in dev mode or when running tests:

```bash
mvn quarkus:dev
```

The Gotenberg URL will be available as `quarkus.gotenberg.url` and can be injected into your application:

```java
@Inject
@ConfigProperty(name = "quarkus.gotenberg.url")
String gotenbergUrl;
```

### Configuration Options

You can customize the Dev Services behavior in your `application.properties`:

```properties
# Disable Dev Services (default: true)
quarkus.gotenberg.devservices.enabled=true

# Docker image to use (default: gotenberg/gotenberg:8)
quarkus.gotenberg.devservices.image-name=gotenberg/gotenberg:8

# Use shared container across test runs (default: true)
quarkus.gotenberg.devservices.shared=true

# Service name for shared containers (default: gotenberg)
quarkus.gotenberg.devservices.service-name=gotenberg

# Fixed port mapping (optional, uses random port if not set)
quarkus.gotenberg.devservices.port=3000
```

### Static Port Configuration

For development scenarios where you need a fixed port:

```properties
# Map Gotenberg to port 3000 on localhost
quarkus.gotenberg.devservices.port=3000
```

Then access Gotenberg at `http://localhost:3000`

### Disabling Dev Services

If you have a Gotenberg instance already running:

```properties
# Disable Dev Services
quarkus.gotenberg.devservices.enabled=false

# Configure the external Gotenberg URL
quarkus.gotenberg.url=http://localhost:3000
```

## How It Works

1. When your Quarkus application starts in dev or test mode, the extension checks if `quarkus.gotenberg.url` is already configured
2. If not configured and Dev Services is enabled, it starts a Gotenberg container using Testcontainers
3. The container URL is automatically injected as `quarkus.gotenberg.url`
4. When shared mode is enabled, the extension reuses existing containers with the same service name
5. The container is automatically stopped when your application shuts down

## Container Sharing

By default, containers are shared across test runs and dev mode sessions. This means:

- Multiple test suites can use the same Gotenberg container
- The container persists between test runs for faster execution
- Containers are identified by the label `quarkus-dev-service-gotenberg`

To use multiple Gotenberg instances simultaneously, configure different service names:

```properties
# In project A
quarkus.gotenberg.devservices.service-name=gotenberg-a

# In project B  
quarkus.gotenberg.devservices.service-name=gotenberg-b
```

## Requirements

- Docker must be installed and running
- Quarkus 3.30.6 or higher
- Java 17 or higher

## Example

```java
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;

public class DocumentService {
    
    @Inject
    @ConfigProperty(name = "quarkus.gotenberg.url")
    String gotenbergUrl;
    
    public void convertToPdf() {
        // Use gotenbergUrl to make requests
        String endpoint = "http://" + gotenbergUrl + "/forms/chromium/convert/html";
        // Your conversion logic here
    }
}
```

## License

This extension follows the same license as the parent project.

