package org.ojalgo.optimisation;

// import org.junit.*;
// import static org.junit.Assert.*;

class ExpressionsBasedModel_getOptimisationSense_Test {

    /**
     * Instance used in the tests.
     */
    private ExpressionsBasedModel model;

//     @BeforeEach
    void setupBeforeEach() {
        // Create a fresh model for each test.
        model = new ExpressionsBasedModel();
    }

//     @AfterEach
    void teardownAfterEach() {
        // Help GC – not strictly necessary but keeps the pattern tidy.
        model = null;
    }

    /**
     * Supplies the different senses (including null) for the parameterised test.
     */
//     private static Stream<Arguments> senseProvider() {
//         return Stream.of(Arguments.of(Optimisation.Sense.MIN), Arguments.of(Optimisation.Sense.MAX), Arguments.of((Optimisation.Sense) null));
//     }

    /**
     * The optimisation sense should be {@code null} when a model is newly created.
     */
//     @Test
//     void testDefaultOptimisationSenseIsNull() {
//         assertNull("A newly created ExpressionsBasedModel should return null for getOptimisationSense().", model.getOptimisationSense());
//     }

    /**
     * Setting the optimisation sense should be reflected by the getter.
     */
//     @Test
//     void testSetOptimisationSense() {
//         // Set to MAX and verify.
//         model.setOptimisationSense(Optimisation.Sense.MAX);
//         assertEquals("The optimisation sense should be MAX after setting it.", Optimisation.Sense.MAX, model.getOptimisationSense());
//         // Change to MIN and verify.
//         model.setOptimisationSense(Optimisation.Sense.MIN);
//         assertEquals("The optimisation sense should be MIN after changing it.", Optimisation.Sense.MIN, model.getOptimisationSense());
//     }

    /**
     * A copy of a model must retain the optimisation sense of the original.
     */
//     @Test
//     void testCopyRetainsOptimisationSense() {
//         // Arrange: set a specific sense on the original model
//         model.setOptimisationSense(Optimisation.Sense.MAX);
//         // Act: create a copy
//         ExpressionsBasedModel copy = model.copy();
//         // Assert: copy is not null and keeps the same sense
//         assertNotNull("The copy returned by copy() should not be null.", copy);
//         assertEquals("The copied model should retain the optimisation sense of the original.", Optimisation.Sense.MAX, copy.getOptimisationSense());
//     }

    /**
     * After calling {@code maximise()}, the model must report {@code MAXIMISE}.
     */
//     @Test
//     void testMaximiseSetsOptimisationSense() {
//         // Preconditions: ensure the sense is not already MAX
//         model.setOptimisationSense(Optimisation.Sense.MIN);
//         assertEquals("Pre‑condition check failed.", Optimisation.Sense.MIN, model.getOptimisationSense());
//         // Act: call maximise()
//         model.maximise();
//         // Assert: the sense is now MAX
//         assertEquals("Calling maximise() should set the optimisation sense to MAX.", Optimisation.Sense.MAX, model.getOptimisationSense());
//     }

//     @Before
//     public void setUp() {
//         // Create a fresh model for each test.
//         model = new ExpressionsBasedModel();
//     }

//     @After
//     public void tearDown() {
//         // Help GC – not strictly necessary but keeps the pattern tidy.
//         model = null;
//     }
}