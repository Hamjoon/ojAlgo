package org.ojalgo.optimisation;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.*;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ExpressionsBasedModel_getIntegration_Test {

    /**
     * Keep a copy of the original static list so we can restore it.
     */
    private static List<ExpressionsBasedModel.Integration<?>> originalIntegrations;

    /**
     * Keep the original solver integration instances.
     */
    private static Optimisation.Integration<ExpressionsBasedModel, ?> originalIntegerIntegration;

    private static Optimisation.Integration<ExpressionsBasedModel, ?> originalConvexIntegration;

    private static Optimisation.Integration<ExpressionsBasedModel, ?> originalLinearIntegration;

    @BeforeAll
    static void setupBeforeAll() throws Exception {
        // Preserve original static state
        Field listField = ExpressionsBasedModel.class.getDeclaredField("INTEGRATIONS");
        listField.setAccessible(true);
        //noinspection unchecked
        originalIntegrations = new ArrayList<>((List<ExpressionsBasedModel.Integration<?>>) listField.get(null));
        originalIntegerIntegration = IntegerSolver.INTEGRATION;
        originalConvexIntegration = ConvexSolver.INTEGRATION;
        originalLinearIntegration = LinearSolver.INTEGRATION;
    }

    @BeforeEach
    void setupBeforeEach() throws Exception {
        // Start each test with a clean integration list
        setStaticFinalField(ExpressionsBasedModel.class, "INTEGRATIONS", new ArrayList<>());
    }

    @AfterEach
    void teardownAfterEach() throws Exception {
        // Ensure the list is cleared after each test
        setStaticFinalField(ExpressionsBasedModel.class, "INTEGRATIONS", new ArrayList<>());
    }

    @AfterAll
    static void teardownAfterAll() throws Exception {
        // Restore original static state
        setStaticFinalField(ExpressionsBasedModel.class, "INTEGRATIONS", originalIntegrations);
        setStaticFinalField(IntegerSolver.class, "INTEGRATION", originalIntegerIntegration);
        setStaticFinalField(ConvexSolver.class, "INTEGRATION", originalConvexIntegration);
        setStaticFinalField(LinearSolver.class, "INTEGRATION", originalLinearIntegration);
    }

    /**
     * Utility to replace a static final field via reflection.
     */
    private static void setStaticFinalField(Class<?> clazz, String fieldName, Object newValue) throws Exception {
        Field f = clazz.getDeclaredField(fieldName);
        f.setAccessible(true);
        // Remove final modifier (works for JDK 9+ as well)
        Field modifiers = Field.class.getDeclaredField("modifiers");
        modifiers.setAccessible(true);
        modifiers.setInt(f, f.getModifiers() & ~java.lang.reflect.Modifier.FINAL);
        f.set(null, newValue);
    }

    @Test
    void testPreferredIntegrationFoundInList() throws Exception {
        // Mock a preferred integration that claims to be capable
        @SuppressWarnings("unchecked")
        Optimisation.Integration<ExpressionsBasedModel, ?> preferred = mock(Optimisation.Integration.class);
        when(preferred.isCapable(any())).thenReturn(true);
        // Add it to the static list
        List<ExpressionsBasedModel.Integration<?>> list = new ArrayList<>();
        //noinspection unchecked
        list.add(preferred);
        setStaticFinalField(ExpressionsBasedModel.class, "INTEGRATIONS", list);
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        // The method should return the preferred mock
        Optimisation.Integration<?, ?> result = model.getIntegration();
        assertSame(preferred, result);
    }

    @Test
    void testIntegerIntegrationChosenWhenModelHasIntegerVariable() throws Exception {
        // No preferred integrations
        setStaticFinalField(ExpressionsBasedModel.class, "INTEGRATIONS", new ArrayList<>());
        // Mock IntegerSolver integration to be capable, others not
        @SuppressWarnings("unchecked")
        Optimisation.Integration<ExpressionsBasedModel, ?> intMock = mock(Optimisation.Integration.class);
        @SuppressWarnings("unchecked")
        Optimisation.Integration<ExpressionsBasedModel, ?> convexMock = mock(Optimisation.Integration.class);
        @SuppressWarnings("unchecked")
        Optimisation.Integration<ExpressionsBasedModel, ?> linearMock = mock(Optimisation.Integration.class);
        when(intMock.isCapable(any())).thenReturn(true);
        when(convexMock.isCapable(any())).thenReturn(false);
        when(linearMock.isCapable(any())).thenReturn(false);
        // Replace static solver integrations
        setStaticFinalField(IntegerSolver.class, "INTEGRATION", intMock);
        setStaticFinalField(ConvexSolver.class, "INTEGRATION", convexMock);
        setStaticFinalField(LinearSolver.class, "INTEGRATION", linearMock);
        // Create a model that contains an integer variable
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        // integer flag makes isAnyVariableInteger() true
        model.newVariable("intVar").integer(true).weight(1);
        Optimisation.Integration<?, ?> result = model.getIntegration();
        assertSame(intMock, result);
    }

    @Test
    void testConvexIntegrationChosenWhenNoIntegerVariable() throws Exception {
        setStaticFinalField(ExpressionsBasedModel.class, "INTEGRATIONS", new ArrayList<>());
        @SuppressWarnings("unchecked")
        Optimisation.Integration<ExpressionsBasedModel, ?> intMock = mock(Optimisation.Integration.class);
        @SuppressWarnings("unchecked")
        Optimisation.Integration<ExpressionsBasedModel, ?> convexMock = mock(Optimisation.Integration.class);
        @SuppressWarnings("unchecked")
        Optimisation.Integration<ExpressionsBasedModel, ?> linearMock = mock(Optimisation.Integration.class);
        when(intMock.isCapable(any())).thenReturn(false);
        when(convexMock.isCapable(any())).thenReturn(true);
        when(linearMock.isCapable(any())).thenReturn(false);
        setStaticFinalField(IntegerSolver.class, "INTEGRATION", intMock);
        setStaticFinalField(ConvexSolver.class, "INTEGRATION", convexMock);
        setStaticFinalField(LinearSolver.class, "INTEGRATION", linearMock);
        // Model without integer variables
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        model.newVariable("x").lower(0).upper(10).weight(1);
        Optimisation.Integration<?, ?> result = model.getIntegration();
        assertSame(convexMock, result);
    }

    @Test
    void testLinearIntegrationChosenWhenConvexNotCapable() throws Exception {
        setStaticFinalField(ExpressionsBasedModel.class, "INTEGRATIONS", new ArrayList<>());
        @SuppressWarnings("unchecked")
        Optimisation.Integration<ExpressionsBasedModel, ?> intMock = mock(Optimisation.Integration.class);
        @SuppressWarnings("unchecked")
        Optimisation.Integration<ExpressionsBasedModel, ?> convexMock = mock(Optimisation.Integration.class);
        @SuppressWarnings("unchecked")
        Optimisation.Integration<ExpressionsBasedModel, ?> linearMock = mock(Optimisation.Integration.class);
        when(intMock.isCapable(any())).thenReturn(false);
        when(convexMock.isCapable(any())).thenReturn(false);
        when(linearMock.isCapable(any())).thenReturn(true);
        setStaticFinalField(IntegerSolver.class, "INTEGRATION", intMock);
        setStaticFinalField(ConvexSolver.class, "INTEGRATION", convexMock);
        setStaticFinalField(LinearSolver.class, "INTEGRATION", linearMock);
        // Model without integer variables
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        model.newVariable("x").lower(0).upper(5).weight(1);
        Optimisation.Integration<?, ?> result = model.getIntegration();
        assertSame(linearMock, result);
    }

    @Test
    void testThrowsProgrammingErrorWhenNoIntegrationCapable() throws Exception {
        setStaticFinalField(ExpressionsBasedModel.class, "INTEGRATIONS", new ArrayList<>());
        @SuppressWarnings("unchecked")
        Optimisation.Integration<ExpressionsBasedModel, ?> intMock = mock(Optimisation.Integration.class);
        @SuppressWarnings("unchecked")
        Optimisation.Integration<ExpressionsBasedModel, ?> convexMock = mock(Optimisation.Integration.class);
        @SuppressWarnings("unchecked")
        Optimisation.Integration<ExpressionsBasedModel, ?> linearMock = mock(Optimisation.Integration.class);
        when(intMock.isCapable(any())).thenReturn(false);
        when(convexMock.isCapable(any())).thenReturn(false);
        when(linearMock.isCapable(any())).thenReturn(false);
        setStaticFinalField(IntegerSolver.class, "INTEGRATION", intMock);
        setStaticFinalField(ConvexSolver.class, "INTEGRATION", convexMock);
        setStaticFinalField(LinearSolver.class, "INTEGRATION", linearMock);
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        ProgrammingError thrown = assertThrows(ProgrammingError.class, model::getIntegration);
        assertEquals("No solver integration available that can handle this model!", thrown.getMessage());
    }

    /**
     * Scenario: the model has integer variables, IntegerSolver is not capable,
     * but ConvexSolver is capable. The method must fall back to ConvexSolver.
     */
    @Test
    void testConvexChosenWhenIntegerSolverNotCapable() throws Exception {
        // Preserve original integrations to restore later
        Optimisation.Integration<ExpressionsBasedModel, ?> originalInt = IntegerSolver.INTEGRATION;
        Optimisation.Integration<ExpressionsBasedModel, ?> originalConv = ConvexSolver.INTEGRATION;
        Optimisation.Integration<ExpressionsBasedModel, ?> originalLin = LinearSolver.INTEGRATION;
        try {
            setStaticFinalField(ExpressionsBasedModel.class, "INTEGRATIONS", new ArrayList<>());
            @SuppressWarnings("unchecked")
            Optimisation.Integration<ExpressionsBasedModel, ?> intMock = mock(Optimisation.Integration.class);
            @SuppressWarnings("unchecked")
            Optimisation.Integration<ExpressionsBasedModel, ?> convexMock = mock(Optimisation.Integration.class);
            @SuppressWarnings("unchecked")
            Optimisation.Integration<ExpressionsBasedModel, ?> linearMock = mock(Optimisation.Integration.class);
            // not capable
            when(intMock.isCapable(any())).thenReturn(false);
            // capable
            when(convexMock.isCapable(any())).thenReturn(true);
            when(linearMock.isCapable(any())).thenReturn(false);
            setStaticFinalField(IntegerSolver.class, "INTEGRATION", intMock);
            setStaticFinalField(ConvexSolver.class, "INTEGRATION", convexMock);
            setStaticFinalField(LinearSolver.class, "INTEGRATION", linearMock);
            ExpressionsBasedModel model = new ExpressionsBasedModel();
            model.newVariable("iVar").integer(true).weight(1);
            Optimisation.Integration<?, ?> result = model.getIntegration();
            assertSame(convexMock, result);
        } finally {
            // Restore original solver integrations
            setStaticFinalField(IntegerSolver.class, "INTEGRATION", originalInt);
            setStaticFinalField(ConvexSolver.class, "INTEGRATION", originalConv);
            setStaticFinalField(LinearSolver.class, "INTEGRATION", originalLin);
        }
    }

    /**
     * Preferred integration after a non‑capable entry.
     * The first mock reports {@code false}, the second reports {@code true}.
     */
    @Test
    void testPreferredIntegrationAfterNonCapableEntry() throws Exception {
        @SuppressWarnings("unchecked")
        Optimisation.Integration<ExpressionsBasedModel, ?> first = mock(Optimisation.Integration.class);
        @SuppressWarnings("unchecked")
        Optimisation.Integration<ExpressionsBasedModel, ?> second = mock(Optimisation.Integration.class);
        when(first.isCapable(any())).thenReturn(false);
        when(second.isCapable(any())).thenReturn(true);
        List<ExpressionsBasedModel.Integration<?>> list = new ArrayList<>();
        //noinspection unchecked
        list.add(first);
        list.add(second);
        setStaticFinalField(ExpressionsBasedModel.class, "INTEGRATIONS", list);
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        Optimisation.Integration<?, ?> result = model.getIntegration();
        assertSame(second, result);
    }

    /**
     * A {@code null} entry as the first element of {@code INTEGRATIONS}
     * should cause a {@link NullPointerException}.
     */
    @Test
    void testNullIntegrationEntryCausesNPE() throws Exception {
        List<ExpressionsBasedModel.Integration<?>> list = new ArrayList<>();
        // first element is null
        list.add(null);
        setStaticFinalField(ExpressionsBasedModel.class, "INTEGRATIONS", list);
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        assertThrows(NullPointerException.class, model::getIntegration);
    }

    /**
     * If an integration's {@code isCapable} method throws a runtime exception,
     * the exception must propagate unchanged.
     */
    @Test
    void testIsCapableThrowsExceptionPropagation() throws Exception {
        @SuppressWarnings("unchecked")
        Optimisation.Integration<ExpressionsBasedModel, ?> faulty = mock(Optimisation.Integration.class);
        when(faulty.isCapable(any())).thenThrow(new IllegalStateException("capability check failed"));
        List<ExpressionsBasedModel.Integration<?>> list = new ArrayList<>();
        //noinspection unchecked
        list.add(faulty);
        setStaticFinalField(ExpressionsBasedModel.class, "INTEGRATIONS", list);
        ExpressionsBasedModel model = new ExpressionsBasedModel();
        IllegalStateException thrown = assertThrows(IllegalStateException.class, model::getIntegration);
        assertEquals("capability check failed", thrown.getMessage());
    }

    /**
     * When the model has integer variables but {@code IntegerSolver.INTEGRATION}
     * is {@code null}, a {@link NullPointerException} is expected.
     */
    @Test
    void testIntegerSolverIntegrationNullCausesNPE() throws Exception {
        Optimisation.Integration<ExpressionsBasedModel, ?> originalInt = IntegerSolver.INTEGRATION;
        try {
            setStaticFinalField(ExpressionsBasedModel.class, "INTEGRATIONS", new ArrayList<>());
            setStaticFinalField(IntegerSolver.class, "INTEGRATION", null);
            ExpressionsBasedModel model = new ExpressionsBasedModel();
            model.newVariable("iVar").integer(true).weight(1);
            assertThrows(NullPointerException.class, model::getIntegration);
        } finally {
            // Restore the original integration
            setStaticFinalField(IntegerSolver.class, "INTEGRATION", originalInt);
        }
    }

    /**
     * When the model is non‑integer and {@code ConvexSolver.INTEGRATION}
     * is {@code null}, a {@link NullPointerException} is expected.
     */
    @Test
    void testConvexSolverIntegrationNullCausesNPE() throws Exception {
        Optimisation.Integration<ExpressionsBasedModel, ?> originalConv = ConvexSolver.INTEGRATION;
        try {
            setStaticFinalField(ExpressionsBasedModel.class, "INTEGRATIONS", new ArrayList<>());
            setStaticFinalField(ConvexSolver.class, "INTEGRATION", null);
            // Ensure IntegerSolver is not chosen (model has no integer vars)
            ExpressionsBasedModel model = new ExpressionsBasedModel();
            model.newVariable("x").lower(0).upper(10).weight(1);
            assertThrows(NullPointerException.class, model::getIntegration);
        } finally {
            setStaticFinalField(ConvexSolver.class, "INTEGRATION", originalConv);
        }
    }

    /**
     * When the model is non‑integer, ConvexSolver is not capable and
     * {@code LinearSolver.INTEGRATION} is {@code null}, a {@link NullPointerException}
     * must be thrown.
     */
    @Test
    void testLinearSolverIntegrationNullCausesNPE() throws Exception {
        Optimisation.Integration<ExpressionsBasedModel, ?> originalConv = ConvexSolver.INTEGRATION;
        Optimisation.Integration<ExpressionsBasedModel, ?> originalLin = LinearSolver.INTEGRATION;
        try {
            setStaticFinalField(ExpressionsBasedModel.class, "INTEGRATIONS", new ArrayList<>());
            // ConvexSolver returns false, LinearSolver is null
            @SuppressWarnings("unchecked")
            Optimisation.Integration<ExpressionsBasedModel, ?> convexMock = mock(Optimisation.Integration.class);
            when(convexMock.isCapable(any())).thenReturn(false);
            setStaticFinalField(ConvexSolver.class, "INTEGRATION", convexMock);
            setStaticFinalField(LinearSolver.class, "INTEGRATION", null);
            ExpressionsBasedModel model = new ExpressionsBasedModel();
            model.newVariable("x").lower(0).upper(5).weight(1);
            assertThrows(NullPointerException.class, model::getIntegration);
        } finally {
            setStaticFinalField(ConvexSolver.class, "INTEGRATION", originalConv);
            setStaticFinalField(LinearSolver.class, "INTEGRATION", originalLin);
        }
    }
}
