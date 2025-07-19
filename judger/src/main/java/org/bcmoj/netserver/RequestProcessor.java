package org.bcmoj.netserver;

import lombok.extern.slf4j.Slf4j;
import org.bcmoj.judgeserver.JudgeServer;
import org.slf4j.MDC;

import java.io.*;
import java.net.Socket;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.util.Random;

@Slf4j
public class RequestProcessor implements Runnable {
    private static final int BUFFER_SIZE = 4096;

    private final Socket clientSocket;
    private final String kwFilePath;
    private final JsonValidator validator = new JsonValidator();

    public RequestProcessor(Socket clientSocket, String kwFilePath) {
        this.clientSocket = clientSocket;
        this.kwFilePath = kwFilePath;
    }

    @Override
    public void run() {
        String clientAddress = clientSocket.getRemoteSocketAddress().toString();
        MDC.put("client", clientAddress);
        File outputFile = null;

        try (DataInputStream dis = new DataInputStream(clientSocket.getInputStream());
             DataOutputStream dos = new DataOutputStream(clientSocket.getOutputStream())) {

            String originalFileName = receiveFileName(dis);
            long fileSize = dis.readLong();
            String newFileName = generateRandomName() + getFileExtension(originalFileName);
            outputFile = new File(newFileName);

            log.info("Receiving file '{}' ({} bytes), saving as: {}", originalFileName, fileSize, newFileName);
            receiveFile(dis, outputFile, fileSize);

            String jsonConfig = receiveJsonConfig(dis);
            log.info("Validating JSON config...");
            if (!validator.validate(jsonConfig, dos)) {
                log.warn("JSON validation failed");
                return;
            }

            log.info("Processing with JudgeServer...");
            String jserverResponse = JudgeServer.JServer(jsonConfig, outputFile, new File(kwFilePath));
            log.info("JudgeServer response: {}", jserverResponse);
            sendResponse(dos, jserverResponse);

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
            byte[] buffer = new byte[BUFFER_SIZE];
            while (remaining > 0) {
                int read = dis.read(buffer, 0, (int) Math.min(BUFFER_SIZE, remaining));
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
        log.info("Response sent to client ({} bytes)", responseBytes.length);
    }

    private static String generateRandomName() {
        Random random = new Random();
        StringBuilder sb = new StringBuilder(10);
        for (int i = 0; i < 10; i++) {
            sb.append(random.nextInt(10));
        }
        return sb.toString();
    }

    private static String getFileExtension(String filename) {
        int dotIndex = filename.lastIndexOf(".");
        return (dotIndex == -1) ? "" : filename.substring(dotIndex);
    }
}
