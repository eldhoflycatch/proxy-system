package com.cruise.proxy.service;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

@Service
public class ProxyRequestHandler {

    private final ProxyConnectionManager connectionManager;
    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;
    private final BlockingQueue<RequestTask> requestQueue = new LinkedBlockingQueue<>();
    private static final int PORT = 8080;

    @Autowired
    public ProxyRequestHandler(ProxyConnectionManager connectionManager) {
        this.connectionManager = connectionManager;
    }

    @PostConstruct
    public void start() {
        bossGroup = new NioEventLoopGroup(1);
        workerGroup = new NioEventLoopGroup();

        new Thread(this::processQueue).start(); // Start queue processing thread

        try {
            ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new ChannelInitializer<Channel>() {
                        @Override
                        protected void initChannel(Channel ch) {
                            ChannelPipeline p = ch.pipeline();
                            p.addLast(new HttpServerCodec());
                            p.addLast(new HttpObjectAggregator(1048576));
                            p.addLast(new ProxyHandler());
                        }
                    });

            ChannelFuture f = b.bind(PORT).sync();
            System.out.println("Ship Proxy started on port " + PORT);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @PreDestroy
    public void stop() {
        if (bossGroup != null) bossGroup.shutdownGracefully();
        if (workerGroup != null) workerGroup.shutdownGracefully();
    }

    private void processQueue() {
        while (true) {
            try {
                RequestTask task = requestQueue.take(); // Blocks until a request is available
                String response = connectionManager.sendRequest(task.request);
                HttpResponseStatus status = "CONNECT".equals(task.request.split(" ")[0])
                        ? HttpResponseStatus.OK
                        : HttpResponseStatus.OK;
                task.channel.writeAndFlush(new DefaultFullHttpResponse(
                        HttpVersion.HTTP_1_1,
                        status,
                        task.channel.alloc().buffer().writeBytes(response.getBytes())
                ));
                if (!task.channel.isOpen()) {
                    task.channel.close();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private class ProxyHandler extends SimpleChannelInboundHandler<FullHttpRequest> {
        @Override
        protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest msg) {
            String request = msg.method() + " " + msg.uri();
            if (msg.method().equals(HttpMethod.CONNECT)) {
                // Handle HTTPS (CONNECT) requests
                request = "CONNECT " + msg.uri();
            } else if (msg.content().isReadable()) {
                request += "\n" + msg.content().toString(io.netty.util.CharsetUtil.UTF_8);
            }
            requestQueue.add(new RequestTask(request, ctx.channel()));
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            ctx.close();
        }
    }

    private static class RequestTask {
        String request;
        Channel channel;

        RequestTask(String request, Channel channel) {
            this.request = request;
            this.channel = channel;
        }
    }
}