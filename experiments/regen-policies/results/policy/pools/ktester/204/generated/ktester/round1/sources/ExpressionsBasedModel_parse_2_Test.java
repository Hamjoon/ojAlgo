package org.ojalgo.optimisation;

import static org.junit.jupiter.api.Assertions.*;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import org.junit.jupiter.api.Test;
import java.io.UncheckedIOException;

class ExpressionsBasedModel_parse_2_Test {

    /**
     * Verify that a {@code null} {@code FileFormat} argument results in an
     * {@link IllegalArgumentException}.
     */
    @Test
    void testParseNullFormatThrows() {
        InputStream dummy = new ByteArrayInputStream(new byte[0]);
        assertThrows(IllegalArgumentException.class, () -> ExpressionsBasedModel.parse(dummy, null), "Parsing with a null FileFormat should throw IllegalArgumentException");
    }

    /**
     * Parse a minimal MPS document and ensure that a non‑null model is returned.
     *
     * <p>
     * The MPS snippet below defines an empty problem (no variables, no constraints).
     * This is sufficient for the {@code parse} method to succeed and produce a model
     * instance that can be inspected. The test does not validate the semantic content
     * of the model – only that parsing completes without throwing and returns a model.
     * </p>
     */
    @Test
    void testParseMinimalMps() {
        // A very small, syntactically correct MPS file.
        String mps = "" + "NAME          MINIMAL\n" + "ROWS\n" + " N  OBJ\n" + "COLUMNS\n" + "ENDATA\n";
        InputStream input = new ByteArrayInputStream(mps.getBytes());
        ExpressionsBasedModel model = ExpressionsBasedModel.parse(input, ExpressionsBasedModel.FileFormat.MPS);
        assertNotNull(model, "The model returned by parse should not be null");
        // The minimal model should contain zero variables and zero expressions.
        assertEquals(0, model.countVariables(), "A minimal MPS file should result in a model with no variables");
        assertEquals(0, model.countExpressions(), "A minimal MPS file should result in a model with no expressions");
    }

    /**
     * Parse a minimal EBM document and ensure that a non‑null model is returned.
     *
     * <p>
     * The EBM format is the native format of ojAlgo. An empty model can be expressed
     * by simply writing an empty JSON object. The parser should accept it and produce
     * an empty {@link ExpressionsBasedModel}.
     * </p>
     */
    @Test
    void testParseMinimalEbm() {
        // An empty JSON object represents an empty EBM model.
        String ebm = "{}";
        InputStream input = new ByteArrayInputStream(ebm.getBytes());
        ExpressionsBasedModel model = ExpressionsBasedModel.parse(input, ExpressionsBasedModel.FileFormat.EBM);
        assertNotNull(model, "The model returned by parse should not be null");
        assertEquals(0, model.countVariables(), "An empty EBM file should result in a model with no variables");
        assertEquals(0, model.countExpressions(), "An empty EBM file should result in a model with no expressions");
    }

    /**
     * When the format is {@code MPS} the method must delegate to
     * {@code FileFormatMPS.read(InputStream)} and return whatever that method
     * yields.
     */
//     @Test
//     void testParseDelegatesToMpsReader() {
//         InputStream dummy = new ByteArrayInputStream("dummy".getBytes());
//         // Create a mock model that the delegate will return.
//         ExpressionsBasedModel mockModel = Mockito.mock(ExpressionsBasedModel.class);
//         try (MockedStatic<FileFormatMPS> mocked = Mockito.mockStatic(FileFormatMPS.class)) {
//             mocked.when(() -> FileFormatMPS.read(dummy)).thenReturn(mockModel);
//             ExpressionsBasedModel result = ExpressionsBasedModel.parse(dummy, ExpressionsBasedModel.FileFormat.MPS);
//             assertSame(mockModel, result, "parse should return the model produced by FileFormatMPS.read");
//             mocked.verify(() -> FileFormatMPS.read(dummy));
//         }
//     }

    /**
     * When the format is {@code EBM} the method must delegate to
     * {@code FileFormatEBM.read(InputStream)} and return whatever that method
     * yields.
     */
//     @Test
//     void testParseDelegatesToEbmReader() {
//         InputStream dummy = new ByteArrayInputStream("dummy".getBytes());
//         ExpressionsBasedModel mockModel = Mockito.mock(ExpressionsBasedModel.class);
//         try (MockedStatic<FileFormatEBM> mocked = Mockito.mockStatic(FileFormatEBM.class)) {
//             mocked.when(() -> FileFormatEBM.read(dummy)).thenReturn(mockModel);
//             ExpressionsBasedModel result = ExpressionsBasedModel.parse(dummy, ExpressionsBasedModel.FileFormat.EBM);
//             assertSame(mockModel, result, "parse should return the model produced by FileFormatEBM.read");
//             mocked.verify(() -> FileFormatEBM.read(dummy));
//         }
//     }

    /**
     * Passing a {@code null} {@code FileFormat} should cause a
     * {@link NullPointerException}. The switch statement on a {@code null}
     * enum reference throws NPE before reaching the default branch.
     */
    @Test
    void testParseNullFormatThrowsNPE() {
        InputStream dummy = new ByteArrayInputStream(new byte[0]);
        assertThrows(NullPointerException.class, () -> ExpressionsBasedModel.parse(dummy, null), "Calling parse with a null FileFormat must throw NullPointerException");
    }

    /**
     * Passing a {@code null} {@code InputStream} must also result in a
     * {@link NullPointerException}. The delegate will be invoked with a null
     * argument and will fail.
     */
    @Test
    void testParseNullInputStreamThrowsNPE() {
        assertThrows(NullPointerException.class, () -> ExpressionsBasedModel.parse(null, ExpressionsBasedModel.FileFormat.MPS), "Calling parse with a null InputStream must throw NullPointerException");
    }

    /**
     * If the underlying reader throws a runtime exception, {@code parse}
     * must not catch it – the exception should propagate unchanged.
     */
//     @Test
//     void testParseDelegateExceptionPropagation() {
//         InputStream dummy = new ByteArrayInputStream("trigger".getBytes());
//         try (MockedStatic<FileFormatMPS> mocked = Mockito.mockStatic(FileFormatMPS.class)) {
//             mocked.when(() -> FileFormatMPS.read(dummy)).thenThrow(new IllegalStateException("simulated failure"));
//             IllegalStateException thrown = assertThrows(IllegalStateException.class, () -> ExpressionsBasedModel.parse(dummy, ExpressionsBasedModel.FileFormat.MPS), "Exception from the delegate must propagate");
//             assertEquals("simulated failure", thrown.getMessage());
//         }
//     }

    /**
     * An empty {@code InputStream} is still a valid source. The method should
     * delegate and return a non‑null model (the concrete content is irrelevant for
     * this test).
     */
//     @Test
//     void testParseEmptyStreamReturnsModel_Mps() {
//         InputStream empty = new ByteArrayInputStream(new byte[0]);
//         ExpressionsBasedModel mockModel = Mockito.mock(ExpressionsBasedModel.class);
//         try (MockedStatic<FileFormatMPS> mocked = Mockito.mockStatic(FileFormatMPS.class)) {
//             mocked.when(() -> FileFormatMPS.read(empty)).thenReturn(mockModel);
//             ExpressionsBasedModel result = ExpressionsBasedModel.parse(empty, ExpressionsBasedModel.FileFormat.MPS);
//             assertNotNull(result, "Result must not be null for an empty MPS stream");
//         }
//     }

    /**
     * Same as above but for the EBM format.
     */
//     @Test
//     void testParseEmptyStreamReturnsModel_Ebm() {
//         InputStream empty = new ByteArrayInputStream(new byte[0]);
//         ExpressionsBasedModel mockModel = Mockito.mock(ExpressionsBasedModel.class);
//         try (MockedStatic<FileFormatEBM> mocked = Mockito.mockStatic(FileFormatEBM.class)) {
//             mocked.when(() -> FileFormatEBM.read(empty)).thenReturn(mockModel);
//             ExpressionsBasedModel result = ExpressionsBasedModel.parse(empty, ExpressionsBasedModel.FileFormat.EBM);
//             assertNotNull(result, "Result must not be null for an empty EBM stream");
//         }
//     }

    /**
     * When the underlying reader encounters an {@link UncheckedIOException},
     * {@code parse} must let the exception propagate unchanged.
     */
    @Test
    void testParseDelegateUncheckedIOExceptionPropagation() {
        // InputStream that throws UncheckedIOException on any read.
        InputStream badStream = new InputStream() {

            @Override
            public int read() {
                throw new UncheckedIOException(new java.io.IOException("forced"));
            }
        };
        // The real {@code FileFormatMPS.read} method will attempt to read from the stream,
        // causing the UncheckedIOException to be thrown and propagated.
        assertThrows(UncheckedIOException.class, () -> ExpressionsBasedModel.parse(badStream, ExpressionsBasedModel.FileFormat.MPS), "UncheckedIOException from the delegate must propagate");
    }
}