import static org.junit.jupiter.api.Assertions.*;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

// @ExtendWith(MockitoExtension.class)
class ExpressionsBasedModel_isAnyVariableFixed_Test {

    /**
     * Helper list that always throws a {@link ConcurrentModificationException}
     * when its iterator is obtained – used to simulate a CME during stream traversal.
     */
    class CmeArrayList<E> extends ArrayList<E> {

        @Override
        public Iterator<E> iterator() {
            throw new ConcurrentModificationException("simulated CME");
        }
    }

    /**
     * A {@link Variable} implementation whose {@code isFixed()} method always throws
     * an {@link IllegalStateException}. Used to verify that the exception propagates
     * out of {@code isAnyVariableFixed()}.
     */
//     class BadVariable extends Variable {

//         BadVariable(ExpressionsBasedModel model) {
            super(model);
        }

        @Override
        public boolean isFixed() {
            throw new IllegalStateException("bad variable");
        }
    }

    /**
     * When the model contains at least one variable whose lower and upper bounds are equal,
     * {@code isAnyVariableFixed()} should return {@code true}.
     */
    @Test
    void testIsAnyVariableFixed_True() {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        // Variable that is not fixed
        model.addVariable().lower(BigDecimal.ZERO).upper(BigDecimal.TEN);
        // Variable that is fixed (lower == upper)
        model.addVariable().lower(BigDecimal.valueOf(5)).upper(BigDecimal.valueOf(5));
        assertTrue(model.isAnyVariableFixed(), "Model should report a fixed variable when one variable has equal lower and upper bounds");
    }

    /**
     * When the model contains only free variables, {@code isAnyVariableFixed()} should return {@code false}.
     */
    @Test
    void testIsAnyVariableFixed_False() {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        // Three free variables with distinct bounds
        model.addVariable().lower(BigDecimal.ZERO).upper(BigDecimal.ONE);
        model.addVariable().lower(BigDecimal.ONE).upper(BigDecimal.TEN);
        model.addVariable().lower(BigDecimal.TEN).upper(BigDecimal.valueOf(100));
        assertFalse(model.isAnyVariableFixed(), "Model with only free variables must report no fixed variables");
    }

    /**
     * Edge case: an empty model (no variables) must also return {@code false}.
     */
    @Test
    void testIsAnyVariableFixed_EmptyModel() {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        assertFalse(model.isAnyVariableFixed(), "An empty model (no variables) should not report any variable as fixed");
    }

    @Test
    void testIsAnyVariableFixed_AllFree() {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        // three free variables with distinct bounds
        model.addVariable().lower(BigDecimal.ZERO).upper(BigDecimal.ONE);
        model.addVariable().lower(BigDecimal.ONE).upper(BigDecimal.TEN);
        model.addVariable().lower(BigDecimal.TEN).upper(BigDecimal.valueOf(100));
        assertFalse(model.isAnyVariableFixed(), "Model with only free variables must report no fixed variables");
    }

    @Test
    void testIsAnyVariableFixed_AtLeastOneFixed() {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        // free variable
        model.addVariable().lower(BigDecimal.ZERO).upper(BigDecimal.TEN);
        // fixed variable (lower == upper)
        model.addVariable().lower(BigDecimal.valueOf(5)).upper(BigDecimal.valueOf(5));
        assertTrue(model.isAnyVariableFixed(), "Model should report a fixed variable when one variable has equal lower and upper bounds");
    }

    @Test
    void testIsAnyVariableFixed_FixAfterCheck() {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        Variable v = model.addVariable().lower(BigDecimal.ZERO).upper(BigDecimal.TEN);
        assertFalse(model.isAnyVariableFixed(), "Initially the model has no fixed variable");
        // fix the variable by setting equal bounds
        v.lower(BigDecimal.valueOf(7)).upper(BigDecimal.valueOf(7));
        assertTrue(model.isAnyVariableFixed(), "After fixing a previously free variable the method must return true");
    }

    @Test
    void testIsAnyVariableFixed_FixedFirstThenManyFree() {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        // fixed variable first
        model.addVariable().lower(BigDecimal.ONE).upper(BigDecimal.ONE);
        // many free variables afterwards
        for (int i = 0; i < 10; i++) {
            model.addVariable().lower(BigDecimal.valueOf(i)).upper(BigDecimal.valueOf(i + 10));
        }
        assertTrue(model.isAnyVariableFixed(), "Presence of a fixed variable should make the method return true regardless of later free variables");
    }

    @Test
    void testIsAnyVariableFixed_SingleVariableFixed() {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        model.addVariable().lower(BigDecimal.ONE).upper(BigDecimal.ONE);
        assertTrue(model.isAnyVariableFixed(), "A model with a single fixed variable must return true");
    }

    @Test
    void testIsAnyVariableFixed_SingleVariableFree() {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        model.addVariable().lower(BigDecimal.ZERO).upper(BigDecimal.TEN);
        assertFalse(model.isAnyVariableFixed(), "A model with a single free variable must return false");
    }

    @Test
    void testIsAnyVariableFixed_ShardCopyPreservesFixedFlag() {
        ExpressionsBasedModel original = new ExpressionsBasedModel();
        // create a fixed variable
        original.addVariable().lower(BigDecimal.valueOf(3)).upper(BigDecimal.valueOf(3));
        // shallow copy (first argument true)
        ExpressionsBasedModel copy = original.copy(true, false);
        assertTrue(copy.isAnyVariableFixed(), "Shallow copy should preserve the fixed state of variables");
    }

    /* --------------------------------------------------------------
    Parameterised test for the two simple scenarios (single variable)
    --------------------------------------------------------------
     */
    @ParameterizedTest(name = "[{index}] fixed={0}")
    @CsvSource({ "true, 5, 5", "false, 0, 10" })
    void testSingleVariableParameterized(boolean expectedFixed, int lower, int upper) {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        model.addVariable().lower(BigDecimal.valueOf(lower)).upper(BigDecimal.valueOf(upper));
        assertEquals(expectedFixed, model.isAnyVariableFixed(), "Single variable with bounds [" + lower + "," + upper + "]");
    }

    @Test
    void testIsAnyVariableFixed_NullInternalList_throwsNPE() throws Exception {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        // Force the private field myVariables to null via reflection
        Field f = ExpressionsBasedModel.class.getDeclaredField("myVariables");
        f.setAccessible(true);
        f.set(model, null);
        assertThrows(NullPointerException.class, model::isAnyVariableFixed, "When myVariables is null a NullPointerException should be thrown");
    }

    @Test
    void testIsAnyVariableFixed_NullElementInList_throwsNPE() throws Exception {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        // Normal variable to keep list non‑empty
        model.addVariable().lower(BigDecimal.ZERO).upper(BigDecimal.ONE);
        // Insert a null entry via reflection
        Field f = ExpressionsBasedModel.class.getDeclaredField("myVariables");
        f.setAccessible(true);
        @SuppressWarnings("unchecked")
        List<Variable> list = (List<Variable>) f.get(model);
        list.add(null);
        assertThrows(NullPointerException.class, model::isAnyVariableFixed, "A null entry inside myVariables should cause a NullPointerException");
    }

    @Test
    void testIsAnyVariableFixed_VariableThrowsIllegalStateException_propagates() throws Exception {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        // Insert the custom bad variable into the internal list via reflection
        Field f = ExpressionsBasedModel.class.getDeclaredField("myVariables");
        f.setAccessible(true);
        @SuppressWarnings("unchecked")
        List<Variable> list = (List<Variable>) f.get(model);
        list.add(new BadVariable(model));
        IllegalStateException thrown = assertThrows(IllegalStateException.class, model::isAnyVariableFixed, "The IllegalStateException from Variable.isFixed() must propagate");
        assertEquals("bad variable", thrown.getMessage());
    }

    @Test
    void testIsAnyVariableFixed_ConcurrentModification_throwsCME() throws Exception {
        // Create a model and replace its internal list with one that throws CME on iterator()
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        Field f = ExpressionsBasedModel.class.getDeclaredField("myVariables");
        f.setAccessible(true);
        f.set(model, new CmeArrayList<>());
        assertThrows(ConcurrentModificationException.class, model::isAnyVariableFixed, "Concurrent modification during stream traversal should result in CME");
    }
}