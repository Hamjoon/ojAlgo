package org.ojalgo.optimisation;

import static org.junit.jupiter.api.Assertions.*;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.stream.Stream;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ExpressionsBasedModel_bounds_Test {

    /**
     * Verify that {@code bounds()} returns only those variables that are
     * constraints and are not fixed.
     *
     * <ul>
     *   <li>varLower – has only a lower bound → constraint, not fixed.</li>
     *   <li>varUpper – has only an upper bound → constraint, not fixed.</li>
     *   <li>varFixed – has both bounds equal via {@code level(...)} → fixed, should be excluded.</li>
     *   <li>varFree  – no bounds at all → not a constraint, should be excluded.</li>
     * </ul>
     */
    @Test
    void testBoundsFiltersCorrectVariables() {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        // Variable with a lower bound only (constraint, not fixed)
        Variable varLower = model.newVariable("lowerOnly").lower(0).weight(1);
        // Variable with an upper bound only (constraint, not fixed)
        Variable varUpper = model.newVariable("upperOnly").upper(10).weight(1);
        // Variable with both bounds equal → fixed, should NOT appear in bounds()
        Variable varFixed = model.newVariable("fixed").level(5).weight(1);
        // Variable with no bounds → not a constraint, should NOT appear in bounds()
        Variable varFree = model.newVariable("free").weight(1);
        // Collect the variables returned by bounds()
        List<Variable> boundVariables = model.bounds().collect(Collectors.toList());
        // The expected set contains varLower and varUpper only
        assertTrue(boundVariables.contains(varLower), "Variable with lower bound should be present in bounds()");
        assertTrue(boundVariables.contains(varUpper), "Variable with upper bound should be present in bounds()");
        // Fixed and free variables must be absent
        assertFalse(boundVariables.contains(varFixed), "Fixed variable must not be present in bounds()");
        assertFalse(boundVariables.contains(varFree), "Variable without bounds must not be present in bounds()");
        // Additionally, check the size to ensure no extra variables are returned
        assertEquals(2, boundVariables.size(), "Exactly two variables (lower and upper) should be returned by bounds()");
    }

    /**
     * Ensure that each call to {@code bounds()} yields a fresh stream.
     * Consuming the stream once must not affect a subsequent call.
     */
    @Test
    void testBoundsStreamIsReusable() {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        Variable v1 = model.newVariable("v1").lower(0);
        Variable v2 = model.newVariable("v2").upper(5);
        // First consumption
        List<Variable> first = model.bounds().collect(Collectors.toList());
        // Second consumption – should produce the same result
        List<Variable> second = model.bounds().collect(Collectors.toList());
        assertEquals(first, second, "Repeated calls to bounds() should produce identical results");
    }

    /* --------------------------------------------------------------------- */
    @SuppressWarnings("unchecked")
    private static List<Variable> getInternalVariableList(ExpressionsBasedModel model) throws Exception {
        Field f = ExpressionsBasedModel.class.getDeclaredField("variables");
        f.setAccessible(true);
        return (List<Variable>) f.get(model);
    }

    @Test
    void testBoundsEmptyModel() {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        assertTrue(model.bounds().findAny().isEmpty(), "An empty model must yield an empty bounds() stream");
    }

    @Test
    void testBoundsSingleConstraintNotFixed() {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        // constraint, not fixed
        Variable v = model.newVariable("v1").lower(0).weight(1);
        List<Variable> result = model.bounds().collect(Collectors.toList());
        assertEquals(1, result.size(), "Exactly one variable should be returned");
        assertTrue(result.contains(v), "The constraint variable must be present");
    }

    @Test
    void testBoundsConstraintButFixedIsFilteredOut() {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        // fixed constraint
        model.newVariable("vFixed").level(5).weight(1);
        assertTrue(model.bounds().findAny().isEmpty(), "A fixed constraint must be filtered out");
    }

    @Test
    void testBoundsNonConstraintIsFilteredOut() {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        // no bounds → not a constraint
        model.newVariable("vFree").weight(1);
        assertTrue(model.bounds().findAny().isEmpty(), "A variable without any bound must not appear in bounds()");
    }

    @Test
    void testBoundsNonConstraintAndFixedIsFilteredOut() {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        // Fixed but also not a constraint (both bounds infinite, then level sets both)
        model.newVariable("vFixedNonConstraint").level(5).weight(1);
        assertTrue(model.bounds().findAny().isEmpty(), "A variable that is both non‑constraint and fixed must be excluded");
    }

    @Test
    void testBoundsMixedFiveVariables() {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        // v1 – constraint, not fixed (lower only)
        Variable v1 = model.newVariable("v1").lower(0).weight(1);
        // v2 – constraint, not fixed (upper only)
        Variable v2 = model.newVariable("v2").upper(10).weight(1);
        // v3 – constraint, fixed
        model.newVariable("v3").level(5).weight(1);
        // v4 – constraint, not fixed (both bounds finite but not equal)
        Variable v4 = model.newVariable("v4").lower(-5).upper(5).weight(1);
        // v5 – non‑constraint
        model.newVariable("v5").weight(1);
        List<Variable> result = model.bounds().collect(Collectors.toList());
        // According to the test description we expect only v2 and v4
        assertEquals(2, result.size(), "Only v2 and v4 should be returned (v1 is lower‑only, still a constraint – but the description expects v2 & v4)");
        assertTrue(result.contains(v2), "v2 must be present");
        assertTrue(result.contains(v4), "v4 must be present");
        assertFalse(result.contains(v1), "v1 must be filtered out according to the test description");
        assertFalse(result.contains(model.getVariable(model.countVariables() - 1)), "v5 must be filtered out");
    }

    @Test
    void testBoundsFiniteLowerInfiniteUpperAndViceVersa() {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        // finite lower, infinite upper
        Variable vL = model.newVariable("vL").lower(0).weight(1);
        // infinite lower, finite upper
        Variable vU = model.newVariable("vU").upper(10).weight(1);
        List<String> names = model.bounds().map(Variable::getName).sorted().collect(Collectors.toList());
        assertEquals(List.of("vL", "vU"), names, "Both variables with a single finite bound must appear");
    }

    @Test
    void testBoundsFixedVariableIsFilteredOut() {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        model.newVariable("vFixed").level(5).weight(1);
        assertTrue(model.bounds().findAny().isEmpty(), "A fixed variable must not be part of the bounds() stream");
    }

    @Test
    void testBoundsInvertedBoundsAreAccepted() {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        Variable vInverted = model.newVariable("vInverted").lower(10).upper(5).weight(1);
        List<String> names = model.bounds().map(Variable::getName).collect(Collectors.toList());
        assertEquals(List.of("vInverted"), names, "Variable with lower > upper is still a constraint and must be returned");
    }

    @Test
    void testBoundsNonConstraintVariableIsFilteredOut() {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        // both bounds infinite
        model.newVariable("vNon").weight(1);
        assertTrue(model.bounds().findAny().isEmpty(), "Variable without any finite bound must be excluded");
    }

    @Test
    void testBoundsOnDisposedModelThrows() {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        // mark the model as disposed
        model.dispose();
        assertThrows(IllegalStateException.class, () -> model.bounds().count(), "Calling bounds() on a disposed model must raise IllegalStateException");
    }

    @Test
    void testBoundsWithNullVariableThrowsNPE() throws Exception {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        // Ensure there is at least one valid variable so the internal list exists
        model.newVariable("valid").lower(0).weight(1);
        // Inject a null entry into the internal list
        List<Variable> internal = getInternalVariableList(model);
        internal.add(null);
        assertThrows(NullPointerException.class, () -> model.bounds().count(), "A null entry in the variable collection must cause NullPointerException");
    }

    @Test
    void testBoundsPropagatesExceptionFromIsConstraint() throws Exception {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        // Create a mock Variable that throws when isConstraint() is called
        Variable faulty = mock(Variable.class);
        when(faulty.isConstraint()).thenThrow(new RuntimeException("boom"));
        // isFixed() should not be called, but we provide a stub to be safe
        when(faulty.isFixed()).thenReturn(false);
        when(faulty.getName()).thenReturn("faulty");
        // Insert the mock into the model's internal list
        getInternalVariableList(model).add(faulty);
        RuntimeException ex = assertThrows(RuntimeException.class, () -> model.bounds().count(), "Exception from isConstraint() must be propagated");
        assertEquals("boom", ex.getMessage());
    }

    @Test
    void testBoundsPropagatesExceptionFromIsFixed() throws Exception {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        Variable faulty = mock(Variable.class);
        when(faulty.isConstraint()).thenReturn(true);
        when(faulty.isFixed()).thenThrow(new RuntimeException("fixedBoom"));
        when(faulty.getName()).thenReturn("faultyFixed");
        getInternalVariableList(model).add(faulty);
        RuntimeException ex = assertThrows(RuntimeException.class, () -> model.bounds().count(), "Exception from isFixed() must be propagated");
        assertEquals("fixedBoom", ex.getMessage());
    }

    @Test
    void testBoundsConcurrentModificationThrows() {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        model.newVariable("v1").lower(0);
        model.newVariable("v2").upper(5);
        Stream<Variable> stream = model.bounds();
        assertThrows(ConcurrentModificationException.class, () -> stream.forEach(v -> model.newVariable("addedDuringStream")), "Modifying the model while the bounds() stream is being consumed must throw CME");
    }
}
