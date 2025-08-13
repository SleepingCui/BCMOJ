package org.bcmoj.netserver;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.Assert.*;

public class RequestProcessorTest {
    private EmbeddedChannel channel;
    @Before
    public void setup() {
        channel = new EmbeddedChannel(new RequestProcessor("src/test/resources/keywords.txt"));
    }
    @After
    public void teardown() {
        channel.close();
    }
    @Test
    public void testFullRequestProcessing() {  //shit code
        String filename = "0721.cpp";
        String jsonConfig = "{\"timeLimit\":1000,\"securityCheck\":true,\"enableO2\":false,\"compareMode\":1,\"checkpoints\":{\"1_in\":\"input data for test 1\",\"1_out\":\"expected output for test 1\",\"2_in\":\"input data for test 2\",\"2_out\":\"expected output for test 2\"}}\n";
        String dummyHash = "fuck you";
        byte[] fileContent = "int main(){return 0;}".getBytes(StandardCharsets.UTF_8);
        byte[] filenameBytes = filename.getBytes(StandardCharsets.UTF_8);
        byte[] jsonBytes = jsonConfig.getBytes(StandardCharsets.UTF_8);
        byte[] hashBytes = dummyHash.getBytes(StandardCharsets.UTF_8);
        ByteBuf buf = Unpooled.buffer();

        buf.writeInt(filenameBytes.length);
        buf.writeBytes(filenameBytes);
        buf.writeLong(fileContent.length);
        buf.writeBytes(fileContent);
        buf.writeInt(jsonBytes.length);
        buf.writeBytes(jsonBytes);
        buf.writeInt(hashBytes.length);
        buf.writeBytes(hashBytes);
        channel.writeInbound(buf);
        Object outboundMsg = channel.readOutbound();
        assertNotNull(outboundMsg);
        assertTrue(outboundMsg instanceof ByteBuf);
        ByteBuf respBuf = (ByteBuf) outboundMsg;
        int respLen = respBuf.readInt();
        byte[] respBytes = new byte[respLen];
        respBuf.readBytes(respBytes);
        String response = new String(respBytes, StandardCharsets.UTF_8);
        assertNotNull(response);
        assertFalse(response.isEmpty());
        respBuf.release();
    }
}
