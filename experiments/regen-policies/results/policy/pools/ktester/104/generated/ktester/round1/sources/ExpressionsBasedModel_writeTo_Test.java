package org.ojalgo.optimisation;

import static org.junit.jupiter.api.Assertions.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import java.io.FileNotFoundException;
import java.math.BigDecimal;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.MockedStatic;

@ExtendWith(MockitoExtension.class)
class ExpressionsBasedModel_writeTo_Test {

    /**
     * Temporary directory that lives for the whole test‑class lifecycle.
     */
    private static Path tempDir;

    /**
     * The file used by each individual test – created in {@link #setupBeforeEach()}.
     */
    private File testFile;

    @BeforeAll
    static void setupBeforeAll() throws IOException {
        tempDir = Files.createTempDirectory("ExpressionsBasedModel_writeTo_Test");
    }

    @BeforeEach
    void setupBeforeEach() throws IOException {
        // Create a fresh temporary file for every test method.
        Path filePath = Files.createTempFile(tempDir, "model", ".ebm");
        testFile = filePath.toFile();
    }

    @AfterEach
    void teardownAfterEach() {
        if (testFile != null && testFile.exists()) {
            //noinspection ResultOfMethodCallIgnored
            testFile.delete();
        }
    }

    @AfterAll
    static void teardownAfterAll() throws IOException {
        // Recursively delete the temporary directory and its contents.
        Files.walk(tempDir).sorted(// delete children before parent
        (a, b) -> b.compareTo(a)).forEach(p -> {
            try {
                Files.deleteIfExists(p);
            } catch (IOException ignored) {
            }
        });
    }

    /* -------------------------------------------------------------------------
     * Helper – builds a tiny optimisation model used by many tests.
     * ---------------------------------------------------------------------- */
    private static ExpressionsBasedModel buildTinyModel() {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        Variable x = model.newVariable("x").lower(0).upper(10).weight(1);
        Variable y = model.newVariable("y").lower(0).upper(10).weight(2);
        //  x + 2*y = 6
        model.newExpression("eq").lower(6).upper(6).set(x, 1).set(y, 2);
        return model;
    }

    /* -------------------------------------------------------------------------
     * 1. testWriteToCreatesFileAndDirectories
     *    – two sub‑cases: nested non‑existent directory & existing directory.
     * ---------------------------------------------------------------------- */
    private static Stream<Arguments> provideDirectoryScenarios() {
        return Stream.of(// nested directory that does not exist yet
        Arguments.of("nested/dir/model1.ebm", "File is created at the specified location, parent directories are created, and the model can be read back with ExpressionsBasedModel.parse(File) yielding an equivalent model."), // existing directory (created by the test harness)
        Arguments.of("existingDir/model2.ebm", "File is created (or overwritten) in an existing directory, and the model can be parsed back correctly."));
    }

    /**
     * Write a minimal model to a file and read it back.
     * The model consists of two variables and a single equality expression.
     */
    @Test
    void testWriteToAndParse() {
        // -------------------------------------------------
        // GIVEN – a tiny optimisation model
        // -------------------------------------------------
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        Variable x = model.newVariable("x").lower(0).upper(10).weight(1);
        Variable y = model.newVariable("y").lower(0).upper(10).weight(2);
        //  x + 2*y = 6
        model.newExpression("eq").lower(6).upper(6).set(x, 1).set(y, 2);
        // -------------------------------------------------
        // WHEN – the model is written to a file
        // -------------------------------------------------
        assertDoesNotThrow(() -> model.writeTo(testFile));
        // -------------------------------------------------
        // THEN – the file exists and can be parsed back
        // -------------------------------------------------
        assertTrue(testFile.isFile(), "File should have been created by writeTo");
        ExpressionsBasedModel parsed = assertDoesNotThrow(() -> ExpressionsBasedModel.parse(testFile));
        // Basic structural checks – we do not compare the whole object graph,
        // only that the important parts are present.
        assertEquals(2, parsed.countVariables(), "Parsed model should contain the same number of variables");
        assertEquals(1, parsed.countExpressions(), "Parsed model should contain the same number of expressions");
        // Verify that the variable names and bounds survived the round‑trip.
        Variable parsedX = parsed.getVariable(parsed.indexOf(x));
        Variable parsedY = parsed.getVariable(parsed.indexOf(y));
        assertEquals("x", parsedX.getName());
        assertEquals(0, parsedX.getLowerLimit().compareTo(BigDecimal.ZERO));
        assertEquals(10, parsedX.getUpperLimit().compareTo(BigDecimal.TEN));
        assertEquals("y", parsedY.getName());
        assertEquals(0, parsedY.getLowerLimit().compareTo(BigDecimal.ZERO));
        assertEquals(10, parsedY.getUpperLimit().compareTo(BigDecimal.TEN));
        // Verify that the expression coefficients are retained.
        Expression parsedExpr = parsed.getExpression("eq");
        assertNotNull(parsedExpr, "Expression 'eq' should be present after parsing");
        assertEquals(6, parsedExpr.getLowerLimit().compareTo(BigDecimal.valueOf(6)));
        assertEquals(6, parsedExpr.getUpperLimit().compareTo(BigDecimal.valueOf(6)));
        assertEquals(BigDecimal.ONE, parsedExpr.get(x, false));
        assertEquals(BigDecimal.valueOf(2), parsedExpr.get(y, false));
    }

    /**
     * Passing {@code null} to {@code writeTo} must result in a {@link NullPointerException}
     * because the implementation dereferences the argument immediately.
     */
    @Test
    void testWriteToWithNullFile() {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        assertThrows(NullPointerException.class, () -> model.writeTo(null));
    }

    @ParameterizedTest(name = "[{index}] {1}")
    @MethodSource("provideDirectoryScenarios")
    void testWriteToCreatesFileAndDirectories(String relativePath, String _unusedDescription) throws IOException {
        // -------------------------------------------------
        // GIVEN – a target file inside a (maybe) non‑existent directory
        // -------------------------------------------------
        Path targetDir = tempDir.resolve("target/test-output");
        // ensure the root of our test hierarchy exists
        Files.createDirectories(targetDir);
        Path filePath = targetDir.resolve(relativePath);
        File file = filePath.toFile();
        // Ensure the parent directory does NOT exist for the first case
        if (relativePath.startsWith("nested")) {
            assertFalse(file.getParentFile().exists(), "Parent directory should not exist before writeTo");
        } else {
            // for the second case we create the parent directory explicitly
            Files.createDirectories(file.getParentFile().toPath());
            assertTrue(file.getParentFile().exists(), "Parent directory should exist before writeTo");
        }
        ExpressionsBasedModel model = buildTinyModel();
        // -------------------------------------------------
        // WHEN – write the model to the file
        // -------------------------------------------------
        assertDoesNotThrow(() -> model.writeTo(file), "writeTo should not throw for a valid path");
        // -------------------------------------------------
        // THEN – the file exists and can be parsed back
        // -------------------------------------------------
        assertTrue(file.isFile(), "File should have been created by writeTo");
        ExpressionsBasedModel parsed = assertDoesNotThrow(() -> ExpressionsBasedModel.parse(file));
        // Basic structural checks – we only verify the important parts.
        assertEquals(2, parsed.countVariables(), "Parsed model should contain the same number of variables");
        assertEquals(1, parsed.countExpressions(), "Parsed model should contain the same number of expressions");
    }

    /* -------------------------------------------------------------------------
     * 2. testWriteToNullFile – already covered in the original test class.
     * ---------------------------------------------------------------------- */
    @Test
    void testWriteToNullFile() {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        assertThrows(NullPointerException.class, () -> model.writeTo(null));
    }

    /* -------------------------------------------------------------------------
     * 3. testWriteToNoParentDirectory – relative file name (no parent).
     * ---------------------------------------------------------------------- */
    @Test
    void testWriteToNoParentDirectory() throws IOException {
        // A file name without any directory component – will be created in the cwd.
        File relativeFile = new File("modelNoParent.ebm");
        // Ensure a clean state before the test.
        if (relativeFile.exists()) {
            assertTrue(relativeFile.delete(), "Pre‑existing test file could not be deleted");
        }
        ExpressionsBasedModel model = buildTinyModel();
        assertDoesNotThrow(() -> model.writeTo(relativeFile));
        assertTrue(relativeFile.isFile(), "File should have been created in the current working directory");
        // Parse back to verify round‑trip.
        ExpressionsBasedModel parsed = assertDoesNotThrow(() -> ExpressionsBasedModel.parse(relativeFile));
        assertEquals(2, parsed.countVariables());
        assertEquals(1, parsed.countExpressions());
        // Clean up.
        assertTrue(relativeFile.delete(), "Failed to delete temporary file");
    }

    /* -------------------------------------------------------------------------
     * 4. testWriteToIOException – passing a directory as the target file.
     * ---------------------------------------------------------------------- */
    @Test
    void testWriteToIOExceptionWhenFileIsDirectory() throws IOException {
        // Create a temporary directory that we will pass as the "file".
        Path dirPath = Files.createTempDirectory(tempDir, "dirAsFile");
        File dirAsFile = dirPath.toFile();
        ExpressionsBasedModel model = buildTinyModel();
        RuntimeException thrown = assertThrows(RuntimeException.class, () -> model.writeTo(dirAsFile), "Expected RuntimeException when trying to open a FileOutputStream on a directory");
        assertNotNull(thrown.getCause(), "RuntimeException should have a cause");
        assertTrue(thrown.getCause() instanceof IOException, "Cause of the RuntimeException should be an IOException");
    }

    /* -------------------------------------------------------------------------
     * 5. testWriteToFileOutputStreamIOException – unwritable directory.
     * ---------------------------------------------------------------------- */
    @Test
    void testWriteToFileOutputStreamIOException_UnwritableDir() throws IOException {
        // Create a directory and remove write permissions.
        Path unwritableDir = Files.createTempDirectory(tempDir, "unwritableDir");
        File dirFile = unwritableDir.toFile();
        // On POSIX systems we can use setWritable(false). On Windows this may not have the same effect,
        // but the test still attempts to provoke a FileNotFoundException.
        boolean permissionChanged = dirFile.setWritable(false, false);
        assumeTrue(permissionChanged, "Could not make directory unwritable – skipping test on this platform");
        File targetFile = new File(dirFile, "model.ebm");
        ExpressionsBasedModel model = buildTinyModel();
        RuntimeException thrown = assertThrows(RuntimeException.class, () -> model.writeTo(targetFile), "Expected RuntimeException when FileOutputStream cannot be created due to permissions");
        assertNotNull(thrown.getCause(), "RuntimeException should have a cause");
        assertTrue(thrown.getCause() instanceof FileNotFoundException, "Cause should be a FileNotFoundException when the file cannot be created");
        // Restore write permission so that the temporary directory can be deleted during teardown.
        dirFile.setWritable(true, false);
    }

    /* -------------------------------------------------------------------------
     * 6. testWriteToWriteIOException – mock FileFormatEBM.write to throw IOException.
     * ---------------------------------------------------------------------- */
    @Test
    void testWriteToWriteIOException_MockedWriteFailure() throws IOException {
        // The class that contains the static write method is assumed to be
        // org.ojalgo.optimisation.FileFormatEBM (as used in ExpressionsBasedModel.writeTo).
        // Mockito's inline mock maker allows mocking static methods.
        try (MockedStatic<FileFormatEBM> mocked = mockStatic(FileFormatEBM.class)) {
            mocked.when(() -> FileFormatEBM.write(any(ExpressionsBasedModel.class), any())).thenThrow(new IOException("simulated write failure"));
            ExpressionsBasedModel model = buildTinyModel();
            RuntimeException thrown = assertThrows(RuntimeException.class, () -> model.writeTo(testFile), "Expected RuntimeException when FileFormatEBM.write throws IOException");
            assertNotNull(thrown.getCause(), "RuntimeException should wrap the original IOException");
            assertTrue(thrown.getCause() instanceof IOException, "Cause of the RuntimeException should be an IOException");
            assertEquals("simulated write failure", thrown.getCause().getMessage());
        }
    }

    /* -------------------------------------------------------------------------
     * 7. testWriteToSecurityException – this scenario is environment‑dependent.
     *    We attempt to provoke a SecurityException by installing a restrictive
     *    SecurityManager. If the platform does not support it, the test is skipped.
     * ---------------------------------------------------------------------- */
    @Test
    void testWriteToSecurityException() throws IOException {
        // Skip on JDKs where a SecurityManager cannot be installed (e.g., JDK 17+ without
        // the legacy security manager enabled).
        SecurityManager original = System.getSecurityManager();
        try {
            System.setSecurityManager(new SecurityManager() {

                @Override
                public void checkWrite(String file) {
                    throw new SecurityException("write access denied by test SecurityManager");
                }

                @Override
                public void checkPermission(java.security.Permission perm) {
                    // allow everything else
                }
            });
            // The target file is inside a directory that does not exist yet.
            File protectedFile = tempDir.resolve("protectedDir/model.ebm").toFile();
            ExpressionsBasedModel model = buildTinyModel();
            // Expect the SecurityException to propagate (not wrapped).
            SecurityException se = assertThrows(SecurityException.class, () -> model.writeTo(protectedFile), "Expected SecurityException when the SecurityManager denies directory creation");
            assertEquals("write access denied by test SecurityManager", se.getMessage());
        } finally {
            // Restore original security manager to avoid side effects on other tests.
            System.setSecurityManager(original);
        }
    }
}
