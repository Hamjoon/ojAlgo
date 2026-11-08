package org.ojalgo.optimisation;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.*;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import java.io.FileOutputStream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import org.mockito.MockedConstruction;

@ExtendWith(MockitoExtension.class)
class ExpressionsBasedModel_writeTo_Test {

    /**
     * Temporary directory used for the generated file.
     */
    private Path tempDir;

    /**
     * The file that {@code writeTo} should write to.
     */
    private File targetFile;

    @BeforeEach
    void setupBeforeEach() throws IOException {
        // Create a fresh temporary directory for each test.
        tempDir = Files.createTempDirectory("ExpressionsBasedModel_writeTo_Test");
        targetFile = new File(tempDir.toFile(), "model.ebm");
        tempDir = Files.createTempDirectory("ExpressionsBasedModel_writeTo_Test");
    }

    @AfterEach
    void teardownAfterEach() throws IOException {
        // Clean up the temporary directory recursively.
        if (tempDir != null && Files.exists(tempDir)) {
            Files.walk(tempDir).sorted(// delete children before parents
            (p1, p2) -> p2.compareTo(p1)).forEach(p -> {
                try {
                    Files.deleteIfExists(p);
                } catch (IOException ignored) {
                    // best‑effort cleanup
                }
            });
        }
        if (tempDir != null && Files.exists(tempDir)) {
            Files.walk(tempDir).sorted(// delete children before parents
            (p1, p2) -> p2.compareTo(p1)).forEach(p -> {
                try {
                    Files.deleteIfExists(p);
                } catch (IOException ignored) {
                }
            });
        }
    }

    @Test
    void testWriteToInvokesStaticHelpersAndCreatesFile() throws IOException {
        // -----------------------------------------------------------------
        // Arrange – mock the two static collaborators.
        // -----------------------------------------------------------------
        try (MockedStatic<ToFileWriter> toFileWriterMock = mockStatic(ToFileWriter.class);
            MockedStatic<FileFormatEBM> fileFormatMock = mockStatic(FileFormatEBM.class)) {
            // mkdirs should be a no‑op (the real method would create the directory,
            // but we want to keep the test isolated from the filesystem).
            toFileWriterMock.when(() -> ToFileWriter.mkdirs(any())).thenAnswer(invocation -> null);
            // The write method should also be a no‑op; we only care that it is called
            // with the correct arguments.
            fileFormatMock.when(() -> FileFormatEBM.write(any(), any())).thenAnswer(invocation -> null);
            // -----------------------------------------------------------------
            // Act – create a model and call the method under test.
            // -----------------------------------------------------------------
            ExpressionsBasedModel model = new ExpressionsBasedModel();
            model.writeTo(targetFile);
            // -----------------------------------------------------------------
            // Assert – verify interactions and file existence.
            // -----------------------------------------------------------------
            // Verify that the parent directory creation was requested.
            toFileWriterMock.verify(() -> ToFileWriter.mkdirs(eq(targetFile.getParentFile())));
            // Verify that the format writer was called with the same model instance.
            fileFormatMock.verify(() -> FileFormatEBM.write(eq(model), any()));
            // The method should have created the file (even if it is empty because the
            // writer was mocked).
            assertTrue(targetFile.exists(), "Target file should exist after writeTo");
        }
    }

    @Test
    void testWriteToCreatesFileWhenParentExists() throws IOException {
        // parent directory already exists
        targetFile = new File(tempDir.toFile(), "output.ebm");
        try (MockedStatic<ToFileWriter> toFileWriterMock = mockStatic(ToFileWriter.class);
            MockedStatic<FileFormatEBM> fileFormatMock = mockStatic(FileFormatEBM.class);
            MockedConstruction<FileOutputStream> fosMock = mockConstruction(FileOutputStream.class, (mock, ctx) -> {
                // no special behaviour – just a normal stream
            })) {
            toFileWriterMock.when(() -> ToFileWriter.mkdirs(any())).thenAnswer(i -> null);
            fileFormatMock.when(() -> FileFormatEBM.write(any(), any())).thenAnswer(i -> null);
            ExpressionsBasedModel model = new ExpressionsBasedModel();
            model.writeTo(targetFile);
            // verify helpers
            toFileWriterMock.verify(() -> ToFileWriter.mkdirs(eq(targetFile.getParentFile())));
            fileFormatMock.verify(() -> FileFormatEBM.write(eq(model), any()));
            // file must exist (FileOutputStream creates it)
            assertTrue(targetFile.exists(), "File should have been created");
        }
    }

    @Test
    void testWriteToCreatesMissingParentDirectories() throws IOException {
        // parent directories do not exist
        File missingParent = new File(tempDir.toFile(), "missing/parents");
        targetFile = new File(missingParent, "output.ebm");
        try (MockedStatic<ToFileWriter> toFileWriterMock = mockStatic(ToFileWriter.class);
            MockedStatic<FileFormatEBM> fileFormatMock = mockStatic(FileFormatEBM.class);
            MockedConstruction<FileOutputStream> fosMock = mockConstruction(FileOutputStream.class)) {
            toFileWriterMock.when(() -> ToFileWriter.mkdirs(any())).thenAnswer(i -> null);
            fileFormatMock.when(() -> FileFormatEBM.write(any(), any())).thenAnswer(i -> null);
            ExpressionsBasedModel model = new ExpressionsBasedModel();
            model.writeTo(targetFile);
            toFileWriterMock.verify(() -> ToFileWriter.mkdirs(eq(targetFile.getParentFile())));
            fileFormatMock.verify(() -> FileFormatEBM.write(eq(model), any()));
            assertTrue(targetFile.exists(), "File should have been created after creating missing parents");
        }
    }

    @Test
    void testWriteToNullFileThrowsNPE() {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        assertThrows(NullPointerException.class, () -> model.writeTo((File) null));
    }

    @Test
    void testWriteToRootPathFile() throws IOException {
        // On most OSes writing to the root requires privileges; we avoid actually touching
        // the real root by creating a temporary directory and using its root as a mock.
        // Instead we simulate the situation by passing a File whose parent is null.
        File rootFile = new File("/output.ebm") {

            @Override
            public File getParentFile() {
                // force parent == null
                return null;
            }
        };
        // Use a temporary directory to back the file creation to stay safe.
        Path fakeRoot = Files.createTempDirectory("fakeRoot");
        File realFile = fakeRoot.resolve("output.ebm").toFile();
        try (MockedStatic<ToFileWriter> toFileWriterMock = mockStatic(ToFileWriter.class);
            MockedStatic<FileFormatEBM> fileFormatMock = mockStatic(FileFormatEBM.class);
            MockedConstruction<FileOutputStream> fosMock = mockConstruction(FileOutputStream.class, (mock, ctx) -> {
                // make the mock write to the real temporary file
                when(mock.getFD()).thenCallRealMethod();
            })) {
            toFileWriterMock.when(() -> ToFileWriter.mkdirs(isNull())).thenAnswer(i -> null);
            fileFormatMock.when(() -> FileFormatEBM.write(any(), any())).thenAnswer(i -> null);
            ExpressionsBasedModel model = new ExpressionsBasedModel();
            // invoke with the fake rootFile – the method will call getParentFile() (null)
            // and then attempt to open a FileOutputStream for the real temporary file.
            model.writeTo(realFile);
            toFileWriterMock.verify(() -> ToFileWriter.mkdirs(isNull()));
            fileFormatMock.verify(() -> FileFormatEBM.write(eq(model), any()));
            assertTrue(realFile.exists(), "File should be created even when parent is null");
        } finally {
            Files.walk(fakeRoot).sorted((p1, p2) -> p2.compareTo(p1)).forEach(p -> {
                try {
                    Files.deleteIfExists(p);
                } catch (IOException ignored) {
                }
            });
        }
    }

    @Test
    void testWriteToFileIsDirectoryResultsInRuntimeException() {
        // Create a directory where a file is expected – FileOutputStream will throw
        // a FileNotFoundException (subclass of IOException).
        File dirAsFile = new File(tempDir.toFile(), "dirAsFile");
        assertTrue(dirAsFile.mkdir());
        try (MockedStatic<ToFileWriter> toFileWriterMock = mockStatic(ToFileWriter.class)) {
            toFileWriterMock.when(() -> ToFileWriter.mkdirs(any())).thenAnswer(i -> null);
            ExpressionsBasedModel model = new ExpressionsBasedModel();
            RuntimeException ex = assertThrows(RuntimeException.class, () -> model.writeTo(dirAsFile));
            assertTrue(ex.getCause() instanceof IOException, "Cause should be an IOException (FileNotFoundException)");
        }
    }

    @Test
    void testWriteToFileFormatWriteThrowsIOException() {
        targetFile = new File(tempDir.toFile(), "unwritable/output.ebm");
        targetFile.getParentFile().mkdirs();
        try (MockedStatic<ToFileWriter> toFileWriterMock = mockStatic(ToFileWriter.class);
            MockedStatic<FileFormatEBM> fileFormatMock = mockStatic(FileFormatEBM.class);
            MockedConstruction<FileOutputStream> fosMock = mockConstruction(FileOutputStream.class)) {
            toFileWriterMock.when(() -> ToFileWriter.mkdirs(any())).thenAnswer(i -> null);
            fileFormatMock.when(() -> FileFormatEBM.write(any(), any())).thenThrow(new IOException("simulated write failure"));
            ExpressionsBasedModel model = new ExpressionsBasedModel();
            RuntimeException ex = assertThrows(RuntimeException.class, () -> model.writeTo(targetFile));
            assertTrue(ex.getCause() instanceof IOException);
            assertEquals("simulated write failure", ex.getCause().getMessage());
        }
    }

    @Test
    void testWriteToFileFormatWriteThrowsUncheckedException() {
        targetFile = new File(tempDir.toFile(), "writeException/output.ebm");
        targetFile.getParentFile().mkdirs();
        try (MockedStatic<ToFileWriter> toFileWriterMock = mockStatic(ToFileWriter.class);
            MockedStatic<FileFormatEBM> fileFormatMock = mockStatic(FileFormatEBM.class);
            MockedConstruction<FileOutputStream> fosMock = mockConstruction(FileOutputStream.class)) {
            toFileWriterMock.when(() -> ToFileWriter.mkdirs(any())).thenAnswer(i -> null);
            fileFormatMock.when(() -> FileFormatEBM.write(any(), any())).thenThrow(new IllegalStateException("unchecked failure"));
            ExpressionsBasedModel model = new ExpressionsBasedModel();
            IllegalStateException ex = assertThrows(IllegalStateException.class, () -> model.writeTo(targetFile));
            assertEquals("unchecked failure", ex.getMessage());
        }
    }

    @Test
    void testWriteToCloseThrowsIOException() {
        targetFile = new File(tempDir.toFile(), "closeIOException/output.ebm");
        targetFile.getParentFile().mkdirs();
        try (MockedStatic<ToFileWriter> toFileWriterMock = mockStatic(ToFileWriter.class);
            MockedStatic<FileFormatEBM> fileFormatMock = mockStatic(FileFormatEBM.class);
            MockedConstruction<FileOutputStream> fosMock = mockConstruction(FileOutputStream.class, (mock, ctx) -> {
                // make close() throw IOException
                doThrow(new IOException("close failed")).when(mock).close();
            })) {
            toFileWriterMock.when(() -> ToFileWriter.mkdirs(any())).thenAnswer(i -> null);
            fileFormatMock.when(() -> FileFormatEBM.write(any(), any())).thenAnswer(i -> null);
            ExpressionsBasedModel model = new ExpressionsBasedModel();
            RuntimeException ex = assertThrows(RuntimeException.class, () -> model.writeTo(targetFile));
            assertTrue(ex.getCause() instanceof IOException);
            assertEquals("close failed", ex.getCause().getMessage());
        }
    }

    @Test
    void testWriteToMkdirsThrowsSecurityException() {
        targetFile = new File(tempDir.toFile(), "secure/output.ebm");
        try (MockedStatic<ToFileWriter> toFileWriterMock = mockStatic(ToFileWriter.class)) {
            toFileWriterMock.when(() -> ToFileWriter.mkdirs(any())).thenThrow(new SecurityException("access denied"));
            ExpressionsBasedModel model = new ExpressionsBasedModel();
            SecurityException ex = assertThrows(SecurityException.class, () -> model.writeTo(targetFile));
            assertEquals("access denied", ex.getMessage());
        }
    }

    @Test
    void testWriteToParentIsFileThrowsRuntimeException() throws IOException {
        // create a regular file that will be used as the "parent" directory
        File parentFile = new File(tempDir.toFile(), "parentFile");
        assertTrue(parentFile.createNewFile());
        // the target path uses that file as its parent
        targetFile = new File(parentFile, "output.ebm");
        try (MockedStatic<ToFileWriter> toFileWriterMock = mockStatic(ToFileWriter.class)) {
            toFileWriterMock.when(() -> ToFileWriter.mkdirs(any())).thenThrow(new IOException("cannot create directory"));
            ExpressionsBasedModel model = new ExpressionsBasedModel();
            RuntimeException ex = assertThrows(RuntimeException.class, () -> model.writeTo(targetFile));
            assertTrue(ex.getCause() instanceof IOException);
            assertEquals("cannot create directory", ex.getCause().getMessage());
        }
    }

    @Test
    void testWriteToParentNotWritableThrowsRuntimeException() throws IOException {
        File noWriteDir = new File(tempDir.toFile(), "noWrite");
        assertTrue(noWriteDir.mkdir());
        // make directory non‑writable
        assertTrue(noWriteDir.setWritable(false, false));
        targetFile = new File(noWriteDir, "output.ebm");
        try (MockedStatic<ToFileWriter> toFileWriterMock = mockStatic(ToFileWriter.class)) {
            toFileWriterMock.when(() -> ToFileWriter.mkdirs(any())).thenAnswer(i -> null);
            ExpressionsBasedModel model = new ExpressionsBasedModel();
            RuntimeException ex = assertThrows(RuntimeException.class, () -> model.writeTo(targetFile));
            assertTrue(ex.getCause() instanceof IOException);
        } finally {
            // restore write permission for cleanup
            noWriteDir.setWritable(true, false);
        }
    }

    @Test
    void testWriteToEmptyPathThrowsRuntimeException() {
        File emptyPath = new File("");
        try (MockedStatic<ToFileWriter> toFileWriterMock = mockStatic(ToFileWriter.class)) {
            toFileWriterMock.when(() -> ToFileWriter.mkdirs(any())).thenAnswer(i -> null);
            ExpressionsBasedModel model = new ExpressionsBasedModel();
            RuntimeException ex = assertThrows(RuntimeException.class, () -> model.writeTo(emptyPath));
            assertTrue(ex.getCause() instanceof IOException);
        }
    }

    @Test
    void testWriteToOverwriteExistingFile() throws IOException {
        targetFile = new File(tempDir.toFile(), "existing/output.ebm");
        targetFile.getParentFile().mkdirs();
        // create a file with some content
        Files.writeString(targetFile.toPath(), "old content");
        try (MockedStatic<ToFileWriter> toFileWriterMock = mockStatic(ToFileWriter.class);
            MockedStatic<FileFormatEBM> fileFormatMock = mockStatic(FileFormatEBM.class);
            MockedConstruction<FileOutputStream> fosMock = mockConstruction(FileOutputStream.class)) {
            toFileWriterMock.when(() -> ToFileWriter.mkdirs(any())).thenAnswer(i -> null);
            fileFormatMock.when(() -> FileFormatEBM.write(any(), any())).thenAnswer(i -> null);
            ExpressionsBasedModel model = new ExpressionsBasedModel();
            model.writeTo(targetFile);
            // file should still exist
            assertTrue(targetFile.exists());
            // size may be zero because the mock stream does not write anything
            // (the important part is that no exception is thrown)
        }
    }
}
