package org.bcmoj.netserver;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
public class SocketServer {
    private final ExecutorService executor = Executors.newCachedThreadPool();
    private ServerSocket serverSocket;
    private volatile boolean stopped = false;

    private final AtomicLong connectionCount = new AtomicLong(0);
    private long startTimeMillis;

    public void start(int port, String host, String kwFilePath) {
        try {
            log.info("Starting server on {}:{} ...", host, port);
            serverSocket = new ServerSocket();
            serverSocket.bind(new InetSocketAddress(host, port));
            startTimeMillis = System.currentTimeMillis();
            log.info("Socket server started on {}:{} with keywords file: {}", host, port, kwFilePath);

            while (!stopped && !serverSocket.isClosed()) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    connectionCount.incrementAndGet();
                    log.info("New client connected: {}", clientSocket.getRemoteSocketAddress());
                    executor.submit(new RequestProcessor(clientSocket, kwFilePath));
                } catch (SocketException e) {
                    if (stopped) {
                        log.info("Server stopped, exiting accept loop");
                        break;
                    }
                    if (!serverSocket.isClosed()) {
                        log.error("Error accepting client connection", e);
                    }
                }
            }
        } catch (IOException e) {
            log.error("Server startup error", e);
        } finally {
            stop();
        }
    }

    public void stop() {
        if (stopped) return;
        stopped = true;
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
            executor.shutdown();

            long endTimeMillis = System.currentTimeMillis();
            long uptimeMillis = endTimeMillis - startTimeMillis;
            long totalConnections = connectionCount.get();
            double connectionsPerSecond = uptimeMillis > 0
                    ? totalConnections / (uptimeMillis / 1000.0)
                    : 0.0;

            log.info("Server stopped gracefully");
            log.info("===== Performance Statistics =====");
            log.info("Uptime: {} ms ({} seconds)", uptimeMillis, uptimeMillis / 1000);
            log.info("Total connections handled: {}", totalConnections);
            log.info("Average connections per second: {}", connectionsPerSecond);
            log.info("==================================");

        } catch (IOException e) {
            log.error("Error stopping server", e);
        }
    }
}

