package org.ojalgo.optimisation;

import org.junit.jupiter.api.*;
import java.util.List;
import java.util.stream.Collectors;
import static org.junit.jupiter.api.Assertions.*;
import java.math.BigDecimal;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(MockitoExtension.class)
class ExpressionsBasedModel_bounds_Test {

    /**
     * Model used in each test
     */
    private ExpressionsBasedModel model;

    @BeforeEach
    void setupBeforeEach() {
        // a fresh model for every test case
        model = new ExpressionsBasedModel();
    }

    /**
     * Verify that {@link ExpressionsBasedModel#bounds()} returns only those variables that
     * are constraints (i.e. have a lower or upper bound) and are not fixed (lower != upper).
     */
    @Test
    void testBoundsReturnsOnlyConstraintVariablesNotFixed() {
        // Variable with both lower and upper bound – constraint, not fixed
        Variable v1 = model.newVariable("v1").lower(0).upper(10);
        // Variable with a single level – lower == upper → fixed, should be excluded
        Variable v2 = model.newVariable("v2").level(5);
        // Variable with no bounds – not a constraint, should be excluded
        Variable v3 = model.newVariable("v3");
        // Variable with only an upper bound – constraint, not fixed
        Variable v4 = model.newVariable("v4").upper(7);
        List<Variable> bounds = model.bounds().collect(Collectors.toList());
        // Expect only v1 and v4 to appear
        assertEquals(2, bounds.size(), "Exactly two variables should be returned by bounds()");
        assertTrue(bounds.contains(v1), "Variable v1 should be part of bounds()");
        assertTrue(bounds.contains(v4), "Variable v4 should be part of bounds()");
        assertFalse(bounds.contains(v2), "Fixed variable v2 must not be part of bounds()");
        assertFalse(bounds.contains(v3), "Non‑constraint variable v3 must not be part of bounds()");
    }

    /**
     * Verify that an empty stream is returned when the model contains no variables that satisfy
     * the {@code isConstraint() && !isFixed()} predicate.
     */
    @Test
    void testBoundsEmptyWhenNoConstraintVariables() {
        // Free variable – not a constraint
        model.newVariable("free");
        // Fixed variable – constraint but also fixed
        model.newVariable("fixed").level(3);
        // Another free variable
        model.newVariable("anotherFree");
        List<Variable> bounds = model.bounds().collect(Collectors.toList());
        assertTrue(bounds.isEmpty(), "bounds() should return an empty list when no qualifying variables exist");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void testBoundsEmptyModel() {
        // No variables added to the model
        List<Variable> bounds = model.bounds().collect(Collectors.toList());
        assertTrue(bounds.isEmpty(), "bounds() should return an empty stream for an empty model");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void testBoundsSingleVariableSatisfiesBoth() {
        Variable x = model.newVariable("x").lower(BigDecimal.ZERO).upper(BigDecimal.TEN);
        List<String> names = model.bounds().map(Variable::getName).collect(Collectors.toList());
        assertEquals(List.of("x"), names, "Only variable x should be returned");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void testBoundsSingleVariableConstraintButFixed() {
        // level(...) sets lower == upper → fixed
        Variable y = model.newVariable("y").level(BigDecimal.valueOf(5));
        List<String> names = model.bounds().map(Variable::getName).collect(Collectors.toList());
        assertTrue(names.isEmpty(), "Fixed variable y must not appear in bounds()");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void testBoundsSingleVariableNotConstraint() {
        // no bounds → not a constraint
        Variable z = model.newVariable("z");
        List<String> names = model.bounds().map(Variable::getName).collect(Collectors.toList());
        assertTrue(names.isEmpty(), "Non‑constraint variable z must not appear in bounds()");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void testBoundsMultipleMixedStates() {
        // constraint, not fixed
        Variable a = model.newVariable("a").lower(BigDecimal.ZERO).upper(BigDecimal.TEN);
        // constraint & fixed
        Variable b = model.newVariable("b").level(BigDecimal.ONE);
        // not a constraint
        Variable c = model.newVariable("c");
        // constraint, not fixed
        Variable d = model.newVariable("d").upper(BigDecimal.valueOf(7));
        List<String> names = model.bounds().map(Variable::getName).collect(Collectors.toList());
        assertEquals(List.of("a", "d"), names, "Only a and d should satisfy the predicates");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void testBoundsOrderPreservation() {
        Variable first = model.newVariable("first").lower(BigDecimal.ZERO).upper(BigDecimal.ONE);
        // fixed
        Variable second = model.newVariable("second").level(BigDecimal.valueOf(3));
        Variable third = model.newVariable("third").upper(BigDecimal.valueOf(5));
        List<String> names = model.bounds().map(Variable::getName).collect(Collectors.toList());
        assertEquals(List.of("first", "third"), names, "The stream must preserve insertion order (first then third)");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void testBoundsIntegerVariableConstraintNotFixed() {
        // integer flag does not affect the filter
        Variable i = model.newVariable("i").lower(BigDecimal.ZERO).upper(BigDecimal.TEN).integer(true);
        List<String> names = model.bounds().map(Variable::getName).collect(Collectors.toList());
        assertEquals(List.of("i"), names, "Integer variable i should be included");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void testBoundsVariableNotConstraintButFixed() {
        // fixed, but no bounds → not a constraint
        Variable nf = model.newVariable("nf").fix();
        List<String> names = model.bounds().map(Variable::getName).collect(Collectors.toList());
        assertTrue(names.isEmpty(), "Variable nf should be excluded (not a constraint)");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void testBoundsVariableToggleFixedStatus() {
        Variable t = model.newVariable("t").lower(BigDecimal.ZERO).upper(BigDecimal.ONE);
        // initially not fixed → should appear
        List<String> before = model.bounds().map(Variable::getName).collect(Collectors.toList());
        assertEquals(List.of("t"), before, "t should be present before fixing");
        // now fix it
        t.fix();
        List<String> after = model.bounds().map(Variable::getName).collect(Collectors.toList());
        assertTrue(after.isEmpty(), "After fixing, t must disappear from bounds()");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void testBoundsVariableToggleConstraintStatus() {
        Variable c = model.newVariable("c").lower(BigDecimal.ZERO).upper(BigDecimal.ONE);
        // initially a constraint
        List<String> before = model.bounds().map(Variable::getName).collect(Collectors.toList());
        assertEquals(List.of("c"), before, "c should be present before disabling constraint");
        // disable constraint flag
        c.setConstraint(false);
        List<String> after = model.bounds().map(Variable::getName).collect(Collectors.toList());
        assertTrue(after.isEmpty(), "After disabling constraint, c must disappear");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void testBoundsRepeatedStreamConsumption() {
        Variable r1 = model.newVariable("r1").lower(BigDecimal.ZERO).upper(BigDecimal.ONE);
        Variable r2 = model.newVariable("r2").lower(BigDecimal.ZERO).upper(BigDecimal.ONE);
        List<String> first = model.bounds().map(Variable::getName).collect(Collectors.toList());
        List<String> second = model.bounds().map(Variable::getName).collect(Collectors.toList());
        assertEquals(List.of("r1", "r2"), first, "First consumption should yield r1, r2");
        assertEquals(List.of("r1", "r2"), second, "Second consumption should yield the same order");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void testBoundsMixedAfterModificationsFixing() {
        Variable a = model.newVariable("a").lower(BigDecimal.ZERO).upper(BigDecimal.ONE);
        // constraint & fixed
        Variable b = model.newVariable("b").level(BigDecimal.valueOf(2));
        // not a constraint
        Variable c = model.newVariable("c");
        // Fix variable a
        a.fix();
        List<String> names = model.bounds().map(Variable::getName).collect(Collectors.toList());
        assertTrue(names.isEmpty(), "All variables are now either fixed or not constraints");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void testBoundsMixedAfterModificationsUnfixing() {
        Variable a = model.newVariable("a").lower(BigDecimal.ZERO).upper(BigDecimal.ONE);
        // fixed initially
        Variable b = model.newVariable("b").level(BigDecimal.valueOf(2));
        // not a constraint
        Variable c = model.newVariable("c");
        // Unfix variable b (makes it non‑fixed but still a constraint)
        b.unfix();
        List<String> names = model.bounds().map(Variable::getName).collect(Collectors.toList());
        assertEquals(List.of("b"), names, "Only b should satisfy both predicates after unfixing");
    }
}
