package com.cruise.proxy.service;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.*;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

@Component
public class ProxyRequestHandler {

    private final ProxyConnectionManager connectionManager;
    private final BlockingQueue<RequestTask> requestQueue = new LinkedBlockingQueue<>();
    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;
    private volatile boolean running = true;

    public ProxyRequestHandler(ProxyConnectionManager connectionManager) {
        this.connectionManager = connectionManager;
    }

    @PostConstruct
    public void start() throws InterruptedException {
        bossGroup = new NioEventLoopGroup(1);
        workerGroup = new NioEventLoopGroup();

        new Thread(this::processQueue).start();
        System.out.println("Queue processing thread started");

        ServerBootstrap bootstrap = new ServerBootstrap();
        bootstrap.group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .childHandler(new ChannelInitializer<Channel>() {
                    @Override
                    protected void initChannel(Channel ch) {
                        ChannelPipeline pipeline = ch.pipeline();
                        pipeline.addLast(new HttpServerCodec());
                        pipeline.addLast(new HttpObjectAggregator(1048576));
                        pipeline.addLast(new ProxyHandler());
                    }
                });

        bootstrap.bind(8080).sync();
        System.out.println("Ship Proxy started on port 8080");
    }

    // Synchronized method to ensure sequential processing
    private synchronized void processQueue() {
        while (running) {
            try {
                RequestTask task = requestQueue.take();
                System.out.println("Processing request: " + task.request);
                String response = connectionManager.sendRequest(task.request);

                String[] responseParts = response.split("\n\n", 2);
                String headers = responseParts[0];
                String body = responseParts.length > 1 ? responseParts[1] : "";
                HttpResponseStatus status = HttpResponseStatus.OK;

                String[] headerLines = headers.split("\n");
                for (String line : headerLines) {
                    if (line.startsWith("HTTP/1.1")) {
                        String[] statusParts = line.split(" ", 3);
                        status = HttpResponseStatus.valueOf(Integer.parseInt(statusParts[1]));
                        break;
                    }
                }

                DefaultFullHttpResponse httpResponse = new DefaultFullHttpResponse(
                        HttpVersion.HTTP_1_1, status,
                        Unpooled.wrappedBuffer(body.getBytes(StandardCharsets.UTF_8))
                );
                httpResponse.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/html");
                httpResponse.headers().set(HttpHeaderNames.CONTENT_LENGTH, body.getBytes().length);
                httpResponse.headers().set(HttpHeaderNames.CONNECTION, "close");

                RequestTask finalTask = task;
                task.channel.writeAndFlush(httpResponse).addListener(future -> {
                    if (!finalTask.channel.isOpen()) {
                        finalTask.channel.close();
                    }
                });
                System.out.println("Response sent to client: " + body);
            } catch (InterruptedException e) {
                System.err.println("Queue processing interrupted: " + e.getMessage());
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                System.err.println("Error processing request: " + e.getMessage());
                e.printStackTrace();
                RequestTask errorTask = requestQueue.peek();
                if (errorTask != null) {
                    DefaultFullHttpResponse errorResponse = new DefaultFullHttpResponse(
                            HttpVersion.HTTP_1_1,
                            HttpResponseStatus.INTERNAL_SERVER_ERROR,
                            Unpooled.wrappedBuffer(("Error: " + e.getMessage()).getBytes(StandardCharsets.UTF_8))
                    );
                    errorResponse.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/plain");
                    errorResponse.headers().set(HttpHeaderNames.CONTENT_LENGTH, errorResponse.content().readableBytes());
                    errorTask.channel.writeAndFlush(errorResponse).addListener(ChannelFutureListener.CLOSE);
                }
            }
        }
    }

    @PreDestroy
    public void shutdown() {
        running = false;
        if (bossGroup != null) bossGroup.shutdownGracefully();
        if (workerGroup != null) workerGroup.shutdownGracefully();
        System.out.println("Ship Proxy shutdown complete");
    }

    public class ProxyHandler extends SimpleChannelInboundHandler<FullHttpRequest> {
        @Override
        protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest msg) {
            String requestLine = msg.method() + " " + msg.uri();
            requestQueue.add(new RequestTask(requestLine, ctx.channel()));
            System.out.println("Queued request: " + requestLine);
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            cause.printStackTrace();
            ctx.close();
        }
    }

    static class RequestTask {
        String request;
        Channel channel;

        RequestTask(String request, Channel channel) {
            this.request = request;
            this.channel = channel;
        }
    }
}