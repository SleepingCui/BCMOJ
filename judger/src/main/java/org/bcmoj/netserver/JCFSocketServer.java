package org.bcmoj.netserver;

import org.bcmoj.judgeserver.JudgeServer;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
public class JCFSocketServer {
    private final ExecutorService executor = Executors.newCachedThreadPool();
    private ServerSocket serverSocket;
    private final JsonValidator validator = new JsonValidator();

    public void start(int port, String host, String kwFilePath) {
        try {
            serverSocket = new ServerSocket();
            serverSocket.bind(new InetSocketAddress(host, port));
            log.info("File server started on {}:{} with keywords file: {}", host, port, kwFilePath);

            while (!serverSocket.isClosed()) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    log.info("New client connected: {}", clientSocket.getRemoteSocketAddress());
                    executor.submit(new ClientHandler(clientSocket, validator, kwFilePath));
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

    private static class ClientHandler implements Runnable {
        private final Socket clientSocket;
        private final JsonValidator validator;
        private final String kwFilePath;

        public ClientHandler(Socket socket, JsonValidator validator, String kwFilePath) {
            this.clientSocket = socket;
            this.validator = validator;
            this.kwFilePath = kwFilePath;
        }

        @Override
        public void run() {
            String clientAddress = clientSocket.getRemoteSocketAddress().toString();
            File outputFile = null;
            MDC.put("client", clientAddress);
            try (DataInputStream dis = new DataInputStream(clientSocket.getInputStream());
                 DataOutputStream dos = new DataOutputStream(clientSocket.getOutputStream())) {

                int fileNameLength = dis.readInt();
                byte[] fileNameBytes = new byte[fileNameLength];
                dis.readFully(fileNameBytes);
                String originalFileName = new String(fileNameBytes, StandardCharsets.UTF_8);
                log.info("Received filename: {}", originalFileName);

                long fileSize = dis.readLong();
                String newFileName = generateRandomName() + getFileExtension(originalFileName);
                outputFile = new File(newFileName);
                log.info("Receiving file ({} bytes), saving as: {}", fileSize, newFileName);

                try (FileOutputStream fos = new FileOutputStream(outputFile)) {
                    long remaining = fileSize;
                    byte[] buffer = new byte[4096];
                    while (remaining > 0) {
                        int read = dis.read(buffer, 0, (int) Math.min(buffer.length, remaining));
                        if (read < 0) break;
                        fos.write(buffer, 0, read);
                        remaining -= read;
                    }
                    log.info("File received successfully. Saved {} bytes", (fileSize - remaining));
                }

                int jsonLength = dis.readInt();
                byte[] jsonBytes = new byte[jsonLength];
                dis.readFully(jsonBytes);
                String jsonConfig = new String(jsonBytes, StandardCharsets.UTF_8);
                log.info("Received JSON config ({} bytes):\n{}", jsonLength, jsonConfig);

                log.info("Validating JSON config...");
                if (!validator.validate(jsonConfig, dos, log)) {
                    log.warn("JSON validation failed");
                    return;
                }
                log.info("JSON validation passed");
                log.info("Processing with JudgeServer...");

                File KWFilePath = new File(kwFilePath);
                String jserverResponse = JudgeServer.JServer(jsonConfig, outputFile, KWFilePath);
                log.info("JudgeServer response: {}", jserverResponse);

                byte[] responseBytes = jserverResponse.getBytes(StandardCharsets.UTF_8);
                dos.writeInt(responseBytes.length);
                dos.write(responseBytes);
                dos.flush();
                log.info("Response sent to client ({} bytes)", responseBytes.length);
            } catch (Exception e) {
                if (e instanceof SocketException) {
                    log.warn("Client {} disconnected abruptly: {}", clientAddress, e.getMessage());
                } else {
                    log.error("Error when handling client {}", clientAddress, e);
                }
            } finally {
                if (outputFile != null && outputFile.exists()) {
                    if (outputFile.delete()) {
                        log.info("Deleted temporary file: {}", outputFile.getName());
                    } else {
                        log.warn("Failed to delete temporary file: {}", outputFile.getName());
                    }
                }
                try {
                    clientSocket.close();
                    log.info("Connection closed");
                } catch (IOException e) {
                    log.error("Error closing socket", e);
                }
                MDC.remove("client");
            }
        }

        private String generateRandomName() {
            Random random = new Random();
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 10; i++) {
                sb.append(random.nextInt(10));
            }
            return sb.toString();
        }

        private String getFileExtension(String filename) {
            int dotIndex = filename.lastIndexOf(".");
            return (dotIndex == -1) ? "" : filename.substring(dotIndex);
        }
    }
}
