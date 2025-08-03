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
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.UUID;

@Slf4j
public class RequestProcessor implements Runnable {
    private static final int BUFFER_SIZE = 4096;
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
        InetSocketAddress remote = (InetSocketAddress) clientSocket.getRemoteSocketAddress();
        String clientAddress =  remote.toString().replace("/", "");
        MDC.put("client", clientAddress);

        File outputFile = null;
        try (DataInputStream dis = new DataInputStream(clientSocket.getInputStream()); DataOutputStream dos = new DataOutputStream(clientSocket.getOutputStream())) {
            outputFile = handleFileReception(dis);
            String jsonConfig = receiveJsonConfig(dis);
            int checkpointCount = parseCheckpointCount(jsonConfig);
            if (!verifyHash(dis, outputFile, checkpointCount, dos)) return;
            if (!validateJson(jsonConfig, dos)) return;
            runJudgeServer(jsonConfig, outputFile, dos);
        } catch (SocketException e) {
            log.warn("Client {} disconnected abruptly: {}", clientAddress, e.getMessage());
        } catch (Exception e) {
            log.error("Error when handling client {}", clientAddress, e);
        } finally {
            cleanup(outputFile);
            MDC.remove("client");
        }
    }

    private File handleFileReception(DataInputStream dis) throws IOException {
        String filename = new String(dis.readNBytes(dis.readInt()), StandardCharsets.UTF_8);
        log.info("Received filename: {}", filename);
        long fileSize = dis.readLong();
        File outputFile = File.createTempFile(UUID.randomUUID().toString(), getFileExtension(filename));
        log.info("Receiving file '{}' ({} bytes), saving as: {}", filename, fileSize, outputFile.getAbsolutePath());
        receiveFile(dis, outputFile, fileSize);
        return outputFile;
    }

    private int parseCheckpointCount(String jsonConfig) {
        try {
            JsonNode checkpointsNode = mapper.readTree(jsonConfig).get("checkpoints");
            int count = validator.countInFiles(checkpointsNode);
            return count <= 0 ? 1 : count;
        } catch (Exception e) {
            log.warn("Failed to parse checkpoints count from JSON config, using default 1");
            return 1;
        }
    }

    private boolean verifyHash(DataInputStream dis, File file, int checkpointCount, DataOutputStream dos) {
        try {
            int hashLength = dis.readInt();
            if (hashLength <= 0) {
                log.warn("Client did not send hash, skipping verification.");
                return true;
            }
            byte[] hashBytes = new byte[hashLength];
            dis.readFully(hashBytes);
            String declaredHash = new String(hashBytes, StandardCharsets.UTF_8);
            String actualHash = FileHashUtil.calculateSHA256(file);
            log.info("Declared hash: {}", declaredHash);
            log.info("Actual   hash: {}", actualHash);
            if (!actualHash.equalsIgnoreCase(declaredHash)) {
                log.warn("File hash mismatch! Sending error result...");
                sendResponse(dos, JudgeResultUtil.buildResult(List.of(), false, true, checkpointCount));
                return false;
            }
        } catch (IOException | NoSuchAlgorithmException e) {
            log.warn("Failed to read hash info from client: {}", e.getMessage());
        }
        return true;
    }

    private boolean validateJson(String jsonConfig, DataOutputStream dos) throws IOException {
        log.info("Validating JSON config...");
        if (validator.validate(jsonConfig)) return true;
        log.warn("JSON validation failed");
        String errorJson = validator.getLastErrorJson();
        if (errorJson != null) sendResponse(dos, errorJson); return false;
    }

    private void runJudgeServer(String jsonConfig, File file, DataOutputStream dos) throws IOException {
        log.info("Processing with JudgeServer...");
        String response = JudgeServer.serve(jsonConfig, file, new File(kwFilePath));
        log.info("JudgeServer response: {}", response);
        sendResponse(dos, response);
    }

    private void cleanup(File outputFile) {
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
    }

    private void receiveFile(DataInputStream dis, File outputFile, long fileSize) throws IOException {
        try (FileOutputStream fos = new FileOutputStream(outputFile)) {
            long remaining = fileSize;
            byte[] buffer = new byte[BUFFER_SIZE];
            while (remaining > 0) {
                int read = dis.read(buffer, 0, (int) Math.min(buffer.length, remaining));
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
        String json = new String(bytes, StandardCharsets.UTF_8);
        log.info("Received JSON config ({} bytes):\n{}", length, json);
        return json;
    }

    private void sendResponse(DataOutputStream dos, String response) throws IOException {
        byte[] responseBytes = response.getBytes(StandardCharsets.UTF_8);
        dos.writeInt(responseBytes.length);
        dos.write(responseBytes);
        dos.flush();
        log.info("Response sent to client ({} bytes):\n{}", responseBytes.length, response);
    }

    private static String getFileExtension(String filename) {
        int dotIndex = filename.lastIndexOf(".");
        return (dotIndex == -1) ? "" : filename.substring(dotIndex);
    }
}

