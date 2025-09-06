package org.bcmoj.netserver;

import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import java.io.File;
import java.lang.reflect.Method;

import static org.junit.Assert.*;
import static org.mockito.Mockito.when;

@RunWith(org.mockito.junit.MockitoJUnitRunner.class)
public class RequestProcessorTest {

    @Mock
    private ChannelHandlerContext ctx;
    private RequestProcessor processor;

    @Before
    public void setUp() {
        processor = new RequestProcessor("kw.txt", "g++", "c++17");
    }

    @Test
    public void testGetFileExtension() throws Exception {
        Method m = RequestProcessor.class.getDeclaredMethod("getFileExtension", String.class);
        m.setAccessible(true);
        assertEquals(".cpp", m.invoke(null, "1.cpp"));
        assertEquals("", m.invoke(null, "fuckyou"));
        assertEquals(".gz", m.invoke(null, "shit.tar.gz"));
    }

    @Test
    public void testParseCheckpointCount_validJson() throws Exception {
        String json = "{ \"checkpoints\": [\"a.in\", \"b.in\"] }";
        Method m = RequestProcessor.class.getDeclaredMethod("parseCheckpointCount", String.class);
        m.setAccessible(true);
        int count = (int) m.invoke(processor, json);
        assertTrue(count >= 1);
    }

    @Test
    public void testParseCheckpointCount_invalidJson() throws Exception {
        String json = "1";
        Method m = RequestProcessor.class.getDeclaredMethod("parseCheckpointCount", String.class);
        m.setAccessible(true);
        int count = (int) m.invoke(processor, json);
        assertEquals(1, count); //fallback
    }

    @Test
    public void testCleanupDeletesTempFile() throws Exception {
        File tmp = File.createTempFile("test", ".txt");
        assertTrue(tmp.exists());
        java.lang.reflect.Field f = RequestProcessor.class.getDeclaredField("tempFile");
        f.setAccessible(true);
        f.set(processor, tmp);
        Method m = RequestProcessor.class.getDeclaredMethod("cleanup");
        m.setAccessible(true);
        m.invoke(processor);
        assertFalse(tmp.exists());
    }

    @Test
    public void testChannelActiveAndInactive() throws Exception {
        EmbeddedChannel channel = new EmbeddedChannel();
        when(ctx.channel()).thenReturn(channel);
        processor.channelActive(ctx);
        processor.channelInactive(ctx);
    }

    @Test
    public void testChannelRead_invalidFilenameLength() {
        io.netty.buffer.ByteBuf buf = Unpooled.buffer();
        buf.writeInt(-114514);

        try {
            processor.channelRead(ctx, buf);
            fail("Expected IOException");
        } catch (Exception e) {
            assertTrue(e.getMessage().contains("Invalid filename length"));
        }
    }
}
