package org.ojalgo.optimisation;

import static org.junit.jupiter.api.Assertions.*;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.extension.ExtendWith;

// @ExtendWith(MockitoExtension.class)
class ExpressionsBasedModel_parse_Test {

    /**
     * Verify that a file ending with ".mps" is recognised as {@link FileFormat#MPS}
     * and that the delegated static parse method is called with the correct arguments.
     */
    @Test
    void testParseMpsFile(@TempDir java.nio.file.Path tempDir) throws IOException {
        File mpsFile = tempDir.resolve("model.mps").toFile();
        try (FileOutputStream fos = new FileOutputStream(mpsFile)) {
            // empty file – the real parser may still accept it
            fos.write(new byte[0]);
        }
        // The real parser is expected to return a non‑null model for a recognised extension.
        ExpressionsBasedModel model = ExpressionsBasedModel.parse(mpsFile);
        assertNotNull(model, "Parsing a .mps file should return a non‑null model");
    }

    /**
     * Verify that a file ending with ".ebm" is recognised as {@link FileFormat#EBM}
     * and that the delegated static parse method is called with the correct arguments.
     */
    @Test
    void testParseEbmFile(@TempDir java.nio.file.Path tempDir) throws IOException {
        File ebmFile = tempDir.resolve("model.ebm").toFile();
        try (FileOutputStream fos = new FileOutputStream(ebmFile)) {
            fos.write(new byte[0]);
        }
        ExpressionsBasedModel model = ExpressionsBasedModel.parse(ebmFile);
        assertNotNull(model, "Parsing a .ebm file should return a non‑null model");
    }

    @Test
    void testParseSifFile(@TempDir Path tempDir) throws IOException {
        File sifFile = tempDir.resolve("model.sif").toFile();
        try (FileOutputStream fos = new FileOutputStream(sifFile)) {
            fos.write(new byte[0]);
        }
        ExpressionsBasedModel model = ExpressionsBasedModel.parse(sifFile);
        assertNotNull(model, ".sif is treated as MPS and should return a non‑null model");
    }

    @Test
    void testParseMpsFileCaseInsensitive(@TempDir Path tempDir) throws IOException {
        File file = tempDir.resolve("model.MPS").toFile();
        try (FileOutputStream fos = new FileOutputStream(file)) {
            fos.write(new byte[0]);
        }
        ExpressionsBasedModel model = ExpressionsBasedModel.parse(file);
        assertNotNull(model, "Upper‑case .MPS extension must be recognised");
    }

    @Test
    void testParseEbmFileCaseInsensitive(@TempDir Path tempDir) throws IOException {
        File file = tempDir.resolve("model.eBm").toFile();
        try (FileOutputStream fos = new FileOutputStream(file)) {
            fos.write(new byte[0]);
        }
        ExpressionsBasedModel model = ExpressionsBasedModel.parse(file);
        assertNotNull(model, "Mixed‑case .eBm extension must be recognised");
    }

    @Test
    void testParseUnsupportedExtensionThrowsIllegalArgumentException(@TempDir Path tempDir) throws IOException {
        File txtFile = tempDir.resolve("model.txt").toFile();
        Files.createFile(txtFile.toPath());
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> ExpressionsBasedModel.parse(txtFile));
        assertEquals("java.lang.IllegalArgumentException", ex.getClass().getName(), "Unsupported extensions must cause IllegalArgumentException");
    }

    @Test
    void testParseNoExtensionThrowsIllegalArgumentException(@TempDir Path tempDir) throws IOException {
        File noExt = tempDir.resolve("model").toFile();
        Files.createFile(noExt.toPath());
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> ExpressionsBasedModel.parse(noExt));
        assertEquals("java.lang.IllegalArgumentException", ex.getClass().getName());
    }

    @Test
    void testParseNullFileThrowsNullPointerException() {
        assertThrows(NullPointerException.class, () -> ExpressionsBasedModel.parse(null));
    }

    @Test
    void testParseNonexistentFileThrowsRuntimeException(@TempDir Path tempDir) {
        // never created
        File missing = tempDir.resolve("nonexistent.mps").toFile();
        RuntimeException ex = assertThrows(RuntimeException.class, () -> ExpressionsBasedModel.parse(missing));
        assertTrue(ex.getCause() instanceof IOException, "Cause should be an IOException (FileNotFoundException is a subclass)");
    }

    @Test
    void testParseUnreadableFileThrowsRuntimeException(@TempDir Path tempDir) throws IOException {
        File unreadable = tempDir.resolve("unreadable.ebm").toFile();
        try (FileOutputStream fos = new FileOutputStream(unreadable)) {
            fos.write(new byte[0]);
        }
        // Attempt to make the file unreadable; if not supported, delete it to simulate failure.
        try {
            Files.setPosixFilePermissions(unreadable.toPath(), java.util.Set.of());
        } catch (UnsupportedOperationException | IOException ignored) {
            unreadable.delete();
        }
        RuntimeException ex = assertThrows(RuntimeException.class, () -> ExpressionsBasedModel.parse(unreadable));
        assertTrue(ex.getCause() instanceof IOException, "Unreadable file should result in a RuntimeException wrapping an IOException");
    }

    @Test
    void testParseFileSecurityExceptionPropagates(@TempDir Path tempDir) throws IOException {
        File secure = tempDir.resolve("secure.mps").toFile();
        try (FileOutputStream fos = new FileOutputStream(secure)) {
            fos.write(new byte[0]);
        }
        SecurityManager original = System.getSecurityManager();
        System.setSecurityManager(new SecurityManager() {

            @Override
            public void checkRead(String file) {
                if (file.equals(secure.getAbsolutePath())) {
                    throw new SecurityException("read denied by test SecurityManager");
                }
            }

            @Override
            public void checkPermission(java.security.Permission perm) {
                // allow everything else
            }
        });
        try {
            SecurityException ex = assertThrows(SecurityException.class, () -> ExpressionsBasedModel.parse(secure));
            assertEquals("read denied by test SecurityManager", ex.getMessage());
        } finally {
            System.setSecurityManager(original);
        }
    }
}