package org.ojalgo.optimisation;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.math.BigDecimal;
import java.util.stream.Stream;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ExpressionsBasedModel_isShallowCopy_Test {

    private ExpressionsBasedModel originalModel;

    private ExpressionsBasedModel shallowCopy;

    private ExpressionsBasedModel deepCopy;

    private ExpressionsBasedModel shallowCopyOfShallow;

    @BeforeEach
    void setupBeforeEach() {
        // Build a minimal model – a single variable and a single expression
        originalModel = new ExpressionsBasedModel();
        // default variable
        Variable var = originalModel.addVariable();
        // default expression
        Expression expr = originalModel.addExpression();
        // give the expression a term
        expr.set(var, BigMath.ONE);
        // Create a shallow copy (shallow = true, prune = false)
        shallowCopy = new ExpressionsBasedModel(originalModel, true, false);
        // Create a deep copy (shallow = false, prune = false)
        deepCopy = new ExpressionsBasedModel(originalModel, false, false);
        // Create a copy of a shallow copy without requesting a shallow copy
        // The constructor should propagate the shallow flag from the source model
        shallowCopyOfShallow = new ExpressionsBasedModel(shallowCopy, false, false);
    }

    @AfterEach
    void teardownAfterEach() {
        // Help GC – not strictly required but keeps the test tidy
        originalModel = null;
        shallowCopy = null;
        deepCopy = null;
        shallowCopyOfShallow = null;
    }

    /**
     * Supplies the different copy‑method scenarios together with the expected
     * {@code isShallowCopy()} result.
     *
     * <p>The {@code Supplier<ExpressionsBasedModel>} creates the copy from {@code originalModel}
     * using the appropriate overload.</p>
     */
    private static Stream<Arguments> copyMethodProvider() {
        return Stream.of(// copy() – no parameters, should be non‑shallow
        Arguments.of(// description
        "copy()", (java.util.function.Supplier<ExpressionsBasedModel>) () -> originalModel.copy(), false), // copy(true) – relax flag only, still non‑shallow
        Arguments.of("copy(true) – relax flag", (java.util.function.Supplier<ExpressionsBasedModel>) () -> originalModel.copy(true), false), // copy(true, false) – shallow = true, prune = false → shallow copy
        Arguments.of("copy(true, false) – shallow true, prune false", (java.util.function.Supplier<ExpressionsBasedModel>) () -> originalModel.copy(true, false), true), // copy(true, true) – shallow = true, prune = true → shallow copy
        Arguments.of("copy(true, true) – shallow true, prune true", (java.util.function.Supplier<ExpressionsBasedModel>) () -> originalModel.copy(true, true), true), // copy(false, true) – shallow = false, prune = true → non‑shallow copy
        Arguments.of("copy(false, true) – shallow false, prune true", (java.util.function.Supplier<ExpressionsBasedModel>) () -> originalModel.copy(false, true), false));
    }

    @Test
    void testOriginalModelIsNotShallowCopy() {
        assertFalse(originalModel.isShallowCopy(), "A freshly constructed model must not be flagged as a shallow copy.");
    }

    @Test
    void testShallowCopyIsFlagged() {
        assertTrue(shallowCopy.isShallowCopy(), "A model created with shallow = true must report isShallowCopy() == true.");
    }

    @Test
    void testDeepCopyIsNotShallow() {
        assertFalse(deepCopy.isShallowCopy(), "A model created with shallow = false must report isShallowCopy() == false.");
    }

    @Test
    void testShallowFlagPropagatesFromSource() {
        // The source (shallowCopy) is already a shallow copy; even though we pass shallow = false,
        // the constructor ORs the flag with the source's flag, so the new model must still be shallow.
        assertTrue(shallowCopyOfShallow.isShallowCopy(), "A copy of a shallow model must retain the shallow flag even when shallow = false.");
    }

    /* --------------------------------------------------------------
     *  1. Default construction
     * -------------------------------------------------------------- */
    @Test
    void testDefaultConstructionIsNotShallow() {
        // A model created with the public default constructor must report false.
        assertFalse(originalModel.isShallowCopy(), "A freshly constructed model must not be flagged as a shallow copy.");
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("copyMethodProvider")
    void testCopyMethodsShallowFlag(String description, java.util.function.Supplier<ExpressionsBasedModel> copySupplier, boolean expectedShallow) {
        // Obtain the copy using the supplied lambda
        ExpressionsBasedModel copy = copySupplier.get();
        // Verify the shallow flag matches the expectation
        assertEquals(expectedShallow, copy.isShallowCopy(), () -> String.format("Copy method %s must return isShallowCopy()==%s", description, expectedShallow));
    }

    /* --------------------------------------------------------------
     *  3. Original model unchanged after shallow copy
     * -------------------------------------------------------------- */
    @Test
    void testOriginalModelRemainsNonShallowAfterShallowCopy() {
        // Create a shallow copy via the (shallow, prune) overload
        ExpressionsBasedModel shallowCopy = originalModel.copy(true, false);
        // The shallow copy reports true …
        assertTrue(shallowCopy.isShallowCopy(), "The shallow copy must report isShallowCopy()==true.");
        // … but the source model must stay false.
        assertFalse(originalModel.isShallowCopy(), "The original model must remain non‑shallow after a shallow copy is created.");
    }
}
