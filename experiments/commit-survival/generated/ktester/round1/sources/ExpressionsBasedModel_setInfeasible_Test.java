package org.ojalgo.optimisation;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.Mockito;

@ExtendWith(MockitoExtension.class)
class ExpressionsBasedModel_setInfeasible_Test {

    private ExpressionsBasedModel model;

    @BeforeEach
    void setupBeforeEach() {
        // Create a fresh model for each test to avoid cross‑test contamination.
        model = new ExpressionsBasedModel();
    }

    @AfterEach
    void teardownAfterEach() {
        // Explicitly release resources if the model ever holds any (it currently does not).
        model.dispose();
    }

    /**
     * Helper that invokes the package‑private {@code setInfeasible()} via reflection.
     */
    private void invokeSetInfeasibleReflectively(ExpressionsBasedModel target) throws Exception {
        Method m = ExpressionsBasedModel.class.getDeclaredMethod("setInfeasible");
        m.setAccessible(true);
        m.invoke(target);
    }

    private static void assertThrowsAnyOf(Class<? extends Throwable> ex1, Class<? extends Throwable> ex2, Executable executable, String message) {
        try {
            executable.execute();
            fail(message + " – no exception thrown");
        } catch (Throwable actual) {
            if (!ex1.isInstance(actual) && !ex2.isInstance(actual)) {
                fail(message + " – unexpected exception type: " + actual);
            }
            // expected exception – test passes
        }
    }

    @Test
    void testSetInfeasibleChangesState() {
        // Pre‑condition: a newly created model must not be infeasible.
        assertFalse(model.isInfeasible(), "New model should not be infeasible");
        // Invoke the package‑private method.  Because this test resides in the same package,
        // it can call the method directly.
        model.setInfeasible();
        // Post‑condition: the model must now report itself as infeasible.
        assertTrue(model.isInfeasible(), "Model should be marked infeasible after setInfeasible()");
    }

    @Test
    void testDefaultInfeasibleState() {
        assertFalse(model.isInfeasible(), "A newly created model must not be infeasible");
    }

    @Test
    void testSetInfeasibleViaReflection() throws Exception {
        invokeSetInfeasibleReflectively(model);
        assertTrue(model.isInfeasible(), "Reflection call must set the infeasible flag");
    }

    @Test
    void testRepeatedSetInfeasibleViaReflection() throws Exception {
        invokeSetInfeasibleReflectively(model);
        invokeSetInfeasibleReflectively(model);
        assertTrue(model.isInfeasible(), "Repeated calls must leave the model infeasible");
    }

    @Test
    void testSetInfeasibleThenAddVariable() throws Exception {
        invokeSetInfeasibleReflectively(model);
        // any modification after infeasibility
        model.addVariable();
        assertTrue(model.isInfeasible(), "Adding a variable must not clear the infeasible flag");
    }

    @Test
    void testAddVariableThenSetInfeasible() throws Exception {
        model.addVariable();
        invokeSetInfeasibleReflectively(model);
        assertTrue(model.isInfeasible(), "Variable added before setting infeasible must not prevent flag change");
    }

    @Test
    void testAddExpressionThenSetInfeasible() throws Exception {
        model.addExpression();
        invokeSetInfeasibleReflectively(model);
        assertTrue(model.isInfeasible(), "Expression added before setting infeasible must not prevent flag change");
    }

    @Test
    void testSetInfeasibleThenRelax() throws Exception {
        invokeSetInfeasibleReflectively(model);
        // public method
        model.relax();
        assertTrue(model.isInfeasible(), "Calling relax() must not reset the infeasible flag");
    }

    @Test
    void testSetInfeasibleThenPresolve() throws Exception {
        invokeSetInfeasibleReflectively(model);
        // package‑private but accessible from same package
        model.presolve();
        assertTrue(model.isInfeasible(), "Calling presolve() must not reset the infeasible flag");
    }

    @Test
    void testSetInfeasiblePropagationDeepCopy() throws Exception {
        invokeSetInfeasibleReflectively(model);
        // deep copy
        ExpressionsBasedModel copy = model.copy();
        assertTrue(model.isInfeasible(), "Original model stays infeasible");
        assertTrue(copy.isInfeasible(), "Deep copy must retain infeasible flag");
    }

    @Test
    void testSetInfeasiblePropagationShallowCopy() throws Exception {
        invokeSetInfeasibleReflectively(model);
        // shallow = true, prune = true
        ExpressionsBasedModel shallow = model.copy(true, true);
        assertTrue(model.isInfeasible(), "Original model stays infeasible");
        assertTrue(shallow.isInfeasible(), "Shallow copy must retain infeasible flag");
    }

    @Test
    void testSetInfeasibleAfterWriteParseRoundTrip() throws Exception {
        invokeSetInfeasibleReflectively(model);
        Path tmp = Files.createTempFile("ebm", ".txt");
        try {
            File file = tmp.toFile();
            model.writeTo(file);
            ExpressionsBasedModel parsed = ExpressionsBasedModel.parse(file);
            assertTrue(parsed.isInfeasible(), "Deserialized model must preserve infeasible flag");
            parsed.dispose();
        } finally {
            Files.deleteIfExists(tmp);
        }
    }

    @Test
    void testSetInfeasibleThenSetKnownSolution() throws Exception {
        invokeSetInfeasibleReflectively(model);
        Optimisation.Result dummy = Mockito.mock(Optimisation.Result.class);
        model.setKnownSolution(dummy);
        assertTrue(model.isInfeasible(), "Setting a known solution must not clear infeasibility");
    }

    @Test
    void testSetInfeasibleThenSetOptimisationSense() throws Exception {
        invokeSetInfeasibleReflectively(model);
        // Optimisation.Sense enum provides MIN and MAX (exact names may differ)
        model.setOptimisationSense(Optimisation.Sense.MIN);
        assertTrue(model.isInfeasible(), "Changing optimisation sense must not affect infeasibility");
    }

    @Test
    void testIdempotenceAfterMultipleModifications() throws Exception {
        model.addVariable();
        model.addExpression();
        invokeSetInfeasibleReflectively(model);
        model.relax();
        model.presolve();
        // second call
        invokeSetInfeasibleReflectively(model);
        assertTrue(model.isInfeasible(), "Infeasible flag must stay true after many modifications and calls");
    }

    @Test
    void testSetInfeasibleOnNullReference() {
        ExpressionsBasedModel nullModel = null;
        assertThrows(NullPointerException.class, () -> nullModel.setInfeasible(), "Calling setInfeasible() on a null reference must throw NPE");
    }

    @Test
    void testReflectionIllegalAccess() throws Exception {
        Method m = ExpressionsBasedModel.class.getDeclaredMethod("setInfeasible");
        // Do NOT make it accessible – the default is false
        assertThrowsAnyOf(IllegalAccessException.class, InaccessibleObjectException.class, () -> m.invoke(model), "Invoking without setAccessible(true) must fail with IllegalAccessException or InaccessibleObjectException");
    }

    @Test
    void testReflectionSecurityException() throws Exception {
        // Install a restrictive SecurityManager that denies reflective access.
        SecurityManager original = System.getSecurityManager();
        SecurityManager restrictive = new SecurityManager() {

            @Override
            public void checkPermission(java.security.Permission perm) {
                // Deny any ReflectPermission
                if (perm instanceof ReflectPermission) {
                    throw new SecurityException("Reflective access denied by test SecurityManager");
                }
                // Allow everything else
            }
        };
        System.setSecurityManager(restrictive);
        try {
            Method m = ExpressionsBasedModel.class.getDeclaredMethod("setInfeasible");
            m.setAccessible(true);
            assertThrows(SecurityException.class, () -> m.invoke(model), "Reflective invocation under a denying SecurityManager must throw SecurityException");
        } finally {
            // restore original manager
            System.setSecurityManager(original);
        }
    }
}
