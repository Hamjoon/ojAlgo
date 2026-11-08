import static org.junit.jupiter.api.Assertions.*;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

@ExtendWith(MockitoExtension.class)
@ExtendWith(MockitoExtension.class)
class ExpressionsBasedModel_parse_Test {

    /**
     * Temporary directory provided by JUnit5 for file‑system tests.
     */
    @TempDir
    static Path tempDir;

    /**
     * Simple unchecked exception used in a few tests.
     */
    static class CustomParseException extends RuntimeException {

        private static final long serialVersionUID = 1L;
    }

    /* --------------------------------------------------------------
     * I/O error scenarios – the wrapper must wrap any IOException in a
     * RuntimeException while preserving the original cause.
     * -------------------------------------------------------------- */
    @Nested
    class IoErrorScenarios {

        @Test
        void testParseNonExistingFileThrowsRuntimeException() {
            File nonExisting = new File(tempDir.toFile(), "does_not_exist_12345.mps");
            assertFalse(nonExisting.exists(), "Pre‑condition: file must not exist");
            RuntimeException ex = assertThrows(RuntimeException.class, () -> org.ojalgo.optimisation.ExpressionsBasedModel.parse(nonExisting));
            assertNotNull(ex.getCause(), "RuntimeException should have a cause");
            assertTrue(ex.getCause() instanceof IOException, "Cause should be an IOException (FileNotFoundException)");
        }

        @Test
        void testParseDirectoryThrowsRuntimeException() throws IOException {
            // GIVEN – a directory (not a regular file).
            File directory = Files.createDirectory(tempDir.resolve("someDir")).toFile();
            RuntimeException ex = assertThrows(RuntimeException.class, () -> org.ojalgo.optimisation.ExpressionsBasedModel.parse(directory));
            assertNotNull(ex.getCause(), "RuntimeException should have a cause");
            assertTrue(ex.getCause() instanceof IOException, "Cause should be an IOException (FileNotFoundException for a directory)");
        }

        @Test
        void testParseUnreadableFileThrowsRuntimeException() throws IOException {
            // GIVEN – a file without read permission.
            Path unreadablePath = Files.createTempFile(tempDir, "unreadable", ".ebm");
            File unreadableFile = unreadablePath.toFile();
            // Remove read permission (platform dependent – works on POSIX).
            unreadableFile.setReadable(false, false);
            try {
                RuntimeException ex = assertThrows(RuntimeException.class, () -> org.ojalgo.optimisation.ExpressionsBasedModel.parse(unreadableFile));
                assertNotNull(ex.getCause(), "RuntimeException should have a cause");
                assertTrue(ex.getCause() instanceof IOException, "Cause should be an IOException (access denied)");
            } finally {
                // Restore permission so the file can be deleted.
                unreadableFile.setReadable(true, false);
                Files.deleteIfExists(unreadablePath);
            }
        }
    }

    /**
     * Verifies that {@code ExpressionsBasedModel.parse(File)} throws a {@link RuntimeException}
     * when the underlying {@link java.io.FileInputStream} cannot be opened (e.g. the file does not
     * exist). The original {@link IOException} must be preserved as the cause.
     */
    @Test
    void testParseNonExistingFileThrowsRuntimeException() {
        // GIVEN – a File reference that points to a non‑existent location.
        File nonExistingFile = new File("this_file_does_not_exist_12345.tmp");
        assertFalse(nonExistingFile.exists(), "Test pre‑condition: file must not exist");
        // WHEN / THEN – parsing should fail with a RuntimeException whose cause is an IOException.
        RuntimeException thrown = assertThrows(RuntimeException.class, () -> org.ojalgo.optimisation.ExpressionsBasedModel.parse(nonExistingFile));
        assertNotNull(thrown.getCause(), "The RuntimeException should have a cause");
        assertTrue(thrown.getCause() instanceof IOException, "The cause of the RuntimeException should be an IOException");
    }

    /**
     * A very light‑weight sanity check that a valid, empty file with a recognised extension
     * does not cause the wrapper itself to fail before delegating to the actual parser.
     * <p>
     * The underlying parser may still reject the empty content, but that is outside the scope of
     * this unit test – we only assert that the wrapper does not throw an unexpected exception
     * such as {@link NullPointerException}.
     * </p>
     */
    @Test
    void testParseEmptyFileWithValidExtensionDoesNotThrowWrapperException() throws IOException {
        // GIVEN – a temporary empty file with a supported ".ebm" extension.
        Path tempFile = Files.createTempFile("emptyModel", ".ebm");
        File file = tempFile.toFile();
        assertTrue(file.exists(), "Temporary file should exist");
        try {
            // WHEN – invoke the parse method. Any exception that bubbles up is considered a failure
            // of the wrapper (i.e. not the parser’s responsibility for content validation).
            org.ojalgo.optimisation.ExpressionsBasedModel model = org.ojalgo.optimisation.ExpressionsBasedModel.parse(file);
            // THEN – the method returned a non‑null model instance.
            assertNotNull(model, "parse(File) should return a non‑null model for an existing file");
        } finally {
            // Cleanup the temporary file.
            Files.deleteIfExists(tempFile);
        }
    }

    /* --------------------------------------------------------------
    Successful parsing – the delegate parser is mocked to return a
    dummy model.  Both supported extensions (MPS and EBM) are covered.
    --------------------------------------------------------------
     */
    @ParameterizedTest(name = "parse({0}) returns non‑null model")
    @CsvSource({ "tmpModel.mps, MPS", "tmpModel.ebm, EBM" })
    void testParseSupportedExtensionReturnsModel(String fileName, String formatEnum) throws Exception {
        // GIVEN – a temporary empty file with the requested extension.
        Path filePath = Files.createTempFile(tempDir, "model", "." + fileName.substring(fileName.lastIndexOf('.') + 1));
        File file = filePath.toFile();
        // A dummy model that will be returned by the mocked overload.
        ExpressionsBasedModel dummyModel = mock(ExpressionsBasedModel.class);
        // Mock the static overload parse(InputStream, FileFormat) while letting other static
        // methods (including parse(File)) execute their real code.
        try (MockedStatic<ExpressionsBasedModel> mocked = mockStatic(ExpressionsBasedModel.class, CALLS_REAL_METHODS)) {
            mocked.when(() -> ExpressionsBasedModel.parse(any(InputStream.class), eq(ExpressionsBasedModel.FileFormat.valueOf(formatEnum)))).thenReturn(dummyModel);
            // WHEN – invoke the wrapper.
            ExpressionsBasedModel result = ExpressionsBasedModel.parse(file);
            // THEN – a non‑null model (the stubbed dummy) is returned.
            assertNotNull(result, "parse(File) should return a non‑null model");
            assertSame(dummyModel, result, "Returned model should be the one provided by the mocked overload");
        } finally {
            Files.deleteIfExists(filePath);
        }
    }

    /* --------------------------------------------------------------
    Unsupported format – FileFormat.from throws IllegalArgumentException.
    --------------------------------------------------------------
     */
    @ParameterizedTest(name = "parse({0}) throws IllegalArgumentException")
    @CsvSource({ // no extension
    "model.txt", "model" })
    void testParseUnsupportedExtensionThrowsIllegalArgumentException(String fileName) throws IOException {
        // GIVEN – a temporary file (or just a File object) with an unsupported name.
        File file = tempDir.resolve(fileName).toFile();
        // Ensure the file exists for both cases.
        Files.createFile(file.toPath());
        // WHEN / THEN – the wrapper should propagate the IllegalArgumentException from FileFormat.from.
        assertThrows(IllegalArgumentException.class, () -> org.ojalgo.optimisation.ExpressionsBasedModel.parse(file));
    }

    /* --------------------------------------------------------------
    Null argument – should result in NullPointerException because
    FileFormat.from dereferences the argument.
    --------------------------------------------------------------
     */
    @Test
    void testParseNullFileThrowsNullPointerException() {
        assertThrows(NullPointerException.class, () -> org.ojalgo.optimisation.ExpressionsBasedModel.parse(null));
    }

    /* --------------------------------------------------------------
    Delegate parser throws an unchecked exception – the wrapper must
    let it propagate unchanged.
    --------------------------------------------------------------
     */
    @Test
    void testParseDelegateThrowsUncheckedExceptionIsPropagated() throws Exception {
        // GIVEN – a temporary file with a supported extension.
        Path filePath = Files.createTempFile(tempDir, "validModel", ".mps");
        File file = filePath.toFile();
        try (MockedStatic<ExpressionsBasedModel> mocked = mockStatic(ExpressionsBasedModel.class, CALLS_REAL_METHODS)) {
            mocked.when(() -> ExpressionsBasedModel.parse(any(InputStream.class), eq(ExpressionsBasedModel.FileFormat.MPS))).thenThrow(new CustomParseException());
            // WHEN / THEN – the custom exception propagates out of parse(File).
            assertThrows(CustomParseException.class, () -> ExpressionsBasedModel.parse(file));
        } finally {
            Files.deleteIfExists(filePath);
        }
    }

    /* --------------------------------------------------------------
    Sanity check – the wrapper does not mutate the supplied File
    object (e.g., it does not rename or delete it).  The test simply
    ensures the file still exists after a successful call.
    --------------------------------------------------------------
     */
    @Test
    void testParseDoesNotModifyInputFile() throws Exception {
        // GIVEN – a temporary empty file with a supported extension.
        Path filePath = Files.createTempFile(tempDir, "model", ".ebm");
        File file = filePath.toFile();
        try {
            // WHEN – invoke the wrapper.
            org.ojalgo.optimisation.ExpressionsBasedModel result = org.ojalgo.optimisation.ExpressionsBasedModel.parse(file);
            // THEN – wrapper returned a model and the original file still exists unchanged.
            assertNotNull(result);
            assertTrue(file.exists(), "The input File must still exist after parsing");
        } finally {
            Files.deleteIfExists(filePath);
        }
    }
}
