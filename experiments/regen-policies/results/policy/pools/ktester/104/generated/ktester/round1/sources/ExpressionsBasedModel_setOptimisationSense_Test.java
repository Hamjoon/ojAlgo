package org.ojalgo.optimisation;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.lang.reflect.Method;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@ExtendWith(MockitoExtension.class)
class ExpressionsBasedModel_setOptimisationSense_Test {

    private ExpressionsBasedModel model;

    @BeforeEach
    void setupBeforeEach() {
        // a fresh model for each test
        model = new ExpressionsBasedModel();
    }

    private static Stream<Arguments> basicEnumProvider() {
        return Stream.of(// corresponds to MINIMIZE
        Arguments.of(Optimisation.Sense.MIN), // corresponds to MAXIMIZE
        Arguments.of(Optimisation.Sense.MAX));
    }

    @Test
    void testSetOptimisationSenseDirectlyToMax() {
        // default sense is null
        assertNull(model.getOptimisationSense(), "Initial optimisation sense should be null");
        // invoke the package‑private method (accessible because the test is in the same package)
        model.setOptimisationSense(Optimisation.Sense.MAX);
        assertEquals(Optimisation.Sense.MAX, model.getOptimisationSense(), "Optimisation sense should be set to MAX");
    }

    @Test
    void testSetOptimisationSenseDirectlyToMin() {
        // set to MAX first to ensure the method really changes the value
        model.setOptimisationSense(Optimisation.Sense.MAX);
        assertEquals(Optimisation.Sense.MAX, model.getOptimisationSense());
        // now change to MIN
        model.setOptimisationSense(Optimisation.Sense.MIN);
        assertEquals(Optimisation.Sense.MIN, model.getOptimisationSense(), "Optimisation sense should be changed to MIN");
    }

    @Test
    void testMaximiseSetsOptimisationSenseToMax() {
        // maximise() internally calls setOptimisationSense(MAX)
        // result is ignored – we only care about the side‑effect
        model.maximise();
        assertEquals(Optimisation.Sense.MAX, model.getOptimisationSense(), "maximise() must set optimisation sense to MAX");
    }

    @Test
    void testMinimiseSetsOptimisationSenseToMin() {
        // minimise() internally calls setOptimisationSense(MIN)
        // result is ignored – we only care about the side‑effect
        model.minimise();
        assertEquals(Optimisation.Sense.MIN, model.getOptimisationSense(), "minimise() must set optimisation sense to MIN");
    }

    @ParameterizedTest(name = "{index} => sense={0}")
    @MethodSource("basicEnumProvider")
    void testSetOptimisationSenseBasicEnums(Optimisation.Sense sense) {
        // Pre‑condition: default is null
        assertNull(model.getOptimisationSense(), "Initial optimisation sense should be null");
        // Set the sense
        model.setOptimisationSense(sense);
        assertEquals(sense, model.getOptimisationSense(), "Optimisation sense should be stored correctly");
    }

    @Test
    void testSetOptimisationSenseNullClearsValue() {
        // Set a non‑null sense first
        model.setOptimisationSense(Optimisation.Sense.MAX);
        assertEquals(Optimisation.Sense.MAX, model.getOptimisationSense());
        // Now clear it with null
        model.setOptimisationSense(null);
        assertNull(model.getOptimisationSense(), "Setting null must clear the stored optimisation sense");
    }

    @Test
    void testSetOptimisationSenseIdempotent() {
        model.setOptimisationSense(Optimisation.Sense.MIN);
        assertEquals(Optimisation.Sense.MIN, model.getOptimisationSense());
        // second call with the same value – should leave the state unchanged
        model.setOptimisationSense(Optimisation.Sense.MIN);
        assertEquals(Optimisation.Sense.MIN, model.getOptimisationSense(), "Repeated setting of the same sense must not alter the stored value");
    }

    @Test
    void testSetOptimisationSenseOverwrite() {
        model.setOptimisationSense(Optimisation.Sense.MIN);
        assertEquals(Optimisation.Sense.MIN, model.getOptimisationSense());
        // overwrite with a different sense
        model.setOptimisationSense(Optimisation.Sense.MAX);
        assertEquals(Optimisation.Sense.MAX, model.getOptimisationSense(), "New sense must replace the previous one");
    }

    @Test
    void testSetOptimisationSenseNullAfterNonNull() {
        model.setOptimisationSense(Optimisation.Sense.MAX);
        assertEquals(Optimisation.Sense.MAX, model.getOptimisationSense());
        model.setOptimisationSense(null);
        assertNull(model.getOptimisationSense(), "Setting null after a valid sense must clear the stored value");
    }

    @Test
    void testSetOptimisationSenseThirdEnumIfPresent() throws Exception {
        // Use reflection to discover a possible FEASIBILITY constant
        Optimisation.Sense feasibility = null;
        for (Optimisation.Sense s : Optimisation.Sense.values()) {
            if ("FEASIBILITY".equalsIgnoreCase(s.name())) {
                feasibility = s;
                break;
            }
        }
        // If the enum does not define FEASIBILITY we simply skip the test
        Assumptions.assumeTrue(feasibility != null, "Optimisation.Sense does not define a FEASIBILITY value – test skipped");
        // Set and verify
        model.setOptimisationSense(feasibility);
        assertEquals(feasibility, model.getOptimisationSense(), "FEASIBILITY sense should be stored and returned correctly");
    }

    @Test
    void testSetOptimisationSenseAfterModelCopy() {
        // Set sense on the original model
        model.setOptimisationSense(Optimisation.Sense.MIN);
        assertEquals(Optimisation.Sense.MIN, model.getOptimisationSense());
        // Create a copy – the copy should inherit the current sense
        ExpressionsBasedModel copy = model.copy();
        assertEquals(Optimisation.Sense.MIN, copy.getOptimisationSense(), "Copy should initially have the same optimisation sense as the original");
        // Change the sense only on the copy
        copy.setOptimisationSense(Optimisation.Sense.MAX);
        assertEquals(Optimisation.Sense.MAX, copy.getOptimisationSense(), "Copy should reflect the newly set sense");
        // Original must remain unchanged
        assertEquals(Optimisation.Sense.MIN, model.getOptimisationSense(), "Original model must retain its original sense after the copy is modified");
    }
}
