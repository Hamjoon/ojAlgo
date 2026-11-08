package org.ojalgo.optimisation;

import static org.junit.jupiter.api.Assertions.*;
import static org.ojalgo.function.constant.BigMath.*;
import java.math.BigDecimal;
import java.util.*;
import org.junit.jupiter.api.*;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.Arguments;

// @ExtendWith(MockitoExtension.class)
class ExpressionsBasedModel_addSpecialOrderedSet_2_Test {

    private ExpressionsBasedModel model;

    private Variable binaryVar1;

    private Variable binaryVar2;

    private Variable nonBinaryVar;

    @BeforeEach
    void setupBeforeEach() {
        // a fresh model for each test
        model = new ExpressionsBasedModel();
        // binary variables that are already inserted in the model
        binaryVar1 = model.addVariable().binary().weight(ONE);
        binaryVar2 = model.addVariable().binary().weight(ONE);
        // a variable that is NOT binary (used for negative tests)
        nonBinaryVar = model.addVariable().weight(ONE);
    }

    /**
     * Creates {@code n} fresh binary variables that are inserted into the model.
     */
    private List<Variable> createBinaryVariables(int n) {
        List<Variable> list = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            list.add(model.addVariable().binary().weight(ONE));
        }
        return list;
    }

    /**
     * Returns a fresh {@link Variable} that is NOT inserted into the model (its index is {@code null}).
     */
    private Variable externalBinaryVariable() {
        // index == null
        return Variable.make("external").binary();
    }

    /**
     * Provides test data for the “invalid min/max values” group.
     */
    private static Stream<Arguments> invalidMinMaxProvider() {
        return Stream.of(// max <= 0
        Arguments.of(Collections.emptyList(), 0, 0, ProgrammingError.class, "Invalid min/max number of ON variables!"), // min > max
        Arguments.of(Collections.emptyList(), 5, 3, ProgrammingError.class, "Invalid min/max number of ON variables!"));
    }

    /**
     * Provides test data for the “invalid variable in ordered set” group.
     */
    private static Stream<Arguments> invalidVariableProvider() {
        return Stream.of(// null element
        Arguments.of(Arrays.asList(null), 1, 2, ProgrammingError.class, "Variables must be binary and already inserted in the model!"), // variable not inserted (index == null)
        Arguments.of(Arrays.asList(Variable.make("external").binary()), 1, 2, ProgrammingError.class, "Variables must be binary and already inserted in the model!"), // variable not binary (will be replaced with a non‑binary variable in the test)
        Arguments.of(Arrays.asList(Variable.make("external").binary()), 1, 2, ProgrammingError.class, "Variables must be binary and already inserted in the model!"));
    }

    /**
     * Provides test data for the “valid SOS – min = 0” group (including empty set).
     */
    private static Stream<Arguments> validMinZeroProvider() {
        return Stream.of(// normal non‑empty set
        Arguments.of(Arrays.asList("v1", "v2"), 0, 2, false), // empty set – allowed when min == 0
        Arguments.of(Collections.emptyList(), 0, 2, true));
    }

    /**
     * Provides test data for the “valid SOS – min > 0” group.
     */
    private static Stream<Arguments> validMinPositiveProvider() {
        return Stream.of(// three variables, min=1, max=3
        Arguments.of(Arrays.asList("v1", "v2", "v3"), 1, 3, false), // duplicate variable (same instance twice)
        Arguments.of(Arrays.asList("dup", "dup"), 1, 2, false), // ordered set larger than max
        Arguments.of(Arrays.asList("v1", "v2", "v3", "v4"), 1, 2, false), // negative min (treated as zero)
        Arguments.of(Arrays.asList("v1", "v2"), -1, 2, false), // min == max
        Arguments.of(Arrays.asList("v1", "v2"), 2, 2, false), // max = 1 (type‑1 SOS)
        Arguments.of(Arrays.asList("single"), 0, 1, false));
    }

    /**
     * Utility to turn a list of names into actual binary variables inserted into the model.
     */
    private List<Variable> variablesFromNames(List<String> names) {
        List<Variable> vars = new ArrayList<>();
        for (String name : names) {
            vars.add(model.addVariable(name).binary().weight(ONE));
        }
        return vars;
    }

    /**
     * Happy path – a proper SOS set is added and the generated expression has the expected bounds and coefficients.
     */
    @Test
    void testAddSpecialOrderedSetValid() {
        Collection<Variable> orderedSet = Arrays.asList(binaryVar1, binaryVar2);
        int min = 1;
        int max = 2;
        model.addSpecialOrderedSet(orderedSet, min, max);
        Expression sosExpr = model.expressions().filter(e -> e.getName() != null && e.getName().contains("SOS" + max)).findFirst().orElseThrow(() -> new AssertionError("SOS expression not found"));
        assertEquals(BigDecimal.valueOf(max), sosExpr.upper());
        assertEquals(BigDecimal.valueOf(min), sosExpr.lower());
        assertEquals(ONE, sosExpr.get(binaryVar1.getIndex(), false));
        assertEquals(ONE, sosExpr.get(binaryVar2.getIndex(), false));
    }

    /**
     * Invalid arguments – max <= 0 should raise a ProgrammingError.
     */
    @Test
    void testAddSpecialOrderedSetInvalidMax() {
        Collection<Variable> orderedSet = Collections.singletonList(binaryVar1);
        assertThrows(ProgrammingError.class, () -> model.addSpecialOrderedSet(orderedSet, 0, 0));
    }

    /**
     * Invalid arguments – min > max should raise a ProgrammingError.
     */
    @Test
    void testAddSpecialOrderedSetMinGreaterThanMax() {
        Collection<Variable> orderedSet = Collections.singletonList(binaryVar1);
        assertThrows(ProgrammingError.class, () -> model.addSpecialOrderedSet(orderedSet, 3, 2));
    }

    /**
     * Variables must be binary – providing a non‑binary variable must raise a ProgrammingError.
     */
    @Test
    void testAddSpecialOrderedSetNonBinaryVariable() {
        Collection<Variable> orderedSet = Arrays.asList(binaryVar1, nonBinaryVar);
        assertThrows(ProgrammingError.class, () -> model.addSpecialOrderedSet(orderedSet, 1, 2));
    }

    /**
     * Variables must already be inserted in the model – using a freshly created variable (not added) must raise a ProgrammingError.
     */
    @Test
    void testAddSpecialOrderedSetVariableNotInserted() {
        Variable external = Variable.make("external").binary();
        Collection<Variable> orderedSet = Arrays.asList(binaryVar1, external);
        assertThrows(ProgrammingError.class, () -> model.addSpecialOrderedSet(orderedSet, 1, 2));
    }

    /**
     * Null variable in the collection must raise a ProgrammingError.
     */
    @Test
    void testAddSpecialOrderedSetNullVariable() {
        Collection<Variable> orderedSet = Arrays.asList(binaryVar1, null);
        assertThrows(ProgrammingError.class, () -> model.addSpecialOrderedSet(orderedSet, 1, 2));
    }

    @ParameterizedTest(name = "[{index}] orderedSet size={0}, min={1}, max={2}")
    @MethodSource("invalidMinMaxProvider")
    void testInvalidMinMaxValues(Collection<Variable> orderedSet, int min, int max, Class<? extends Throwable> expectedException, String expectedMessage) {
        assertThrows(expectedException, () -> model.addSpecialOrderedSet(orderedSet, min, max), expectedMessage);
    }

    @Test
    void testNullOrderedSet() {
        assertThrows(NullPointerException.class, () -> model.addSpecialOrderedSet(null, 1, 2));
    }

    @ParameterizedTest(name = "[{index}] orderedSet contains invalid variable")
    @MethodSource("invalidVariableProvider")
    void testInvalidVariableInOrderedSet(Collection<Variable> orderedSet, int min, int max, Class<? extends Throwable> expectedException, String expectedMessage) {
        // Convert raw variables to the appropriate form for the test
        List<Variable> actualSet = new ArrayList<>();
        for (Variable v : orderedSet) {
            if (v == null) {
                actualSet.add(null);
            } else if (v.isBinary()) {
                // keep as‑is (external binary variable – not inserted)
                actualSet.add(v);
            } else {
                // replace with the non‑binary variable from the fixture
                actualSet.add(nonBinaryVar);
            }
        }
        assertThrows(expectedException, () -> model.addSpecialOrderedSet(actualSet, min, max), expectedMessage);
    }

    @Test
    void testVariableNotInserted() {
        Variable external = externalBinaryVariable();
        Collection<Variable> orderedSet = Arrays.asList(binaryVar1, external);
        assertThrows(ProgrammingError.class, () -> model.addSpecialOrderedSet(orderedSet, 1, 2), "Variables must be binary and already inserted in the model!");
    }

    @ParameterizedTest(name = "[{index}] orderedSet={0}, min={1}, max={2}, emptySetAllowed={3}")
    @MethodSource("validMinZeroProvider")
    void testValidSOS_MinZero(List<String> varNames, int min, int max, boolean emptySetAllowed) {
        List<Variable> orderedSet = variablesFromNames(varNames);
        model.addSpecialOrderedSet(orderedSet, min, max);
        // Retrieve the created SOS expression
        Expression sosExpr = model.expressions().filter(e -> e.getName() != null && e.getName().contains("SOS" + max)).findFirst().orElseThrow(() -> new AssertionError("SOS expression not found"));
        assertEquals(BigDecimal.valueOf(max), sosExpr.upper());
        if (min > 0) {
            assertEquals(BigDecimal.valueOf(min), sosExpr.lower());
        } else {
            assertNull(sosExpr.lower());
        }
        // Coefficients must be ONE for each variable
        for (Variable v : orderedSet) {
            assertEquals(ONE, sosExpr.get(v.getIndex(), false));
        }
        // When the set is empty, there should be no non‑zero coefficients
        if (orderedSet.isEmpty()) {
            assertTrue(sosExpr.getVariables().isEmpty(), "No coefficients should be present for an empty ordered set");
        }
    }

    @ParameterizedTest(name = "[{index}] orderedSet={0}, min={1}, max={2}")
    @MethodSource("validMinPositiveProvider")
    void testValidSOS_MinPositive(List<String> varNames, int min, int max, boolean dummy) {
        List<Variable> orderedSet = variablesFromNames(varNames);
        model.addSpecialOrderedSet(orderedSet, min, max);
        Expression sosExpr = model.expressions().filter(e -> e.getName() != null && e.getName().contains("SOS" + max)).findFirst().orElseThrow(() -> new AssertionError("SOS expression not found"));
        assertEquals(BigDecimal.valueOf(max), sosExpr.upper(), "Upper bound should be set to max");
        if (min > 0) {
            assertEquals(BigDecimal.valueOf(min), sosExpr.lower(), "Lower bound should be set to min");
        } else {
            assertNull(sosExpr.lower(), "Lower bound must be absent when min <= 0");
        }
        for (Variable v : orderedSet) {
            assertEquals(ONE, sosExpr.get(v.getIndex(), false), "Coefficient for variable " + v.getName() + " must be ONE");
        }
        // Ensure exactly one coefficient per distinct variable
        Set<Integer> distinctIndices = new HashSet<>();
        for (Variable v : orderedSet) {
            distinctIndices.add(v.getIndex());
        }
        long nonZeroCount = sosExpr.getVariables().stream().filter(idx -> {
            BigDecimal val = sosExpr.get(idx, false);
            return val != null && !val.equals(BigDecimal.ZERO);
        }).count();
        assertEquals(distinctIndices.size(), nonZeroCount, "Expression should contain exactly one coefficient per distinct variable");
    }

    @Test
    void testDuplicateVariablesInOrderedSet() {
        // Create a single binary variable and use it twice
        Variable dup = model.addVariable().binary().weight(ONE);
        Collection<Variable> orderedSet = Arrays.asList(dup, dup);
        int min = 1;
        int max = 2;
        // Spy to capture the three‑argument call
        Expression mockExpr = mock(Expression.class);
        doReturn(mockExpr).when(model).newExpression(anyString());
        model.addSpecialOrderedSet(orderedSet, min, max);
        // The expression should have been set twice for the same index – last call wins
        verify(mockExpr, times(2)).set(dup.getIndex(), ONE);
        // Verify that the overloaded three‑argument method was invoked
        verify(model, times(1)).addSpecialOrderedSet(eq(orderedSet), eq(max), eq(mockExpr));
    }

    @Test
    void testOrderedSetLargerThanMax() {
        List<Variable> vars = createBinaryVariables(4);
        Collection<Variable> orderedSet = vars;
        int min = 1;
        int max = 2;
        model.addSpecialOrderedSet(orderedSet, min, max);
        Expression sosExpr = model.expressions().filter(e -> e.getName() != null && e.getName().contains("SOS" + max)).findFirst().orElseThrow(() -> new AssertionError("SOS expression not found"));
        assertEquals(BigDecimal.valueOf(max), sosExpr.upper());
        assertEquals(BigDecimal.valueOf(min), sosExpr.lower());
        for (Variable v : vars) {
            assertEquals(ONE, sosExpr.get(v.getIndex(), false));
        }
    }

    @Test
    void testNegativeMinIsTreatedAsZero() {
        List<Variable> orderedSet = variablesFromNames(Arrays.asList("v1", "v2"));
        int min = -1;
        int max = 2;
        model.addSpecialOrderedSet(orderedSet, min, max);
        Expression sosExpr = model.expressions().filter(e -> e.getName() != null && e.getName().contains("SOS" + max)).findFirst().orElseThrow(() -> new AssertionError("SOS expression not found"));
        assertEquals(BigDecimal.valueOf(max), sosExpr.upper());
        assertNull(sosExpr.lower(), "Lower bound should be absent when min is negative");
    }

    @Test
    void testMinEqualsMaxExactNumberOnVariables() {
        List<Variable> orderedSet = variablesFromNames(Arrays.asList("v1", "v2", "v3"));
        int min = 2;
        int max = 2;
        model.addSpecialOrderedSet(orderedSet, min, max);
        Expression sosExpr = model.expressions().filter(e -> e.getName() != null && e.getName().contains("SOS" + max)).findFirst().orElseThrow(() -> new AssertionError("SOS expression not found"));
        assertEquals(BigDecimal.valueOf(2), sosExpr.upper());
        assertEquals(BigDecimal.valueOf(2), sosExpr.lower());
    }

    @Test
    void testMaxEqualsOneTypeOneSOS() {
        Variable single = model.addVariable().binary().weight(ONE);
        Collection<Variable> orderedSet = Collections.singletonList(single);
        int min = 0;
        int max = 1;
        model.addSpecialOrderedSet(orderedSet, min, max);
        Expression sosExpr = model.expressions().filter(e -> e.getName() != null && e.getName().contains("SOS" + max)).findFirst().orElseThrow(() -> new AssertionError("SOS expression not found"));
        assertEquals(BigDecimal.ONE, sosExpr.upper());
        assertNull(sosExpr.lower());
    }
}