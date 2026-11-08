package org.ojalgo.optimisation;

import static org.junit.jupiter.api.Assertions.*;
import java.io.File;
import java.io.IOException;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.*;

class ExpressionsBasedModel_writeTo_Test {

    /* --------------------------------------------------------------------- *
     *  Helper methods to build a minimal model used by all test cases.
     * --------------------------------------------------------------------- */
    private static ExpressionsBasedModel minimalModel() {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        // a single variable x with bounds and a weight
        Variable x = model.newVariable("x").lower(0).upper(10).weight(1);
        // a simple equality expression: x = 5
        model.newExpression("eq").set(x, 1).level(5);
        return model;
    }

    /**
     * Verify that {@code writeTo(File)} creates the parent directory (if missing),
     * writes a non‑empty file and that the file can be parsed back into an equivalent model.
     */
    @Test
    void testWriteToCreatesFileAndIsParseable(@TempDir Path tempDir) throws IOException {
        // -----------------------------------------------------------------
        // GIVEN – a simple model with one variable and one expression
        // -----------------------------------------------------------------
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        Variable x = model.newVariable("x").lower(0).upper(10).weight(1);
        // expression:  x = 5   (i.e. lower == upper == 5)
        model.newExpression("eq").set(x, 1).level(5);
        // -----------------------------------------------------------------
        // AND – a target file inside a non‑existent sub‑directory
        // -----------------------------------------------------------------
        Path subDir = tempDir.resolve("nonExisting/sub");
        Path filePath = subDir.resolve("model.ebm");
        File targetFile = filePath.toFile();
        assertFalse(Files.exists(subDir), "Parent directory must not exist before the call");
        assertFalse(Files.exists(filePath), "Target file must not exist before the call");
        // -----------------------------------------------------------------
        // WHEN – the model is written to the file
        // -----------------------------------------------------------------
        model.writeTo(targetFile);
        // -----------------------------------------------------------------
        // THEN – the directory hierarchy has been created and the file is non‑empty
        // -----------------------------------------------------------------
        assertTrue(Files.isDirectory(subDir), "Parent directory should have been created");
        assertTrue(Files.isRegularFile(filePath), "File should have been created");
        assertTrue(Files.size(filePath) > 0, "Written file should not be empty");
        // -----------------------------------------------------------------
        // AND – the file can be parsed back into a model that is structurally equivalent
        // -----------------------------------------------------------------
        ExpressionsBasedModel parsed = ExpressionsBasedModel.parse(targetFile);
        assertNotNull(parsed, "Parsed model must not be null");
        assertEquals(model.countVariables(), parsed.countVariables(), "Number of variables should be identical after parsing");
        assertEquals(model.countExpressions(), parsed.countExpressions(), "Number of expressions should be identical after parsing");
        // The variable name and bounds should survive the round‑trip
        Variable parsedX = parsed.getVariable(0);
        assertEquals(x.getName(), parsedX.getName(), "Variable name should be preserved");
        assertEquals(x.lower().doubleValue(), parsedX.lower().doubleValue(), 1e-12, "Lower bound should be preserved");
        assertEquals(x.upper().doubleValue(), parsedX.upper().doubleValue(), 1e-12, "Upper bound should be preserved");
    }

    /**
     * Verify that a {@code null} argument results in a {@link NullPointerException}.
     * The implementation calls {@code file.getParentFile()}, which will throw NPE
     * when {@code file} is {@code null}.
     */
    @Test
    void testWriteToNullFileThrows() {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        assertThrows(NullPointerException.class, () -> model.writeTo(null));
    }

    /* --------------------------------------------------------------------- *
     *  3. IOException handling – write to a non‑writable directory.
     *     The method should wrap the IOException into a RuntimeException.
     * --------------------------------------------------------------------- */
    @Test
    void testWriteToIOExceptionIsWrapped(@TempDir Path tempDir) throws IOException {
        // create a directory and make it non‑writable (POSIX only; on Windows the test is skipped)
        Path nonWritableDir = tempDir.resolve("noWrite");
        Files.createDirectory(nonWritableDir);
        try {
            Set<PosixFilePermission> perms = PosixFilePermissions.fromString("r-xr-xr-x");
            Files.setPosixFilePermissions(nonWritableDir, perms);
        } catch (UnsupportedOperationException ex) {
            // Windows – fall back to making the directory read‑only via File API
            nonWritableDir.toFile().setWritable(false);
        }
        File target = nonWritableDir.resolve("model.ebm").toFile();
        ExpressionsBasedModel model = minimalModel();
        RuntimeException re = assertThrows(RuntimeException.class, () -> model.writeTo(target), "Expected a RuntimeException because the directory is not writable");
        assertTrue(re.getCause() instanceof IOException, "The cause of the RuntimeException should be an IOException");
    }

    /* --------------------------------------------------------------------- *
     *  4. File path points to an existing directory → RuntimeException
     * --------------------------------------------------------------------- */
    @Test
    void testWriteToExistingDirectoryThrows(@TempDir Path tempDir) {
        // create a directory that we will pass as the *file* argument
        Path existingDir = tempDir.resolve("existingDir");
        assertDoesNotThrow(() -> Files.createDirectory(existingDir));
        File dirAsFile = existingDir.toFile();
        ExpressionsBasedModel model = minimalModel();
        RuntimeException re = assertThrows(RuntimeException.class, () -> model.writeTo(dirAsFile), "Writing to an existing directory should result in a RuntimeException");
        assertTrue(re.getCause() instanceof IOException, "The cause should be an IOException (FileNotFoundException)");
    }

    /* --------------------------------------------------------------------- *
     *  5. File with null parent (relative path) → file created in CWD.
     * --------------------------------------------------------------------- */
    @Test
    void testWriteToFileWithNullParentCreatesInCwd() throws IOException {
        // Use a simple relative file name; its parent is null.
        File relativeFile = new File("relativeModel.ebm");
        // Ensure a clean state before the test
        if (relativeFile.exists()) {
            assertTrue(relativeFile.delete(), "Pre‑existing test file could not be removed");
        }
        assertFalse(relativeFile.exists(), "File must not exist before test");
        ExpressionsBasedModel model = minimalModel();
        // invoke writeTo – should succeed and create the file in the current working directory
        model.writeTo(relativeFile);
        assertTrue(relativeFile.isFile(), "Relative file should have been created");
        assertTrue(relativeFile.length() > 0, "Relative file should not be empty");
        // clean up
        assertTrue(relativeFile.delete(), "Failed to delete temporary relative file");
    }

    /* --------------------------------------------------------------------- *
     *  6. Parent path is a regular file (cannot become a directory) → RuntimeException
     * --------------------------------------------------------------------- */
    @Test
    void testWriteToParentIsRegularFileThrows(@TempDir Path tempDir) throws IOException {
        // create a regular file that will later be used as the *parent* of the target file
        Path regularFile = tempDir.resolve("regularParent.txt");
        Files.write(regularFile, "I am a regular file".getBytes());
        // construct a File whose parent is the regular file (File(File parent, String child))
        File target = new File(regularFile.toFile(), "child.ebm");
        ExpressionsBasedModel model = minimalModel();
        RuntimeException re = assertThrows(RuntimeException.class, () -> model.writeTo(target), "When the parent path is a regular file, writeTo should throw RuntimeException");
        assertTrue(re.getCause() instanceof IOException, "Cause should be an IOException (most likely FileNotFoundException)");
    }

    /* --------------------------------------------------------------------- *
     *  7. SecurityManager blocks directory creation → SecurityException propagates.
     * --------------------------------------------------------------------- */
    @Test
    void testWriteToSecurityManagerBlocksMkdirs() throws IOException {
        // Skip the test on Java versions where SecurityManager is disabled by default
        Assumptions.assumeTrue(System.getSecurityManager() == null, "SecurityManager already installed – test skipped to avoid interference");
        // Prepare a directory that we will deny write access to
        Path restrictedDir = Files.createTempDirectory("restrictedDir");
        File targetFile = restrictedDir.resolve("model.ebm").toFile();
        // Install a SecurityManager that denies write permission to the restricted directory
        SecurityManager original = System.getSecurityManager();
        SecurityManager blocker = new SecurityManager() {

            @Override
            public void checkWrite(String file) {
                if (file != null && file.startsWith(restrictedDir.toAbsolutePath().toString())) {
                    throw new SecurityException("Write access denied by test SecurityManager");
                }
            }

            @Override
            public void checkPermission(java.security.Permission perm) {
                // allow everything else
            }
        };
        System.setSecurityManager(blocker);
        try {
            ExpressionsBasedModel model = minimalModel();
            // The call should propagate the SecurityException (not be wrapped)
            SecurityException se = assertThrows(SecurityException.class, () -> model.writeTo(targetFile), "SecurityException should propagate when directory creation is blocked");
            assertEquals("Write access denied by test SecurityManager", se.getMessage());
        } finally {
            // Restore the original SecurityManager to avoid side‑effects for other tests
            System.setSecurityManager(original);
        }
    }

    /* --------------------------------------------------------------------- *
     *  8. Successful write to a valid file when parent directories already exist.
     * --------------------------------------------------------------------- */
    @Test
    void testWriteToValidFileWhenParentsExist(@TempDir Path tempDir) throws IOException {
        // create the parent hierarchy beforehand
        Path parent = tempDir.resolve("already/there");
        Files.createDirectories(parent);
        File target = parent.resolve("model.ebm").toFile();
        ExpressionsBasedModel model = minimalModel();
        // should succeed without creating any new directories
        assertDoesNotThrow(() -> model.writeTo(target), "writeTo should succeed when parents exist");
        assertTrue(target.isFile(), "Target file should have been created");
        assertTrue(target.length() > 0, "File should contain data");
    }
}
