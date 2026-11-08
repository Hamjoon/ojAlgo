package org.ojalgo.optimisation;

import static org.junit.jupiter.api.Assertions.*;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.ojalgo.netio.InMemoryFile;
import static org.mockito.Mockito.*;
import java.io.OutputStream;
import org.ojalgo.optimisation.fileformat.FileFormatEBM;

@ExtendWith(MockitoExtension.class)
class ExpressionsBasedModel_writeTo_2_Test {

    private ExpressionsBasedModel model;

    /**
     * Build a tiny model that will produce some output when written.
     */
    @BeforeEach
    void setupBeforeEach() {
        model = new ExpressionsBasedModel();
        // a single variable x with bounds and a weight
        Variable x = model.newVariable("x").lower(0).upper(10).weight(1);
        // an expression that forces x = 5 (lower == upper)
        model.newExpression("eq").set(x, java.math.BigDecimal.ONE).lower(java.math.BigDecimal.valueOf(5)).upper(java.math.BigDecimal.valueOf(5));
    }

    /**
     * The normal happy‑path: a file is created and contains data.
     */
    @Test
    void testWriteToCreatesNonEmptyFile() throws IOException {
        InMemoryFile file = new InMemoryFile();
        // method under test – should not throw
        model.writeTo(file);
        // read back the bytes written
        try (InputStream in = file.newInputStream()) {
            byte[] buffer = in.readAllBytes();
            assertTrue(buffer.length > 0, "writeTo should have produced some output");
        }
    }

    /**
     * When the underlying {@link InMemoryFile} throws an {@link IOException},
     *  {@code writeTo} must wrap it in a {@link RuntimeException}.
     */
    @Test
    void testWriteToWrapsIOException() throws IOException {
        // mock an InMemoryFile whose newOutputStream() throws IOException
        InMemoryFile mockedFile = mock(InMemoryFile.class);
        when(mockedFile.newOutputStream()).thenThrow(new IOException("simulated I/O failure"));
        RuntimeException thrown = assertThrows(RuntimeException.class, () -> model.writeTo(mockedFile));
        // the cause should be the original IOException
        assertNotNull(thrown.getCause());
        assertTrue(thrown.getCause() instanceof IOException);
        assertEquals("simulated I/O failure", thrown.getCause().getMessage());
    }

    /* -------------------------------------------------------------
     *  Group: writeTo_successfulWrite
     * ------------------------------------------------------------- */
    @Test
    void testWriteTo_successfulWrite_callsFileFormatEBM_andClosesStream() throws Exception {
        // mock InMemoryFile and OutputStream
        InMemoryFile mockedFile = mock(InMemoryFile.class);
        OutputStream mockedOut = mock(OutputStream.class);
        when(mockedFile.newOutputStream()).thenReturn(mockedOut);
        // mock static FileFormatEBM.write – it should be called exactly once
        try (MockedStatic<FileFormatEBM> mockedStatic = mockStatic(FileFormatEBM.class)) {
            // no exception – just a no‑op
            mockedStatic.when(() -> FileFormatEBM.write(model, mockedOut)).thenAnswer(invocation -> null);
            // method under test – should not throw
            model.writeTo(mockedFile);
            // verify interaction with static method
            mockedStatic.verify(() -> FileFormatEBM.write(model, mockedOut), times(1));
            // verify that the OutputStream is closed exactly once (try‑with‑resources)
            verify(mockedOut, times(1)).close();
        }
    }

    /* -------------------------------------------------------------
     *  Group: writeTo_nullFile
     * ------------------------------------------------------------- */
    @Test
    void testWriteTo_nullFile_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> model.writeTo(null));
    }

    /* -------------------------------------------------------------
     *  Group: writeTo_outputStreamIOException
     * ------------------------------------------------------------- */
    @Test
    void testWriteTo_outputStreamIOException_wrapsInRuntimeException() throws Exception {
        InMemoryFile mockedFile = mock(InMemoryFile.class);
        when(mockedFile.newOutputStream()).thenThrow(new IOException("simulated I/O failure"));
        RuntimeException thrown = assertThrows(RuntimeException.class, () -> model.writeTo(mockedFile));
        assertNotNull(thrown.getCause());
        assertTrue(thrown.getCause() instanceof IOException);
        assertEquals("simulated I/O failure", thrown.getCause().getMessage());
    }

    /* -------------------------------------------------------------
     *  Group: writeTo_fileFormatWriteThrows
     * ------------------------------------------------------------- */
    @Test
    void testWriteTo_fileFormatWriteThrows_propagatesRuntimeException() throws Exception {
        InMemoryFile mockedFile = mock(InMemoryFile.class);
        OutputStream mockedOut = mock(OutputStream.class);
        when(mockedFile.newOutputStream()).thenReturn(mockedOut);
        try (MockedStatic<FileFormatEBM> mockedStatic = mockStatic(FileFormatEBM.class)) {
            IllegalStateException writeException = new IllegalStateException("write failed");
            mockedStatic.when(() -> FileFormatEBM.write(model, mockedOut)).thenThrow(writeException);
            IllegalStateException thrown = assertThrows(IllegalStateException.class, () -> model.writeTo(mockedFile));
            assertSame(writeException, thrown);
            // even though write failed, the stream must still be closed
            verify(mockedOut, times(1)).close();
        }
    }

    /* -------------------------------------------------------------
     *  Group: writeTo_outputStreamClosedOnSuccess
     * ------------------------------------------------------------- */
    @Test
    void testWriteTo_outputStreamClosedOnSuccess() throws Exception {
        InMemoryFile mockedFile = mock(InMemoryFile.class);
        OutputStream mockedOut = mock(OutputStream.class);
        when(mockedFile.newOutputStream()).thenReturn(mockedOut);
        try (MockedStatic<FileFormatEBM> mockedStatic = mockStatic(FileFormatEBM.class)) {
            mockedStatic.when(() -> FileFormatEBM.write(model, mockedOut)).thenReturn(null);
            model.writeTo(mockedFile);
            verify(mockedOut, times(1)).close();
        }
    }

    /* -------------------------------------------------------------
     *  Group: writeTo_outputStreamClosedOnException
     * ------------------------------------------------------------- */
    @Test
    void testWriteTo_outputStreamClosedOnException() throws Exception {
        InMemoryFile mockedFile = mock(InMemoryFile.class);
        OutputStream mockedOut = mock(OutputStream.class);
        when(mockedFile.newOutputStream()).thenReturn(mockedOut);
        try (MockedStatic<FileFormatEBM> mockedStatic = mockStatic(FileFormatEBM.class)) {
            RuntimeException writeException = new RuntimeException("boom");
            mockedStatic.when(() -> FileFormatEBM.write(model, mockedOut)).thenThrow(writeException);
            RuntimeException thrown = assertThrows(RuntimeException.class, () -> model.writeTo(mockedFile));
            assertSame(writeException, thrown);
            // verify close still happens
            verify(mockedOut, times(1)).close();
        }
    }

    /* -------------------------------------------------------------
     *  Group: writeTo_outputStreamIsNull
     * ------------------------------------------------------------- */
    @Test
    void testWriteTo_outputStreamIsNull_throwsNullPointerException() throws Exception {
        InMemoryFile mockedFile = mock(InMemoryFile.class);
        when(mockedFile.newOutputStream()).thenReturn(null);
        // The static write method will receive a null OutputStream and we let it
        // throw a NullPointerException (as the real implementation would).
        try (MockedStatic<FileFormatEBM> mockedStatic = mockStatic(FileFormatEBM.class)) {
            mockedStatic.when(() -> FileFormatEBM.write(model, null)).thenThrow(new NullPointerException("OutputStream is null"));
            NullPointerException thrown = assertThrows(NullPointerException.class, () -> model.writeTo(mockedFile));
            assertEquals("OutputStream is null", thrown.getMessage());
        }
    }

    /* -------------------------------------------------------------
     *  Group: writeTo_newOutputStreamRuntimeException
     * ------------------------------------------------------------- */
    @Test
    void testWriteTo_newOutputStreamRuntimeException_propagatesUnchanged() throws Exception {
        InMemoryFile mockedFile = mock(InMemoryFile.class);
        RuntimeException runtime = new RuntimeException("runtime from newOutputStream");
        when(mockedFile.newOutputStream()).thenThrow(runtime);
        RuntimeException thrown = assertThrows(RuntimeException.class, () -> model.writeTo(mockedFile));
        assertSame(runtime, thrown);
    }

    /* -------------------------------------------------------------
     *  Group: writeTo_fileFormatWriteIOException
     * ------------------------------------------------------------- */
    @Test
    void testWriteTo_fileFormatWriteIOException_wrapsInRuntimeException() throws Exception {
        InMemoryFile mockedFile = mock(InMemoryFile.class);
        OutputStream mockedOut = mock(OutputStream.class);
        when(mockedFile.newOutputStream()).thenReturn(mockedOut);
        try (MockedStatic<FileFormatEBM> mockedStatic = mockStatic(FileFormatEBM.class)) {
            IOException ioEx = new IOException("write IO failure");
            mockedStatic.when(() -> FileFormatEBM.write(model, mockedOut)).thenThrow(ioEx);
            RuntimeException thrown = assertThrows(RuntimeException.class, () -> model.writeTo(mockedFile));
            assertSame(ioEx, thrown.getCause());
            // stream must still be closed
            verify(mockedOut, times(1)).close();
        }
    }

    /* -------------------------------------------------------------
     *  Group: writeTo_outputStreamCloseIOException
     * ------------------------------------------------------------- */
    @Test
    void testWriteTo_outputStreamCloseIOException_wrapsInRuntimeException() throws Exception {
        InMemoryFile mockedFile = mock(InMemoryFile.class);
        OutputStream mockedOut = mock(OutputStream.class);
        when(mockedFile.newOutputStream()).thenReturn(mockedOut);
        // write succeeds
        try (MockedStatic<FileFormatEBM> mockedStatic = mockStatic(FileFormatEBM.class)) {
            mockedStatic.when(() -> FileFormatEBM.write(model, mockedOut)).thenReturn(null);
            // close throws IOException
            doThrow(new IOException("close failed")).when(mockedOut).close();
            RuntimeException thrown = assertThrows(RuntimeException.class, () -> model.writeTo(mockedFile));
            assertTrue(thrown.getCause() instanceof IOException);
            assertEquals("close failed", thrown.getCause().getMessage());
        }
    }

    /* -------------------------------------------------------------
     *  Group: writeTo_outputStreamCloseRuntimeException
     * ------------------------------------------------------------- */
    @Test
    void testWriteTo_outputStreamCloseRuntimeException_propagatesUnchanged() throws Exception {
        InMemoryFile mockedFile = mock(InMemoryFile.class);
        OutputStream mockedOut = mock(OutputStream.class);
        when(mockedFile.newOutputStream()).thenReturn(mockedOut);
        try (MockedStatic<FileFormatEBM> mockedStatic = mockStatic(FileFormatEBM.class)) {
            mockedStatic.when(() -> FileFormatEBM.write(model, mockedOut)).thenReturn(null);
            RuntimeException closeEx = new RuntimeException("close runtime");
            doThrow(closeEx).when(mockedOut).close();
            RuntimeException thrown = assertThrows(RuntimeException.class, () -> model.writeTo(mockedFile));
            // the RuntimeException from close should be the one that propagates
            assertSame(closeEx, thrown);
        }
    }
}
