package org.ojalgo.optimisation;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.ojalgo.structure.Structure1D;
import org.junit.jupiter.api.extension.ExtendWith;
// import org.ojalgo.function.error.ProgrammingError;

// @ExtendWith(MockitoExtension.class)
class ExpressionsBasedModel_indexOf_Test {

    private ExpressionsBasedModel model;

    @BeforeEach
    void setupBeforeEach() {
        // a fresh model for each test
        model = new ExpressionsBasedModel();
    }

    /* -------------------------------------------------
     *  Normal Cases
     * ------------------------------------------------- */
    private static Stream<Arguments> normalCasesProvider() {
        return Stream.of(// first variable (index 0)
        Arguments.of(new Variable("v0", 0), 0), // mid‑range variable (index 5)
        Arguments.of(new Variable("v5", 5), 5), // large positive index
        Arguments.of(new Variable("vBig", 12_345), 12_345), // negative index – method does not validate
        Arguments.of(new Variable("vNeg", -1), -1));
    }

    /* -------------------------------------------------
     *  Edge Cases (Integer.MAX_VALUE / MIN_VALUE)
     * ------------------------------------------------- */
    private static Stream<Arguments> edgeCasesProvider() {
        return Stream.of(Arguments.of(new Variable("max", Integer.MAX_VALUE), Integer.MAX_VALUE), Arguments.of(new Variable("min", Integer.MIN_VALUE), Integer.MIN_VALUE));
    }

    /* -------------------------------------------------
     *  Near Limit Cases
     * ------------------------------------------------- */
    private static Stream<Arguments> nearLimitCasesProvider() {
        return Stream.of(Arguments.of(new Variable("maxMinusOne", Integer.MAX_VALUE - 1), Integer.MAX_VALUE - 1), Arguments.of(new Variable("minPlusOne", Integer.MIN_VALUE + 1), Integer.MIN_VALUE + 1));
    }

    @Test
    void testIndexOfSingleVariable() {
        // create the first variable in the model
        Variable var = model.addVariable();
        // the index returned by the focal method should be 0
        int index = model.indexOf(var);
        assertEquals(0, index, "The first variable should have index 0");
        // the same variable should be reachable via getVariable(index)
        assertSame(var, model.getVariable(index), "Variable retrieved by index should be identical to the original variable");
    }

    @Test
    void testIndexOfMultipleVariables() {
        // add three variables – two unnamed, one with a name
        // expected index 0
        Variable v0 = model.addVariable();
        // expected index 1
        Variable v1 = model.addVariable();
        // expected index 2
        Variable v2 = model.newVariable("namedVar");
        // verify each index matches the order of insertion
        assertEquals(0, model.indexOf(v0), "First variable should have index 0");
        assertEquals(1, model.indexOf(v1), "Second variable should have index 1");
        assertEquals(2, model.indexOf(v2), "Third variable should have index 2");
        // cross‑check that getVariable returns the same instances
        assertSame(v0, model.getVariable(0), "Variable at index 0 should be v0");
        assertSame(v1, model.getVariable(1), "Variable at index 1 should be v1");
        assertSame(v2, model.getVariable(2), "Variable at index 2 should be v2");
    }

    @Test
    void testIndexOfVariableCreatedOutsideModel() {
        // Create a variable manually (simulating a variable that does not belong to the model)
        Variable external = new Variable("external", 99);
        // The method simply returns the internal index of the Variable, it does not validate membership.
        // Therefore we expect the returned value to be the index stored inside the Variable (99).
        int externalIndex = model.indexOf(external);
        assertEquals(99, externalIndex, "indexOf should return the variable's own index even if it is not part of the model");
    }

    @ParameterizedTest(name = "[{index}] Normal case – expected index {1}")
    @MethodSource("normalCasesProvider")
    void testIndexOf_NormalCases(Variable variable, int expected) {
        int actual = model.indexOf(variable);
        assertEquals(expected, actual, () -> "indexOf should return the stored index of the variable");
    }

    @ParameterizedTest(name = "[{index}] Edge case – expected index {1}")
    @MethodSource("edgeCasesProvider")
    void testIndexOf_EdgeCases(Variable variable, int expected) {
        assertEquals(expected, model.indexOf(variable));
    }

    @ParameterizedTest(name = "[{index}] Near‑limit case – expected index {1}")
    @MethodSource("nearLimitCasesProvider")
    void testIndexOf_NearLimitCases(Variable variable, int expected) {
        assertEquals(expected, model.indexOf(variable));
    }

    /* -------------------------------------------------
     *  Consistency Cases
     * ------------------------------------------------- */
    @Test
    void testIndexOf_Consistency_MultipleCalls() {
        Variable v = new Variable("consistent", 42);
        int first = model.indexOf(v);
        int second = model.indexOf(v);
        int third = model.indexOf(v);
        assertAll(() -> assertEquals(42, first, "First call should return 42"), () -> assertEquals(42, second, "Second call should return the same value"), () -> assertEquals(42, third, "Third call should also return the same value"));
    }

    @Test
    void testIndexOf_Consistency_IndexZeroAfterManyAdds() {
        // add a bunch of variables first
        for (int i = 0; i < 100; i++) {
            model.addVariable();
        }
        // now add a variable with explicit index 0 (using the constructor)
        Variable zero = new Variable("zero", 0);
        assertEquals(0, model.indexOf(zero), "Even if added after many others, indexOf must return the stored index value");
    }

    /* -------------------------------------------------
     *  Custom IntIndex Cases (using Mockito)
     * ------------------------------------------------- */
    @Test
    void testIndexOf_CustomIntIndex_Subclass() {
        // a custom subclass of IntIndex with index 7
        Structure1D.IntIndex custom = new Structure1D.IntIndex(7) {
        };
        // Create a Variable that returns this custom IntIndex
        Variable customVar = new Variable("custom", 0) {

            @Override
            public Structure1D.IntIndex getIndex() {
                return custom;
            }
        };
        assertEquals(7, model.indexOf(customVar), "indexOf should work with a subclass of IntIndex");
    }

    @Test
    void testIndexOf_CustomIntIndex_LazyComputation() {
        // simulate a lazily computed index by overriding hashCode (no real effect, just for completeness)
        Structure1D.IntIndex lazy = new Structure1D.IntIndex(13) {

            @Override
            public int hashCode() {
                return super.hashCode();
            }
        };
        Variable lazyVar = new Variable("lazy", 0) {

            @Override
            public Structure1D.IntIndex getIndex() {
                return lazy;
            }
        };
        assertEquals(13, model.indexOf(lazyVar), "indexOf should retrieve the index even if IntIndex computes it lazily");
    }

    /* -------------------------------------------------
     *  Exception Cases
     * ------------------------------------------------- */
    @Test
    void testIndexOf_NullVariable_throwsNPE() {
        assertThrows(NullPointerException.class, () -> model.indexOf(null), "Calling indexOf with null should throw NullPointerException");
    }

    @Test
    void testIndexOf_VariableWithNullIntIndex_throwsNPE() {
        Variable varWithNullIndex = new Variable("nullIndex", 0) {

            @Override
            public Structure1D.IntIndex getIndex() {
                return null;
            }
        };
        assertThrows(NullPointerException.class, () -> model.indexOf(varWithNullIndex), "If getIndex() returns null, a NullPointerException should be thrown");
    }

    @Test
    void testIndexOf_VariableGetIndexThrows_IllegalStateException() {
        Variable throwingVar = new Variable("throws", 0) {

            @Override
            public Structure1D.IntIndex getIndex() {
                throw new IllegalStateException("forced");
            }
        };
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> model.indexOf(throwingVar));
        assertEquals("forced", ex.getMessage(), "Exception from getIndex() must propagate unchanged");
    }

    @Test
    void testIndexOf_VariableGetIndexThrows_ProgrammingError() {
        Variable throwingVar = new Variable("throws", 0) {

            @Override
            public Structure1D.IntIndex getIndex() {
                throw new ProgrammingError("programming error");
            }
        };
        ProgrammingError ex = assertThrows(ProgrammingError.class, () -> model.indexOf(throwingVar));
        assertEquals("programming error", ex.getMessage(), "Custom unchecked exception should propagate");
    }

    @Test
    void testIndexOf_VariableGetIndexThrows_UnsupportedOperationException() {
        Variable throwingVar = new Variable("throws", 0) {

            @Override
            public Structure1D.IntIndex getIndex() {
                throw new UnsupportedOperationException("unsupported");
            }
        };
        UnsupportedOperationException ex = assertThrows(UnsupportedOperationException.class, () -> model.indexOf(throwingVar));
        assertEquals("unsupported", ex.getMessage(), "UnsupportedOperationException should propagate");
    }
}