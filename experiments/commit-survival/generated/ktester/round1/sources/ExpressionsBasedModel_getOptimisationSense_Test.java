package org.ojalgo.optimisation;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

@ExtendWith(MockitoExtension.class)
class ExpressionsBasedModel_getOptimisationSense_Test {

    @Test
    void testDefaultOptimisationSenseIsNull() {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        assertNull(model.getOptimisationSense(), "A freshly created model should have a null optimisation sense");
    }

    @Test
    void testSetOptimisationSenseMax() {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        // setOptimisationSense is package‑private, therefore accessible from this test class.
        model.setOptimisationSense(Optimisation.Sense.MAX);
        assertEquals(Optimisation.Sense.MAX, model.getOptimisationSense(), "Optimisation sense should be MAX after setting it explicitly");
    }

    @Test
    void testSetOptimisationSenseMin() {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        model.setOptimisationSense(Optimisation.Sense.MIN);
        assertEquals(Optimisation.Sense.MIN, model.getOptimisationSense(), "Optimisation sense should be MIN after setting it explicitly");
    }

    @Test
    void testCopyPreservesOptimisationSense() {
        ExpressionsBasedModel original = new ExpressionsBasedModel();
        original.setOptimisationSense(Optimisation.Sense.MAX);
        // Add a simple variable so that the copy constructor has something to copy.
        original.addVariable().weight(1);
        ExpressionsBasedModel copy = new ExpressionsBasedModel(original, false, false);
        assertEquals(Optimisation.Sense.MAX, copy.getOptimisationSense(), "A shallow copy should retain the optimisation sense of the source model");
    }

    @Test
    void testMinimiseSetsSenseToMin() {
        // Simple model with a single variable and a trivial objective.
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        // minimise will try to push x to its lower bound (0)
        Variable x = model.addVariable().weight(1);
        // this call should set the sense to MIN
        model.minimise();
        assertEquals(Optimisation.Sense.MIN, model.getOptimisationSense(), "Calling minimise() should set the optimisation sense to MIN");
    }

    @Test
    void testMaximiseSetsSenseToMax() {
        // Simple model with a single variable and a trivial objective.
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        Variable x = model.addVariable().weight(1);
        // this call should set the sense to MAX
        model.maximise();
        assertEquals(Optimisation.Sense.MAX, model.getOptimisationSense(), "Calling maximise() should set the optimisation sense to MAX");
    }

    /**
     * 2. After setting MINIMIZE via the package‑private setter.
     */
    @Test
    void testSetOptimisationSenseMinimize() {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        model.setOptimisationSense(Optimisation.Sense.MINIMIZE);
        assertEquals(Optimisation.Sense.MINIMIZE, model.getOptimisationSense(), "Optimisation sense should be MINIMIZE after explicit setting");
    }

    /**
     * 3. After setting MAXIMIZE via the package‑private setter.
     */
    @Test
    void testSetOptimisationSenseMaximize() {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        model.setOptimisationSense(Optimisation.Sense.MAXIMIZE);
        assertEquals(Optimisation.Sense.MAXIMIZE, model.getOptimisationSense(), "Optimisation sense should be MAXIMIZE after explicit setting");
    }

    /**
     * 4. Calling {@code maximise()} must set the sense to {@code MAXIMIZE}.
     */
    @Test
    void testMaximiseSetsSenseToMaximize() {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        // a trivial objective – a single variable with weight 1
        model.addVariable().weight(1);
        // this should also set the internal sense
        model.maximise();
        assertEquals(Optimisation.Sense.MAXIMIZE, model.getOptimisationSense(), "Calling maximise() should set the optimisation sense to MAXIMIZE");
    }

    /**
     * 5. Calling {@code minimise()} must set the sense to {@code MINIMIZE}.
     */
    @Test
    void testMinimiseSetsSenseToMinimize() {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        model.addVariable().weight(1);
        model.minimise();
        assertEquals(Optimisation.Sense.MINIMIZE, model.getOptimisationSense(), "Calling minimise() should set the optimisation sense to MINIMIZE");
    }

    /**
     * 6. Copying a model preserves the optimisation sense (MINIMIZE case).
     */
    @Test
    void testCopyPreservesSenseMinimize() {
        ExpressionsBasedModel original = new ExpressionsBasedModel();
        original.setOptimisationSense(Optimisation.Sense.MINIMIZE);
        original.addVariable().weight(1);
        // shallow copy by default
        ExpressionsBasedModel copy = original.copy();
        assertEquals(Optimisation.Sense.MINIMIZE, copy.getOptimisationSense(), "A copy should retain the MINIMIZE sense of the original model");
    }

    /**
     * 7. Copying a model preserves the optimisation sense (MAXIMIZE case).
     */
    @Test
    void testCopyPreservesSenseMaximize() {
        ExpressionsBasedModel original = new ExpressionsBasedModel();
        original.setOptimisationSense(Optimisation.Sense.MAXIMIZE);
        original.addVariable().weight(1);
        ExpressionsBasedModel copy = original.copy();
        assertEquals(Optimisation.Sense.MAXIMIZE, copy.getOptimisationSense(), "A copy should retain the MAXIMIZE sense of the original model");
    }

    /**
     * 8. Resetting the optimisation sense to {@code null} via reflection.
     */
    @Test
    void testResetOptimisationSenseToNullUsingReflection() throws Exception {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        model.setOptimisationSense(Optimisation.Sense.MINIMIZE);
        // Use reflection to invoke the package‑private setter with a null argument.
        java.lang.reflect.Method setter = ExpressionsBasedModel.class.getDeclaredMethod("setOptimisationSense", Optimisation.Sense.class);
        setter.setAccessible(true);
        setter.invoke(model, (Object) null);
        assertNull(model.getOptimisationSense(), "After resetting via reflection, the optimisation sense should be null");
    }

    /**
     * 9. Multiple successive changes – final sense should reflect the last call.
     */
    @Test
    void testMultipleChangesToSense() {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        // Change MIN → MAX
        model.setOptimisationSense(Optimisation.Sense.MINIMIZE);
        model.setOptimisationSense(Optimisation.Sense.MAXIMIZE);
        assertEquals(Optimisation.Sense.MAXIMIZE, model.getOptimisationSense(), "After setting MIN then MAX, the final sense must be MAXIMIZE");
        // Change MAX → MIN
        model.setOptimisationSense(Optimisation.Sense.MINIMIZE);
        assertEquals(Optimisation.Sense.MINIMIZE, model.getOptimisationSense(), "After setting MAX then MIN, the final sense must be MINIMIZE");
    }

    /**
     * 10. {@code relax()} must not alter the stored optimisation sense.
     */
    @Test
    void testRelaxDoesNotChangeSense() {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        model.setOptimisationSense(Optimisation.Sense.MINIMIZE);
        // relax should keep the current sense unchanged
        model.relax();
        assertEquals(Optimisation.Sense.MINIMIZE, model.getOptimisationSense(), "Calling relax() must not modify the optimisation sense");
    }
}
