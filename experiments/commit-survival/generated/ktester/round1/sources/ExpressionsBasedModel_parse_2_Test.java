package org.ojalgo.optimisation;

import static org.junit.jupiter.api.Assertions.*;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

@ExtendWith(MockitoExtension.class)
class ExpressionsBasedModel_parse_2_Test {

    /**
     * Mocked static for {@code FileFormatMPS.read(...)}.
     */
    private MockedStatic<FileFormatMPS> mpsMock;

    /**
     * Mocked static for {@code FileFormatEBM.read(...)}.
     */
    private MockedStatic<FileFormatEBM> ebmMock;

    @BeforeEach
    void setupBeforeEach() {
        // Create fresh static mocks before each test so that interactions are isolated
        mpsMock = mockStatic(FileFormatMPS.class);
        ebmMock = mockStatic(FileFormatEBM.class);
    }

    @AfterEach
    void teardownAfterEach() {
        // Release the static mocks – otherwise they leak into other tests
        if (mpsMock != null) {
            mpsMock.close();
        }
        if (ebmMock != null) {
            ebmMock.close();
        }
    }

    /**
     * Supplies arguments for {@link #testParseDelegatesForNonEmptyContent}.
     */
    private static Stream<Arguments> provideNonEmptyStreams() {
        ExpressionsBasedModel dummyMpsModel = new ExpressionsBasedModel();
        ExpressionsBasedModel dummyEbmModel = new ExpressionsBasedModel();
        InputStream mpsStream = new ByteArrayInputStream("* MPS dummy content".getBytes(StandardCharsets.UTF_8));
        InputStream ebmStream = new ByteArrayInputStream("* EBM dummy content".getBytes(StandardCharsets.UTF_8));
        return Stream.of(Arguments.of(ExpressionsBasedModel.FileFormat.MPS, mpsStream, dummyMpsModel), Arguments.of(ExpressionsBasedModel.FileFormat.EBM, ebmStream, dummyEbmModel));
    }

    @Test
    void testParseMpsDelegatesToFileFormatMPS() throws Exception {
        InputStream dummyInput = new ByteArrayInputStream(new byte[0]);
        ExpressionsBasedModel expectedModel = new ExpressionsBasedModel();
        mpsMock.when(() -> FileFormatMPS.read(dummyInput)).thenReturn(expectedModel);
        ExpressionsBasedModel actual = ExpressionsBasedModel.parse(dummyInput, ExpressionsBasedModel.FileFormat.MPS);
        assertSame(expectedModel, actual, "The returned model should be the one supplied by FileFormatMPS.read");
        mpsMock.verify(() -> FileFormatMPS.read(dummyInput), times(1));
        ebmMock.verifyNoInteractions();
    }

    @Test
    void testParseEbmDelegatesToFileFormatEBM() throws Exception {
        InputStream dummyInput = new ByteArrayInputStream(new byte[0]);
        ExpressionsBasedModel expectedModel = new ExpressionsBasedModel();
        ebmMock.when(() -> FileFormatEBM.read(dummyInput)).thenReturn(expectedModel);
        ExpressionsBasedModel actual = ExpressionsBasedModel.parse(dummyInput, ExpressionsBasedModel.FileFormat.EBM);
        assertSame(expectedModel, actual, "The returned model should be the one supplied by FileFormatEBM.read");
        ebmMock.verify(() -> FileFormatEBM.read(dummyInput), times(1));
        mpsMock.verifyNoInteractions();
    }

    @Test
    void testParseNullFormatThrowsException() {
        InputStream dummyInput = new ByteArrayInputStream(new byte[0]);
        // The exact exception type depends on the JDK version (null switch throws NPE)
        assertThrows(RuntimeException.class, () -> ExpressionsBasedModel.parse(dummyInput, null), "Parsing with a null format should throw a RuntimeException");
    }

    @Test
    void testParseMpsWithNullInputStreamThrowsNPE() {
        assertThrows(NullPointerException.class, () -> ExpressionsBasedModel.parse(null, ExpressionsBasedModel.FileFormat.MPS), "Parsing a null InputStream for MPS should throw NPE");
    }

    @Test
    void testParseEbmWithNullInputStreamThrowsNPE() {
        assertThrows(NullPointerException.class, () -> ExpressionsBasedModel.parse(null, ExpressionsBasedModel.FileFormat.EBM), "Parsing a null InputStream for EBM should throw NPE");
    }

    @Test
    void testParseMpsWithEmptyInputStreamReturnsModel() {
        InputStream emptyInput = new ByteArrayInputStream(new byte[0]);
        ExpressionsBasedModel model = ExpressionsBasedModel.parse(emptyInput, ExpressionsBasedModel.FileFormat.MPS);
        assertNotNull(model, "Parsing an empty MPS stream should return a non‑null model");
    }

    @Test
    void testParseEbmWithEmptyInputStreamReturnsModel() {
        InputStream emptyInput = new ByteArrayInputStream(new byte[0]);
        ExpressionsBasedModel model = ExpressionsBasedModel.parse(emptyInput, ExpressionsBasedModel.FileFormat.EBM);
        assertNotNull(model, "Parsing an empty EBM stream should return a non‑null model");
    }

    @ParameterizedTest(name = "parse({0}) delegates correctly")
    @MethodSource("provideNonEmptyStreams")
    void testParseDelegatesForNonEmptyContent(ExpressionsBasedModel.FileFormat format, InputStream stream, ExpressionsBasedModel expectedModel) {
        if (format == ExpressionsBasedModel.FileFormat.MPS) {
            mpsMock.when(() -> FileFormatMPS.read(stream)).thenReturn(expectedModel);
        } else {
            ebmMock.when(() -> FileFormatEBM.read(stream)).thenReturn(expectedModel);
        }
        ExpressionsBasedModel actual = ExpressionsBasedModel.parse(stream, format);
        assertSame(expectedModel, actual, () -> "Model returned for format " + format + " should be the mock's result");
        if (format == ExpressionsBasedModel.FileFormat.MPS) {
            mpsMock.verify(() -> FileFormatMPS.read(stream), times(1));
            ebmMock.verifyNoInteractions();
        } else {
            ebmMock.verify(() -> FileFormatEBM.read(stream), times(1));
            mpsMock.verifyNoInteractions();
        }
    }

    @Test
    void testParseEmptyStreamReturnsModel() {
        InputStream emptyInput = new ByteArrayInputStream(new byte[0]);
        // MPS format
        ExpressionsBasedModel mpsModel = ExpressionsBasedModel.parse(emptyInput, ExpressionsBasedModel.FileFormat.MPS);
        assertNotNull(mpsModel, "Parsing an empty MPS stream should return a model");
        // Reset stream for the next call
        emptyInput = new ByteArrayInputStream(new byte[0]);
        // EBM format
        ExpressionsBasedModel ebmModel = ExpressionsBasedModel.parse(emptyInput, ExpressionsBasedModel.FileFormat.EBM);
        assertNotNull(ebmModel, "Parsing an empty EBM stream should return a model");
    }

    @Test
    void testParseEmptyStreamReturnsModelForBothFormats() {
        InputStream emptyInput = new ByteArrayInputStream(new byte[0]);
        // MPS format
        ExpressionsBasedModel mpsModel = ExpressionsBasedModel.parse(emptyInput, ExpressionsBasedModel.FileFormat.MPS);
        assertNotNull(mpsModel, "Parsing an empty MPS stream should return a model");
        // Reset stream for the next call
        emptyInput = new ByteArrayInputStream(new byte[0]);
        // EBM format
        ExpressionsBasedModel ebmModel = ExpressionsBasedModel.parse(emptyInput, ExpressionsBasedModel.FileFormat.EBM);
        assertNotNull(ebmModel, "Parsing an empty EBM stream should return a model");
    }
}
