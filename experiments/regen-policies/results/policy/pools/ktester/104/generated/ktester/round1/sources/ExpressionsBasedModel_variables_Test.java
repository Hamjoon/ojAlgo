package org.ojalgo.optimisation;

import static org.junit.jupiter.api.Assertions.*;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentModificationException;

@ExtendWith(MockitoExtension.class)
class ExpressionsBasedModel_variables_Test {

    private ExpressionsBasedModel model;

    @BeforeEach
    void setupBeforeEach() {
        // A fresh model for each test.
        model = new ExpressionsBasedModel();
    }

    @AfterEach
    void teardownAfterEach() {
        // Help GC – not strictly required but keeps the pattern consistent.
        model = null;
    }

    /**
     * Verify that a variable defined as an equality constraint (lower == upper) is filtered out.
     */
    @Test
    void testEqualityConstraintIsFilteredOut() {
        // Equality‑constrained variable (level sets both lower and upper to the same value)
        Variable eqVar = model.newVariable("eq").level(BigDecimal.valueOf(5));
        // Regular variables – not equality constraints
        Variable lowerUpper = model.newVariable("lu").lower(BigDecimal.ZERO).upper(BigDecimal.TEN);
        // no bounds
        Variable free = model.newVariable("free");
        // Collect the variables returned by the method under test
        List<Variable> vars = model.variables().collect(Collectors.toList());
        // The equality‑constrained variable must not be present
        assertFalse(vars.contains(eqVar), "Equality‑constrained variable should be filtered out");
        // The other variables must be present
        assertTrue(vars.contains(lowerUpper), "Variable with lower/upper bounds should be present");
        assertTrue(vars.contains(free), "Unbounded variable should be present");
        // The total number of returned variables should be 2
        assertEquals(2, vars.size(), "Only non‑equality variables should be returned");
    }

    /**
     * Verify that when no variable is an equality constraint, all variables are returned.
     */
    @Test
    void testAllVariablesReturnedWhenNoEqualityConstraint() {
        Variable v1 = model.newVariable("v1").lower(BigDecimal.ZERO).upper(BigDecimal.ONE);
        // free variable
        Variable v2 = model.newVariable("v2");
        // only lower bound
        Variable v3 = model.newVariable("v3").lower(BigDecimal.TEN);
        List<Variable> vars = model.variables().collect(Collectors.toList());
        assertEquals(3, vars.size(), "All variables should be returned when none are equality constraints");
        assertTrue(vars.containsAll(List.of(v1, v2, v3)));
    }

    /**
     * Ensure that the stream returned by {@code variables()} can be consumed multiple times
     * (i.e. each call creates a fresh stream).
     */
    @Test
    void testStreamIsReusable() {
        model.newVariable("a");
        // equality constraint, should be filtered
        model.newVariable("b").level(BigDecimal.ONE);
        // First consumption
        List<Variable> first = model.variables().map(Variable::getName).collect(Collectors.toList());
        // Second consumption
        List<Variable> second = model.variables().map(Variable::getName).collect(Collectors.toList());
        assertEquals(first, second, "Each call to variables() should produce an independent stream");
        assertEquals(1, first.size(), "Only the non‑equality variable should be present");
        assertEquals("a", first.get(0));
    }

    /**
     * Empty model – variables() must return an empty stream.
     */
    @Test
    void testEmptyModelReturnsEmptyStream() {
        List<Variable> vars = model.variables().collect(Collectors.toList());
        assertTrue(vars.isEmpty(), "Empty model should produce an empty variable stream");
    }

    /**
     * All variables are non‑equality – the complete set must be returned.
     */
    @Test
    void testAllNonEqualityVariablesReturned() {
        Variable v1 = model.newVariable("v1").lower(BigDecimal.ZERO).upper(BigDecimal.ONE);
        // free
        Variable v2 = model.newVariable("v2");
        // only upper bound
        Variable v3 = model.newVariable("v3").upper(BigDecimal.valueOf(10));
        List<Variable> vars = model.variables().collect(Collectors.toList());
        assertEquals(3, vars.size());
        assertTrue(vars.containsAll(List.of(v1, v2, v3)));
    }

    /**
     * Mixed equality / non‑equality variables – only non‑equality should appear.
     */
    @Test
    void testMixedEqualityAndNonEqualityVariables() {
        // equality
        Variable vA = model.newVariable("vA").level(BigDecimal.valueOf(3));
        // non‑equality
        Variable vB = model.newVariable("vB").lower(BigDecimal.ZERO).upper(BigDecimal.TEN);
        // equality
        Variable vC = model.newVariable("vC").level(BigDecimal.valueOf(7));
        // free, non‑equality
        Variable vD = model.newVariable("vD");
        List<String> names = model.variables().map(Variable::getName).collect(Collectors.toList());
        assertEquals(List.of("vB", "vD"), names, "Only non‑equality variables should be present");
    }

    /**
     * All variables are equality constraints – result must be empty.
     */
    @Test
    void testAllEqualityConstraintsFilteredOut() {
        model.newVariable("e1").level(BigDecimal.ONE);
        model.newVariable("e2").level(BigDecimal.TEN);
        model.newVariable("e3").level(BigDecimal.ZERO);
        List<Variable> vars = model.variables().collect(Collectors.toList());
        assertTrue(vars.isEmpty(), "All equality‑constrained variables should be filtered out");
    }

    /**
     * Each call to variables() reflects the current model state.
     */
    @Test
    void testStreamReflectsCurrentModelStateOnEachCall() {
        model.newVariable("v1");
        model.newVariable("v2");
        List<String> first = model.variables().map(Variable::getName).collect(Collectors.toList());
        // Add third variable after first collection
        model.newVariable("v3");
        List<String> second = model.variables().map(Variable::getName).collect(Collectors.toList());
        assertEquals(List.of("v1", "v2"), first);
        assertEquals(List.of("v1", "v2", "v3"), second);
    }

    /**
     * A stream obtained before a new variable is added must not see the new variable.
     */
    @Test
    void testStreamDoesNotIncludeVariablesAddedAfterCreation() {
        model.newVariable("v1");
        model.newVariable("v2");
        // capture snapshot
        Stream<Variable> snapshot = model.variables();
        // Add a new variable after the stream has been created
        model.newVariable("v3");
        List<String> collected = snapshot.map(Variable::getName).collect(Collectors.toList());
        assertEquals(List.of("v1", "v2"), collected, "Stream created before addition must not contain the newly added variable");
    }

    /**
     * Changing a variable to become an equality constraint after it was added.
     */
    @Test
    void testVariableEqualityFlagChangeAfterAddition() {
        // initially non‑equality
        Variable vX = model.newVariable("vX");
        List<String> before = model.variables().map(Variable::getName).collect(Collectors.toList());
        assertEquals(List.of("vX"), before);
        // Turn it into an equality constraint
        vX.level(BigDecimal.valueOf(5));
        List<String> after = model.variables().map(Variable::getName).collect(Collectors.toList());
        assertTrue(after.isEmpty(), "Variable that became an equality constraint must be filtered out");
    }

    /**
     * Verify that insertion order is preserved by the stream.
     */
    @Test
    void testOrderPreservationOfVariablesStream() {
        Variable vA = model.newVariable("vA");
        Variable vB = model.newVariable("vB");
        Variable vC = model.newVariable("vC");
        Variable vD = model.newVariable("vD");
        List<String> names = model.variables().map(Variable::getName).collect(Collectors.toList());
        assertEquals(List.of("vA", "vB", "vC", "vD"), names, "The variables stream must preserve insertion order");
    }

    /**
     * Large number of non‑equality variables – ensure count matches.
     */
    @Test
    void testLargeNumberOfNonEqualityVariables() {
        final int COUNT = 1_000;
        for (int i = 0; i < COUNT; i++) {
            model.newVariable("v" + i);
        }
        long actual = model.variables().count();
        assertEquals(COUNT, actual, "variables() must return exactly " + COUNT + " elements");
    }

    /**
     * Inserting a null entry into the internal list should cause NPE when streamed.
     */
    @Test
    void testNullVariableInModelThrowsNPE() throws Exception {
        // Use reflection to inject a null into the private myVariables list.
        Field field = ExpressionsBasedModel.class.getDeclaredField("myVariables");
        field.setAccessible(true);
        @SuppressWarnings("unchecked")
        ArrayList<Variable> list = (ArrayList<Variable>) field.get(model);
        // deliberately corrupt the internal state
        list.add(null);
        // The stream's filter will attempt to call isEqualityConstraint on the null entry.
        assertThrows(NullPointerException.class, () -> model.variables().collect(Collectors.toList()), "A null variable in the list should cause NullPointerException");
    }

    /**
     * Adding a variable while a previously obtained stream is being consumed should raise CME.
     */
    @Test
    void testConcurrentModificationDuringStreamConsumption() {
        model.newVariable("v1");
        model.newVariable("v2");
        Stream<Variable> stream = model.variables();
        // Modify the underlying list before the terminal operation
        model.newVariable("v3");
        assertThrows(ConcurrentModificationException.class, () -> stream.map(Variable::getName).collect(Collectors.toList()), "Modifying the model while a stream is being consumed must throw ConcurrentModificationException");
    }
}
