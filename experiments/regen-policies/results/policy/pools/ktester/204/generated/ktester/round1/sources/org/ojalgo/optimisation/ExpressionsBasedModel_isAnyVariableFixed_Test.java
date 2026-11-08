import java.math.BigDecimal;
import java.util.ConcurrentModificationException;
import java.util.List;
import java.util.stream.IntStream;
import java.lang.reflect.Field;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
// import org.mockito.junit.jupiter.MockitoExtension;
import org.ojalgo.optimisation.ExpressionsBasedModel;
import org.ojalgo.optimisation.Variable;

// @ExtendWith(MockitoExtension.class)
class ExpressionsBasedModel_isAnyVariableFixed_Test {

    private ExpressionsBasedModel model;

//     class AddingVariable extends Variable {

        private final ExpressionsBasedModel parent;

        AddingVariable(ExpressionsBasedModel parent) {
//             super(parent);
            this.parent = parent;
        }

//         @Override
//         public boolean isFixed() {
//             // Attempt to modify the underlying list while the stream is evaluating
//             parent.addVariable().lower(0).upper(1);
//             return super.isFixed();
//         }
    }

    /**
     * Helper variable that tries to modify the model while its {@code isFixed()}
     * method is evaluated.
     */
//     class AddingVariable extends Variable {

        private final ExpressionsBasedModel parent;

        AddingVariable(ExpressionsBasedModel parent) {
            super(parent);
            this.parent = parent;
        }

        @Override
        public boolean isFixed() {
            // Attempt to modify the underlying list while the stream is evaluating
            parent.addVariable().lower(0).upper(1);
            return super.isFixed();
        }
    }

//     class AddingVariable extends Variable {

        private final ExpressionsBasedModel parent;

        AddingVariable(ExpressionsBasedModel parent) {
            super(parent);
            this.parent = parent;
        }

        @Override
        public boolean isFixed() {
            // Attempt to modify the underlying list while the stream is evaluating
            parent.addVariable().lower(0).upper(1);
            return super.isFixed();
        }
    }

    // Attempt to modify the underlying list while the stream is evaluating
    @BeforeEach
    void setupBeforeEach() {
        // A fresh model for each test case.
        model = new ExpressionsBasedModel();
    }

    @AfterEach
    void teardownAfterEach() {
        // The model does not hold external resources that need explicit cleanup.
        model = null;
    }

    @Test
    void testIsAnyVariableFixed_EmptyModel() {
        // No variables have been added – the method must return false.
        Assertions.assertFalse(model.isAnyVariableFixed(), "An empty model should report no fixed variables.");
    }

    @Test
    void testIsAnyVariableFixed_NoFixedVariables() {
        // Add a few variables with distinct lower/upper bounds (i.e. not fixed).
        model.addVariable().lower(0).upper(10);
        model.addVariable().lower(-5).upper(5);
        model.addVariable().lower(2).upper(8);
        Assertions.assertFalse(model.isAnyVariableFixed(), "Model with only free variables should report no fixed variables.");
    }

    @Test
    void testIsAnyVariableFixed_WithFixedVariable() {
        // Variable 0 – free
        model.addVariable().lower(0).upper(10);
        // Variable 1 – fixed by setting the same lower and upper bound via level(...)
        // level sets both lower and upper to the same value
        model.addVariable().level(7);
        // Variable 2 – free
        model.addVariable().lower(-3).upper(3);
        Assertions.assertTrue(model.isAnyVariableFixed(), "Model containing at least one fixed variable should report true.");
    }

    @Test
    void testSingleVariable_NotFixed() {
        // default bounds are not equal → not fixed
        model.addVariable();
        Assertions.assertFalse(model.isAnyVariableFixed(), "A model with a single non‑fixed variable should return false.");
    }

//     @Test
//     void testSingleVariable_Fixed() {
//         model.addVariable().fix(BigDecimal.ONE);
//         Assertions.assertTrue(model.isAnyVariableFixed(), "A model with a single fixed variable should return true.");
//     }

//     @Test
//     void testFirstVariableFixedInList() {
//         // A (fixed), B (free), C (free)
//         // A
//         model.addVariable().fix(BigDecimal.ONE);
//         // B
//         model.addVariable().lower(0).upper(5);
//         // C
//         model.addVariable().lower(-2).upper(2);
//         Assertions.assertTrue(model.isAnyVariableFixed(), "When the first variable is fixed, the method should return true.");
//     }

//     @Test
//     void testLastVariableFixedInList() {
//         // A (free), B (free), C (fixed)
//         // A
//         model.addVariable().lower(0).upper(5);
//         // B
//         model.addVariable().lower(-2).upper(2);
//         // C
//         model.addVariable().fix(BigDecimal.TEN);
//         Assertions.assertTrue(model.isAnyVariableFixed(), "When the last variable is fixed, the method should return true.");
//     }

    @Test
    void testAllVariablesUnfixed() {
        // A, B, C – all free
        model.addVariable().lower(0).upper(5);
        model.addVariable().lower(-2).upper(2);
        model.addVariable().lower(1).upper(10);
        Assertions.assertFalse(model.isAnyVariableFixed(), "All variables are unfixed → should return false.");
    }

//     @Test
//     void testVariableFixedThenUnfixed() {
//         Variable var = model.addVariable().fix(BigDecimal.ONE);
//         // now not fixed any more
//         var.unfix();
//         Assertions.assertFalse(model.isAnyVariableFixed(), "Variable was fixed then unfixed – method should return false.");
//     }

    @Test
    void testLargeModel_NoFixedVariables() {
        IntStream.range(0, 1000).forEach(i -> model.addVariable().lower(i).upper(i + 10));
        Assertions.assertFalse(model.isAnyVariableFixed(), "Large model with no fixed variables should return false.");
    }

//     @Test
//     void testLargeModel_WithMiddleFixedVariable() {
//         IntStream.range(0, 1000).forEach(i -> model.addVariable().lower(i).upper(i + 10));
//         // fix the variable at index 500
//         model.getVariable(500).fix(BigDecimal.valueOf(123));
//         Assertions.assertTrue(model.isAnyVariableFixed(), "Large model with a fixed variable in the middle should return true.");
//     }

//     @Test
//     void testFixedVariableRemovedFromModel() throws Exception {
//         Variable v = model.addVariable().fix(BigDecimal.ONE);
//         // Remove the variable via reflection (no public remove method)
//         Field listField = ExpressionsBasedModel.class.getDeclaredField("myVariables");
//         listField.setAccessible(true);
//         @SuppressWarnings("unchecked")
//         List<Variable> list = (List<Variable>) listField.get(model);
//         list.remove(v);
//         Assertions.assertFalse(model.isAnyVariableFixed(), "After removing the only fixed variable, method should return false.");
//     }

//     @Test
//     void testUnfixVariableInModelWithOtherFixed() {
//         // fixed
//         Variable a = model.addVariable().fix(BigDecimal.ONE);
//         // free
//         model.addVariable();
//         // fixed
//         Variable c = model.addVariable().fix(BigDecimal.TEN);
//         // now only c remains fixed
//         a.unfix();
//         Assertions.assertTrue(model.isAnyVariableFixed(), "After unfixing one of several fixed variables, the method should still return true.");
//     }

//     @Test
//     void testShallowCopySharesVariableInstances() {
//         Variable a = model.addVariable().fix(BigDecimal.ONE);
//         // shallow copy – the boolean flag 'shallow' is the first argument
//         ExpressionsBasedModel shallowCopy = model.copy(true, false);
//         // Unfix the variable in the original model
//         a.unfix();
//         // The shallow copy sees the same Variable instance, therefore no fixed vars
//         Assertions.assertFalse(shallowCopy.isAnyVariableFixed(), "Shallow copy should reflect changes made to the original variable instances.");
//     }

//     @Test
//     void testDeepCopyKeepsFixedVariable() {
//         Variable a = model.addVariable().fix(BigDecimal.ONE);
//         // deep copy – shallow = false
//         ExpressionsBasedModel deepCopy = model.copy(false, false);
//         // Unfix the variable in the original model
//         a.unfix();
//         // The deep copy has its own copy of the variable, still fixed
//         Assertions.assertTrue(deepCopy.isAnyVariableFixed(), "Deep copy should retain the fixed state of its own variable instances.");
//     }

    @Test
    void testNullEntryInVariableList_throwsNullPointerException() throws Exception {
        // Insert a null entry into the internal list
        Field listField = ExpressionsBasedModel.class.getDeclaredField("myVariables");
        listField.setAccessible(true);
        @SuppressWarnings("unchecked")
        List<Variable> list = (List<Variable>) listField.get(model);
        list.add(null);
        Assertions.assertThrows(NullPointerException.class, () -> model.isAnyVariableFixed(), "A null element in the variable list should cause NPE.");
    }

    @Test
    void testNullVariableList_throwsNullPointerException() throws Exception {
        // Set the internal list reference to null
        Field listField = ExpressionsBasedModel.class.getDeclaredField("myVariables");
        listField.setAccessible(true);
        listField.set(model, null);
        Assertions.assertThrows(NullPointerException.class, () -> model.isAnyVariableFixed(), "If the internal variable list is null, a NPE should be thrown.");
    }

//     @Test
//     void testConcurrentModificationDuringStream_throwsConcurrentModificationException() throws Exception {
//         // Custom Variable that adds another variable when isFixed() is invoked
//         class AddingVariable extends Variable {
// 
//             private final ExpressionsBasedModel parent;
// 
//             AddingVariable(ExpressionsBasedModel parent) {
//                 super(parent);
//                 this.parent = parent;
//             }
// 
//             @Override
//             public boolean isFixed() {
//                 // Attempt to modify the underlying list while the stream is evaluating
//                 parent.addVariable().lower(0).upper(1);
//                 return super.isFixed();
//             }
//         }
//         Field listField = ExpressionsBasedModel.class.getDeclaredField("myVariables");
//         listField.setAccessible(true);
//         @SuppressWarnings("unchecked")
//         List<Variable> list = (List<Variable>) listField.get(model);
//         // not fixed initially
//         list.add(new AddingVariable(model));
//         // a normal fixed variable
//         list.add(model.addVariable().fix(BigDecimal.ONE));
//         Assertions.assertThrows(ConcurrentModificationException.class, () -> model.isAnyVariableFixed(), "Modifying the variable list during stream processing should throw CME.");
//     }
}