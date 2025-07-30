package org.bcmoj.netserver;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.bcmoj.judgeserver.JudgeServer;
import org.bcmoj.utils.FileHashUtil;
import org.bcmoj.utils.JsonValidateUtil;
import org.bcmoj.utils.JudgeResultUtil;
import org.slf4j.MDC;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

@Slf4j
public class RequestProcessor implements Runnable {
    private final Socket clientSocket;
    private final String kwFilePath;
    private final JsonValidateUtil validator = new JsonValidateUtil();
    private static final ObjectMapper mapper = new ObjectMapper();

    public RequestProcessor(Socket clientSocket, String kwFilePath) {
        this.clientSocket = clientSocket;
        this.kwFilePath = kwFilePath;
    }

    @Override
    public void run() {
        String clientAddress = String.format("%s:%d", ((InetSocketAddress) clientSocket.getRemoteSocketAddress()).getAddress().getHostAddress(), ((InetSocketAddress) clientSocket.getRemoteSocketAddress()).getPort());
        MDC.put("client", clientAddress);

        File outputFile = null;
        try (DataInputStream dis = new DataInputStream(clientSocket.getInputStream()); DataOutputStream dos = new DataOutputStream(clientSocket.getOutputStream())) {

            String originalFileName = receiveFileName(dis);
            long fileSize = dis.readLong();
            String fileExtension = getFileExtension(originalFileName);
            outputFile = File.createTempFile(UUID.randomUUID().toString(), fileExtension);
            log.info("Receiving file '{}' ({} bytes), saving as: {}", originalFileName, fileSize, outputFile.getAbsolutePath());

            receiveFile(dis, outputFile, fileSize);
            String jsonConfig = receiveJsonConfig(dis);
            int checkpointCount = 1;
            try {
                JsonNode rootNode = mapper.readTree(jsonConfig);
                JsonNode checkpointsNode = rootNode.get("checkpoints");
                checkpointCount = validator.countInFiles(checkpointsNode);
                if (checkpointCount <= 0) checkpointCount = 1;
            } catch (Exception e) {
                log.warn("Failed to parse checkpoints count from JSON config, using default 1");
            }
            try {
                int hashLength = dis.readInt();
                if (hashLength > 0) {
                    byte[] hashBytes = new byte[hashLength];
                    dis.readFully(hashBytes);
                    String declaredHash = new String(hashBytes, StandardCharsets.UTF_8);
                    String actualHash = FileHashUtil.calculateSHA256(outputFile);
                    log.info("Declared hash: {}", declaredHash);
                    log.info("Actual   hash: {}", actualHash);
                    if (!actualHash.equalsIgnoreCase(declaredHash)) {
                        log.warn("File hash mismatch! Sending default error result...");
                        String errorJson = JudgeResultUtil.buildResult(List.of(), false, true, checkpointCount);
                        sendResponse(dos, errorJson);
                        return;
                    }
                } else {
                    log.warn("Client did not send hash, skipping hash verification.");
                }
            } catch (IOException e) {
                log.warn("Failed to read hash info from client, skipping verification: {}", e.getMessage());
            }

            log.info("Validating JSON config...");
            if (!validator.validate(jsonConfig)) {
                log.warn("JSON validation failed");
                String errorJson = validator.getLastErrorJson();
                if (errorJson != null) {
                    sendResponse(dos, errorJson);
                }
                return;
            }
            log.info("Processing with JudgeServer...");
            String serverResponse = JudgeServer.JServer(jsonConfig, outputFile, new File(kwFilePath));
            log.info("JudgeServer response: {}", serverResponse);
            sendResponse(dos, serverResponse);

        } catch (SocketException e) {
            log.warn("Client {} disconnected abruptly: {}", clientAddress, e.getMessage());
        } catch (Exception e) {
            log.error("Error when handling client {}", clientAddress, e);
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

    private String receiveFileName(DataInputStream dis) throws IOException {
        int length = dis.readInt();
        byte[] bytes = new byte[length];
        dis.readFully(bytes);
        String filename = new String(bytes, StandardCharsets.UTF_8);
        log.info("Received filename: {}", filename);
        return filename;
    }

    private void receiveFile(DataInputStream dis, File outputFile, long fileSize) throws IOException {
        try (FileOutputStream fos = new FileOutputStream(outputFile)) {
            long remaining = fileSize;
            byte[] buffer = new byte[4096];
            while (remaining > 0) {
                int read = dis.read(buffer, 0, (int) Math.min(4096, remaining));
                if (read < 0) throw new EOFException("Unexpected end of stream while receiving file");
                fos.write(buffer, 0, read);
                remaining -= read;
            }
            log.info("File received successfully, saved {} bytes", fileSize);
        }
    }

    private String receiveJsonConfig(DataInputStream dis) throws IOException {
        int length = dis.readInt();
        byte[] bytes = new byte[length];
        dis.readFully(bytes);
        String jsonConfig = new String(bytes, StandardCharsets.UTF_8);
        log.info("Received JSON config ({} bytes):\n{}", length, jsonConfig);
        return jsonConfig;
    }

    private void sendResponse(DataOutputStream dos, String response) throws IOException {
        byte[] responseBytes = response.getBytes(StandardCharsets.UTF_8);
        dos.writeInt(responseBytes.length);
        dos.write(responseBytes);
        dos.flush();
        log.info("Response sent to client ({} bytes):\n{} ", responseBytes.length, response);
    }

    private static String getFileExtension(String filename) {
        int dotIndex = filename.lastIndexOf(".");
        return (dotIndex == -1) ? "" : filename.substring(dotIndex);
    }
}
