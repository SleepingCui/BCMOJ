package org.bcmoj.netserver;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import lombok.extern.slf4j.Slf4j;
import org.bcmoj.judgeserver.JudgeServer;
import org.bcmoj.utils.FileHashUtil;
import org.bcmoj.utils.JsonValidateUtil;
import org.bcmoj.utils.JudgeResultUtil;
import org.slf4j.MDC;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.UUID;

/**
 * Netty channel handler that processes incoming client requests for
 * file-based judging tasks.
 *
 * <p>The communication protocol expects:
 * <ol>
 *   <li>Filename length (int, 4 bytes)</li>
 *   <li>Filename (UTF-8 bytes)</li>
 *   <li>File size (long, 8 bytes)</li>
 *   <li>File content (binary data)</li>
 *   <li>JSON config length (int, 4 bytes)</li>
 *   <li>JSON config (UTF-8 bytes)</li>
 *   <li>Hash length (int, 4 bytes)</li>
 *   <li>Hash string (UTF-8 bytes, optional)</li>
 * </ol>
 *
 * <p>Data is read step-by-step using a state machine to handle partial
 * incoming buffers. Once all data is received, the handler validates
 * the hash, validates the JSON configuration, invokes the judge server,
 * and returns the result back to the client.
 * </p>
 *
 * <p>Temporary files are created to store uploaded files and deleted
 * after processing completes or upon disconnection.</p>
 *
 * <p>Logging is done with client context information for traceability.</p>
 *
 * @author SleepingCui
 */
@Slf4j
public class RequestProcessor extends ChannelInboundHandlerAdapter {

    /**
     * States of the reading process, forming a state machine
     * for processing incoming data step-by-step.
     */
    private enum State {
        READ_FILENAME_LENGTH,
        READ_FILENAME,
        READ_FILE_SIZE,
        READ_FILE_CONTENT,
        READ_JSON_LENGTH,
        READ_JSON,
        READ_HASH_LENGTH,
        READ_HASH,
        PROCESSING
    }

    private static final ObjectMapper mapper = new ObjectMapper();
    private static final int MAX_FILENAME_LENGTH = 512;
    private static final int MAX_FILENAME_CHARS = 128;

    private final JsonValidateUtil validator = new JsonValidateUtil();
    private final String kwFilePath;
    private State state = State.READ_FILENAME_LENGTH;

    private int filenameLength;
    private long fileSize;
    private long bytesReadForFile;
    private File tempFile;
    private FileOutputStream fos;
    private int jsonLength;
    private String jsonConfig;
    private int hashLength;
    private String declaredHash;

    private final StringBuilder jsonBuilder = new StringBuilder();
    private final StringBuilder hashBuilder = new StringBuilder();

    /**
     * Constructs a RequestProcessor with the given keyword file path
     * for judge server processing.
     *
     * @param kwFilePath path to the keyword file used by JudgeServer
     */
    public RequestProcessor(String kwFilePath) {
        this.kwFilePath = kwFilePath;
    }

    /**
     * Called when the channel is active (client connected).
     * Sets MDC context for logging.
     *
     * @param ctx the channel handler context
     */
    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        String clientAddr = ctx.channel().remoteAddress().toString().replaceFirst("^/", "");
        MDC.put("client", clientAddr);
        log.info("Client connected: {}", clientAddr);
    }

    /**
     * Called when the channel becomes inactive (client disconnected).
     * Cleans up temporary files and removes logging context.
     *
     * @param ctx the channel handler context
     * @throws Exception if an error occurs
     */
    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        cleanup();
        String clientAddr = ctx.channel().remoteAddress().toString();
        log.info("Client disconnected: {}", clientAddr);
        MDC.remove("client");
        super.channelInactive(ctx);
    }

    /**
     * Handles exceptions thrown during processing.
     * Logs error, cleans up resources, and closes channel.
     *
     * @param ctx   the channel handler context
     * @param cause the exception caught
     */
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        log.error("Exception in client handler", cause);
        cleanup();
        ctx.close();
    }

    /**
     * Called when data is received from the client.
     * Implements a state machine to read protocol fields step-by-step,
     * handling partial data buffers correctly.
     *
     * @param ctx the channel handler context
     * @param msg the incoming message buffer
     * @throws Exception if an I/O or protocol error occurs
     */
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        ByteBuf in = (ByteBuf) msg;

        try {
            while (in.isReadable()) {
                switch (state) {
                    case READ_FILENAME_LENGTH:
                        if (in.readableBytes() < 4) return;
                        filenameLength = in.readInt();
                        if (filenameLength <= 0 || filenameLength > MAX_FILENAME_LENGTH) {
                            throw new IOException("Invalid filename length: " + filenameLength);
                        }
                        state = State.READ_FILENAME;
                        break;

                    case READ_FILENAME:
                        if (in.readableBytes() < filenameLength) return;
                        byte[] nameBytes = new byte[filenameLength];
                        in.readBytes(nameBytes);
                        String filename = new String(nameBytes, StandardCharsets.UTF_8);
                        if (filename.length() > MAX_FILENAME_CHARS) {
                            throw new IOException("Filename too long: " + filename.length());
                        }
                        log.info("Received filename: {}", filename);
                        tempFile = File.createTempFile(UUID.randomUUID().toString(), getFileExtension(filename));
                        fos = new FileOutputStream(tempFile);
                        state = State.READ_FILE_SIZE;
                        break;

                    case READ_FILE_SIZE:
                        if (in.readableBytes() < 8) return;
                        fileSize = in.readLong();
                        if (fileSize < 0) {
                            throw new IOException("Invalid file size: " + fileSize);
                        }
                        bytesReadForFile = 0;
                        state = State.READ_FILE_CONTENT;
                        log.info("Expecting file content size: {}", fileSize);
                        break;

                    case READ_FILE_CONTENT:
                        long toRead = Math.min(in.readableBytes(), fileSize - bytesReadForFile);
                        byte[] fileBytes = new byte[(int) toRead];
                        in.readBytes(fileBytes);
                        fos.write(fileBytes);
                        bytesReadForFile += toRead;

                        if (bytesReadForFile == fileSize) {
                            fos.close();
                            log.info("File received successfully, saved {} bytes", fileSize);
                            state = State.READ_JSON_LENGTH;
                        }
                        break;

                    case READ_JSON_LENGTH:
                        if (in.readableBytes() < 4) return;
                        jsonLength = in.readInt();
                        if (jsonLength <= 0) {
                            throw new IOException("Invalid JSON length: " + jsonLength);
                        }
                        jsonBuilder.setLength(0);
                        state = State.READ_JSON;
                        break;

                    case READ_JSON:
                        int jsonBytesToRead = Math.min(in.readableBytes(), jsonLength - jsonBuilder.length());
                        byte[] jsonBytes = new byte[jsonBytesToRead];
                        in.readBytes(jsonBytes);
                        jsonBuilder.append(new String(jsonBytes, StandardCharsets.UTF_8));
                        if (jsonBuilder.length() == jsonLength) {
                            jsonConfig = jsonBuilder.toString();
                            log.info("Received JSON config ({} bytes):\n{}", jsonLength, jsonConfig);
                            state = State.READ_HASH_LENGTH;
                        }
                        break;

                    case READ_HASH_LENGTH:
                        if (in.readableBytes() < 4) return;
                        hashLength = in.readInt();
                        if (hashLength < 0) {
                            throw new IOException("Invalid hash length: " + hashLength);
                        }
                        if (hashLength == 0) {  // Skip hash validation if none sent
                            declaredHash = null;
                            state = State.PROCESSING;
                            processJudge(ctx);
                        } else {
                            hashBuilder.setLength(0);
                            state = State.READ_HASH;
                        }
                        break;

                    case READ_HASH:
                        int hashBytesToRead = Math.min(in.readableBytes(), hashLength - hashBuilder.length());
                        byte[] hashBytes = new byte[hashBytesToRead];
                        in.readBytes(hashBytes);
                        hashBuilder.append(new String(hashBytes, StandardCharsets.UTF_8));
                        if (hashBuilder.length() == hashLength) {
                            declaredHash = hashBuilder.toString();
                            log.info("Declared hash: {}", declaredHash);
                            state = State.PROCESSING;
                            processJudge(ctx);
                        }
                        break;

                    case PROCESSING:
                        in.skipBytes(in.readableBytes()); // Ignore any extra data until processing is complete
                        return;
                }
            }
        } finally {
            in.release();
        }
    }

    /**
     * Performs hash verification, JSON validation,
     * calls the judge server and sends the response back to client.
     * Closes the channel after processing.
     *
     * @param ctx the channel handler context
     * @throws IOException if I/O errors occur
     */
    private void processJudge(ChannelHandlerContext ctx) throws IOException {
        try {
            if (declaredHash != null) {
                String actualHash = FileHashUtil.calculateSHA256(tempFile);
                log.info("Actual hash: {}", actualHash);
                if (!actualHash.equalsIgnoreCase(declaredHash)) {
                    log.warn("File hash mismatch! Sending error result...");
                    sendResponse(ctx, JudgeResultUtil.buildResult(List.of(), false, true, parseCheckpointCount(jsonConfig)));
                    cleanup();
                    ctx.close();
                    return;
                }
            }
        } catch (NoSuchAlgorithmException e) {
            log.warn("Hash calculation failed: {}", e.getMessage());
        }

        if (!validator.validate(jsonConfig)) {
            String errorJson = validator.getLastErrorJson();
            if (errorJson != null) {
                sendResponse(ctx, errorJson);
            }
            cleanup();
            ctx.close();
            return;
        }

        String response = JudgeServer.serve(jsonConfig, tempFile, new File(kwFilePath));
        log.info("JudgeServer response: {}", response);
        sendResponse(ctx, response);
        cleanup();
        ctx.close();
    }

    /**
     * Sends a UTF-8 encoded response string to the client,
     * prefixed by the length of the response in bytes.
     *
     * @param ctx      the channel handler context
     * @param response the response string to send
     */
    private void sendResponse(ChannelHandlerContext ctx, String response) {
        byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
        ByteBuf buf = ctx.alloc().buffer(4 + bytes.length);
        buf.writeInt(bytes.length);
        buf.writeBytes(bytes);
        ctx.writeAndFlush(buf);
        log.info("Response sent to client ({} bytes)", bytes.length);
    }

    /**
     * Cleans up temporary files and closes any open streams.
     */
    private void cleanup() {
        try {
            if (fos != null) {
                fos.close();
            }
        } catch (IOException e) {
            log.warn("Failed to close file output stream", e);
        }
        if (tempFile != null && tempFile.exists()) {
            if (tempFile.delete()) {
                log.info("Deleted temporary file: {}", tempFile.getName());
            } else {
                log.warn("Failed to delete temporary file: {}", tempFile.getName());
            }
        }
    }

    /**
     * Parses the checkpoint count from the JSON config.
     * Returns 1 if no checkpoints found or on error.
     *
     * @param jsonConfig the JSON configuration string
     * @return number of checkpoints (at least 1)
     */
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

    /**
     * Extracts the file extension from the given filename.
     * Returns empty string if no extension found.
     *
     * @param filename the filename string
     * @return the file extension including dot, or empty string
     */
    private static String getFileExtension(String filename) {
        int dotIndex = filename.lastIndexOf(".");
        return (dotIndex == -1) ? "" : filename.substring(dotIndex);
    }
}
