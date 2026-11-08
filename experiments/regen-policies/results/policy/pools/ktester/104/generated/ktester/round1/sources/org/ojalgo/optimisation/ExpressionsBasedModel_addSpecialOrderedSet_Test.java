package org.ojalgo.optimisation;

import static org.junit.jupiter.api.Assertions.*;
import java.math.BigDecimal;
import java.util.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.ojalgo.ProgrammingError;

// @ExtendWith(MockitoExtension.class)
class ExpressionsBasedModel_addSpecialOrderedSet_Test {

//     private MockedStatic<ExpressionsBasedModel> mockedStatic;

//     @BeforeEach
    void setupBeforeEach() {
        // Mock static calls on ExpressionsBasedModel (addPresolver) for each test.
        mockedStatic = mockStatic(ExpressionsBasedModel.class);
        // By default make the static method return true (its return type is boolean).
        mockedStatic.when(() -> ExpressionsBasedModel.addPresolver(any())).thenReturn(true);
        // Mock the static method addPresolver – it returns true by default.
        mockedStatic = mockStatic(ExpressionsBasedModel.class);
        mockedStatic.when(() -> ExpressionsBasedModel.addPresolver(any())).thenReturn(true);
    }

//     @AfterEach
    void teardownAfterEach() {
        // Release the static mock.
        mockedStatic.close();
        mockedStatic.close();
    }

    private static Expression mockExpression(boolean isConstraint) {
        Expression expr = mock(Expression.class);
        when(expr.isConstraint()).thenReturn(isConstraint);
        return expr;
    }

    private static Variable mockVariableWithIndex() {
        Variable var = mock(Variable.class);
        // IntIndex is an internal class; we can mock it as a plain Object.
        when(var.getIndex()).thenReturn(mock(ExpressionsBasedModel.IntIndex.class));
        return var;
    }

    private static Variable mockVariableWithNullIndex() {
        Variable var = mock(Variable.class);
        when(var.getIndex()).thenReturn(null);
        return var;
    }

    private static Variable mockVariableThrowingGetIndex() {
        Variable var = mock(Variable.class);
        when(var.getIndex()).thenThrow(new RuntimeException("getIndex failure"));
        return var;
    }

//     private static Stream<Arguments> variableValidationFailureProvider() {
//         // case 1: null variable inside collection
//         Variable v = mockVariableWithIndex();
//         Collection<Variable> setWithNull = new ArrayList<>();
//         setWithNull.add(v);
//         setWithNull.add(null);
//         // case 2: variable whose getIndex() returns null
//         Variable vNoIndex = mockVariableWithNullIndex();
//         Collection<Variable> setNoIndex = List.of(vNoIndex);
//         Expression constraint = mockExpression(true);
//         return Stream.of(Arguments.of("null element in ordered set", setWithNull, constraint), Arguments.of("variable with null index", setNoIndex, constraint));
//     }

//     private static Stream<Arguments> successfulAdditionProvider() {
//         // Helper variables
//         Variable v0 = mockVariableWithIndex();
//         Variable v1 = mockVariableWithIndex();
//         Variable v2 = mockVariableWithIndex();
//         // 1. Normal three‑variable SOS
//         Collection<Variable> threeVars = List.of(v0, v1, v2);
//         // 2. Empty ordered set
//         Collection<Variable> emptySet = Collections.emptyList();
//         // 3. Duplicate variables (v0 appears twice)
//         Collection<Variable> duplicateVars = List.of(v0, v0, v1);
//         // 4. Max int type
//         Collection<Variable> maxIntVars = List.of(v1, v2);
//         // 5. Set implementation (LinkedHashSet)
//         Collection<Variable> setImpl = new LinkedHashSet<>(Arrays.asList(v1, v2));
//         Expression constraint = mockExpression(true);
//         return Stream.of(Arguments.of("Normal three‑variable SOS", threeVars, 2, constraint), Arguments.of("Empty ordered set", emptySet, 1, constraint), Arguments.of("Duplicate variables in ordered set", duplicateVars, 2, constraint), Arguments.of("Maximum integer SOS type", maxIntVars, Integer.MAX_VALUE, constraint), Arguments.of("Ordered set as a Set implementation", setImpl, 3, constraint));
//     }

//     @Test
//     void testAddSpecialOrderedSetSuccessfulInvocation() {
//         // Arrange
//         ExpressionsBasedModel model = new ExpressionsBasedModel();
//         // two binary variables (binary flag is true by default for new variables)
//         Variable v1 = model.addVariable();
//         Variable v2 = model.addVariable();
//         // a constraint expression (set lower/upper makes it a constraint)
//         Expression constraint = model.addExpression();
//         constraint.lower(BigDecimal.ZERO).upper(BigDecimal.ONE);
//         List<Variable> orderedSet = new ArrayList<>();
//         orderedSet.add(v1);
//         orderedSet.add(v2);
//         // Act & Assert – should not throw
//         assertDoesNotThrow(() -> model.addSpecialOrderedSet(orderedSet, 1, constraint));
//     }

//     @Test
//     void testAddSpecialOrderedSetInvalidTypeThrows() {
//         // Arrange
//         ExpressionsBasedModel model = new ExpressionsBasedModel();
//         Collection<Variable> orderedSet = Collections.emptyList();
//         // a valid constraint
//         Expression constraint = model.addExpression();
//         constraint.lower(BigDecimal.ZERO).upper(BigDecimal.ONE);
//         // Act & Assert
//         ProgrammingError ex = assertThrows(ProgrammingError.class, () -> model.addSpecialOrderedSet(orderedSet, 0, constraint));
//         assertTrue(ex.getMessage().contains("Invalid SOS type"));
//     }

//     @Test
//     void testAddSpecialOrderedSetLinkedExpressionNotConstraintThrows() {
//         // Arrange
//         ExpressionsBasedModel model = new ExpressionsBasedModel();
//         Variable v = model.addVariable();
//         // Expression without any bounds – not a constraint
//         Expression notConstraint = model.addExpression();
//         Collection<Variable> set = List.of(v);
//         // Act & Assert
//         ProgrammingError ex = assertThrows(ProgrammingError.class, () -> model.addSpecialOrderedSet(set, 1, notConstraint));
//         assertTrue(ex.getMessage().contains("linked to expression needs to be a constraint"));
//     }

//     @Test
//     void testAddSpecialOrderedSetVariableNullThrows() {
//         // Arrange
//         ExpressionsBasedModel model = new ExpressionsBasedModel();
//         Variable v = model.addVariable();
//         Expression constraint = model.addExpression();
//         constraint.lower(BigDecimal.ZERO).upper(BigDecimal.ONE);
//         Collection<Variable> set = new ArrayList<>();
//         set.add(v);
//         // introduce a null variable
//         set.add(null);
//         // Act & Assert
//         ProgrammingError ex = assertThrows(ProgrammingError.class, () -> model.addSpecialOrderedSet(set, 1, constraint));
//         assertTrue(ex.getMessage().contains("Variables must be already inserted"));
//     }

//     @Test
//     void testAddSpecialOrderedSetVariableWithoutIndexThrows() {
//         // Arrange
//         ExpressionsBasedModel model = new ExpressionsBasedModel();
//         // Mock a Variable whose getIndex() returns null
//         Variable mockedVar = mock(Variable.class);
//         when(mockedVar.getIndex()).thenReturn(null);
//         // The variable must also be considered binary for the method to pass the earlier checks.
//         when(mockedVar.isBinary()).thenReturn(true);
//         Expression constraint = model.addExpression();
//         constraint.lower(BigDecimal.ZERO).upper(BigDecimal.ONE);
//         Collection<Variable> set = List.of(mockedVar);
//         // Act & Assert
//         ProgrammingError ex = assertThrows(ProgrammingError.class, () -> model.addSpecialOrderedSet(set, 1, constraint));
//         assertTrue(ex.getMessage().contains("Variables must be already inserted"));
//     }

//     @ParameterizedTest(name = "Invalid SOS type {0}")
//     @ValueSource(ints = { 0, -5 })
//     void testInvalidTypeThrowsProgrammingError(int invalidType) {
//         ExpressionsBasedModel model = new ExpressionsBasedModel();
//         Collection<Variable> orderedSet = Collections.emptyList();
//         // a valid constraint
//         Expression constraint = mockExpression(true);
//         ProgrammingError ex = assertThrows(ProgrammingError.class, () -> model.addSpecialOrderedSet(orderedSet, invalidType, constraint));
//         assertTrue(ex.getMessage().contains("Invalid SOS type"));
//         // No presolver should have been added.
//         mockedStatic.verify(() -> ExpressionsBasedModel.addPresolver(any()), never());
//     }

//     @Test
//     void testLinkedExpressionNotConstraintThrows() {
//         ExpressionsBasedModel model = new ExpressionsBasedModel();
//         Variable v1 = mockVariableWithIndex();
//         Variable v2 = mockVariableWithIndex();
//         Collection<Variable> orderedSet = List.of(v1, v2);
//         // isConstraint = false
//         Expression notConstraint = mockExpression(false);
//         ProgrammingError ex = assertThrows(ProgrammingError.class, () -> model.addSpecialOrderedSet(orderedSet, 1, notConstraint));
//         assertTrue(ex.getMessage().contains("linked to expression needs to be a constraint"));
//         mockedStatic.verify(() -> ExpressionsBasedModel.addPresolver(any()), never());
//     }

//     @ParameterizedTest(name = "{index}: {0}")
//     @MethodSource("variableValidationFailureProvider")
//     void testVariableValidationFailures(String description, Collection<Variable> orderedSet, Expression linkedTo) {
//         ExpressionsBasedModel model = new ExpressionsBasedModel();
//         ProgrammingError ex = assertThrows(ProgrammingError.class, () -> model.addSpecialOrderedSet(orderedSet, 1, linkedTo));
//         assertTrue(ex.getMessage().contains("Variables must be already inserted"));
//         mockedStatic.verify(() -> ExpressionsBasedModel.addPresolver(any()), never());
//     }

//     @ParameterizedTest(name = "{index}: {0}")
//     @MethodSource("successfulAdditionProvider")
//     void testSuccessfulAddition(String description, Collection<Variable> orderedSet, int type, Expression linkedTo) {
//         ExpressionsBasedModel model = new ExpressionsBasedModel();
//         // Act – should not throw
//         assertDoesNotThrow(() -> model.addSpecialOrderedSet(orderedSet, type, linkedTo));
//         // Verify that the presolver registration happened exactly once.
//         mockedStatic.verify(() -> ExpressionsBasedModel.addPresolver(any()), times(1));
//         // Reset for the next iteration (otherwise the verification would accumulate).
//         mockedStatic.reset();
//         mockedStatic.when(() -> ExpressionsBasedModel.addPresolver(any())).thenReturn(true);
//     }

//     @Test
//     void testNullLinkedExpressionThrowsNPE() {
//         ExpressionsBasedModel model = new ExpressionsBasedModel();
//         Variable v = model.addVariable();
//         Collection<Variable> set = List.of(v);
//         assertThrows(NullPointerException.class, () -> model.addSpecialOrderedSet(set, 1, null));
//     }

//     @Test
//     void testNullOrderedSetThrowsNPE() {
//         ExpressionsBasedModel model = new ExpressionsBasedModel();
//         Expression constraint = model.addExpression();
//         constraint.lower(BigDecimal.ZERO).upper(BigDecimal.ONE);
//         assertThrows(NullPointerException.class, () -> model.addSpecialOrderedSet(null, 1, constraint));
//     }

//     @Test
//     void testVariableGetIndexThrowsExceptionPropagates() {
//         ExpressionsBasedModel model = new ExpressionsBasedModel();
//         Variable badVar = mockVariableThrowingGetIndex();
//         Variable goodVar = mockVariableWithIndex();
//         Collection<Variable> set = List.of(badVar, goodVar);
//         Expression constraint = mockExpression(true);
//         RuntimeException ex = assertThrows(RuntimeException.class, () -> model.addSpecialOrderedSet(set, 2, constraint));
//         assertEquals("getIndex failure", ex.getMessage());
//         // The method does not catch the exception, so addPresolver must not be called.
//         mockedStatic.verify(() -> ExpressionsBasedModel.addPresolver(any()), never());
//     }
}