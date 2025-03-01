package com.cruise.proxy.service;

import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.*;
import java.net.Socket;

@Service
public class ProxyConnectionManager {
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private static final String OFFSHORE_PROXY_HOST = "localhost"; // Replace with actual host
    private static final int OFFSHORE_PROXY_PORT = 8081;

    @PostConstruct
    public void init() throws IOException {
        socket = new Socket(OFFSHORE_PROXY_HOST, OFFSHORE_PROXY_PORT);
        out = new PrintWriter(socket.getOutputStream(), true);
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
    }

    public synchronized String sendRequest(String request) {
        try {
            out.println(request);
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = in.readLine()) != null && !line.isEmpty()) {
                response.append(line).append("\n");
            }
            return response.toString();
        } catch (IOException e) {
            throw new RuntimeException("Error communicating with offshore proxy", e);
        }
    }

    @PreDestroy
    public void cleanup() throws IOException {
        if (in != null) in.close();
        if (out != null) out.close();
        if (socket != null) socket.close();
    }
}