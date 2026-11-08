package org.ojalgo.optimisation;

import static org.junit.jupiter.api.Assertions.*;
import java.lang.reflect.Field;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@ExtendWith(MockitoExtension.class)
class ExpressionsBasedModel_checkSimilarity_Test {

    private ExpressionsBasedModel model;

    private MockedStatic<Presolvers> presolversMock;

    /* -------------------------------------------------
     *  Helper holder for passing state between lambdas
     * ------------------------------------------------- */
    private static class TestStateHolder {

        static ExpressionsBasedModel model;

        static Expression potential;
    }

    @BeforeEach
    void setupBeforeEach() {
        // fresh model for each test
        model = new ExpressionsBasedModel();
        model.addExpression("existing").lower(0).upper(10);
    }

    /**
     * Provides test data for the behavioural scenarios described in the JSON
     * specification.
     */
    private static Stream<Arguments> similarityScenarios() {
        // Helper to inject a map into the model via reflection.
        java.util.function.BiConsumer<ExpressionsBasedModel, Map<String, Expression>> injectMap = (m, map) -> {
            try {
                Field f = ExpressionsBasedModel.class.getDeclaredField("myExpressions");
                f.setAccessible(true);
                f.set(m, map);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        };
        // 1. null potential, empty model → false
        Runnable emptyModel = () -> {
            try {
                injectMap.accept(new ExpressionsBasedModel(), new HashMap<>());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        };
        // 2. non‑null potential, empty model → false
        Runnable emptyModel2 = emptyModel;
        // 3. potential is the exact same object already stored → true
        Runnable sameObject = () -> {
            try {
                ExpressionsBasedModel m = new ExpressionsBasedModel();
                Expression e = m.addExpression("same");
                Map<String, Expression> map = new HashMap<>();
                map.put("same", e);
                injectMap.accept(m, map);
                TestStateHolder.model = m;
                TestStateHolder.potential = e;
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        };
        // 4. potential is a clone (identical content) → true
        Runnable cloneObject = () -> {
            try {
                ExpressionsBasedModel m = new ExpressionsBasedModel();
                Expression original = m.addExpression("orig");
                Expression clone = mock(Expression.class);
                when(clone.getName()).thenReturn("orig");
                Map<String, Expression> map = new HashMap<>();
                map.put("orig", original);
                injectMap.accept(m, map);
                TestStateHolder.model = m;
                TestStateHolder.potential = clone;
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        };
        // 5. potential different content → false
        Runnable differentObject = () -> {
            try {
                ExpressionsBasedModel m = new ExpressionsBasedModel();
                m.addExpression("a");
                Map<String, Expression> map = new HashMap<>();
                injectMap.accept(m, map);
                TestStateHolder.model = m;
                TestStateHolder.potential = mock(Expression.class);
                when(TestStateHolder.potential.getName()).thenReturn("different");
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        };
        // 6. multiple expressions, second matches → true
        Runnable secondMatches = () -> {
            try {
                ExpressionsBasedModel m = new ExpressionsBasedModel();
                m.addExpression("first");
                Expression second = m.addExpression("second");
                Map<String, Expression> map = new HashMap<>();
                // expose the internal map (empty is fine for compilation)
                injectMap.accept(m, map);
                TestStateHolder.model = m;
                TestStateHolder.potential = second;
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        };
        // 7. multiple expressions, none match → false
        Runnable noneMatch = () -> {
            try {
                ExpressionsBasedModel m = new ExpressionsBasedModel();
                m.addExpression("first");
                m.addExpression("second");
                Map<String, Expression> map = new HashMap<>();
                injectMap.accept(m, map);
                TestStateHolder.model = m;
                TestStateHolder.potential = mock(Expression.class);
                when(TestStateHolder.potential.getName()).thenReturn("unrelated");
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        };
        // 8. same name but different content → false
        Runnable sameNameDiffContent = () -> {
            try {
                ExpressionsBasedModel m = new ExpressionsBasedModel();
                m.addExpression("dup");
                Map<String, Expression> map = new HashMap<>();
                injectMap.accept(m, map);
                TestStateHolder.model = m;
                Expression potential = mock(Expression.class);
                when(potential.getName()).thenReturn("dup");
                TestStateHolder.potential = potential;
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        };
        return Stream.of(Arguments.of("null potential, empty model → false", (Runnable) emptyModel, null, false, null), Arguments.of("non‑null potential, empty model → false", (Runnable) emptyModel2, mock(Expression.class), false, null), Arguments.of("potential same instance as stored expression → true", (Runnable) sameObject, null, true, null), Arguments.of("potential clone with identical content → true", (Runnable) cloneObject, null, true, null), Arguments.of("potential different content → false", (Runnable) differentObject, null, false, null), Arguments.of("multiple expressions, second matches → true", (Runnable) secondMatches, null, true, null), Arguments.of("multiple expressions, none match → false", (Runnable) noneMatch, null, false, null), Arguments.of("same name but different content → false", (Runnable) sameNameDiffContent, null, false, null));
    }

    /**
     * Provides a few behavioural scenarios without using Mockito.
     */
    private static Stream<Arguments> simpleScenarios() {
        // empty model
        ExpressionsBasedModel emptyModel = new ExpressionsBasedModel();
        // model with a single expression
        ExpressionsBasedModel modelWithOne = new ExpressionsBasedModel();
        Expression e = modelWithOne.addExpression("e");
        // model that already contains the potential (same instance)
        ExpressionsBasedModel modelWithSame = new ExpressionsBasedModel();
        modelWithSame.addExpression("e");
        // model that contains an expression with the same name (different instance)
        ExpressionsBasedModel modelSameName = new ExpressionsBasedModel();
        modelSameName.addExpression("sameName");
        Expression potentialSameName = modelSameName.newExpression("sameName");
        return Stream.of(Arguments.of("empty model, null potential", emptyModel, null, false), Arguments.of("empty model, unrelated potential", emptyModel, emptyModel.newExpression("loner"), false), Arguments.of("model contains potential (same instance)", modelWithOne, e, true), Arguments.of("model contains expression with same name", modelSameName, potentialSameName, true));
    }

    @Test
    void testCheckSimilarityReturnsTrue() {
        Expression potential = model.newExpression("potential");
        presolversMock.when(() -> Presolvers.checkSimilarity(anyCollection(), eq(potential))).thenReturn(true);
        boolean similarity = model.checkSimilarity(potential);
        assertTrue(similarity, "checkSimilarity should return true when the static method returns true");
        presolversMock.verify(() -> Presolvers.checkSimilarity(anyCollection(), eq(potential)), times(1));
    }

    @Test
    void testCheckSimilarityReturnsFalse() {
        Expression potential = model.newExpression("potential");
        presolversMock.when(() -> Presolvers.checkSimilarity(anyCollection(), eq(potential))).thenReturn(false);
        boolean similarity = model.checkSimilarity(potential);
        assertFalse(similarity, "checkSimilarity should return false when the static method returns false");
        presolversMock.verify(() -> Presolvers.checkSimilarity(anyCollection(), eq(potential)), times(1));
    }

    @Test
    void testCheckSimilarityPassesCorrectCollection() {
        Expression potential = model.newExpression("potential");
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Collection<Expression>> collectionCaptor = ArgumentCaptor.forClass(Collection.class);
        presolversMock.when(() -> Presolvers.checkSimilarity(anyCollection(), eq(potential))).thenReturn(true);
        model.checkSimilarity(potential);
        presolversMock.verify(() -> Presolvers.checkSimilarity(collectionCaptor.capture(), eq(potential)), times(1));
        Collection<Expression> passedCollection = collectionCaptor.getValue();
        assertNotNull(passedCollection, "The collection passed to Presolvers.checkSimilarity must not be null");
        assertEquals(1, passedCollection.size(), "The collection should contain the model's existing expressions");
        assertTrue(passedCollection.stream().anyMatch(e -> "existing".equals(e.getName())), "The collection must contain the expression that was added to the model");
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("similarityScenarios")
    void testCheckSimilarityBehaviour(String description, Runnable modelSetup, Expression explicitPotential, boolean expectedResult, Class<? extends Throwable> expectedException) {
        modelSetup.run();
        ExpressionsBasedModel currentModel = TestStateHolder.model != null ? TestStateHolder.model : model;
        Expression potential = explicitPotential != null ? explicitPotential : TestStateHolder.potential;
        // reset holder for next iteration
        TestStateHolder.model = null;
        TestStateHolder.potential = null;
        presolversMock.when(() -> Presolvers.checkSimilarity(anyCollection(), eq(potential))).thenReturn(expectedResult);
        if (expectedException != null) {
            assertThrows(expectedException, () -> currentModel.checkSimilarity(potential), "Expected exception was not thrown");
        } else {
            boolean actual = currentModel.checkSimilarity(potential);
            assertEquals(expectedResult, actual, "Unexpected similarity result for scenario: " + description);
        }
        presolversMock.verify(() -> Presolvers.checkSimilarity(anyCollection(), eq(potential)), times(1));
    }

    @Test
    void testCheckSimilarityThrowsWhenCollectionContainsNull() {
        try {
            Field f = ExpressionsBasedModel.class.getDeclaredField("myExpressions");
            f.setAccessible(true);
            Map<String, Expression> map = new HashMap<>();
            map.put("nullEntry", null);
            f.set(model, map);
        } catch (Exception e) {
            fail("Reflection failure while injecting null entry", e);
        }
        Expression potential = model.newExpression("potential");
        assertThrows(NullPointerException.class, () -> model.checkSimilarity(potential), "Expected NullPointerException due to null entry in the model's expression collection");
    }

    @Test
    void testCheckSimilarityThrowsWhenPotentialHasNullCoefficients() {
        Expression potential = mock(Expression.class);
        presolversMock.when(() -> Presolvers.checkSimilarity(anyCollection(), eq(potential))).thenThrow(new NullPointerException("coefficients map is null"));
        assertThrows(NullPointerException.class, () -> model.checkSimilarity(potential), "Expected NullPointerException when potential expression has null coefficients");
    }

    @Test
    void testCheckSimilarityThrowsWhenPotentialHasNullVariable() {
        Expression potential = mock(Expression.class);
        presolversMock.when(() -> Presolvers.checkSimilarity(anyCollection(), eq(potential))).thenThrow(new NullPointerException("variable reference is null"));
        assertThrows(NullPointerException.class, () -> model.checkSimilarity(potential), "Expected NullPointerException when potential expression contains a null variable");
    }

    @Test
    void testCheckSimilarityThrowsProgrammingErrorForUninitializedExpression() {
        Expression potential = mock(Expression.class);
        presolversMock.when(() -> Presolvers.checkSimilarity(anyCollection(), eq(potential))).thenThrow(new ProgrammingError("Expression not properly initialised"));
        assertThrows(ProgrammingError.class, () -> model.checkSimilarity(potential), "Expected ProgrammingError for an uninitialised expression");
    }

    @Test
    void testCheckSimilarityReturnsTrueWhenExpressionIsStored() {
        Expression stored = model.addExpression("stored");
        boolean result = model.checkSimilarity(stored);
        assertTrue(result, "checkSimilarity should return true for an expression that is stored in the model");
    }

    @Test
    void testCheckSimilarityReturnsFalseWhenExpressionIsNotStored() {
        Expression notStored = model.newExpression("notStored");
        boolean result = model.checkSimilarity(notStored);
        assertFalse(result, "checkSimilarity should return false for an expression that is not stored in the model");
    }

    @Test
    void testCheckSimilarityReturnsFalseForNullPotential() {
        assertThrows(NullPointerException.class, () -> model.checkSimilarity(null), "Expected NullPointerException when the potential expression is null");
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("simpleScenarios")
    void testSimpleScenarios(String description, ExpressionsBasedModel testModel, Expression potential, boolean expected) {
        boolean actual = testModel.checkSimilarity(potential);
        assertEquals(expected, actual, "Unexpected result for scenario: " + description);
    }
}
