package org.ojalgo.optimisation;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.io.IOException;
import java.io.OutputStream;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.ojalgo.netio.InMemoryFile;
import java.io.ByteArrayOutputStream;
import org.mockito.MockedStatic;

@ExtendWith(MockitoExtension.class)
class ExpressionsBasedModel_writeTo_2_Test {

    /**
     * The model used in all tests – it is created fresh for each test.
     */
    private ExpressionsBasedModel model;

    @BeforeEach
    void setupBeforeEach() {
        // a minimal model – a single variable and a trivial expression
        model = new ExpressionsBasedModel();
        Variable x = model.newVariable("x").lower(0).upper(10).weight(1);
        model.newExpression("expr").set(x, 1).level(5);
        model = new ExpressionsBasedModel();
        // a tiny model sufficient for the write operation
        Variable x = model.newVariable("x").lower(0).upper(10).weight(1);
        model.newExpression("expr").set(x, 1).level(5);
    }

    @Test
    void testWriteToInvokesOutputStreamExactlyOnce() throws IOException {
        // Spy on a real InMemoryFile so we can verify interaction while still
        // providing a functional OutputStream.
        InMemoryFile realFile = new InMemoryFile();
        InMemoryFile fileSpy = spy(realFile);
        // Act
        model.writeTo(fileSpy);
        // Assert – newOutputStream() must have been called exactly once.
        verify(fileSpy, times(1)).newOutputStream();
        // Additionally, ensure that the returned OutputStream was closed by the
        // try‑with‑resources block inside the method.  The close() call is part
        // of the OutputStream contract; we verify it via the spy.
        // obtain a fresh stream for verification
        OutputStream os = fileSpy.newOutputStream();
        // the stream used inside writeTo() should be closed
        verify(os, atLeastOnce()).close();
    }

    @Test
    void testWriteToWrapsIOExceptionInRuntimeException() throws IOException {
        // Mock an InMemoryFile that throws an IOException when asked for an OutputStream.
        InMemoryFile faultyFile = mock(InMemoryFile.class);
        when(faultyFile.newOutputStream()).thenThrow(new IOException("simulated I/O error"));
        // Act & Assert
        RuntimeException thrown = assertThrows(RuntimeException.class, () -> model.writeTo(faultyFile), "Expected writeTo to wrap IOException in RuntimeException");
        // The cause of the RuntimeException must be the original IOException.
        assertTrue(thrown.getCause() instanceof IOException);
        assertEquals("simulated I/O error", thrown.getCause().getMessage());
    }

    /* --------------------------------------------------------------
     *  Happy‑path: the InMemoryFile returns a normal OutputStream and
     *  FileFormatEBM.write completes without error.
     * -------------------------------------------------------------- */
    @Test
    void testWriteToInvokesFileFormatEBMWriteExactlyOnce() throws IOException {
        // Mock InMemoryFile and its OutputStream
        InMemoryFile fileMock = mock(InMemoryFile.class);
        OutputStream osMock = mock(OutputStream.class);
        when(fileMock.newOutputStream()).thenReturn(osMock);
        // Mock the static FileFormatEBM.write method
        try (MockedStatic<FileFormatEBM> mocked = mockStatic(FileFormatEBM.class)) {
            // Act
            model.writeTo(fileMock);
            // Verify that newOutputStream() was called exactly once
            verify(fileMock, times(1)).newOutputStream();
            // Verify that FileFormatEBM.write was invoked exactly once with the correct args
            mocked.verify(() -> FileFormatEBM.write(model, osMock), times(1));
            // Verify that the OutputStream was closed by the try‑with‑resources block
            verify(osMock, atLeastOnce()).close();
        }
    }

    /* --------------------------------------------------------------
     *  IOException while obtaining the OutputStream.
     * -------------------------------------------------------------- */
    @Test
    void testWriteToWrapsIOExceptionFromNewOutputStream() throws IOException {
        InMemoryFile fileMock = mock(InMemoryFile.class);
        when(fileMock.newOutputStream()).thenThrow(new IOException("disk error"));
        RuntimeException ex = assertThrows(RuntimeException.class, () -> model.writeTo(fileMock), "IOException should be wrapped in RuntimeException");
        assertTrue(ex.getCause() instanceof IOException);
        assertEquals("disk error", ex.getCause().getMessage());
    }

    /* --------------------------------------------------------------
     *  IOException thrown by FileFormatEBM.write().
     * -------------------------------------------------------------- */
    @Test
    void testWriteToWrapsIOExceptionFromFileFormatEBMWrite() throws IOException {
        InMemoryFile fileMock = mock(InMemoryFile.class);
        OutputStream os = new ByteArrayOutputStream();
        when(fileMock.newOutputStream()).thenReturn(os);
        try (MockedStatic<FileFormatEBM> mocked = mockStatic(FileFormatEBM.class)) {
            mocked.when(() -> FileFormatEBM.write(model, os)).thenThrow(new IOException("write failed"));
            RuntimeException ex = assertThrows(RuntimeException.class, () -> model.writeTo(fileMock), "IOException from FileFormatEBM.write should be wrapped");
            assertTrue(ex.getCause() instanceof IOException);
            assertEquals("write failed", ex.getCause().getMessage());
        }
    }

    /* --------------------------------------------------------------
     *  IOException thrown while closing the OutputStream.
     * -------------------------------------------------------------- */
    @Test
    void testWriteToWrapsIOExceptionFromClose() throws IOException {
        InMemoryFile fileMock = mock(InMemoryFile.class);
        OutputStream osMock = mock(OutputStream.class);
        when(fileMock.newOutputStream()).thenReturn(osMock);
        // write succeeds (no exception)
        try (MockedStatic<FileFormatEBM> mocked = mockStatic(FileFormatEBM.class)) {
            mocked.when(() -> FileFormatEBM.write(model, osMock)).thenAnswer(inv -> null);
            // close throws IOException
            doThrow(new IOException("close error")).when(osMock).close();
            RuntimeException ex = assertThrows(RuntimeException.class, () -> model.writeTo(fileMock), "IOException from OutputStream.close should be wrapped");
            assertTrue(ex.getCause() instanceof IOException);
            assertEquals("close error", ex.getCause().getMessage());
        }
    }

    /* --------------------------------------------------------------
     *  Null InMemoryFile argument – should throw NPE before any I/O.
     * -------------------------------------------------------------- */
    @Test
    void testWriteToNullFileThrowsNullPointerException() {
        assertThrows(NullPointerException.class, () -> model.writeTo(null), "Passing null should result in NullPointerException");
    }

    /* --------------------------------------------------------------
     *  RuntimeException from FileFormatEBM.write – should propagate unchanged.
     * -------------------------------------------------------------- */
    @Test
    void testWriteToPropagatesRuntimeExceptionFromFileFormatEBMWrite() throws IOException {
        InMemoryFile fileMock = mock(InMemoryFile.class);
        OutputStream os = new ByteArrayOutputStream();
        when(fileMock.newOutputStream()).thenReturn(os);
        try (MockedStatic<FileFormatEBM> mocked = mockStatic(FileFormatEBM.class)) {
            IllegalStateException cause = new IllegalStateException("illegal state");
            mocked.when(() -> FileFormatEBM.write(model, os)).thenThrow(cause);
            IllegalStateException thrown = assertThrows(IllegalStateException.class, () -> model.writeTo(fileMock), "RuntimeException from FileFormatEBM.write must not be wrapped");
            assertSame(cause, thrown);
        }
    }

    /* --------------------------------------------------------------
     *  RuntimeException from InMemoryFile.newOutputStream – should propagate unchanged.
     * -------------------------------------------------------------- */
    @Test
    void testWriteToPropagatesRuntimeExceptionFromNewOutputStream() {
        InMemoryFile fileMock = mock(InMemoryFile.class);
        IllegalStateException cause = new IllegalStateException("unexpected error");
        when(fileMock.newOutputStream()).thenThrow(cause);
        IllegalStateException thrown = assertThrows(IllegalStateException.class, () -> model.writeTo(fileMock), "RuntimeException from newOutputStream must not be wrapped");
        assertSame(cause, thrown);
    }
}
