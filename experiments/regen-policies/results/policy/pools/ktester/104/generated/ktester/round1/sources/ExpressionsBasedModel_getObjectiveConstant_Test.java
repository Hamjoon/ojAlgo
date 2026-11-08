package org.ojalgo.optimisation;

import static org.ojalgo.function.constant.BigMath.ZERO;
import static org.junit.jupiter.api.Assertions.*;
import java.math.BigDecimal;
import org.junit.jupiter.api.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@ExtendWith(MockitoExtension.class)
class ExpressionsBasedModel_getObjectiveConstant_Test {

    /* -----------------------------------------------------------------------
     * Additional parameterised tests for cumulative additions using the public
     * (package‑private) method without reflection – demonstrates normal usage.
     * ----------------------------------------------------------------------- */
    private static Stream<Arguments> cumulativeAdditionProvider() {
        return Stream.of(Arguments.of(new BigDecimal[] { new BigDecimal("5"), new BigDecimal("3") }, new BigDecimal("8")), Arguments.of(new BigDecimal[] { new BigDecimal("10"), new BigDecimal("-4") }, new BigDecimal("6")), Arguments.of(new BigDecimal[] { new BigDecimal("100"), new BigDecimal("-50"), new BigDecimal("25") }, new BigDecimal("75")));
    }

    @Test
    void testDefaultObjectiveConstantIsZero() {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        // package‑private access
        BigDecimal constant = model.getObjectiveConstant();
        assertEquals(ZERO, constant, "The default objective constant should be ZERO");
    }

    @Test
    void testAddObjectiveConstantUpdatesValue() {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        // first addition
        model.addObjectiveConstant(BigDecimal.valueOf(5));
        assertEquals(BigDecimal.valueOf(5), model.getObjectiveConstant(), "Objective constant should reflect the first addition");
        // second addition (cumulative)
        model.addObjectiveConstant(BigDecimal.valueOf(3));
        assertEquals(BigDecimal.valueOf(8), model.getObjectiveConstant(), "Objective constant should be cumulative after a second addition");
    }

    @Test
    void testAddObjectiveConstantWithNegativeValue() {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        model.addObjectiveConstant(BigDecimal.valueOf(10));
        model.addObjectiveConstant(BigDecimal.valueOf(-4));
        assertEquals(BigDecimal.valueOf(6), model.getObjectiveConstant(), "Adding a negative value should correctly decrease the constant");
    }

    /**
     * Directly invoke the package‑private {@code addObjectiveConstant} using reflection
     *  and verify the getter returns the exact value that was added.
     */
    @Test
    void testAddObjectiveConstantViaReflectionPositive() throws Exception {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        // Obtain the method via reflection (package‑private, but accessible because we are in same package)
        Method addMethod = ExpressionsBasedModel.class.getDeclaredMethod("addObjectiveConstant", BigDecimal.class);
        addMethod.setAccessible(true);
        addMethod.invoke(model, new BigDecimal("123.456"));
        BigDecimal constant = model.getObjectiveConstant();
        assertEquals(new BigDecimal("123.456"), constant, "Getter should return the value added via reflection");
    }

    /**
     * Same as above but with a negative value.
     */
    @Test
    void testAddObjectiveConstantViaReflectionNegative() throws Exception {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        Method addMethod = ExpressionsBasedModel.class.getDeclaredMethod("addObjectiveConstant", BigDecimal.class);
        addMethod.setAccessible(true);
        addMethod.invoke(model, new BigDecimal("-987.654"));
        BigDecimal constant = model.getObjectiveConstant();
        assertEquals(new BigDecimal("-987.654"), constant, "Getter should return the negative value added via reflection");
    }

    /**
     * Cumulative addition test – three invocations (two positive, one negative).
     */
    @Test
    void testAddObjectiveConstantCumulativeViaReflection() throws Exception {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        Method addMethod = ExpressionsBasedModel.class.getDeclaredMethod("addObjectiveConstant", BigDecimal.class);
        addMethod.setAccessible(true);
        addMethod.invoke(model, new BigDecimal("1000"));
        addMethod.invoke(model, new BigDecimal("2000"));
        addMethod.invoke(model, new BigDecimal("-500"));
        BigDecimal expected = new BigDecimal("1000").add(new BigDecimal("2000")).add(new BigDecimal("-500"));
        assertEquals(expected, model.getObjectiveConstant(), "Getter should return the cumulative sum of all additions");
    }

    /**
     * Ensure that the getter returns the exact same instance stored internally (no defensive copy).
     */
    @Test
    void testGetObjectiveConstantReturnsSameInstance() throws Exception {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        // Directly set the internal field to a known instance via reflection
        Field field = ExpressionsBasedModel.class.getDeclaredField("myObjectiveConstant");
        field.setAccessible(true);
        BigDecimal sentinel = new BigDecimal("42.42");
        field.set(model, sentinel);
        // The getter should return the *same* instance (==)
        BigDecimal returned = model.getObjectiveConstant();
        assertSame(sentinel, returned, "Getter should return the exact same BigDecimal instance stored in the model");
    }

    /**
     * Adding ZERO should leave the constant unchanged and still reference the original ZERO instance.
     */
    @Test
    void testAddZeroViaReflectionKeepsSameInstance() throws Exception {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        // Capture the original instance
        BigDecimal original = model.getObjectiveConstant();
        // Invoke addObjectiveConstant(ZERO) via reflection
        Method addMethod = ExpressionsBasedModel.class.getDeclaredMethod("addObjectiveConstant", BigDecimal.class);
        addMethod.setAccessible(true);
        addMethod.invoke(model, ZERO);
        // Verify value and identity
        BigDecimal after = model.getObjectiveConstant();
        assertEquals(ZERO, after, "Adding ZERO should keep the value ZERO");
        assertSame(original, after, "Adding ZERO should not replace the original instance");
    }

    /**
     * Simulate a corrupted internal state where the field is null; the getter is expected to throw NPE.
     */
    @Test
    void testGetObjectiveConstantThrowsWhenFieldIsNull() throws Exception {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        // Force the private field to null via reflection
        Field field = ExpressionsBasedModel.class.getDeclaredField("myObjectiveConstant");
        field.setAccessible(true);
        field.set(model, null);
        // Expect a NullPointerException when calling the getter
        assertThrows(NullPointerException.class, model::getObjectiveConstant, "Getter should throw NullPointerException when internal field is null");
    }

    @ParameterizedTest
    @MethodSource("cumulativeAdditionProvider")
    void testAddObjectiveConstantCumulativeNormalUsage(BigDecimal[] additions, BigDecimal expected) throws Exception {
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        // Use reflection to call the package‑private method (keeps test independent of visibility)
        Method addMethod = ExpressionsBasedModel.class.getDeclaredMethod("addObjectiveConstant", BigDecimal.class);
        addMethod.setAccessible(true);
        for (BigDecimal add : additions) {
            addMethod.invoke(model, add);
        }
        assertEquals(expected, model.getObjectiveConstant(), "Cumulative addition via normal usage should match expected value");
    }
}
