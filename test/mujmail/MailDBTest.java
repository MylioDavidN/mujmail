package test.mujmail;

import java.util.Random;

import j2meunit.framework.Test;
import j2meunit.framework.TestCase;
import j2meunit.framework.TestMethod;
import j2meunit.framework.TestSuite;
import mujmail.MailDB;

public class MailDBTest extends TestCase {

    /** Flag signals if we want to print debug prints */
    private static final boolean DEBUG = false;

    public MailDBTest() {
        super();
    }
    
    public MailDBTest(String name, TestMethod testMethod) {
        super(name, testMethod);
    }

    public void testSaveLoadNullableNull() {
        final String str = null;
        try {
            final String saved = MailDB.saveNullable( str );
              if (DEBUG) System.out.println( "saved='" + saved + "'" );
            final String loaded = MailDB.loadNullable( saved );
              if (DEBUG) System.out.println( "loaded='" + loaded + "'" );
            if ( str != MailDB.loadNullable(saved) ) {
                fail();
            }
        } catch (Throwable t) {
            System.err.println("Unexpected error");
            t.printStackTrace();
        }
    }

    public void testSaveLoadNullableEmptyString() {
        final String str = "";
        try {
            final String saved = MailDB.saveNullable( str );
              if (DEBUG) System.out.println( "saved='" + saved + "'" );
            final String loaded = MailDB.loadNullable( saved );
              if (DEBUG) System.out.println( "loaded='" + loaded + "'" );
            if ( !str.equals( MailDB.loadNullable(saved) ) ) {
                fail();
            }
        } catch (Throwable t) {
            System.err.println("Unexpected error");
            t.printStackTrace();
        }
    }

    public void testSaveLoadNullableBackslash() {
        final String str = "\\";
        try {
            final String saved = MailDB.saveNullable( str );
              if (DEBUG) System.out.println( "saved='" + saved + "'" );
            final String loaded = MailDB.loadNullable( saved );
              if (DEBUG) System.out.println( "loaded='" + loaded + "'" );
            if ( !str.equals( MailDB.loadNullable(saved) ) ) {
                fail();
            }
        } catch (Throwable t) {
            System.err.println("Unexpected error");
            t.printStackTrace();
        }
    }

    public void testSaveLoadNullableStringWithBackslash() {
        final Random random = new Random();
        final long randomLong = random.nextLong();
        final String str = "\\" + Long.toString(randomLong, 35);
        try {
            final String saved = MailDB.saveNullable( str );
              if (DEBUG) System.out.println( "saved='" + saved + "'" );
            final String loaded = MailDB.loadNullable( saved );
              if (DEBUG) System.out.println( "loaded='" + loaded + "'" );
            if ( !str.equals( MailDB.loadNullable(saved) ) ) {
                fail();
            }
        } catch (Throwable t) {
            System.err.println("Unexpected error");
            t.printStackTrace();
        }
    }

    public void testSaveLoadNullableStringWithoutBackslash() {
        final Random random = new Random();
        final long randomLong = random.nextLong();
        final String str = Long.toString(randomLong, 35);
        try {
            final String saved = MailDB.saveNullable( str );
              if (DEBUG) System.out.println( "saved='" + saved + "'" );
            final String loaded = MailDB.loadNullable( saved );
              if (DEBUG) System.out.println( "loaded='" + loaded + "'" );
            if ( !str.equals( MailDB.loadNullable(saved) ) ) {
                fail();
            }
        } catch (Throwable t) {
            System.err.println("Unexpected error");
            t.printStackTrace();
        }
    }

    public Test suite() {
        TestSuite suite = new TestSuite();

        suite.addTest( new MailDBTest("testSaveLoadNullableNull", new TestMethod() {
            public void run(TestCase testCase) throws Throwable {
                ((MailDBTest)testCase).testSaveLoadNullableNull();
            }
        }) );

        suite.addTest( new MailDBTest("testSaveLoadNullableEmptyString", new TestMethod() {
            public void run(TestCase testCase) throws Throwable {
                ((MailDBTest)testCase).testSaveLoadNullableEmptyString();
            }
        }) );

        suite.addTest( new MailDBTest("testSaveLoadNullableBackslash", new TestMethod() {
            public void run(TestCase testCase) throws Throwable {
                ((MailDBTest)testCase).testSaveLoadNullableBackslash();
            }
        }) );

        suite.addTest( new MailDBTest("testSaveLoadNullableStringWithBackslash", new TestMethod() {
            public void run(TestCase testCase) throws Throwable {
                ((MailDBTest)testCase).testSaveLoadNullableStringWithBackslash();
            }
        }) );

        suite.addTest( new MailDBTest("testSaveLoadNullableStringWithoutBackslash", new TestMethod() {
            public void run(TestCase testCase) throws Throwable {
                ((MailDBTest)testCase).testSaveLoadNullableStringWithoutBackslash();
            }
        }) );

        return suite;
    }
}
