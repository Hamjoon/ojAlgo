package org.ojalgo.optimisation;

import static org.junit.Assert.*;

import java.io.File;
import java.math.BigDecimal;
import java.util.*;

import org.junit.Test;
import org.ojalgo.type.context.NumberContext;
import org.ojalgo.netio.BasicLogger;

/**
 * Comprehensive JUnit‑4 test suite for {@link ExpressionsBasedModel}.
 *
 * The tests exercise public behaviour only – no mocking, no subclassing of library
 * internals – and avoid invoking the actual solvers (which would require external
 * binaries).  All objects are created through the constructors or factory methods
 * that appear in the source code supplied.
 */
public class ExpressionsBasedModelTest {

    /** Helper to create a model with a reasonable feasibility context. */
    private ExpressionsBasedModel newModel() {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        // Use a tight numeric context to avoid unexpected rounding issues.
        model.options.feasibility = NumberContext.of(8);
        return model;
    }

    @Test
    public void testAddVariablesAndBasicProperties() {
        ExpressionsBasedModel model = newModel();

        Variable x = model.addVariable("x");
        Variable y = model.addVariable("y");
        Variable z = model.addVariable("z");

        // Set bounds and integer flags
        x.lower(BigDecimal.ZERO).upper(BigDecimal.TEN);               // free variable
        y.lower(BigDecimal.ONE).upper(BigDecimal.ONE);                // fixed variable (lower == upper)
        z.lower(BigDecimal.valueOf(-5)).upper(BigDecimal.valueOf(5)).integer(); // integer variable

        // Verify counts via Description
        ExpressionsBasedModel.Description desc = model.describe();

        assertEquals(3, desc.nbVariables);
        assertEquals(1, desc.nbIntegerVariables);
        assertEquals(1, desc.nbPositiveVariables); // x and y are positive (upper >=0), but z also positive range
        assertEquals(1, desc.nbNegativeVariables); // z has negative lower bound
        assertEquals(1, desc.nbLowerBounds); // y lower bound (also x)
        assertEquals(2, desc.nbUpperBounds); // x and z upper bounds
        assertEquals(0, desc.nbEqualityBounds); // no equality constraints on variables

        // Check free / integer / positive / negative lists
        List<Variable> free = model.getFreeVariables();
        assertTrue(free.contains(x));
        assertTrue(free.contains(z));
        assertFalse(free.contains(y)); // y is fixed

        List<Variable> intVars = model.getIntegerVariables();
        assertTrue(intVars.contains(z));
        assertFalse(intVars.contains(x));
        assertFalse(intVars.contains(y));

        List<Variable> posVars = model.getPositiveVariables();
        assertTrue(posVars.contains(x));
        assertTrue(posVars.contains(y));
        assertTrue(posVars.contains(z));

        List<Variable> negVars = model.getNegativeVariables();
        assertTrue(negVars.contains(z));
        assertFalse(negVars.contains(x));
        assertFalse(negVars.contains(y));
    }

    @Test
    public void testIndexMappingAndFreeVariableIndices() {
        ExpressionsBasedModel model = newModel();

        Variable a = model.addVariable("a");
        Variable b = model.addVariable("b");
        Variable c = model.addVariable("c");

        // a and c are free, b will be fixed
        b.lower(BigDecimal.ONE).upper(BigDecimal.ONE); // fixed

        // Global indices
        int idxA = model.indexOf(a);
        int idxB = model.indexOf(b);
        int idxC = model.indexOf(c);

        // Free variable local indices
        assertEquals(0, model.indexOfFreeVariable(idxA));
        assertEquals(-1, model.indexOfFreeVariable(idxB)); // fixed -> not free
        assertEquals(1, model.indexOfFreeVariable(idxC));

        // Integer variable indices (none are integer yet)
        assertEquals(-1, model.indexOfIntegerVariable(idxA));
        assertEquals(-1, model.indexOfIntegerVariable(idxB));
        assertEquals(-1, model.indexOfIntegerVariable(idxC));

        // Make c integer and test mapping
        c.integer();
        assertEquals(0, model.indexOfIntegerVariable(idxC));
    }

    @Test
    public void testVariableValueRetrievalAndValidation() {
        ExpressionsBasedModel model = newModel();

        Variable v1 = model.addVariable("v1");
        Variable v2 = model.addVariable("v2");
        Variable v3 = model.addVariable("v3");

        // Set explicit values
        v1.setValue(BigDecimal.valueOf(2));
        v2.setValue(BigDecimal.valueOf(-1));
        // v3 left without a value – will be treated as 0 by getVariableValues()

        Optimisation.Result result = model.getVariableValues();

        // Verify state is FEASIBLE (all variables have enough info)
        assertEquals(Optimisation.State.FEASIBLE, result.getState());

        // Verify the solution vector matches expectations
        assertEquals(2.0, result.doubleValue(0), 1e-12);
        assertEquals(-1.0, result.doubleValue(1), 1e-12);
        assertEquals(0.0, result.doubleValue(2), 1e-12);

        // Validate the model against the obtained solution
        assertTrue(model.validate(result));
    }

    @Test
    public void testObjectiveConstructionAndLimitObjective() {
        ExpressionsBasedModel model = newModel();

        Variable x = model.addVariable("x").weight(BigDecimal.ONE);
        Variable y = model.addVariable("y").weight(BigDecimal.valueOf(2));

        // Create a separate expression that also contributes to the objective
        Expression expr = model.addExpression("extra");
        expr.add(x, BigDecimal.valueOf(3));
        expr.add(y, BigDecimal.valueOf(-1));
        expr.weight(BigDecimal.valueOf(2)); // contribution weight

        // Build the aggregated objective
        Expression objective = model.objective();

        // Expected linear coefficients:
        // x: 1 (from variable) + 2*3 = 7
        // y: 2 (from variable) + 2*(-1) = 0
        assertEquals(BigDecimal.valueOf(7), objective.get(x));
        assertEquals(BigDecimal.ZERO, objective.get(y));

        // Apply a limit on the objective value
        Expression limited = model.limitObjective(BigDecimal.ZERO, BigDecimal.valueOf(10));
        // The limited expression should be the same as the generated constraint (or the objective itself)
        assertNotNull(limited);
        assertEquals(BigDecimal.ZERO, limited.getLowerLimit());
        assertEquals(BigDecimal.valueOf(10), limited.getUpperLimit());
    }

    @Test
    public void testFileFormatParsing() {
        assertEquals(ExpressionsBasedModel.FileFormat.MPS, ExpressionsBasedModel.FileFormat.from("model.mps"));
        assertEquals(ExpressionsBasedModel.FileFormat.MPS, ExpressionsBasedModel.FileFormat.from("MODEL.SIF"));
        assertEquals(ExpressionsBasedModel.FileFormat.EBM, ExpressionsBasedModel.FileFormat.from("data.ebm"));
        assertEquals(ExpressionsBasedModel.FileFormat.EBM, ExpressionsBasedModel.FileFormat.from("path/to/FILE.EBM"));

        try {
            ExpressionsBasedModel.FileFormat.from("unknown.xyz");
            fail("Expected IllegalArgumentException for unknown extension");
        } catch (IllegalArgumentException expected) {
            // ok
        }
    }

    @Test
    public void testIntegerAndRelaxedFlags() {
        ExpressionsBasedModel model = newModel();

        Variable i1 = model.addVariable("i1").integer();
        Variable i2 = model.addVariable("i2").integer();
        Variable f1 = model.addVariable("f1"); // free (continuous)

        assertTrue(model.isAnyVariableInteger());

        // Relax the model – integer flags are ignored for optimisation
        model.relax();
        assertFalse(model.isAnyVariableInteger());

        // Soft relaxation – integer flag stays but model is marked relaxed
        model.relax(true);
        assertTrue(model.isRelaxed());
        // isAnyVariableInteger should still report true because variables are still integer
        assertTrue(model.isAnyVariableInteger());
    }

    @Test
    public void testFixedAndInfeasibleDetection() {
        ExpressionsBasedModel model = newModel();

        Variable fixed = model.addVariable("fixed");
        fixed.lower(BigDecimal.valueOf(5)).upper(BigDecimal.valueOf(5)); // fixed at 5

        Variable infeas = model.addVariable("infeas");
        infeas.lower(BigDecimal.valueOf(10)).upper(BigDecimal.valueOf(2)); // contradictory bounds

        assertTrue(model.isAnyVariableFixed());
        assertTrue(model.isInfeasible());
    }

    @Test
    public void testUnboundedDetection() {
        ExpressionsBasedModel model = newModel();

        Variable u = model.addVariable("u"); // no bounds
        assertTrue(model.isUnbounded());

        Variable bounded = model.addVariable("b").lower(BigDecimal.ZERO).upper(BigDecimal.ONE);
        assertTrue(model.isUnbounded()); // still true because at least one variable is unbounded
    }

    @Test
    public void testSpecialOrderedSetCreation() {
        ExpressionsBasedModel model = newModel();

        Variable v1 = model.addVariable("v1").binary();
        Variable v2 = model.addVariable("v2").binary();
        Variable v3 = model.addVariable("v3").binary();

        List<Variable> sos = Arrays.asList(v1, v2, v3);
        // Create a SOS with min=0 and max=2 (type = 2)
        model.addSpecialOrderedSet(sos, 0, 2);

        // The SOS creates an internal expression named "SOS2-..."
        // Verify that an expression with that prefix exists and has correct bounds
        Optional<Expression> sosExpr = model.getExpressions().stream()
                .filter(e -> e.getName().startsWith("SOS2-"))
                .findFirst();

        assertTrue("SOS expression should be present", sosExpr.isPresent());
        Expression expr = sosExpr.get();
        assertEquals(BigDecimal.ZERO, expr.getLowerLimit());
        assertEquals(BigDecimal.valueOf(2), expr.getUpperLimit());
    }

    @Test
    public void testCopyAndSnapshotBehaviour() {
        ExpressionsBasedModel original = newModel();

        Variable a = original.addVariable("a").lower(BigDecimal.ONE).upper(BigDecimal.TEN);
        Variable b = original.addVariable("b").integer();
        original.addExpression("c1").add(a, BigDecimal.ONE).upper(BigDecimal.valueOf(5));

        // Shallow copy (no pruning)
        ExpressionsBasedModel shallowCopy = original.copy(true, false);
        assertEquals(original.countVariables(), shallowCopy.countVariables());
        assertEquals(original.countExpressions(), shallowCopy.countExpressions());

        // Deep copy with pruning (remove non‑objective, non‑constraint expressions)
        ExpressionsBasedModel deepCopy = original.copy(false, true);
        // The deep copy should still contain the same expression because it is a constraint
        assertEquals(1, deepCopy.countExpressions());

        // Snapshot creates a shallow copy that is relaxed (soft)
        ExpressionsBasedModel snapshot = original.snapshot();
        assertTrue(snapshot.isRelaxed());
        assertTrue(snapshot.isShallowCopy());
    }

    @Test
    public void testValidateWithCustomContextAndLogger() {
        ExpressionsBasedModel model = newModel();

        Variable v = model.addVariable("v");
        v.lower(BigDecimal.ZERO).upper(BigDecimal.valueOf(3));

        // Set a feasible value
        v.setValue(BigDecimal.valueOf(2));

        // Use a custom NumberContext that is very strict
        NumberContext strict = NumberContext.of(12);
        BasicLogger logger = BasicLogger.NULL;

        assertTrue(model.validate(strict, logger));
    }

    @Test
    public void testDescribeCountsWithConstraints() {
        ExpressionsBasedModel model = newModel();

        Variable x = model.addVariable("x").lower(BigDecimal.ZERO).upper(BigDecimal.TEN);
        Variable y = model.addVariable("y").lower(BigDecimal.ONE).upper(BigDecimal.ONE); // fixed

        // Add constraint expressions
        Expression expr1 = model.addExpression("c1");
        expr1.add(x, BigDecimal.ONE).add(y, BigDecimal.ONE).upper(BigDecimal.valueOf(8));

        Expression expr2 = model.addExpression("c2");
        expr2.add(x, BigDecimal.valueOf(2)).lower(BigDecimal.valueOf(5));

        ExpressionsBasedModel.Description desc = model.describe();

        // Variable related counts
        assertEquals(2, desc.nbVariables);
        assertEquals(0, desc.nbIntegerVariables);
        assertEquals(1, desc.nbLowerBounds);   // x lower bound
        assertEquals(1, desc.nbUpperBounds);   // x upper bound
        assertEquals(0, desc.nbEqualityBounds); // none

        // Expression (constraint) related counts
        assertEquals(2, desc.nbLowerConstraints);   // expr2 lower
        assertEquals(1, desc.nbUpperConstraints);   // expr1 upper
        assertEquals(0, desc.nbEqualityConstraints); // none
    }

    @Test
    public void testIntegrationSwitchAndValidatorCreation() {
        ExpressionsBasedModel model = newModel();

        // The integration switch is initially false
        assertFalse(model.isIntegrationSwitch());

        // Create a dummy integration (using the built‑in LinearSolver integration)
        ExpressionsBasedModel.Integration<?> integration = model.getIntegration();

        // The validator should be created without throwing
        ExpressionsBasedModel.Validator validator = integration.newValidator(model);
        assertNotNull(validator);

        // Toggle the switch via the protected method through the integration
        integration.setSwitch(model, true);
        assertTrue(model.isIntegrationSwitch());

        // Verify that the switch flag is respected by the integration helper
        assertTrue(integration.isSwitch(model));
    }
}
