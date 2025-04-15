package com.cruise.offshore.service;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.*;
import java.net.*;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

@Component
public class OffshoreProxyServer {

    @Value("${server.port:8081}")
    private int port;

    @Value("${offshore.http.client.connect-timeout:5000}")
    private long connectTimeout;

    @Value("${offshore.http.client.read-timeout:10000}")
    private long readTimeout;

    private HttpClient httpClient;

    @PostConstruct
    public void init() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(connectTimeout))
                .build();
        startServer();
    }

    private void startServer() {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Offshore Proxy started on port " + port);
            while (true) {
                Socket clientSocket = serverSocket.accept();
                new Thread(() -> handleClient(clientSocket)).start();
            }
        } catch (IOException e) {
            System.err.println("Error starting server: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void handleClient(Socket clientSocket) {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
             PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true)) {
            String request;
            while ((request = in.readLine()) != null && !request.isEmpty()) {
                String response = handleRequest(request);
                out.println(response);
            }
        } catch (Exception e) {
            System.err.println("Error handling client: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public String handleRequest(String request) {
        try {
            System.out.println("Received request: " + request);
            String[] parts = request.split(" ", 2);
            String method = parts[0];
            String url = parts[1].split("\n")[0];

            // Remove redundant 'http://' if present
            if (url.startsWith("http://http://")) {
                url = url.replaceFirst("http://", "");
            } else if (!url.startsWith("http://")) {
                url = "http://" + url; // Add http:// if no protocol
            }

            System.out.println("Forwarding to: " + url);
            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                    .uri(new URI(url))
                    .timeout(Duration.ofMillis(readTimeout));

            if ("GET".equals(method)) {
                requestBuilder.GET();
            } else if ("POST".equals(method)) {
                String body = parts[1].contains("\n") ? parts[1].split("\n")[1] : "";
                requestBuilder.POST(HttpRequest.BodyPublishers.ofString(body));
            }

            HttpResponse<String> response = httpClient.send(
                    requestBuilder.build(),
                    HttpResponse.BodyHandlers.ofString());
            System.out.println("Response received: " + response.body());
            StringBuilder fullResponse = new StringBuilder();
            fullResponse.append("HTTP/1.1 ").append(response.statusCode()).append(" OK\n");
            fullResponse.append("Content-Type: text/html\n");
            fullResponse.append("Content-Length: ").append(response.body().length()).append("\n");
            fullResponse.append("\n");
            fullResponse.append(response.body());
            return fullResponse.toString();
        } catch (Exception e) {
            System.err.println("Error handling request: " + e.getMessage());
            e.printStackTrace();
            return "HTTP/1.1 500 Internal Server Error\n\nError: " + (e.getMessage() != null ? e.getMessage() : "Unknown error");
        }
    }
}