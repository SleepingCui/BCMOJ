package org.bcmoj.netserver;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
public class SocketServer {
    private final ExecutorService executor = Executors.newCachedThreadPool();
    private ServerSocket serverSocket;
    private volatile boolean stopped = false;

    public void start(int port, String host, String kwFilePath) {
        try {
            serverSocket = new ServerSocket();
            serverSocket.bind(new InetSocketAddress(host, port));
            log.info("File server started on {}:{} with keywords file: {}", host, port, kwFilePath);

            while (!stopped && !serverSocket.isClosed()) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    log.info("New client connected: {}", clientSocket.getRemoteSocketAddress());
                    executor.submit(new RequestProcessor(clientSocket, kwFilePath));
                } catch (SocketException e) {
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
            log.info("Server stopped gracefully");
        } catch (IOException e) {
            log.error("Error stopping server", e);
        }
    }
}
