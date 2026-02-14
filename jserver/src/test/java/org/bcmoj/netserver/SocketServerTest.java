package org.bcmoj.netserver;

import org.bcmoj.config.ServerConfig; // Import the new config class
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

import static org.junit.Assert.assertFalse;

public class SocketServerTest {

    private SocketServer server;
    private Thread serverThread;
    private static final String HOST = "127.0.0.1";
    private static final int PORT = 11451;
    private static final String KEYWORD_FILE_PATH = "src/test/resources/keywords.txt";

    @Before
    public void startServer() throws InterruptedException {
        // --- Refactored: Create ServerConfiguration using the Builder ---
        ServerConfig config = ServerConfig.builder()
                .host(HOST)
                .port(PORT)
                .disableSecurityArgs(false) // corresponds to the third argument in old constructor
                .disableMemLimit(true)      // corresponds to the fourth argument
                .useOldFormat(true)         // corresponds to the fifth argument
                .keywordFilePath(KEYWORD_FILE_PATH) // corresponds to the sixth argument
                .compilerPath("g++")        // corresponds to the seventh argument
                .cppStandard("c++11")       // corresponds to the eighth argument
                .build(); // Finalize the config object

        // --- Refactored: Pass the config object to the new constructor ---
        server = new SocketServer(config);
        serverThread = new Thread(() -> {
            try {
                // --- Refactored: Pass netty threads count separately to start method ---
                // Or, if you prefer, you could store nettyThreads in ServerConfiguration too
                // and pass config.getNettyThreads() here. Let's assume start takes it separately for now
                // as per the previous refactored SocketServer.java code example above.
                server.start(1); // Number of netty threads passed here
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });
        serverThread.start();
        Thread.sleep(1000); // Give the server some time to start
    }

    @After
    public void stopServer() {
        server.stop();
        try {
            serverThread.join(2000); // Wait for the server thread to finish gracefully
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testClientConnection() throws Exception {
        try (Socket socket = new Socket(HOST, PORT);
             DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
             DataInputStream dis = new DataInputStream(socket.getInputStream())) {

            String jsonConfig = "{\"timeLimit\":1000,\"securityCheck\":true,\"enableO2\":false,\"compareMode\":1,\"checkpoints\":{\"1_in\":\"input data for test 1\",\"1_out\":\"expected output for test 1\",\"2_in\":\"input data for test 2\",\"2_out\":\"expected output for test 2\"}}\n";
            String filename = "0721.cpp";
            String dHash = "fuck you";
            byte[] filenameBytes = filename.getBytes(StandardCharsets.UTF_8);
            byte[] fileContent = "int main(){return 0;}".getBytes(StandardCharsets.UTF_8);
            byte[] jsonBytes = jsonConfig.getBytes(StandardCharsets.UTF_8);
            byte[] hashBytes = dHash.getBytes(StandardCharsets.UTF_8);

            dos.writeInt(filenameBytes.length);
            dos.write(filenameBytes);
            dos.writeLong(fileContent.length);
            dos.write(fileContent);
            dos.writeInt(jsonBytes.length);
            dos.write(jsonBytes);
            dos.writeInt(hashBytes.length);
            dos.write(hashBytes);
            dos.flush();

            int respLen = dis.readInt();
            byte[] respBytes = new byte[respLen];
            dis.readFully(respBytes);
            String response = new String(respBytes, StandardCharsets.UTF_8);
            System.out.println("Server response: " + response);
            assertFalse(response.isEmpty());
        }
    }
}