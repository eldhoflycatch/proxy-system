## Proxy System
## Overview
The proxy-system is a multi-module Java application built using Spring Boot and Netty. It implements a two-tier proxy architecture:

- Ship Proxy (running on localhost:8080): Accepts incoming HTTP requests (e.g., from curl) and forwards them to the Offshore Proxy.
- Offshore Proxy (running on localhost:8081): Receives requests from the Ship Proxy, fetches content from the target URL (e.g., http://example.com), and returns the response back through the chain.
- This project serves as a basic example of a proxy chain, currently supporting HTTP GET requests and returning HTML content from target websites.

## Prerequisites
- Java 17: Ensure Java 17 is installed (java -version should show 17.x.x).
- Maven 3.6+: Required for building the project (mvn -version to verify).
- Network Access: Ensure your system can reach target URLs (e.g., http://example.com) for testing.
- Available Ports: Ensure ports 8080 (Ship Proxy) and 8081 (Offshore Proxy) are free.

## Setup Instructions
1. - Clone or Set Up the Project
   - Clone the repository if hosted (e.g., git clone <repository-url>), or manually create the project structure and copy the provided files into their respective locations.
2. - Build the Project
   - Navigate to the root directory (proxy-system) and build using Maven:
   ```bash
   cd proxy-system
   mvn clean install
    ```
3. - Start the Offshore Proxy
   - Start the Offshore Proxy first, as the Ship Proxy depends on it:
   ```bash
   cd offshore-proxy
   mvn spring-boot:run
    ```
   ## Expected Output: Offshore Proxy started on port 8081.
4. - Start the Ship Proxy
   - In a separate terminal, start the Ship Proxy:
   ```bash
   cd ship-proxy
   mvn spring-boot:run
    ```
   ## Expected Output:
   - Connected to Offshore Proxy at localhost:8081
   - Ship Proxy started on port 8080
   ## Test multiple
   - Test Sequential Processing
     To verify that requests are handled sequentially (one by one) when sent in parallel:

macOS/Linux:
bash

Copy
curl -v -x http://localhost:8080 http://example.com &
curl -v -x http://localhost:8080 http://example.com &
curl -v -x http://localhost:8080 http://example.com &
6. ## Usage
   Test the Proxy
   Use curl to send a request through the proxy chain to a target HTTP URL (e.g., http://example.com):
    ```
7. ## Expected Output:
    *   Trying [::1]:8080...
* Connected to localhost (::1) port 8080
> GET http://example.com/ HTTP/1.1
> Host: example.com
> User-Agent: curl/8.5.0
> Accept: */*
> Proxy-Connection: Keep-Alive
>
< HTTP/1.1 200 OK
< Content-Type: text/html
< Content-Length: 1257
< Connection: close
<
<!doctype html>
<html>
<head>
    <title>Example Domain</title>
    <meta charset="utf-8" />
    <meta http-equiv="Content-type" content="text/html; charset=utf-8" />
    <meta name="viewport" content="width=device-width, initial-scale=1" />
    <style type="text/css">
    body {
        background-color: #f0f0f2;
        margin: 0;
        padding: 0;
        font-family: -apple-system, system-ui, BlinkMacSystemFont, "Segoe UI", "Open Sans", "Helvetica Neue", Helvetica, Arial, sans-serif;
    }
    div {
        width: 600px;
        margin: 5em auto;
        padding: 2em;
        background-color: #fdfdff;
        border-radius: 0.5em;
        box-shadow: 2px 3px 7px 2px rgba(0,0,0,0.02);
    }
    a:link, a:visited {
        color: #38488f;
        text-decoration: none;
    }
    @media (max-width: 700px) {
        div {
            margin: 0 auto;
            width: auto;
        }
    }
    </style>    
</head>
<body>
<div>
    <h1>Example Domain</h1>
    <p>This domain is for use in illustrative examples in documents. You may use this domain in literature without prior coordination or asking for permission.</p>
    <p><a href="https://www.iana.org/domains/example">More information...</a></p>
</div>
</body>
</html>
* Closing connection