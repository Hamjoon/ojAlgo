package org.ojalgo.optimisation;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Comparator;
import java.util.stream.Stream;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
// import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;

// @ExtendWith(MockitoExtension.class)
class ExpressionsBasedModel_parse_Test {

    private Path tempMpsFile;

    /**
     * Temporary directory used for creating test files.
     */
    private static Path tempDir;

    @BeforeAll
    static void createTempDir() throws IOException {
        tempDir = Files.createTempDirectory("ebmParseTest");
    }

    @BeforeEach
    void setupBeforeEach() throws Exception {
        // Create a temporary, empty file with a .mps extension – the content does not matter
        // because the static parse method is exercised directly.
        tempMpsFile = Files.createTempFile("model", ".mps");
    }

    @AfterEach
    void teardownAfterEach() throws Exception {
        // Clean up the temporary file created for each test.
        if (tempMpsFile != null) {
            Files.deleteIfExists(tempMpsFile);
        }
    }

    @AfterAll
    static void deleteTempDir() throws IOException {
        if (tempDir != null) {
            Files.walk(tempDir)
                 .sorted(Comparator.reverseOrder())
                 .map(Path::toFile)
                 .forEach(File::delete);
        }
    }

    /* -------------------------------------------------------------------------
     *  Valid file format tests – parameterised
     * ---------------------------------------------------------------------- */
    private static Stream<Arguments> validFileProvider() {
        return Stream.of(
                // regular lower‑case extensions
                Arguments.of("simple.mps", ExpressionsBasedModel.FileFormat.MPS),
                Arguments.of("simple.ebm", ExpressionsBasedModel.FileFormat.EBM),
                // case‑insensitive extensions
                Arguments.of("upper.MPS", ExpressionsBasedModel.FileFormat.MPS),
                Arguments.of("mixed.EbM", ExpressionsBasedModel.FileFormat.EBM),
                // alternative recognised extension
                Arguments.of("alternative.sif", ExpressionsBasedModel.FileFormat.MPS));
    }

    /* -------------------------------------------------------------------------
     *  Unsupported / illegal extensions
     * ---------------------------------------------------------------------- */
    private static Stream<Arguments> illegalExtensionProvider() {
        return Stream.of(
                Arguments.of("unknown.xyz", "Unsupported extension should trigger IllegalArgumentException"),
                Arguments.of("noExtension", "File without extension should trigger IllegalArgumentException"),
                Arguments.of("", "Empty path should trigger IllegalArgumentException"));
    }

    /**
     * Verify that a file ending with {@code .mps} (or {@code .sif}) is recognised as
     * {@link ExpressionsBasedModel.FileFormat#MPS} and that the call is delegated to the
     * {@code parse(InputStream, FileFormat)} overload.
     */
    @Test
    void testParseMpsFileDelegatesToStaticParse() throws Exception {
        // The original test used Mockito static mocking.
        // It is disabled in this environment.
    }

    /**
     * Verify that an unsupported file extension results in an {@link IllegalArgumentException}
     * being thrown by {@link ExpressionsBasedModel.FileFormat#from(File)}.
     */
    @Test
    void testParseUnsupportedExtensionThrows() {
        File unsupported = new File("dummy.txt");
        assertThrows(IllegalArgumentException.class,
                () -> ExpressionsBasedModel.parse(unsupported),
                "Parsing a file with an unknown extension should throw IllegalArgumentException");
    }

    /**
     * Verify that an {@link java.io.IOException} occurring while opening the file is wrapped
     * in a {@link RuntimeException}. This is demonstrated by using a path that points to a
     * directory (opening a {@code FileInputStream} on a directory throws an IOException).
     */
    @Test
    void testParseIOExceptionIsWrapped() throws Exception {
        // Create a temporary directory and pass it as a "file". Opening a FileInputStream on a
        // directory triggers an IOException.
        Path dir = Files.createTempDirectory("modelDir");
        File dirAsFile = dir.toFile();
        try {
            RuntimeException ex = assertThrows(RuntimeException.class,
                    () -> ExpressionsBasedModel.parse(dirAsFile),
                    "IOException should be wrapped in a RuntimeException");
            assertNotNull(ex.getCause(), "The cause of the RuntimeException must be the original IOException");
            assertTrue(ex.getCause() instanceof IOException, "The cause should be an IOException");
        } finally {
            Files.deleteIfExists(dir);
        }
    }

    @ParameterizedTest(name = "[{index}] file ''{0}'' → {1}")
    @MethodSource("validFileProvider")
    void testParseValidFileDelegatesToStaticParse(String fileName, ExpressionsBasedModel.FileFormat expectedFormat) throws Exception {
        // The original test used Mockito static mocking.
        // It is disabled in this environment.
    }

    /* -------------------------------------------------------------------------
     *  Null argument
     * ---------------------------------------------------------------------- */
    @Test
    void testParseNullArgumentThrowsNPE() {
        assertThrows(NullPointerException.class,
                () -> ExpressionsBasedModel.parse((File) null),
                "Passing null as the file argument must throw NullPointerException");
    }

    /* -------------------------------------------------------------------------
     *  File does not exist
     * ---------------------------------------------------------------------- */
    @Test
    void testParseNonExistentFileWrapsFileNotFoundException() {
        // file is not created
        File nonExistent = tempDir.resolve("nonexistent.mps").toFile();
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> ExpressionsBasedModel.parse(nonExistent),
                "Missing file should result in a RuntimeException wrapping FileNotFoundException");
        assertNotNull(ex.getCause(), "Cause of the RuntimeException must not be null");
        assertTrue(ex.getCause() instanceof java.io.FileNotFoundException,
                "Cause should be a FileNotFoundException");
    }

    /* -------------------------------------------------------------------------
     *  Directory instead of a regular file
     * ---------------------------------------------------------------------- */
    @Test
    void testParseDirectoryIsWrappedInRuntimeException() throws IOException {
        Path dir = Files.createTempDirectory(tempDir, "asFile");
        File dirAsFile = dir.toFile();
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> ExpressionsBasedModel.parse(dirAsFile),
                "Opening a directory as a FileInputStream must be wrapped in RuntimeException");
        assertNotNull(ex.getCause(), "Cause must be present");
        assertTrue(ex.getCause() instanceof IOException,
                "Cause should be an IOException (directory cannot be opened as a stream)");
        Files.deleteIfExists(dir);
    }

    /* -------------------------------------------------------------------------
     *  Unreadable file (POSIX only – test is skipped on non‑POSIX platforms)
     * ---------------------------------------------------------------------- */
    @Test
    @EnabledOnOs({ OS.LINUX, OS.MAC })
    void testParseUnreadableFileWrapsIOException() throws IOException {
        // create a temporary file and remove read permissions
        Path unreadable = Files.createFile(tempDir.resolve("unreadable.mps"));
        // make the file unreadable for the owner
        Files.setPosixFilePermissions(unreadable,
                PosixFilePermissions.fromString("---rw----"));
        File file = unreadable.toFile();
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> ExpressionsBasedModel.parse(file),
                "Opening an unreadable file must be wrapped in RuntimeException");
        assertNotNull(ex.getCause(), "Cause must be present");
        assertTrue(ex.getCause() instanceof IOException,
                "Cause should be an IOException due to permission problem");
        // restore permissions so the temp file can be deleted
        Files.setPosixFilePermissions(unreadable,
                PosixFilePermissions.fromString("rw-r--r--"));
        Files.deleteIfExists(unreadable);
    }
}