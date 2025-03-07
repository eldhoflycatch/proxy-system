package com.cruise.proxy.service;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.*;
import java.net.Socket;

@Component
public class ProxyConnectionManager {

    @Value("${offshore.proxy.host:localhost}")
    private String offshoreProxyHost;

    @Value("${offshore.proxy.port:8081}")
    private int offshoreProxyPort;

    protected Socket socket;
    protected PrintWriter out;
    protected BufferedReader in;

    @PostConstruct
    public void init() throws IOException {
        if (offshoreProxyPort <= 0 || offshoreProxyPort > 65535) {
            throw new IllegalArgumentException("Invalid offshore proxy port: " + offshoreProxyPort);
        }
        socket = new Socket(offshoreProxyHost, offshoreProxyPort);
        System.out.println("Connected to Offshore Proxy at " + offshoreProxyHost + ":" + offshoreProxyPort);
        out = new PrintWriter(socket.getOutputStream(), true);
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
    }

    public String sendRequest(String request) throws IOException {
        System.out.println("Sending to Offshore Proxy: " + request);
        out.println(request);
        StringBuilder response = new StringBuilder();
        String line;
        while ((line = in.readLine()) != null) {
            response.append(line).append("\n");
        }
        System.out.println("Received from Offshore Proxy: " + response.toString());
        return response.toString();
    }
}