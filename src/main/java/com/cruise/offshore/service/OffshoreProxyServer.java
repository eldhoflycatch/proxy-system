package com.cruise.offshore.service;

import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

@Service
public class OffshoreProxyServer {

    private static final int PORT = 8081;
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private Socket clientSocket;

    @PostConstruct
    public void startServer() throws IOException {
        ServerSocket serverSocket = new ServerSocket(PORT);

        new Thread(() -> {
            try {
                clientSocket = serverSocket.accept();
                processRequests();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
        System.out.println("Offshore Proxy started on port " + PORT);
    }

    private void processRequests() throws IOException {
        try (PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()))) {

            String request;
            while ((request = in.readLine()) != null) {
                String response = handleRequest(request);
                out.println(response);
                out.println(""); // Empty line as delimiter
            }
        }
    }

    private String handleRequest(String request) {
        try {
            String[] parts = request.split(" ", 2);
            String method = parts[0];
            String url = parts[1];

            if ("CONNECT".equals(method)) {
                // Minimal HTTPS support: return success message
                return "HTTP/1.1 200 Connection established";
            }

            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                    .uri(new URI("http://" + url.split("\n")[0])); // Handle URL only

            if ("GET".equals(method)) {
                requestBuilder.GET();
            } else if ("POST".equals(method)) {
                String body = url.contains("\n") ? url.split("\n")[1] : "";
                requestBuilder.POST(HttpRequest.BodyPublishers.ofString(body));
            }

            HttpResponse<String> response = httpClient.send(
                    requestBuilder.build(),
                    HttpResponse.BodyHandlers.ofString());

            return response.body();
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }
}
