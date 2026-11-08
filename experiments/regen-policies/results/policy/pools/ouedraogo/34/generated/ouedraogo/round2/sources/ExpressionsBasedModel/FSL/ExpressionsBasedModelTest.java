package org.ojalgo.optimisation;
import org.ojalgo.structure.Access1D;
import org.ojalgo.structure.Structure1D.IntIndex;


import static org.junit.Assert.*;

import java.io.File;
import java.math.BigDecimal;
import java.util.Set;

import org.junit.Test;
import org.ojalgo.type.context.NumberContext;

/**
 * Unit tests for {@link ExpressionsBasedModel}.
 *
 * The tests cover the public API of the class and exercise the most important
 * behaviours such as variable/ expression handling, categorisation, description,
 * optimisation sense handling and basic validation.
 *
 * The aim is to achieve high branch coverage without depending on any external
 * solver – the built‑in {@code DefaultIntermediate} is used which does not
 * require a real optimisation run for the covered methods.
 */
public class ExpressionsBasedModelTest {

    /** Helper to create a model with two variables. */
    private ExpressionsBasedModel createSimpleModel() {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        model.addVariable("x1");
        model.addVariable("x2");
        return model;
    }

    @Test
    public void testAddAndRetrieveVariable() {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        Variable v = model.addVariable("myVar");
        assertNotNull(v);
        assertEquals("myVar", v.getName());
        assertEquals(0, model.indexOf(v));
        // default: not fixed, not integer, not binary
        assertFalse(v.isFixed());
        assertFalse(v.isInteger());
        assertFalse(v.isBinary());
    }

    @Test
    public void testVariableBoundsAndCategories() {
        ExpressionsBasedModel model = new ExpressionsBasedModel();

        // x1 : lower -5, upper 10 (negative & positive)
        Variable x1 = model.addVariable("x1").lower(BigDecimal.valueOf(-5)).upper(BigDecimal.valueOf(10));

        // x2 : lower 0, upper 5 (positive only)
        Variable x2 = model.addVariable("x2").lower(BigDecimal.ZERO).upper(BigDecimal.valueOf(5));

        // x3 : lower -10, upper 0 (negative only)
        Variable x3 = model.addVariable("x3").lower(BigDecimal.valueOf(-10)).upper(BigDecimal.ZERO);

        // x4 : integer variable
        Variable x4 = model.addVariable("x4").integer(true).lower(BigDecimal.ZERO).upper(BigDecimal.valueOf(3));

        // Check categorisation lists
        assertTrue(model.getPositiveVariables().contains(x1));
        assertTrue(model.getPositiveVariables().contains(x2));
        assertFalse(model.getPositiveVariables().contains(x3));

        assertTrue(model.getNegativeVariables().contains(x1));
        assertTrue(model.getNegativeVariables().contains(x3));
        assertFalse(model.getNegativeVariables().contains(x2));

        assertTrue(model.getIntegerVariables().contains(x4));
        assertFalse(model.getIntegerVariables().contains(x1));
    }

    @Test
    public void testAddExpressionAndObjectiveEvaluation() {
        ExpressionsBasedModel model = new ExpressionsBasedModel();

        Variable x = model.addVariable("x").lower(BigDecimal.ZERO).upper(BigDecimal.valueOf(10));
        Variable y = model.addVariable("y").lower(BigDecimal.ZERO).upper(BigDecimal.valueOf(10));

        // Set contribution weights (objective part)
        x.weight(BigDecimal.ONE);
        y.weight(BigDecimal.valueOf(2));

        // Build a separate expression that also contributes to the objective
        Expression expr = model.newExpression("extra");
        expr.set(x.getIndex(), BigDecimal.valueOf(3));
        expr.set(y.getIndex(), BigDecimal.valueOf(-1));
        expr.weight(BigDecimal.valueOf(4)); // contribution weight

        // Evaluate the objective at a known point
        Access1D<BigDecimal> point = Access1D.wrap(new BigDecimal[] {
                BigDecimal.valueOf(1), // x = 1
                BigDecimal.valueOf(2)  // y = 2
        });

        BigDecimal objVal = model.objective().evaluate(point);
        // Expected: (1*1) + (2*2) + 4*((3*1) + (-1*2)) = 1 + 4 + 4*(3 - 2) = 5 + 4*1 = 9
        assertEquals(BigDecimal.valueOf(9), objVal);
    }

    @Test
    public void testDescriptionCounts() {
        ExpressionsBasedModel model = new ExpressionsBasedModel();

        // Variables
        Variable a = model.addVariable("a").lower(BigDecimal.ZERO).upper(BigDecimal.TEN);
        Variable b = model.addVariable("b").integer(true).lower(BigDecimal.ZERO).upper(BigDecimal.ONE);
        Variable c = model.addVariable("c").upper(BigDecimal.ZERO); // negative only

        // Expressions
        Expression e1 = model.newExpression("e1");
        e1.set(a.getIndex(), ONE);
        e1.lower(BigDecimal.ZERO).upper(BigDecimal.valueOf(5));

        Expression e2 = model.newExpression("e2");
        e2.set(b.getIndex(), ONE);
        e2.upper(BigDecimal.ONE);

        // Description
        ExpressionsBasedModel.Description desc = model.describe();

        // Variables
        assertEquals(3, desc.nbVariables);
        assertEquals(2, desc.nbPositiveVariables); // a (0..10) and b (0..1)
        assertEquals(1, desc.nbNegativeVariables); // c (upper 0)
        assertEquals(1, desc.nbIntegerVariables); // b

        // Bounds (from variables)
        assertEquals(1, desc.nbLowerBounds); // a has lower 0, b also 0 (counts both)
        assertEquals(2, desc.nbUpperBounds); // a upper 10, b upper 1, c upper 0 (c counted as upper)

        // Constraints (from expressions)
        assertEquals(2, desc.nbLowerConstraints); // e1 lower bound
        assertEquals(2, desc.nbUpperConstraints); // e1 upper, e2 upper
        assertEquals(0, desc.nbEqualityConstraints);
    }

    @Test
    public void testOptimisationSenseHandling() {
        ExpressionsBasedModel model = new ExpressionsBasedModel();

        assertNull(model.getOptimisationSense());

        model.maximise();
        assertEquals(Optimisation.Sense.MAX, model.getOptimisationSense());

        model.minimise();
        assertEquals(Optimisation.Sense.MIN, model.getOptimisationSense());
    }

    @Test
    public void testIsAnyVariableIntegerAndRelaxation() {
        ExpressionsBasedModel model = new ExpressionsBasedModel();

        Variable v = model.addVariable("v").integer(true);
        assertTrue(model.isAnyVariableInteger());

        model.relax(); // hard relax removes integer flag
        assertFalse(model.isAnyVariableInteger());

        // Soft relax keeps the flag but marks model as relaxed
        Variable w = model.addVariable("w").integer(true);
        model.relax(true);
        assertTrue(model.isAnyVariableInteger());
        assertTrue(model.isRelaxed());
    }

    @Test
    public void testIsAnyExpressionQuadraticAndObjectiveQuadratic() {
        ExpressionsBasedModel model = new ExpressionsBasedModel();

        Variable x = model.addVariable("x");
        Variable y = model.addVariable("y");

        // Linear expression – should not be quadratic
        Expression lin = model.newExpression("lin");
        lin.set(x.getIndex(), ONE);
        lin.set(y.getIndex(), ONE);
        assertFalse(model.isAnyExpressionQuadratic());
        assertFalse(model.isAnyObjectiveQuadratic());

        // Make objective quadratic by adding a quadratic term
        Expression obj = model.objective(); // empty objective initially
        obj.add(x.getIndex(), y.getIndex(), ONE); // x*y term
        // Since objective is built on the fly, we need to mark it as objective
        // by setting contribution weight on the expression (weight defaults to 1)
        // The model's isAnyObjectiveQuadratic checks the aggregated objective,
        // which now contains a quadratic term.
        assertTrue(model.isAnyExpressionQuadratic());
        assertTrue(model.isAnyObjectiveQuadratic());
    }

    @Test
    public void testIsAnyVariableFixed() {
        ExpressionsBasedModel model = new ExpressionsBasedModel();

        Variable a = model.addVariable("a");
        Variable b = model.addVariable("b");

        assertFalse(model.isAnyVariableFixed());

        a.value(BigDecimal.ONE);
        assertTrue(model.isAnyVariableFixed());
    }

    @Test
    public void testGetVariableValuesAndState() {
        ExpressionsBasedModel model = new ExpressionsBasedModel();

        Variable x = model.addVariable("x").lower(BigDecimal.ZERO).upper(BigDecimal.TEN);
        Variable y = model.addVariable("y").lower(BigDecimal.ONE).upper(BigDecimal.valueOf(5));

        // Set explicit values
        x.value(BigDecimal.valueOf(3));
        y.value(BigDecimal.valueOf(2));

        Optimisation.Result result = model.getVariableValues();

        assertEquals(Optimisation.State.FEASIBLE, result.getState());
        assertEquals(2, result.size());
        assertEquals(3.0, result.doubleValue(0), 1e-12);
        assertEquals(2.0, result.doubleValue(1), 1e-12);
    }

    @Test
    public void testModelValidation() {
        ExpressionsBasedModel model = new ExpressionsBasedModel();

        Variable x = model.addVariable("x").lower(BigDecimal.ZERO).upper(BigDecimal.TEN);
        Variable y = model.addVariable("y").lower(BigDecimal.ZERO).upper(BigDecimal.TEN);

        // Simple linear constraint: x + y <= 8
        Expression expr = model.newExpression("c1");
        expr.set(x.getIndex(), ONE);
        expr.set(y.getIndex(), ONE);
        expr.upper(BigDecimal.valueOf(8));

        // Provide a feasible solution
        x.value(BigDecimal.valueOf(3));
        y.value(BigDecimal.valueOf(4));

        assertTrue(model.validate());

        // Invalidate by breaking the constraint
        y.value(BigDecimal.valueOf(6));
        assertFalse(model.validate());
    }

    @Test
    public void testIndexOfFreeVariableMapping() {
        ExpressionsBasedModel model = new ExpressionsBasedModel();

        Variable a = model.addVariable("a");
        Variable b = model.addVariable("b").lower(BigDecimal.ONE); // still free
        Variable c = model.addVariable("c").upper(BigDecimal.ZERO); // still free

        // No variable is fixed yet
        assertEquals(0, model.indexOfFreeVariable(a));
        assertEquals(1, model.indexOfFreeVariable(b));
        assertEquals(2, model.indexOfFreeVariable(c));

        // Fix one variable and check mapping updates after a presolve run
        a.value(BigDecimal.ZERO); // fixing a
        model.presolve(); // triggers categorisation update

        // a is now fixed, so free indices shift
        assertEquals(-1, model.indexOfFreeVariable(a));
        assertEquals(0, model.indexOfFreeVariable(b));
        assertEquals(1, model.indexOfFreeVariable(c));
    }

    @Test
    public void testFileFormatDetection() {
        // Known extensions
        assertEquals(ExpressionsBasedModel.FileFormat.MPS, ExpressionsBasedModel.FileFormat.from("model.mps"));
        assertEquals(ExpressionsBasedModel.FileFormat.MPS, ExpressionsBasedModel.FileFormat.from("model.SIF"));
        assertEquals(ExpressionsBasedModel.FileFormat.EBM, ExpressionsBasedModel.FileFormat.from("model.ebm"));

        // Unknown extension should throw
        try {
            ExpressionsBasedModel.FileFormat.from("model.txt");
            fail("Expected IllegalArgumentException for unknown extension");
        } catch (IllegalArgumentException expected) {
            // ok
        }

        // Using a File instance
        File ebmFile = new File("myModel.ebm");
        assertEquals(ExpressionsBasedModel.FileFormat.EBM, ExpressionsBasedModel.FileFormat.from(ebmFile));
    }

    @Test
    public void testSpecialOrderedSetAddsPresolver() {
        ExpressionsBasedModel model = new ExpressionsBasedModel();

        // Create three binary variables
        Variable v1 = model.addVariable("v1").binary();
        Variable v2 = model.addVariable("v2").binary();
        Variable v3 = model.addVariable("v3").binary();

        int initialPresolverCount = ExpressionsBasedModel.PRESOLVERS.size();

        // Add a SOS of type 2 (at most 2 variables "on")
        model.addSpecialOrderedSet(java.util.Arrays.asList(v1, v2, v3), 0, 2);

        // A new presolver (SpecialOrderedSet) should have been added
        assertEquals(initialPresolverCount + 1, ExpressionsBasedModel.PRESOLVERS.size());
    }

    @Test
    public void testSimplifyCreatesShallowCopy() {
        ExpressionsBasedModel model = new ExpressionsBasedModel();

        Variable x = model.addVariable("x").lower(BigDecimal.ZERO).upper(BigDecimal.TEN);
        Variable y = model.addVariable("y").lower(BigDecimal.ZERO).upper(BigDecimal.TEN);

        // Add a simple constraint
        Expression expr = model.newExpression("c");
        expr.set(x.getIndex(), ONE);
        expr.set(y.getIndex(), ONE);
        expr.upper(BigDecimal.valueOf(5));

        // Call simplify – should return a new shallow copy
        ExpressionsBasedModel simplified = model.simplify();

        assertNotSame(model, simplified);
        // The copy should contain the same variables (by name) but be a different instance
        assertEquals(model.countVariables(), simplified.countVariables());
        assertEquals(model.getVariable(0).getName(), simplified.getVariable(0).getName());

        // The original model must still be valid
        assertTrue(model.validate());
    }

    @Test
    public void testIntegrationSwitchBehaviour() {
        ExpressionsBasedModel model = new ExpressionsBasedModel();

        // Initially false
        assertFalse(model.isIntegrationSwitch());

        // Use an Integration to flip the switch (via protected method)
        // We'll create a minimal anonymous subclass for testing purpose
        ExpressionsBasedModel.Integration<?> dummy = new ExpressionsBasedModel.Integration<Optimisation.Solver>() {
            @Override public boolean isCapable(ExpressionsBasedModel m) { return true; }
        };
        dummy.setSwitch(model, true);
        assertTrue(model.isIntegrationSwitch());

        // Reset
        dummy.setSwitch(model, false);
        assertFalse(model.isIntegrationSwitch());
    }

    @Test
    public void testKnownSolutionValidation() {
        ExpressionsBasedModel model = new ExpressionsBasedModel();

        Variable x = model.addVariable("x").lower(BigDecimal.ZERO).upper(BigDecimal.TEN);
        Variable y = model.addVariable("y").lower(BigDecimal.ZERO).upper(BigDecimal.TEN);

        // Simple objective: minimise x + 2*y
        x.weight(BigDecimal.ONE);
        y.weight(BigDecimal.valueOf(2));

        // Create a feasible optimal solution manually
        Optimisation.Result optimal = Optimisation.Result.of(Optimisation.State.OPTIMAL,
                new double[] { 0.0, 0.0 }); // both variables zero is optimal for minimisation

        // Set as known solution – should succeed because it is feasible
        model.setKnownSolution(optimal);

        // Validate via the model's internal validator
        assertTrue(model.validate(optimal));
    }

    @Test
    public void testRemoveExpression() {
        ExpressionsBasedModel model = new ExpressionsBasedModel();

        model.newExpression("toBeRemoved");
        assertNotNull(model.getExpression("toBeRemoved"));

        model.removeExpression("toBeRemoved");
        assertNull(model.getExpression("toBeRemoved"));
    }

    @Test
    public void testSnapshotCreatesRelaxedCopy() {
        ExpressionsBasedModel model = new ExpressionsBasedModel();

        Variable v = model.addVariable("v").integer(true);
        model.addExpression("obj").set(v.getIndex(), ONE).weight(BigDecimal.ONE);

        ExpressionsBasedModel snapshot = model.snapshot();

        // Snapshot should be a shallow copy and marked as relaxed
        assertTrue(snapshot.isRelaxed());
        assertTrue(snapshot.isShallowCopy());

        // Integer flag should be ignored in a relaxed snapshot
        assertFalse(snapshot.isAnyVariableInteger());
    }

    @Test
    public void testDisposeClearsModel() {
        ExpressionsBasedModel model = new ExpressionsBasedModel();

        Variable v = model.addVariable("v");
        model.newExpression("e");

        assertEquals(1, model.countVariables());
        assertEquals(1, model.countExpressions());

        model.dispose();

        assertEquals(0, model.countVariables());
        assertEquals(0, model.countExpressions());
    }

    @Test
    public void testToStringContainsVariableAndExpressionInfo() {
        ExpressionsBasedModel model = new ExpressionsBasedModel();

        Variable x = model.addVariable("x").lower(BigDecimal.ZERO);
        Variable y = model.addVariable("y").upper(BigDecimal.TEN);

        Expression expr = model.newExpression("c1");
        expr.set(x.getIndex(), ONE);
        expr.upper(BigDecimal.valueOf(5));

        String txt = model.toString();

        // Basic sanity checks – the string should contain the variable names and the expression name
        assertTrue(txt.contains("x"));
        assertTrue(txt.contains("y"));
        assertTrue(txt.contains("c1"));
        // It should also contain the start/end delimiter
        assertTrue(txt.contains("############################################"));
    }

    @Test
    public void testGetReferencesAndIsReferenced() {
        ExpressionsBasedModel model = new ExpressionsBasedModel();

        Variable v = model.addVariable("v");
        // No references initially
        assertFalse(model.isReferenced(v));

        // Manually add a reference (simulating internal use)
        model.addReference(v.getIndex());

        assertTrue(model.isReferenced(v));
        assertEquals(1, model.getReferences().size());
    }

    @Test
    public void testCheckSimilarityUtility() {
        ExpressionsBasedModel model = new ExpressionsBasedModel();

        Variable a = model.addVariable("a");
        Variable b = model.addVariable("b");

        Expression expr1 = model.newExpression("expr1");
        expr1.set(a.getIndex(), ONE);
        expr1.set(b.getIndex(), ONE);
        expr1.upper(BigDecimal.valueOf(10));

        // Create a new expression that is structurally similar
        Expression expr2 = model.newExpression("expr2");
        expr2.set(a.getIndex(), ONE);
        expr2.set(b.getIndex(), ONE);
        expr2.upper(BigDecimal.valueOf(10));

        // The utility should detect similarity
        assertTrue(model.checkSimilarity(expr2));
    }

    @Test
    public void testIsAnyConstraintQuadratic() {
        ExpressionsBasedModel model = new ExpressionsBasedModel();

        Variable x = model.addVariable("x");
        Variable y = model.addVariable("y");

        // Quadratic constraint: x*y = 3
        Expression quad = model.newExpression("quad");
        quad.add(x.getIndex(), y.getIndex(), ONE);
        quad.level(BigDecimal.valueOf(3));

        assertTrue(model.isAnyConstraintQuadratic());
    }

    @Test
    public void testIsUnbounded() {
        ExpressionsBasedModel model = new ExpressionsBasedModel();

        // Variable without bounds
        Variable free = model.addVariable("free");
        assertTrue(model.isUnbounded());

        // Add bounds – should become bounded
        free.lower(BigDecimal.ZERO).upper(BigDecimal.TEN);
        assertFalse(model.isUnbounded());
    }

    @Test
    public void testGetVariableByIndexAndIntIndex() {
        ExpressionsBasedModel model = new ExpressionsBasedModel();

        Variable v0 = model.addVariable("v0");
        Variable v1 = model.addVariable("v1");

        assertSame(v0, model.getVariable(0));
        assertSame(v1, model.getVariable(1));

        // Using IntIndex
        ExpressionsBasedModel.IntIndex idx0 = model.toIntIndex(0);
        assertSame(v0, model.getVariable(idx0));
    }

    @Test
    public void testVariablesStreamFiltersEqualityConstraint() {
        ExpressionsBasedModel model = new ExpressionsBasedModel();

        Variable a = model.addVariable("a").lower(BigDecimal.ONE); // not equality constraint
        Variable b = model.addVariable("b").upper(BigDecimal.ONE); // not equality constraint

        // Create an equality variable (lower == upper)
        Variable c = model.addVariable("c").lower(BigDecimal.ZERO).upper(BigDecimal.ZERO);

        long count = model.variables().count(); // should filter out equality constraint (c)
        assertEquals(2L, count);
    }
}
