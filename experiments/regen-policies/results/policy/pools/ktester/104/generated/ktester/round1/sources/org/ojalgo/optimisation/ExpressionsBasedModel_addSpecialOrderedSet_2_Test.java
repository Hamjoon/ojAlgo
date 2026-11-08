package org.ojalgo.optimisation;

import static org.junit.jupiter.api.Assertions.*;
import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Stream;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.api.extension.ExtendWith;
// import org.mockito.Mockito;

// @ExtendWith(MockitoExtension.class)
class ExpressionsBasedModel_addSpecialOrderedSet_2_Test {

    private static Stream<Arguments> invalidMinMaxProvider() {
        return Stream.of(// max <= 0
        Arguments.of(1, 0, "Invalid min/max number of ON variables!"), // min > max
        Arguments.of(5, 3, "Invalid min/max number of ON variables!"));
    }

    /**
     * Normal case – a valid ordered set of binary variables with min = 1 and max = 2.
     * The method should create an expression whose name follows the pattern
     * {@code SOS<max>-<orderedSet.toString()>}, with coefficient ONE for each variable
     * and appropriate lower/upper bounds.
     */
    @Test
    void testAddSpecialOrderedSetValid() {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        // Create two binary variables and add them to the model
        Variable v1 = model.addVariable().binary();
        Variable v2 = model.addVariable().binary();
        Collection<Variable> orderedSet = Arrays.asList(v1, v2);
        int min = 1;
        int max = 2;
        // Invoke the method under test
        model.addSpecialOrderedSet(orderedSet, min, max);
        // Expected expression name
        String expectedName = "SOS" + max + "-" + orderedSet.toString();
        // Retrieve the created expression
        Expression expr = model.getExpression(expectedName);
        assertNotNull(expr, "Expression should have been created with the expected name");
        // Verify bounds
        assertEquals(BigDecimal.valueOf(max), expr.upper(), "Upper bound must equal max");
        assertEquals(BigDecimal.valueOf(min), expr.lower(), "Lower bound must equal min");
        // Verify that each variable has coefficient ONE
        assertEquals(BigDecimal.ONE, expr.get(v1.getIndex()), "Coefficient for v1 must be ONE");
        assertEquals(BigDecimal.ONE, expr.get(v2.getIndex()), "Coefficient for v2 must be ONE");
    }

    /**
     * Invalid parameters – max must be positive and min must not exceed max.
     * Both situations should raise a {@link ProgrammingError}.
     */
    @Test
    void testAddSpecialOrderedSetInvalidMinMax() {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        Variable v1 = model.addVariable().binary();
        Variable v2 = model.addVariable().binary();
        Collection<Variable> orderedSet = Arrays.asList(v1, v2);
        // max <= 0
        assertThrows(ProgrammingError.class, () -> model.addSpecialOrderedSet(orderedSet, 0, 0), "max <= 0 should trigger ProgrammingError");
        // min > max
        assertThrows(ProgrammingError.class, () -> model.addSpecialOrderedSet(orderedSet, 3, 2), "min > max should trigger ProgrammingError");
    }

    /**
     * The ordered set must contain only non‑null binary variables that are already
     * part of the model. Supplying a null element or a non‑binary variable must
     * raise a {@link ProgrammingError}.
     */
    @Test
    void testAddSpecialOrderedSetInvalidVariables() {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        Variable binaryVar = model.addVariable().binary();
        // default is not binary
        Variable nonBinaryVar = model.addVariable();
        // 1. Null variable in the collection
        List<Variable> withNull = new ArrayList<>();
        withNull.add(binaryVar);
        withNull.add(null);
        assertThrows(ProgrammingError.class, () -> model.addSpecialOrderedSet(withNull, 1, 2), "Collection containing null should trigger ProgrammingError");
        // 2. Non‑binary variable in the collection
        List<Variable> withNonBinary = Arrays.asList(binaryVar, nonBinaryVar);
        assertThrows(ProgrammingError.class, () -> model.addSpecialOrderedSet(withNonBinary, 1, 2), "Collection containing a non‑binary variable should trigger ProgrammingError");
    }

    @Test
    void testAddSpecialOrderedSetNullVariableInCollection() {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        Variable binary = model.addVariable().binary();
        List<Variable> list = new ArrayList<>();
        list.add(binary);
        // null entry
        list.add(null);
        ProgrammingError ex = assertThrows(ProgrammingError.class, () -> model.addSpecialOrderedSet(list, 0, 2));
        assertTrue(ex.getMessage().contains("Variables must be binary and already inserted in the model!"));
    }

    @Test
    void testAddSpecialOrderedSetVariableWithoutIndex() {
        // Create a mock Variable whose index is null but reports as binary
        Variable mockVar = Mockito.mock(Variable.class);
        Mockito.when(mockVar.getIndex()).thenReturn(null);
        Mockito.when(mockVar.isBinary()).thenReturn(true);
        Mockito.when(mockVar.toString()).thenReturn("mockVar");
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        List<Variable> list = Collections.singletonList(mockVar);
        ProgrammingError ex = assertThrows(ProgrammingError.class, () -> model.addSpecialOrderedSet(list, 0, 2));
        assertTrue(ex.getMessage().contains("Variables must be binary and already inserted in the model!"));
    }

    @Test
    void testAddSpecialOrderedSetNonBinaryVariable() {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        // default is not binary
        Variable nonBinary = model.addVariable();
        List<Variable> list = Collections.singletonList(nonBinary);
        ProgrammingError ex = assertThrows(ProgrammingError.class, () -> model.addSpecialOrderedSet(list, 0, 2));
        assertTrue(ex.getMessage().contains("Variables must be binary and already inserted in the model!"));
    }

    @Test
    void testAddSpecialOrderedSetValid_NoLowerBound() {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        Variable v1 = model.addVariable().binary();
        Variable v2 = model.addVariable().binary();
        Variable v3 = model.addVariable().binary();
        Collection<Variable> orderedSet = Arrays.asList(v1, v2, v3);
        int min = 0;
        int max = 2;
        model.addSpecialOrderedSet(orderedSet, min, max);
        String expectedName = "SOS" + max + "-" + orderedSet.toString();
        Expression expr = model.getExpression(expectedName);
        assertNotNull(expr, "Expression should be created");
        // bounds
        assertEquals(BigDecimal.valueOf(max), expr.upper(), "Upper bound must equal max");
        assertNull(expr.lower(), "Lower bound must be null when min == 0");
        // coefficients
        for (Variable v : orderedSet) {
            assertEquals(BigDecimal.ONE, expr.get(v.getIndex()), "Coefficient for variable " + v + " must be ONE");
        }
    }

    @Test
    void testAddSpecialOrderedSetValid_WithLowerBound() {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        Variable vA = model.addVariable().binary();
        Variable vB = model.addVariable().binary();
        Collection<Variable> orderedSet = Arrays.asList(vA, vB);
        int min = 1;
        int max = 3;
        model.addSpecialOrderedSet(orderedSet, min, max);
        String expectedName = "SOS" + max + "-" + orderedSet.toString();
        Expression expr = model.getExpression(expectedName);
        assertNotNull(expr, "Expression should be created");
        assertEquals(BigDecimal.valueOf(max), expr.upper(), "Upper bound must equal max");
        assertEquals(BigDecimal.valueOf(min), expr.lower(), "Lower bound must equal min");
        for (Variable v : orderedSet) {
            assertEquals(BigDecimal.ONE, expr.get(v.getIndex()), "Coefficient for variable " + v + " must be ONE");
        }
    }

    @Test
    void testAddSpecialOrderedSet_EmptySet_ValidBounds() {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        Collection<Variable> orderedSet = Collections.emptyList();
        int min = 0;
        int max = 2;
        model.addSpecialOrderedSet(orderedSet, min, max);
        // "SOS2-[]"
        String expectedName = "SOS" + max + "-" + orderedSet.toString();
        Expression expr = model.getExpression(expectedName);
        assertNotNull(expr, "Expression should be created even for empty set");
        assertEquals(BigDecimal.valueOf(max), expr.upper(), "Upper bound must equal max");
        assertNull(expr.lower(), "Lower bound must be null when min == 0");
        // No coefficients should be present
        assertTrue(expr.getLinearKeySet().isEmpty(), "Expression should contain no variable coefficients");
    }

    @Test
    void testAddSpecialOrderedSet_SingleVariable() {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        Variable singleVar = model.addVariable().binary();
        Collection<Variable> orderedSet = Collections.singletonList(singleVar);
        int min = 0;
        int max = 1;
        model.addSpecialOrderedSet(orderedSet, min, max);
        // "SOS1-[singleVar]"
        String expectedName = "SOS" + max + "-" + orderedSet.toString();
        Expression expr = model.getExpression(expectedName);
        assertNotNull(expr, "Expression should be created");
        assertEquals(BigDecimal.valueOf(max), expr.upper(), "Upper bound must equal max");
        assertNull(expr.lower(), "Lower bound must be null when min == 0");
        assertEquals(BigDecimal.ONE, expr.get(singleVar.getIndex()), "Coefficient for the single variable must be ONE");
    }

    @Test
    void testAddSpecialOrderedSet_MaxEqualsSetSize() {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        Variable a = model.addVariable().binary();
        Variable b = model.addVariable().binary();
        Variable c = model.addVariable().binary();
        Collection<Variable> orderedSet = Arrays.asList(a, b, c);
        int min = 0;
        // equals size
        int max = 3;
        model.addSpecialOrderedSet(orderedSet, min, max);
        // "SOS3-[a, b, c]"
        String expectedName = "SOS" + max + "-" + orderedSet.toString();
        Expression expr = model.getExpression(expectedName);
        assertNotNull(expr, "Expression should be created");
        assertEquals(BigDecimal.valueOf(max), expr.upper(), "Upper bound must equal max");
        assertNull(expr.lower(), "Lower bound must be null when min == 0");
        for (Variable v : orderedSet) {
            assertEquals(BigDecimal.ONE, expr.get(v.getIndex()), "Coefficient for variable " + v + " must be ONE");
        }
    }

    @Test
    void testAddSpecialOrderedSet_MinEqualsMax() {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        Variable x = model.addVariable().binary();
        Variable y = model.addVariable().binary();
        Collection<Variable> orderedSet = Arrays.asList(x, y);
        int min = 2;
        int max = 2;
        model.addSpecialOrderedSet(orderedSet, min, max);
        // "SOS2-[x, y]"
        String expectedName = "SOS" + max + "-" + orderedSet.toString();
        Expression expr = model.getExpression(expectedName);
        assertNotNull(expr, "Expression should be created");
        assertEquals(BigDecimal.valueOf(max), expr.upper(), "Upper bound must equal max");
        assertEquals(BigDecimal.valueOf(min), expr.lower(), "Lower bound must equal min");
        for (Variable v : orderedSet) {
            assertEquals(BigDecimal.ONE, expr.get(v.getIndex()), "Coefficient for variable " + v + " must be ONE");
        }
    }

    @Test
    void testAddSpecialOrderedSet_DuplicateVariables() {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        Variable dup = model.addVariable().binary();
        // List contains the same instance twice
        Collection<Variable> orderedSet = Arrays.asList(dup, dup);
        int min = 0;
        int max = 2;
        model.addSpecialOrderedSet(orderedSet, min, max);
        // "SOS2-[dup, dup]"
        String expectedName = "SOS" + max + "-" + orderedSet.toString();
        Expression expr = model.getExpression(expectedName);
        assertNotNull(expr, "Expression should be created");
        assertEquals(BigDecimal.valueOf(max), expr.upper(), "Upper bound must equal max");
        assertNull(expr.lower(), "Lower bound must be null when min == 0");
        // Even though the variable appears twice, the coefficient should still be ONE
        assertEquals(BigDecimal.ONE, expr.get(dup.getIndex()), "Coefficient for duplicated variable must be ONE");
    }

    @Test
    void testAddSpecialOrderedSet_MaxIntegerBound() {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        Variable vMax = model.addVariable().binary();
        Collection<Variable> orderedSet = Collections.singletonList(vMax);
        int min = 0;
        int max = Integer.MAX_VALUE;
        model.addSpecialOrderedSet(orderedSet, min, max);
        // huge name
        String expectedName = "SOS" + max + "-" + orderedSet.toString();
        Expression expr = model.getExpression(expectedName);
        assertNotNull(expr, "Expression should be created with huge max");
        assertEquals(BigDecimal.valueOf(max), expr.upper(), "Upper bound must equal Integer.MAX_VALUE");
        assertNull(expr.lower(), "Lower bound must be null when min == 0");
        assertEquals(BigDecimal.ONE, expr.get(vMax.getIndex()), "Coefficient for the variable must be ONE");
    }

    @Test
    void testAddSpecialOrderedSet_NullOrderedSet() {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        assertThrows(NullPointerException.class, () -> model.addSpecialOrderedSet(null, 0, 1), "Passing a null collection should raise NullPointerException");
    }
}