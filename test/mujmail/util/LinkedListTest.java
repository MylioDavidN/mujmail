package test.mujmail.util;

import mujmail.util.Iterator;
import mujmail.util.LinkedList;
import j2meunit.framework.Test;
import j2meunit.framework.TestCase;
import j2meunit.framework.TestMethod;
import j2meunit.framework.TestSuite;

/**
 * Provides tests for {@link LinkedList} class.
 * 
 * @author Betlista
 */
public class LinkedListTest extends TestCase {

    //private static final boolean DEBUG = false;

	/**
	 * Constructor simply calls <code>super()</code>, but it's needed
	 * to run the testing, when missing error "Could not create and run test
	 * suite" shown to console.
	 */
    public LinkedListTest() {
        super();
    }

    /**
     * Constructor used to add test methods to suite.
     * 
     * @param name name of the method to run, when error occurs this name
     *        is shown to identify the method where error occurred
     * @param testMethod method to be tested
     */
    public LinkedListTest(String name, TestMethod testMethod) {
        super(name, testMethod);
    }

    //==========================================================================

    /**
     * First of all the adding to the list have to work.
     * This method tests adding to the list functionality.
     * Uses only the simplest methods in {@link LinkedList}:
     * {@link LinkedList#size() size()}, {@link LinkedList#getFirst() getFirst()},
     * {@link LinkedList#getLast() getLast()}.
     */
    public void testAdding() {
    	LinkedList list = new LinkedList();
    	assertEquals( 0, list.size() );
    	list.add( "A" );
    	assertEquals( 1, list.size() );
    	assertEquals( "A", list.getFirst() );
    	assertEquals( "A", list.getLast() );
    	list.add( "B" );
    	assertEquals( 2, list.size() );
    	assertEquals( "A", list.getFirst() );
    	assertEquals( "B", list.getLast() );
    	list.add( "C" );
    	assertEquals( 3, list.size() );
    	assertEquals( "A", list.getFirst() );
    	assertEquals( "C", list.getLast() );
    }

    /**
     * Tests removing from list.<br>
     * When adding is working fine
     * (see {@link LinkedListTest#testAdding() testAdding()}) we have to test
     * removing.
     */
    public void testRemoving() {
    	LinkedList list = new LinkedList();
    	  // test removing first
    	list.add( "A" );
    	String value = (String)list.removeFirst();
    	assertEquals( 0, list.size() );
    	assertEquals( "A", value );
    	
          // test removing last
    	list.add( "B" );
    	value = (String)list.removeLast();
    	assertEquals( 0, list.size() );
    	assertEquals( "B", value );

    	list.add( "K" );
    	list.add( "L" );
    	value = (String)list.removeLast();
    	assertEquals( 1, list.size() );
    	assertEquals( "L", value );
    	assertEquals( "K", list.getFirst() );
    	assertEquals( "K", list.getLast() );
    	
    	list.add(value);
    	value = (String)list.removeFirst();
    	assertEquals( 1, list.size() );
    	assertEquals( "K", value );
    	assertEquals( "L", list.getFirst() );
    	assertEquals( "L", list.getLast() );
    }

    /**
     * Test iterating over list elements.
     */
    public void testIteration() {
    	Object[] values = new Object[] { "1", "2", "3", "4" };
    	LinkedList list = new LinkedList();
    	for ( int i = 0; i < values.length; ++i ) {
    		list.add( values[i] );
    	}

    	final Iterator iter = list.getIterator();
    	int i = 0;
    	Object o;
    	while ( iter.hasNext() ) {
    		o = iter.next();
    		assertEquals(values[i], o);
    		++i;
    	}
    }

    /**
     * Test concatenation of two lists.
     */
    public void testConcatenation() {
    	final int NUMBER_COUNT = 5; 
    	final int CHAR_COUNT = 'D' - 'A' + 1; 
    	final Object[] values = new Object[] { "1", "2", "3", "4", "5", "A", "B", "C", "D"};

    	final LinkedList numbers = new LinkedList();
    	for ( int i = 0; i < NUMBER_COUNT; ++i) {
    		numbers.add( values[i] );
    	}

    	final LinkedList characters = new LinkedList();
    	for ( int i = 0; i < CHAR_COUNT; ++i) {
    		characters.add( values[NUMBER_COUNT + i] );
    	}

    	numbers.concatenateWith( characters );

    	final Iterator iter = numbers.getIterator();
    	for ( int i = 0; i < values.length; ++i ) {
    		assertEquals( values[i], iter.next());
    	}
    }

    //==========================================================================

    /**
     * Creates J2ME test suite that contains tests which are invoked and these
     * methods test functionality.
     */
    public Test suite() {
        TestSuite suite = new TestSuite();

        suite.addTest( new LinkedListTest("testAdding", new TestMethod() {
            public void run(TestCase testCase) throws Throwable {
                ((LinkedListTest)testCase).testAdding();
            }
        }) );

        suite.addTest( new LinkedListTest("testRemoving", new TestMethod() {
            public void run(TestCase testCase) throws Throwable {
                ((LinkedListTest)testCase).testRemoving();
            }
        }) );

        suite.addTest( new LinkedListTest("testIteration", new TestMethod() {
            public void run(TestCase testCase) throws Throwable {
                ((LinkedListTest)testCase).testIteration();
            }
        }) );

        suite.addTest( new LinkedListTest("testConcatenation", new TestMethod() {
            public void run(TestCase testCase) throws Throwable {
                ((LinkedListTest)testCase).testConcatenation();
            }
        }) );

        return suite;
    }
}
