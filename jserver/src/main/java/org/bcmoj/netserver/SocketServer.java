package org.bcmoj.netserver;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import lombok.extern.slf4j.Slf4j;

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
 * <p><b>Shutdown process:</b></p>
 * <ul>
 *   <li>Gracefully shut down bossGroup and workerGroup to release thread resources.</li>
 * </ul>
 *
 * @author SleepingCui
 * @version 1.0
 */
@Slf4j
public class SocketServer {
    private final String host;
    private final int port;
    private final String cppStandard;
    private final String kwFilePath;
    private final String compilerPath;
    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;

    /**
     * Constructs the server with specified host, port, and keyword file path.
     *
     * @param host                  the host address to bind, e.g. "0.0.0.0"
     * @param port                  the port number to listen on, e.g. 12345
     * @param kwFilePath            the path to the keyword file used by the judge service
     * @param compilerPath          path to the compiler used by the judge service
     * @param cppStandard           C++ standard version, e.g. "c++11"
     * 
     */
    public SocketServer(String host, int port, String kwFilePath, String compilerPath, String cppStandard) {
        this.host = host;
        this.port = port;
        this.kwFilePath = kwFilePath;
        this.compilerPath = compilerPath;
        this.cppStandard = cppStandard;
    }

    /**
     * Starts the Netty server, binding to the specified host and port,
     * and listens for client connection requests.
     * <p>
     * This method blocks the calling thread until the server is shut down.
     * </p>
     *
     * @throws InterruptedException if the thread is interrupted during startup or operation
     */

    public void start() throws InterruptedException {
        bossGroup = new NioEventLoopGroup(1);
        workerGroup = new NioEventLoopGroup();
        try {
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(bossGroup, workerGroup).channel(NioServerSocketChannel.class).childHandler(new ChannelInitializer<>() {
                        @Override
                        protected void initChannel(Channel ch) {
                            ch.pipeline().addLast(new RequestProcessor(kwFilePath,compilerPath,cppStandard));
                        }
                    })
                    .option(ChannelOption.SO_BACKLOG, 128).childOption(ChannelOption.SO_KEEPALIVE, true);
            ChannelFuture future = bootstrap.bind(host, port).sync();
            log.info("Server started on {}:{}", host, port);
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