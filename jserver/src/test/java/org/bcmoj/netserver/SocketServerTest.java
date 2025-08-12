package org.bcmoj.netserver;

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

    @Before
    public void startServer() throws InterruptedException {
        server = new SocketServer(HOST, PORT, "src/test/resources/keywords.txt");
        serverThread = new Thread(() -> {
            try {
                server.start();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });
        serverThread.start();
        Thread.sleep(1000);
    }
    @After
    public void stopServer() {
        server.stop();
        try {
            serverThread.join(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
    @Test
    public void testClientConnection() throws Exception {
        try (Socket socket = new Socket(HOST, PORT); DataOutputStream dos = new DataOutputStream(socket.getOutputStream()); DataInputStream dis = new DataInputStream(socket.getInputStream())) {

            String filename = "0721.cpp";
            byte[] filenameBytes = filename.getBytes(StandardCharsets.UTF_8);
            byte[] fileContent = "int main(){return 0;}".getBytes(StandardCharsets.UTF_8);
            String jsonConfig = "{\"timeLimit\":1000,\"securityCheck\":true,\"enableO2\":false,\"compareMode\":1,\"checkpoints\":{\"1_in\":\"input data for test 1\",\"1_out\":\"expected output for test 1\",\"2_in\":\"input data for test 2\",\"2_out\":\"expected output for test 2\"}}\n";
            byte[] jsonBytes = jsonConfig.getBytes(StandardCharsets.UTF_8);
            String dummyHash = "";
            byte[] hashBytes = dummyHash.getBytes(StandardCharsets.UTF_8);

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
