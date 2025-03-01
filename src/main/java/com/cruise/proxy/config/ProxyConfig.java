package com.cruise.proxy.config;

import com.cruise.proxy.service.ProxyConnectionManager;
import com.cruise.proxy.service.ProxyRequestHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ProxyConfig {

    @Bean
    public ProxyConnectionManager proxyConnectionManager() {
        return new ProxyConnectionManager();
    }

    @Bean
    public ProxyRequestHandler proxyRequestHandler(ProxyConnectionManager connectionManager) {
        return new ProxyRequestHandler(connectionManager);
    }
}