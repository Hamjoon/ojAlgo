package org.ojalgo.optimisation;

import static org.junit.jupiter.api.Assertions.*;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.MockedStatic;
import org.ojalgo.netio.InMemoryFile;
import org.ojalgo.netio.FileFormatEBM;

@ExtendWith(MockitoExtension.class)
class ExpressionsBasedModel_writeTo_2_Test {

    private ExpressionsBasedModel model;

    private Variable x;

    private Expression expr;

    /**
     * Simple holder for the data needed to build a particular test scenario.
     */
    private static final class WriteToScenario {

        final String name;

        final java.util.function.Function<WriteToScenario, InMemoryFile> fileSupplier;

        final boolean makeWriteThrowIOException;

        final boolean makeWriteThrowUnchecked;

        // not used currently, kept for possible extensions
        final Boolean expectNullPointer;

        // not used currently
        final Boolean expectRuntimeWrapper;

        WriteToScenario(String name, java.util.function.Function<WriteToScenario, InMemoryFile> fileSupplier, boolean makeWriteThrowIOException, boolean makeWriteThrowUnchecked, Boolean expectNullPointer, Boolean expectRuntimeWrapper) {
            this.name = name;
            this.fileSupplier = fileSupplier;
            this.makeWriteThrowIOException = makeWriteThrowIOException;
            this.makeWriteThrowUnchecked = makeWriteThrowUnchecked;
            this.expectNullPointer = expectNullPointer;
            this.expectRuntimeWrapper = expectRuntimeWrapper;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Build a minimal but valid model: one variable and one constraint.
        model = new ExpressionsBasedModel();
        // variable x with bounds [0,10] and weight 1 (contributes to the objective)
        x = model.newVariable("x").lower(0).upper(10).weight(1);
        // expression: 1 * x = 5  (i.e. minimise the objective while satisfying the equality)
        expr = model.newExpression("eq").level(5);
        expr.set(x, 1);
    }

    private static Stream<WriteToScenario> successfulPathProvider() {
        return Stream.of(new WriteToScenario("mockInMemoryFileSuccess", file -> {
            // normal file – returns a real OutputStream that we can inspect
            return new InMemoryFile();
        }, false, false, null, null));
    }

    private static Stream<WriteToScenario> ioExceptionOnNewOutputStreamProvider() {
        return Stream.of(new WriteToScenario("mockInMemoryFileThrowsOnNewOutputStream", file -> {
            InMemoryFile mock = Mockito.mock(InMemoryFile.class);
            Mockito.when(mock.newOutputStream()).thenThrow(new IOException("simulated I/O failure"));
            return mock;
        }, false, false, null, null));
    }

    private static Stream<WriteToScenario> ioExceptionOnWriteProvider() {
        return Stream.of(new WriteToScenario("mockInMemoryFileSuccess", file -> new InMemoryFile(), // make FileFormatEBM.write throw IOException
        true, false, null, null));
    }

    private static Stream<WriteToScenario> uncheckedExceptionOnWriteProvider() {
        return Stream.of(new WriteToScenario("mockInMemoryFileSuccess", file -> new InMemoryFile(), false, // make FileFormatEBM.write throw unchecked exception
        true, null, null));
    }

    private static Stream<WriteToScenario> nullFileProvider() {
        return Stream.of(new WriteToScenario("null", file -> null, false, false, null, null));
    }

    private static Stream<WriteToScenario> nullOutputStreamProvider() {
        return Stream.of(new WriteToScenario("mockInMemoryFileReturnsNullStream", file -> {
            InMemoryFile mock = Mockito.mock(InMemoryFile.class);
            Mockito.when(mock.newOutputStream()).thenReturn(null);
            return mock;
        }, false, false, null, null));
    }

    private static Stream<WriteToScenario> closeThrowsIOExceptionProvider() {
        return Stream.of(new WriteToScenario("mockInMemoryFileCloseThrowsIOException", file -> {
            OutputStream os = Mockito.mock(OutputStream.class);
            // write works fine
            Mockito.doNothing().when(os).write(Mockito.anyInt());
            // close throws checked IOException
            Mockito.doThrow(new IOException("close failure")).when(os).close();
            InMemoryFile mock = Mockito.mock(InMemoryFile.class);
            Mockito.when(mock.newOutputStream()).thenReturn(os);
            return mock;
        }, false, false, null, null));
    }

    private static Stream<WriteToScenario> closeThrowsUncheckedProvider() {
        return Stream.of(new WriteToScenario("mockInMemoryFileCloseThrowsUnchecked", file -> {
            OutputStream os = Mockito.mock(OutputStream.class);
            Mockito.doNothing().when(os).write(Mockito.anyInt());
            // close throws unchecked exception
            Mockito.doThrow(new IllegalStateException("close unchecked")).when(os).close();
            InMemoryFile mock = Mockito.mock(InMemoryFile.class);
            Mockito.when(mock.newOutputStream()).thenReturn(os);
            return mock;
        }, false, false, null, null));
    }

    @Test
    void testWriteToCreatesNonEmptyContent() throws IOException {
        // GIVEN a fresh in‑memory file
        InMemoryFile file = new InMemoryFile();
        // WHEN the model is written to the file
        model.writeTo(file);
        // THEN the file must contain some bytes (the EBM representation)
        try (InputStream in = file.newInputStream()) {
            List<Byte> bytes = new ArrayList<>();
            int b;
            while ((b = in.read()) != -1) {
                bytes.add((byte) b);
            }
            assertFalse(bytes.isEmpty(), "The written file should not be empty");
        }
    }

    @Test
    void testWriteToWrapsIOExceptionInRuntimeException(@Mock InMemoryFile faultyFile) throws IOException {
        // GIVEN a mocked InMemoryFile that throws IOException when an OutputStream is requested
        OutputStream brokenStream = Mockito.mock(OutputStream.class);
        Mockito.when(faultyFile.newOutputStream()).thenThrow(new IOException("simulated I/O failure"));
        // WHEN writeTo is invoked, it should wrap the IOException
        RuntimeException thrown = assertThrows(RuntimeException.class, () -> model.writeTo(faultyFile), "Expected a RuntimeException because the underlying OutputStream threw IOException");
        // THEN the cause of the RuntimeException must be the original IOException
        assertTrue(thrown.getCause() instanceof IOException, "The cause of the RuntimeException should be the original IOException");
        assertEquals("simulated I/O failure", thrown.getCause().getMessage());
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("successfulPathProvider")
    void testWriteToSuccessfulPath(WriteToScenario scenario) {
        // GIVEN a correctly behaving InMemoryFile
        InMemoryFile file = scenario.fileSupplier.get(scenario);
        try (MockedStatic<FileFormatEBM> mocked = Mockito.mockStatic(FileFormatEBM.class)) {
            // WHEN writeTo is invoked
            model.writeTo(file);
            // THEN FileFormatEBM.write must have been called exactly once with the model and the stream
            mocked.verify(() -> FileFormatEBM.write(Mockito.eq(model), Mockito.any(OutputStream.class)), Mockito.times(1));
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("ioExceptionOnNewOutputStreamProvider")
    void testWriteToNewOutputStreamThrowsIOException(WriteToScenario scenario) {
        InMemoryFile file = scenario.fileSupplier.get(scenario);
        RuntimeException thrown = assertThrows(RuntimeException.class, () -> model.writeTo(file), "Expected a RuntimeException because newOutputStream() throws IOException");
        assertTrue(thrown.getCause() instanceof IOException);
        assertEquals("simulated I/O failure", thrown.getCause().getMessage());
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("ioExceptionOnWriteProvider")
    void testWriteToFileFormatEBMWriteThrowsIOException(WriteToScenario scenario) {
        InMemoryFile file = scenario.fileSupplier.get(scenario);
        try (MockedStatic<FileFormatEBM> mocked = Mockito.mockStatic(FileFormatEBM.class)) {
            mocked.when(() -> FileFormatEBM.write(Mockito.eq(model), Mockito.any(OutputStream.class))).thenThrow(new IOException("write failure"));
            RuntimeException thrown = assertThrows(RuntimeException.class, () -> model.writeTo(file), "Expected a RuntimeException because FileFormatEBM.write throws IOException");
            assertTrue(thrown.getCause() instanceof IOException);
            assertEquals("write failure", thrown.getCause().getMessage());
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("uncheckedExceptionOnWriteProvider")
    void testWriteToFileFormatEBMWriteThrowsUnchecked(WriteToScenario scenario) {
        InMemoryFile file = scenario.fileSupplier.get(scenario);
        try (MockedStatic<FileFormatEBM> mocked = Mockito.mockStatic(FileFormatEBM.class)) {
            IllegalStateException unchecked = new IllegalStateException("unchecked failure");
            mocked.when(() -> FileFormatEBM.write(Mockito.eq(model), Mockito.any(OutputStream.class))).thenThrow(unchecked);
            IllegalStateException thrown = assertThrows(IllegalStateException.class, () -> model.writeTo(file), "Unchecked exception should propagate unchanged");
            assertSame(unchecked, thrown);
        }
    }

    @Test
    void testWriteToNullArgument() {
        assertThrows(NullPointerException.class, () -> model.writeTo((InMemoryFile) null), "Calling writeTo with null should throw NullPointerException");
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("nullOutputStreamProvider")
    void testWriteToNullOutputStream(WriteToScenario scenario) {
        InMemoryFile file = scenario.fileSupplier.get(scenario);
        // The method will attempt to pass a null OutputStream to FileFormatEBM.write,
        // which will cause a NullPointerException inside that static call.
        try (MockedStatic<FileFormatEBM> mocked = Mockito.mockStatic(FileFormatEBM.class)) {
            mocked.when(() -> FileFormatEBM.write(Mockito.eq(model), Mockito.isNull())).thenThrow(new NullPointerException("null stream"));
            RuntimeException thrown = assertThrows(RuntimeException.class, () -> model.writeTo(file), "Expected a RuntimeException caused by NullPointerException from FileFormatEBM.write");
            assertTrue(thrown.getCause() instanceof NullPointerException);
            assertEquals("null stream", thrown.getCause().getMessage());
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("closeThrowsIOExceptionProvider")
    void testWriteToCloseThrowsIOException(WriteToScenario scenario) {
        InMemoryFile file = scenario.fileSupplier.get(scenario);
        try (MockedStatic<FileFormatEBM> mocked = Mockito.mockStatic(FileFormatEBM.class)) {
            // make the write succeed (do nothing)
            mocked.when(() -> FileFormatEBM.write(Mockito.eq(model), Mockito.any(OutputStream.class))).thenAnswer(invocation -> null);
            RuntimeException thrown = assertThrows(RuntimeException.class, () -> model.writeTo(file), "Expected RuntimeException because close() throws IOException");
            assertTrue(thrown.getCause() instanceof IOException);
            assertEquals("close failure", thrown.getCause().getMessage());
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("closeThrowsUncheckedProvider")
    void testWriteToCloseThrowsUnchecked(WriteToScenario scenario) {
        InMemoryFile file = scenario.fileSupplier.get(scenario);
        try (MockedStatic<FileFormatEBM> mocked = Mockito.mockStatic(FileFormatEBM.class)) {
            // write succeeds
            mocked.when(() -> FileFormatEBM.write(Mockito.eq(model), Mockito.any(OutputStream.class))).thenAnswer(invocation -> null);
            IllegalStateException thrown = assertThrows(IllegalStateException.class, () -> model.writeTo(file), "Unchecked exception from close() should propagate unchanged");
            assertEquals("close unchecked", thrown.getMessage());
        }
    }
}
