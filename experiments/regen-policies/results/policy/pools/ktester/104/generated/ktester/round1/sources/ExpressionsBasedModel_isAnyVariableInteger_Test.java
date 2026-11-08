package org.ojalgo.optimisation;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.*;
import java.lang.reflect.Field;
import java.util.List;
import java.util.stream.IntStream;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.params.ParameterizedTest;

@ExtendWith(MockitoExtension.class)
class ExpressionsBasedModel_isAnyVariableInteger_Test {

    private ExpressionsBasedModel model;

    /**
     * Simple mock/subclass of {@link Variable} used in place of Mockito.
     */
    private static class MockVariable extends Variable {

        private final Boolean integerResult;

        private final Boolean fixedResult;

        private final RuntimeException integerException;

        private final RuntimeException fixedException;

        private final Runnable sideEffect;

        MockVariable(Boolean integerResult, Boolean fixedResult, RuntimeException integerException, RuntimeException fixedException, Runnable sideEffect) {
            // Variable has a public no‑arg constructor in ojAlgo.
            super();
            this.integerResult = integerResult;
            this.fixedResult = fixedResult;
            this.integerException = integerException;
            this.fixedException = fixedException;
            this.sideEffect = sideEffect;
        }

        MockVariable(Boolean integerResult, Boolean fixedResult) {
            this(integerResult, fixedResult, null, null, null);
        }

        @Override
        public boolean isInteger() {
            if (integerException != null) {
                throw integerException;
            }
            if (sideEffect != null) {
                sideEffect.run();
            }
            return integerResult != null ? integerResult : super.isInteger();
        }

        @Override
        public boolean isFixed() {
            if (fixedException != null) {
                throw fixedException;
            }
            return fixedResult != null ? fixedResult : super.isFixed();
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // fresh model for every test
        model = new ExpressionsBasedModel();
    }

    @AfterEach
    void teardownAfterEach() {
        model = null;
    }

    @Test
    void testNoIntegerVariablesReturnsFalse() {
        // Given: a model with only continuous variables
        model.addVariable().lower(0).upper(10);
        model.addVariable().lower(-5).upper(5);
        // When
        boolean result = model.isAnyVariableInteger();
        // Then
        assertFalse(result, "Model with no integer variables should return false");
    }

    @Test
    void testIntegerVariableNotFixedReturnsTrue() {
        // Given: a model containing an integer variable whose bounds differ (not fixed)
        model.addVariable().integer(true).lower(0).upper(10);
        // When
        boolean result = model.isAnyVariableInteger();
        // Then
        assertTrue(result, "Model with a non‑fixed integer variable should return true");
    }

    @Test
    void testIntegerVariableFixedReturnsFalse() {
        // Given: an integer variable that is fixed (lower == upper)
        model.addVariable().integer(true).level(5);
        // When
        boolean result = model.isAnyVariableInteger();
        // Then
        assertFalse(result, "Fixed integer variables must be ignored by isAnyVariableInteger()");
    }

    @Test
    void testRelaxedModelIgnoresIntegerVariables() {
        // Given: a model with a non‑fixed integer variable
        model.addVariable().integer(true).lower(0).upper(10);
        // And the model is put into a relaxed state
        model.relax();
        // When
        boolean result = model.isAnyVariableInteger();
        // Then
        assertFalse(result, "When the model is relaxed, isAnyVariableInteger() must return false");
    }

    @Test
    void testRelaxedFlagBehaviorEvenWithVariables() {
        // Relaxed flag should short‑circuit regardless of variable composition.
        model.addVariable().integer(true).lower(0).upper(10);
        // continuous variable
        model.addVariable();
        model.relax();
        assertFalse(model.isAnyVariableInteger(), "Relaxed model must return false even when integer variables exist");
    }

    @Test
    void testNoVariablesPresentReturnsFalse() {
        // Model is fresh and contains no variables.
        assertFalse(model.isAnyVariableInteger(), "Empty model (no variables) should return false");
    }

    @Test
    void testSingleNonIntegerVariableReturnsFalse() {
        // default is continuous
        model.addVariable().lower(-1).upper(1);
        assertFalse(model.isAnyVariableInteger(), "Model with a single non‑integer variable must return false");
    }

    @Test
    void testMultipleVariablesNoneQualify() {
        // Mix of continuous and fixed integer variables – none satisfy (isInteger && !isFixed)
        // continuous
        model.addVariable().lower(0).upper(5);
        // fixed integer
        model.addVariable().integer(true).level(3);
        // fixed integer
        model.addVariable().integer(true).level(-2);
        assertFalse(model.isAnyVariableInteger(), "When no variable is integer & non‑fixed, result must be false");
    }

    @Test
    void testEarlyExitSideEffectVerification() {
        // First variable qualifies; second variable is a mock that throws if accessed.
        Variable first = model.addVariable().integer(true).lower(0).upper(10);
        MockVariable badMock = new MockVariable(null, null, new RuntimeException("Should not be called"), new RuntimeException("Should not be called"), null);
        // Inject the bad mock as the second element via reflection.
        try {
            Field f = ExpressionsBasedModel.class.getDeclaredField("myVariables");
            f.setAccessible(true);
            @SuppressWarnings("unchecked")
            List<Variable> list = (List<Variable>) f.get(model);
            list.add(badMock);
        } catch (Exception e) {
            fail("Reflection failed while injecting bad mock: " + e);
        }
        // The method should return true after evaluating the first variable and never touch the second.
        assertTrue(model.isAnyVariableInteger(), "Method must exit early after first qualifying variable without invoking later mocks");
    }

    @Test
    void testQualifyingVariableLaterInList() {
        // First two variables are non‑qualifying, third qualifies.
        // continuous
        model.addVariable();
        // fixed integer
        model.addVariable().integer(true).level(4);
        // qualifying
        model.addVariable().integer(true).lower(1).upper(2);
        assertTrue(model.isAnyVariableInteger(), "Qualifying integer variable later in the list should cause true");
    }

    @Test
    void testLargeVariableListQualifyingAtEnd() {
        // 1000 continuous variables
        IntStream.range(0, 1000).forEach(i -> model.addVariable().lower(i).upper(i + 1));
        // Append a qualifying integer variable
        model.addVariable().integer(true).lower(0).upper(5);
        assertTrue(model.isAnyVariableInteger(), "Method must scan large list and return true when qualifying variable is last");
    }

    @Test
    void testAllIntegerVariablesFixedReturnsFalse() {
        // Three integer variables, all fixed via level(...)
        model.addVariable().integer(true).level(1);
        model.addVariable().integer(true).level(2);
        model.addVariable().integer(true).level(3);
        assertFalse(model.isAnyVariableInteger(), "All integer variables are fixed → should return false");
    }

    @Test
    void testMixedIntegerAndNonIntegerNoQualifyingReturnsFalse() {
        // Mixed list: integer‑fixed, continuous, integer‑fixed
        model.addVariable().integer(true).level(0);
        model.addVariable().lower(-10).upper(10);
        model.addVariable().integer(true).level(5);
        assertFalse(model.isAnyVariableInteger(), "Mixed list without a non‑fixed integer must return false");
    }

    @Test
    void testNullMyVariablesReferenceThrowsNPE() throws Exception {
        // Force myVariables to null via reflection.
        Field f = ExpressionsBasedModel.class.getDeclaredField("myVariables");
        f.setAccessible(true);
        f.set(model, null);
        assertThrows(NullPointerException.class, () -> model.isAnyVariableInteger(), "Null myVariables should cause NullPointerException");
    }

    @Test
    void testNullElementInVariableListThrowsNPE() throws Exception {
        // Insert a null entry into the internal list.
        Field f = ExpressionsBasedModel.class.getDeclaredField("myVariables");
        f.setAccessible(true);
        @SuppressWarnings("unchecked")
        List<Variable> list = (List<Variable>) f.get(model);
        // now list size = 1 with a null element
        list.add(null);
        assertThrows(NullPointerException.class, () -> model.isAnyVariableInteger(), "Null element in myVariables should cause NullPointerException");
    }

    @Test
    void testVariableMethodThrowsRuntimeExceptionPropagates() {
        MockVariable exploding = new MockVariable(null, null, new RuntimeException("Boom"), null, null);
        // Inject the mock as the sole variable.
        try {
            Field f = ExpressionsBasedModel.class.getDeclaredField("myVariables");
            f.setAccessible(true);
            @SuppressWarnings("unchecked")
            List<Variable> list = (List<Variable>) f.get(model);
            list.add(exploding);
        } catch (Exception e) {
            fail("Reflection failed while injecting exploding mock: " + e);
        }
        RuntimeException thrown = assertThrows(RuntimeException.class, () -> model.isAnyVariableInteger(), "Exception from Variable should propagate");
        assertEquals("Boom", thrown.getMessage());
    }

    @Test
    void testConcurrentModificationLeadsToIndexOutOfBoundsException() throws Exception {
        // First variable will clear the internal list when isInteger() is called.
        MockVariable mutator = new MockVariable(true, false, null, null, () -> {
            try {
                Field f = ExpressionsBasedModel.class.getDeclaredField("myVariables");
                f.setAccessible(true);
                @SuppressWarnings("unchecked")
                List<Variable> list = (List<Variable>) f.get(model);
                list.clear();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        MockVariable second = new MockVariable(false, false);
        // Populate list via reflection to control order.
        Field f = ExpressionsBasedModel.class.getDeclaredField("myVariables");
        f.setAccessible(true);
        @SuppressWarnings("unchecked")
        List<Variable> list = (List<Variable>) f.get(model);
        list.add(mutator);
        list.add(second);
        // The loop captures limit = 2, then after mutator clears the list,
        // the next get(i) will cause IndexOutOfBoundsException.
        assertThrows(IndexOutOfBoundsException.class, () -> model.isAnyVariableInteger(), "Concurrent modification should result in IndexOutOfBoundsException");
    }

    @ParameterizedTest(name = "{index} ⇒ expect {0}")
    @ValueSource(booleans = { true, false })
    void testEarlyExitVersusFullScan(boolean firstQualifies) {
        // firstQualifies == true  → method should stop after first variable (early exit)
        // firstQualifies == false → method must scan whole list and finally return false
        MockVariable first = new MockVariable(firstQualifies, !firstQualifies, null, null, null);
        MockVariable second = new MockVariable(false, false);
        // Inject both variables via reflection.
        try {
            Field f = ExpressionsBasedModel.class.getDeclaredField("myVariables");
            f.setAccessible(true);
            @SuppressWarnings("unchecked")
            List<Variable> list = (List<Variable>) f.get(model);
            list.add(first);
            list.add(second);
        } catch (Exception e) {
            fail("Reflection failed while injecting mocks: " + e);
        }
        if (firstQualifies) {
            assertTrue(model.isAnyVariableInteger(), "First variable qualifies – method must return true early");
        } else {
            assertFalse(model.isAnyVariableInteger(), "No variable qualifies – method must return false after full scan");
        }
    }
}
