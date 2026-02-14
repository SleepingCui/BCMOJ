package org.bcmoj.netserver;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioIoHandler;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import lombok.extern.slf4j.Slf4j;
import org.bcmoj.config.ServerConfig;

/**
 * Asynchronous non-blocking network server based on Netty,
 * designed to accept client connections and handle judge requests.
 * <p>
 * This server uses Netty's NIO model to achieve high-performance network communication,
 * supporting concurrent client connections.
 * Business logic is handled by {@link RequestProcessor}, which receives files and JSON configurations from clients,
 * validates them, and calls the judge service to return results.
 * </p>
 *
 * <p><b>Startup process:</b></p>
 * <ul>
 *   <li>Create two thread groups: bossGroup for accepting connections, workerGroup for processing IO events.</li>
 *   <li>Use {@link ServerBootstrap} to bind to the specified host and port.</li>
 *   <li>For each client connection, create a {@link io.netty.channel.Channel} and initialize its pipeline handlers.</li>
 *   <li>Block and wait for server shutdown while handling all client connections and requests.</li>
 * </ul>
 *
 * @author SleepingCui
 */
@Slf4j
public class SocketServer {
    private final ServerConfig config;
    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;

    /**
     * Constructs the server with the provided configuration object.
     *
     * @param config The server configuration containing all necessary settings.
     */
    public SocketServer(ServerConfig config) {
        this.config = config;
    }

    /**
     * Starts the Netty server, binding to the specified host and port,
     * and listens for client connection requests.
     * <p>
     * This method blocks the calling thread until the server is shut down.
     * </p>
     *
     * @param nettyThreads Number of threads for the Netty event loop group.
     * @throws InterruptedException if the thread is interrupted during startup or operation
     */

    public void start(int nettyThreads) throws InterruptedException {
        bossGroup = new MultiThreadIoEventLoopGroup(nettyThreads, NioIoHandler.newFactory());
        workerGroup = new MultiThreadIoEventLoopGroup(NioIoHandler.newFactory());

        try {
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class).childHandler(new ChannelInitializer<>() {
                        @Override
                        protected void initChannel(Channel ch) {
                            ch.pipeline().addLast(new RequestProcessor(SocketServer.this.config));
                        }
                    })
                    .option(ChannelOption.SO_BACKLOG, 128).childOption(ChannelOption.SO_KEEPALIVE, true);
            ChannelFuture future = bootstrap.bind(config.getHost(), config.getPort()).sync();
            log.info("Server successfully started and listening on {}:{}", config.getHost(), config.getPort());
            future.channel().closeFuture().sync();
        } finally {
            log.debug("Server shutting down...");
            stop();
        }
    }

    /**
     * Gracefully shuts down the server and releases thread pool resources.
     * <p>
     * This method shuts down both bossGroup and workerGroup to clean up server resources.
     * </p>
     */
    public void stop() {
        if (bossGroup != null) {
            bossGroup.shutdownGracefully();
        }
        if (workerGroup != null) {
            workerGroup.shutdownGracefully();
        }
        log.info("Server stopped");
    }
}