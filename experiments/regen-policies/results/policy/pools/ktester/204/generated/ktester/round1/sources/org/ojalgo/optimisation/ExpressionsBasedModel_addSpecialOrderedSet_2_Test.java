package org.ojalgo.optimisation;

import static org.junit.jupiter.api.Assertions.*;
import static org.ojalgo.function.constant.BigMath.ONE;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Collections;
import java.util.stream.Stream;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;

// @ExtendWith(MockitoExtension.class)
class ExpressionsBasedModel_addSpecialOrderedSet_2_Test {

    private ExpressionsBasedModel model;

    @BeforeEach
    void setupBeforeEach() {
        // a fresh model for each test
        model = new ExpressionsBasedModel();
    }

    private static Stream<Arguments> invalidMinMaxProvider() {
        return Stream.of(// negative min
        Arguments.of(-1, 5, "min is negative"), // min greater than max
        Arguments.of(6, 5, "min > max"), // max non‑positive (zero)
        Arguments.of(0, 0, "max == 0"));
    }

    private static Stream<Arguments> invalidVariableProvider() {
        // 1) null entry
        Collection<Variable> nullEntry = List.of((Variable) null);
        // 2) variable with null index (mocked)
        Variable mockNoIndex = mock(Variable.class);
        when(mockNoIndex.getIndex()).thenReturn(null);
        when(mockNoIndex.isBinary()).thenReturn(true);
        Collection<Variable> noIndex = List.of(mockNoIndex);
        // 3) non‑binary variable (real variable, not set to binary)
        // continuous by default
        Variable nonBinary = new ExpressionsBasedModel().addVariable();
        Collection<Variable> nonBinaryColl = List.of(nonBinary);
        return Stream.of(Arguments.of(nullEntry, "ordered set contains a null variable"), Arguments.of(noIndex, "ordered set contains a variable with null index (not inserted)"), Arguments.of(nonBinaryColl, "ordered set contains a non‑binary variable"));
    }

    /**
     * Happy‑path: a valid SOS constraint is added and the generated expression has the expected bounds.
     */
    @Test
    void testAddSpecialOrderedSetValid() {
        // GIVEN – three binary variables already inserted into the model
        Variable v0 = model.addVariable().binary();
        Variable v1 = model.addVariable().binary();
        Variable v2 = model.addVariable().binary();
        List<Variable> orderedSet = new ArrayList<>();
        orderedSet.add(v0);
        orderedSet.add(v1);
        orderedSet.add(v2);
        int min = 1;
        int max = 2;
        // WHEN – the SOS constraint is added
        model.addSpecialOrderedSet(orderedSet, min, max);
        // THEN – an expression named "SOS2-[v0, v1, v2]" (the exact toString of the list) must exist
        String expectedName = "SOS" + max + "-" + orderedSet.toString();
        Expression expr = model.getExpression(expectedName);
        assertNotNull(expr, "Expression with name " + expectedName + " should have been created");
        // Upper bound must be equal to {@code max}
        assertEquals(BigDecimal.valueOf(max), expr.getUpperLimit(), "Upper bound of the SOS expression must match the supplied max");
        // Lower bound must be equal to {@code min} (since min > 0)
        assertEquals(BigDecimal.valueOf(min), expr.getLowerLimit(), "Lower bound of the SOS expression must match the supplied min");
        // All variables must have a coefficient of ONE in the expression
        for (Variable var : orderedSet) {
            assertEquals(ONE, expr.get(var.getIndex()), "Variable " + var + " should have coefficient ONE");
        }
    }

    /**
     * Invalid {@code min}/{@code max} arguments must cause a {@link ProgrammingError}.
     */
    @Test
    void testAddSpecialOrderedSetInvalidMinMax() {
        // GIVEN – a non‑empty ordered set (the actual content is irrelevant because validation fails earlier)
        Variable v = model.addVariable().binary();
        Collection<Variable> set = List.of(v);
        // invalid
        int min = -1;
        int max = 5;
        // WHEN / THEN – a ProgrammingError must be thrown with the exact message from the implementation
        ProgrammingError ex = assertThrows(ProgrammingError.class, () -> model.addSpecialOrderedSet(set, min, max), "Invalid min/max should raise ProgrammingError");
        assertEquals("Invalid min/max number of ON variables!", ex.getMessage(), "Exception message must match the one defined in the source");
    }

    /**
     * Adding a variable that is not binary or not yet part of the model must raise {@link ProgrammingError}.
     */
    @Test
    void testAddSpecialOrderedSetInvalidVariable() {
        // Variable that is NOT binary (default is continuous)
        Variable nonBinary = model.addVariable();
        // Collection containing a non‑binary variable
        Collection<Variable> setWithNonBinary = List.of(nonBinary);
        // Collection containing a null entry
        Collection<Variable> setWithNull = List.of((Variable) null);
        // 1) non‑binary variable
        assertThrows(ProgrammingError.class, () -> model.addSpecialOrderedSet(setWithNonBinary, 1, 1), "Non‑binary variable should cause ProgrammingError");
        // 2) null variable entry
        assertThrows(ProgrammingError.class, () -> model.addSpecialOrderedSet(setWithNull, 1, 1), "Null variable entry should cause ProgrammingError");
    }

    @Test
    void testAddSpecialOrderedSetValid_MinZero() {
        // GIVEN – two binary variables already part of the model
        Variable v1 = model.addVariable().binary();
        Variable v2 = model.addVariable().binary();
        List<Variable> orderedSet = List.of(v1, v2);
        int min = 0;
        int max = 2;
        // WHEN
        model.addSpecialOrderedSet(orderedSet, min, max);
        // THEN – expression exists with correct name and bounds
        String expectedName = "SOS" + max + "-" + orderedSet.toString();
        Expression expr = model.getExpression(expectedName);
        assertNotNull(expr, "Expression should have been created");
        assertEquals(BigDecimal.valueOf(max), expr.getUpperLimit(), "Upper bound must equal the supplied max");
        assertNull(expr.getLowerLimit(), "Lower bound must be null when min == 0");
        // All variables must have coefficient ONE
        for (Variable var : orderedSet) {
            assertEquals(ONE, expr.get(var.getIndex()), "Variable " + var + " should have coefficient ONE");
        }
    }

    @Test
    void testAddSpecialOrderedSetValid_MinPositive() {
        // GIVEN – three binary variables
        Variable a = model.addVariable().binary();
        Variable b = model.addVariable().binary();
        Variable c = model.addVariable().binary();
        List<Variable> orderedSet = List.of(a, b, c);
        int min = 1;
        int max = 3;
        // WHEN
        model.addSpecialOrderedSet(orderedSet, min, max);
        // THEN
        String expectedName = "SOS" + max + "-" + orderedSet.toString();
        Expression expr = model.getExpression(expectedName);
        assertNotNull(expr, "Expression should have been created");
        assertEquals(BigDecimal.valueOf(max), expr.getUpperLimit(), "Upper bound must equal max");
        assertEquals(BigDecimal.valueOf(min), expr.getLowerLimit(), "Lower bound must equal min");
        for (Variable var : orderedSet) {
            assertEquals(ONE, expr.get(var.getIndex()), "Variable " + var + " should have coefficient ONE");
        }
    }

    @Test
    void testAddSpecialOrderedSetEmptyOrderedSet() {
        // GIVEN – an empty collection
        List<Variable> orderedSet = Collections.emptyList();
        int min = 0;
        int max = 1;
        // WHEN
        model.addSpecialOrderedSet(orderedSet, min, max);
        // THEN – expression exists with correct name and only upper bound
        String expectedName = "SOS" + max + "-" + orderedSet.toString();
        Expression expr = model.getExpression(expectedName);
        assertNotNull(expr, "Expression for empty set should be created");
        assertEquals(BigDecimal.valueOf(max), expr.getUpperLimit(), "Upper bound must be set even for empty set");
        assertNull(expr.getLowerLimit(), "Lower bound must be null when min == 0");
        // No coefficients should be present – the loop will not execute for an empty set
        for (Variable var : orderedSet) {
            fail("Ordered set is empty – this block must not be executed");
        }
    }

    @Test
    void testAddSpecialOrderedSetMinEqualsMax() {
        // GIVEN – two binary variables
        Variable x = model.addVariable().binary();
        Variable y = model.addVariable().binary();
        List<Variable> orderedSet = List.of(x, y);
        int min = 2;
        int max = 2;
        // WHEN
        model.addSpecialOrderedSet(orderedSet, min, max);
        // THEN
        String expectedName = "SOS" + max + "-" + orderedSet.toString();
        Expression expr = model.getExpression(expectedName);
        assertNotNull(expr, "Expression should have been created");
        assertEquals(BigDecimal.valueOf(max), expr.getUpperLimit(), "Upper bound must equal max");
        assertEquals(BigDecimal.valueOf(min), expr.getLowerLimit(), "Lower bound must equal min (exact count)");
    }

    @Test
    void testAddSpecialOrderedSetMultipleVariables() {
        // GIVEN – five binary variables
        Variable v1 = model.addVariable().binary();
        Variable v2 = model.addVariable().binary();
        Variable v3 = model.addVariable().binary();
        Variable v4 = model.addVariable().binary();
        Variable v5 = model.addVariable().binary();
        List<Variable> orderedSet = List.of(v1, v2, v3, v4, v5);
        int min = 1;
        int max = 5;
        // WHEN
        model.addSpecialOrderedSet(orderedSet, min, max);
        // THEN
        String expectedName = "SOS" + max + "-" + orderedSet.toString();
        Expression expr = model.getExpression(expectedName);
        assertNotNull(expr, "Expression should have been created");
        assertEquals(BigDecimal.valueOf(max), expr.getUpperLimit(), "Upper bound must be max");
        assertEquals(BigDecimal.valueOf(min), expr.getLowerLimit(), "Lower bound must be min");
        // Verify coefficient ONE for each variable
        for (Variable var : orderedSet) {
            assertEquals(ONE, expr.get(var.getIndex()), "Variable " + var + " must have coefficient ONE");
        }
    }

    @Test
    void testExpressionCoefficientVerification() {
        Variable a = model.addVariable().binary();
        Variable b = model.addVariable().binary();
        List<Variable> orderedSet = List.of(a, b);
        model.addSpecialOrderedSet(orderedSet, 0, 2);
        String expectedName = "SOS2-" + orderedSet.toString();
        Expression expr = model.getExpression(expectedName);
        assertNotNull(expr, "Expression must exist");
        assertEquals(ONE, expr.get(a.getIndex()), "Coefficient for variable a must be ONE");
        assertEquals(ONE, expr.get(b.getIndex()), "Coefficient for variable b must be ONE");
    }

    @Test
    void testExpressionCountIncrement() {
        int before = model.countExpressions();
        Variable p = model.addVariable().binary();
        Variable q = model.addVariable().binary();
        List<Variable> orderedSet = List.of(p, q);
        model.addSpecialOrderedSet(orderedSet, 0, 2);
        int after = model.countExpressions();
        assertEquals(before + 1, after, "Adding an SOS constraint must increase the expression count by exactly one");
    }

    @Test
    void testAddSpecialOrderedSetNullOrderedSet() {
        // The implementation calls orderedSet.toString() before any explicit null‑check,
        // therefore a NullPointerException is expected.
        assertThrows(NullPointerException.class, () -> model.addSpecialOrderedSet(null, 1, 2), "Passing a null ordered set should raise NullPointerException");
    }
}