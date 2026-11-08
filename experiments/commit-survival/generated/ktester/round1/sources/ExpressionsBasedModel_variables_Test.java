package org.ojalgo.optimisation;

import static org.junit.jupiter.api.Assertions.*;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentModificationException;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class ExpressionsBasedModel_variables_Test {

    private ExpressionsBasedModel model;

    @BeforeEach
    void setupBeforeEach() {
        // a fresh model for each test
        model = new ExpressionsBasedModel();
    }

    /**
     * Sets the internal equality‑constraint flag of a {@link Variable}.
     *
     * @param variable the variable to modify
     * @param flag     true → treat as equality‑constraint, false → normal variable
     */
    private static void setEqualityConstraint(Variable variable, boolean flag) {
        try {
            // Most recent versions expose a package‑private method; we try to invoke it.
            var method = Variable.class.getDeclaredMethod("setEqualityConstraint", boolean.class);
            method.setAccessible(true);
            method.invoke(variable, flag);
        } catch (NoSuchMethodException e) {
            // Fall back to using bounds: lower == upper → equality constraint.
            // Clear any previous bounds first.
            variable.lower(null);
            variable.upper(null);
            if (flag) {
                // Use a dummy value (e.g., 0) for both bounds.
                variable.lower(0).upper(0);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Verify that a model with only regular variables (no equality constraints)
     * returns all of them via {@code variables()}.
     */
    @Test
    void testVariablesReturnsAllWhenNoEqualityConstraints() {
        // free variable
        Variable v1 = model.addVariable();
        // lower‑bounded
        Variable v2 = model.addVariable().lower(0);
        // upper‑bounded
        Variable v3 = model.addVariable().upper(10);
        // bounded but not fixed
        Variable v4 = model.addVariable().lower(1).upper(5);
        List<Variable> varsFromMethod = model.variables().collect(Collectors.toList());
        // All four variables should be present
        assertEquals(4, varsFromMethod.size(), "variables() should contain all variables when none are equality constraints");
        assertTrue(varsFromMethod.containsAll(List.of(v1, v2, v3, v4)), "variables() missing expected variables");
    }

    /**
     * Verify that variables which are equality constraints (lower == upper) are
     * filtered out by {@code variables()}.
     */
    @Test
    void testVariablesFiltersEqualityConstraints() {
        // regular variables
        Variable free = model.addVariable();
        Variable bounded = model.addVariable().lower(0).upper(10);
        // equality‑constrained variables (fixed at a value)
        // lower == upper == 3
        Variable fixed1 = model.addVariable().level(3);
        // also fixed
        Variable fixed2 = model.addVariable().lower(5).upper(5);
        List<Variable> varsFromMethod = model.variables().collect(Collectors.toList());
        // The stream should contain only the non‑equality variables
        assertEquals(2, varsFromMethod.size(), "variables() should exclude equality‑constrained (fixed) variables");
        assertTrue(varsFromMethod.containsAll(List.of(free, bounded)), "variables() missing expected non‑equality variables");
        assertFalse(varsFromMethod.contains(fixed1), "variables() should not contain a fixed variable created via level()");
        assertFalse(varsFromMethod.contains(fixed2), "variables() should not contain a fixed variable created via lower/upper equal values");
    }

    /**
     * Retrieves the private {@code myVariables} list from the model.
     *
     * @return the mutable list holding the model's variables
     */
    @SuppressWarnings("unchecked")
    private static ArrayList<Variable> getInternalVariableList(ExpressionsBasedModel model) {
        try {
            Field field = ExpressionsBasedModel.class.getDeclaredField("myVariables");
            field.setAccessible(true);
            return (ArrayList<Variable>) field.get(model);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void testVariablesEmptyModel() {
        // No variables added – expect an empty stream
        List<Variable> result = model.variables().collect(Collectors.toList());
        assertTrue(result.isEmpty(), "variables() should return an empty list for an empty model");
    }

    @Test
    void testVariablesAllNonEquality() {
        Variable v1 = model.addVariable();
        Variable v2 = model.addVariable();
        Variable v3 = model.addVariable();
        // Ensure none are marked as equality‑constraint
        setEqualityConstraint(v1, false);
        setEqualityConstraint(v2, false);
        setEqualityConstraint(v3, false);
        List<Variable> result = model.variables().collect(Collectors.toList());
        assertEquals(3, result.size(), "All three variables should be returned");
        assertTrue(result.containsAll(List.of(v1, v2, v3)));
    }

    @Test
    void testVariablesOneEqualityFilteredOut() {
        Variable v1 = model.addVariable();
        Variable v2 = model.addVariable();
        Variable v3 = model.addVariable();
        // make v2 an equality‑constraint
        setEqualityConstraint(v2, true);
        List<Variable> result = model.variables().collect(Collectors.toList());
        assertEquals(2, result.size(), "Only two variables should be returned after filtering");
        assertTrue(result.containsAll(List.of(v1, v3)));
        assertFalse(result.contains(v2));
    }

    @Test
    void testVariablesAllEqualityConstraints() {
        Variable v1 = model.addVariable();
        Variable v2 = model.addVariable();
        setEqualityConstraint(v1, true);
        setEqualityConstraint(v2, true);
        List<Variable> result = model.variables().collect(Collectors.toList());
        assertTrue(result.isEmpty(), "All variables are equality‑constraints, so result must be empty");
    }

    @Test
    void testVariablesMixedFiveVariables() {
        Variable v1 = model.addVariable();
        Variable v2 = model.addVariable();
        Variable v3 = model.addVariable();
        Variable v4 = model.addVariable();
        Variable v5 = model.addVariable();
        setEqualityConstraint(v2, true);
        setEqualityConstraint(v4, true);
        List<Variable> result = model.variables().collect(Collectors.toList());
        assertEquals(List.of(v1, v3, v5), result, "Only v1, v3 and v5 should be present after filtering equality‑constraints");
    }

    @Test
    void testEqualityFlagCleared() {
        Variable v1 = model.addVariable();
        // initially an equality‑constraint
        setEqualityConstraint(v1, true);
        // clear the flag
        setEqualityConstraint(v1, false);
        List<Variable> result = model.variables().collect(Collectors.toList());
        assertEquals(1, result.size());
        assertTrue(result.contains(v1), "Variable should appear after equality flag is cleared");
    }

    @Test
    void testLazyEvaluationReflectsLatestFlagState() {
        Variable v1 = model.addVariable();
        Variable v2 = model.addVariable();
        Variable v3 = model.addVariable();
        // mark as equality‑constraint
        setEqualityConstraint(v1, true);
        // Obtain the stream but do NOT terminal‑operate yet
        Stream<Variable> lazyStream = model.variables();
        // Change the flag before the terminal operation
        setEqualityConstraint(v1, false);
        List<Variable> result = lazyStream.collect(Collectors.toList());
        assertEquals(3, result.size(), "All three variables should be present after flag cleared");
        assertTrue(result.containsAll(List.of(v1, v2, v3)));
    }

    @Test
    void testIndependentStreamsProduceIdenticalResults() {
        Variable v1 = model.addVariable();
        Variable v2 = model.addVariable();
        List<Variable> first = model.variables().collect(Collectors.toList());
        List<Variable> second = model.variables().collect(Collectors.toList());
        assertEquals(List.of(v1, v2), first, "First stream should contain v1 and v2");
        assertEquals(List.of(v1, v2), second, "Second stream should also contain v1 and v2");
        assertNotSame(first, second, "Each call must return a new independent list");
    }

    @Test
    void testVariablesNullEntryThrowsNPE() throws Exception {
        // Inject a null entry into the internal list
        ArrayList<Variable> internal = getInternalVariableList(model);
        // now the list contains a null element
        internal.add(null);
        // Collecting the stream should trigger a NPE when the filter accesses the null element
        assertThrows(NullPointerException.class, () -> model.variables().collect(Collectors.toList()), "A null entry in the variable list should cause NullPointerException");
    }

    @Test
    void testConcurrentModificationDuringStreamAdd() {
        // Obtain a stream first
        Stream<Variable> stream = model.variables();
        // Modify the underlying list before terminal operation
        model.addVariable();
        // The stream should now fail with CME when we try to collect it
        assertThrows(ConcurrentModificationException.class, () -> stream.collect(Collectors.toList()), "Adding a variable while a stream is in use must cause ConcurrentModificationException");
    }

    @Test
    void testConcurrentModificationDuringStreamRemove() throws Exception {
        // Add a variable so that the list is non‑empty
        model.addVariable();
        // Obtain a stream
        Stream<Variable> stream = model.variables();
        // Remove an element via reflection
        ArrayList<Variable> internal = getInternalVariableList(model);
        internal.remove(0);
        // Collecting should now throw CME
        assertThrows(ConcurrentModificationException.class, () -> stream.collect(Collectors.toList()), "Removing a variable while a stream is in use must cause ConcurrentModificationException");
    }
}
