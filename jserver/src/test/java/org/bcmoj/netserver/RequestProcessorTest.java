package org.bcmoj.netserver;

import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.embedded.EmbeddedChannel;
import org.bcmoj.config.ServerConfig; // Import the config class
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
    private ServerConfig mockConfig; // Create a config object for testing

    @Before
    public void setUp() {
        // --- Refactored: Create a ServerConfiguration object with test values ---
        mockConfig = ServerConfig.builder()
                .keywordFilePath("kw.txt") // corresponds to kwFile
                .compilerPath("g++")       // corresponds to compilerPath
                .cppStandard("c++17")      // corresponds to cppStandard
                .disableSecurityArgs(false) // corresponds to DisableSecurityArgs
                .disableMemLimit(true)     // corresponds to DisableMemLimit
                .useOldFormat(true)        // corresponds to UseOldFormat
                // Host, Port, etc. might not be used by RequestProcessor directly, so we don't set them
                .build();

        // --- Refactored: Pass the config object to the new constructor ---
        processor = new RequestProcessor(mockConfig);
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
    public void testChannelRead_invalidFilenameLength() throws Exception { // Added throws Exception
        io.netty.buffer.ByteBuf buf = Unpooled.buffer();
        buf.writeInt(-114514);

        try {
            processor.channelRead(ctx, buf);
            fail("Expected IOException");
        } catch (Exception e) {
            // Check if the caught exception is an IOException (or one of its subclasses like the specific one thrown)
            // The original code threw IOException("Invalid filename length: " + filenameLength)
            assertTrue("Exception was not an IOException or subtype: " + e.getClass().getName(), e instanceof java.io.IOException);
            assertTrue("Exception message did not contain 'Invalid filename length'", e.getMessage().contains("Invalid filename length"));
        }
    }
}