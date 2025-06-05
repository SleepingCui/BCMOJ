package org.bcmoj.netserver;

import org.bcmoj.config.ConfigProcess;
import org.bcmoj.judgeserver.JudgeServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
 /**
 * JCF (Judge Communication Framework) Socket Server
 * <p>
 * A high-performance network server that handles client connections for code submission and judging.
 * Manages file transfers, JSON configuration validation, and communication with the JudgeServer.
 * </p>
 *
 * <p><b>Communication Protocol:</b></p>
 * <ol>
 *   <li><b>Client Request Format:</b>
 *     <pre>[4-byte filename length][filename][8-byte file size][file content][4-byte JSON length][JSON]</pre>
 *     <p><b>Example JSON Request:</b></p>
 *     <pre>{
 *   "timeLimit": 1000,
 *   "checkpoints": {
 *     "1_in": "1 1",
 *     "1_out": "2",
 *     "2_in": "1 2",
 *     "2_out": "3",
 *     "3_in": "2 2",
 *     "3_out": "4",
 *     "4_in": "35 3",
 *     "4_out": "38",
 *     "5_in": "482 3",
 *     "5_out": "485"
 *   },
 *   "securityCheck": true
 * }</pre>
 *   </li>
 *
 *   <li><b>Server Response Format:</b>
 *     <pre>[4-byte response length][response content]</pre>
 *     <p><b>Example JSON Response:</b></p>
 *     <pre>{
 *   "1_res": 1,
 *   "1_time": 149.4676,
 *   "2_res": 1,
 *   "2_time": 165.1431,
 *   "3_res": 1,
 *   "3_time": 144.3808,
 *   "4_res": 1,
 *   "4_time": 155.1627,
 *   "5_res": 1,
 *   "5_time": 159.8404
 * }</pre>
 *     <p>Where result codes are:
 *     <ul>
 *       <li>1 = Accepted</li>
 *       <li>-3 = Wrong Answer</li>
 *       <li>2 = Time Limit Exceeded</li>
 *       <li>4 = Runtime Error</li>
 *       <li>-4 = Compile Error</li>
 *     </ul>
 *     </p>
 *   </li>
 * </ol>
 *
 * @author SleepingCui
 * @version 1.0-SNAPSHOT
 * @since 2025
 * @see <a href="https://github.com/SleepingCui/bcmoj-judge-server">GitHub Repository</a>
 * @see JudgeServer
 * @see JsonValidator
 */

public class JCFSocketServer {
    private static final Logger logger = LoggerFactory.getLogger(JCFSocketServer.class);

    private final ExecutorService executor = Executors.newCachedThreadPool();
    private ServerSocket serverSocket;
    private final JsonValidator validator = new JsonValidator();

    public void start(int port,String host) {
        try {
            serverSocket = new ServerSocket();
            serverSocket.bind(new InetSocketAddress(host, port));
            logger.info("File server started on {}:{}", host, port);

            while (!serverSocket.isClosed()) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    logger.info("New client connected: {}", clientSocket.getRemoteSocketAddress());
                    executor.submit(new ClientHandler(clientSocket, validator));
                } catch (SocketException e) {
                    if (!serverSocket.isClosed()) {
                        logger.error("Error accepting client connection", e);
                    }
                }
            }
        } catch (IOException e) {
            logger.error("Server startup error", e);
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
            logger.info("Server stopped gracefully");
        } catch (IOException e) {
            logger.error("Error stopping server", e);
        }
    }

    private static class ClientHandler implements Runnable {
        private final Socket clientSocket;
        private final JsonValidator validator;
        private final Logger clientLogger = LoggerFactory.getLogger(ClientHandler.class);

        public ClientHandler(Socket socket, JsonValidator validator) {
            this.clientSocket = socket;
            this.validator = validator;
        }

        @Override
        public void run() {
            String clientAddress = clientSocket.getRemoteSocketAddress().toString();
            File outputFile = null;

            try (DataInputStream dis = new DataInputStream(clientSocket.getInputStream());
                 DataOutputStream dos = new DataOutputStream(clientSocket.getOutputStream())) {

                int fileNameLength = dis.readInt();
                byte[] fileNameBytes = new byte[fileNameLength];
                dis.readFully(fileNameBytes);
                String originalFileName = new String(fileNameBytes, StandardCharsets.UTF_8);
                clientLogger.info("[{}] Received filename: {}", clientAddress, originalFileName);

                long fileSize = dis.readLong();
                String newFileName = generateRandomName() + getFileExtension(originalFileName);
                outputFile = new File(newFileName);
                clientLogger.info("[{}] Receiving file ({} bytes), saving as: {}", clientAddress, fileSize, newFileName);

                try (FileOutputStream fos = new FileOutputStream(outputFile)) {
                    long remaining = fileSize;
                    byte[] buffer = new byte[4096];
                    while (remaining > 0) {
                        int read = dis.read(buffer, 0, (int) Math.min(buffer.length, remaining));
                        if (read < 0) break;
                        fos.write(buffer, 0, read);
                        remaining -= read;
                    }
                    clientLogger.info("[{}] File received successfully. Saved {} bytes", clientAddress, (fileSize - remaining));
                }

                int jsonLength = dis.readInt();
                byte[] jsonBytes = new byte[jsonLength];
                dis.readFully(jsonBytes);
                String jsonConfig = new String(jsonBytes, StandardCharsets.UTF_8);
                clientLogger.info("[{}] Received JSON config ({} bytes):\n{}", clientAddress, jsonLength, jsonConfig);

                clientLogger.info("[{}] Validating JSON config...", clientAddress);
                if (!validator.validate(jsonConfig, dos, clientLogger)) {
                    clientLogger.warn("[{}] JSON validation failed", clientAddress);
                    return;
                }
                clientLogger.info("[{}] JSON validation passed", clientAddress);
                clientLogger.info("[{}] Processing with JudgeServer...", clientAddress);

                File KWFilePath = new File(ConfigProcess.GetConfig("KeywordsFilePath"));
                String jserverResponse = JudgeServer.JServer(jsonConfig, outputFile, KWFilePath);
                clientLogger.info("[{}] JudgeServer response: {}", clientAddress, jserverResponse);

                byte[] responseBytes = jserverResponse.getBytes(StandardCharsets.UTF_8);
                dos.writeInt(responseBytes.length);
                dos.write(responseBytes);
                dos.flush();
                clientLogger.info("[{}] Response sent to client ({} bytes)", clientAddress, responseBytes.length);
            } catch (SocketException e) {
                clientLogger.warn("[{}] Client disconnected abruptly: {}", clientAddress, e.getMessage());
            } catch (IOException e) {
                clientLogger.error("[{}] Client handling error", clientAddress, e);
            } catch (Exception e) {
                clientLogger.error("[{}] Unexpected error", clientAddress, e);
            } finally {
                if (outputFile != null && outputFile.exists()) {
                    try {
                        if (outputFile.delete()) {
                            clientLogger.info("[{}] Deleted temporary file: {}",
                                    clientAddress, outputFile.getName());
                        } else {
                            clientLogger.warn("[{}] Failed to delete temporary file: {}",
                                    clientAddress, outputFile.getName());
                        }
                    } catch (SecurityException e) {
                        clientLogger.error("[{}] Security exception when deleting file: {}", clientAddress, e.getMessage());
                    }
                }
                try {
                    clientSocket.close();
                    clientLogger.info("[{}] Connection closed", clientAddress);
                } catch (IOException e) {
                    clientLogger.error("[{}] Error closing socket", clientAddress, e);
                }
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