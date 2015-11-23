package test.mujmail.threading;

import j2meunit.framework.Test;
import j2meunit.framework.TestCase;
import j2meunit.framework.TestMethod;
import j2meunit.framework.TestSuite;

import java.util.Enumeration;
//#ifdef MUJMAIL_TEST_BACKWARD_ITERATING
//# import java.util.Vector;
//#endif

import mujmail.MessageHeader;
//#ifdef MUJMAIL_TEST_BACKWARD_ITERATING
//# import mujmail.Settings;
//#endif
import mujmail.ordering.ComparatorStrategy;
import mujmail.ordering.Criterion;
import mujmail.ordering.Ordering;
import mujmail.threading.ThreadedEmails;
//#ifdef MUJMAIL_TEST_BACKWARD_ITERATING
//# import mujmail.threading.ThreadedEmails.Enumerator;
//#endif

public class ThreadedEmailTest extends TestCase {

    private static final boolean DEBUG = false;

    public ThreadedEmailTest() {
        super();
    }
    
    public ThreadedEmailTest(String name, TestMethod testMethod) {
        super(name, testMethod);
    }

    public void testEnumeration() {
        try {
            ThreadedEmails tm = new ThreadedEmails();
    
              // create first root message 
            MessageHeader rootMessageA = new MessageHeader(null);
            rootMessageA.setThreadingMessageID( "A" );
              //   and it's children
            MessageHeader messageAA = new MessageHeader(null);
            messageAA.setThreadingMessageID( "AA" );
            messageAA.setParentID("A");
            MessageHeader messageAB = new MessageHeader(null);
            messageAB.setThreadingMessageID( "AB" );
            messageAB.setParentID("A");
    
              // create second root message 
            MessageHeader rootMessageB = new MessageHeader(null);
            rootMessageB.setThreadingMessageID( "B" );
              //   and it's children
            MessageHeader messageBA = new MessageHeader(null);
            messageBA.setThreadingMessageID( "BA" );
            messageBA.setParentID("B");
            MessageHeader messageBB = new MessageHeader(null);
            messageBB.setThreadingMessageID( "BB" );
            messageBB.setParentID("B");
    
            tm.addRoot( rootMessageB );
            tm.addRoot( rootMessageA );

            tm.addMessage(rootMessageA, messageAA);
            tm.addMessage(rootMessageA, messageAB);
    
            tm.addMessage(rootMessageB, messageBB);
            tm.addMessage(rootMessageB, messageBA);

              // order messages by subject
            //tm.sort( ComparatorStrategy.getStrategy().getComparator(Ordering.NATURAL, Criterion.MESSAGE_ID) );
            tm.sort( ComparatorStrategy.getStrategy().getComparator(Ordering.NATURAL, Criterion.THREADING_MESSAGE_ID));

            Enumeration tmEnumeration = tm.getEnumeration();
            Object o;
              // test that message A is 1.
            assertTrue( tmEnumeration.hasMoreElements() );
            o = tmEnumeration.nextElement();
            assertEquals(rootMessageA, o);

              // test that message AA is 2.
            assertTrue( tmEnumeration.hasMoreElements() );
            o = tmEnumeration.nextElement();
            assertEquals(messageAA, o);

              // test that message AB is 3.
            assertTrue( tmEnumeration.hasMoreElements() );
            o = tmEnumeration.nextElement();
            assertEquals(messageAB, o);

        
              // test that message B is 4.
            assertTrue( tmEnumeration.hasMoreElements() );
            o = tmEnumeration.nextElement();
            assertEquals(rootMessageB, o);

              // test that message BA is 5.
            assertTrue( tmEnumeration.hasMoreElements() );
            o = tmEnumeration.nextElement();
            assertEquals(messageBA, o);

              // test that message BB is 6.
            assertTrue( tmEnumeration.hasMoreElements() );
            o = tmEnumeration.nextElement();
            assertEquals(messageBB, o);
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    public void testEnumerationFail() {
        int i = 0;
        ThreadedEmails tm = new ThreadedEmails();

          // create first root message 
        MessageHeader rootMessageA = new MessageHeader(null);
        MessageHeader messageAA = new MessageHeader(null);
        MessageHeader messageAB = new MessageHeader(null);

          // create second root message 
        MessageHeader rootMessageB = new MessageHeader(null);
        MessageHeader messageBA = new MessageHeader(null);
        MessageHeader messageBB = new MessageHeader(null);

        rootMessageA.setThreadingMessageID( "A" );
        tm.addRoot( rootMessageA );
        messageAA.setThreadingMessageID( "AA" );
        tm.addMessage(rootMessageA, messageAA);
        messageAB.setThreadingMessageID( "AB" );
        tm.addMessage(rootMessageA, messageAB);

        rootMessageB.setThreadingMessageID( "B" );
        tm.addRoot( rootMessageB );
        messageBA.setThreadingMessageID( "BA" );
        tm.addMessage(rootMessageB, messageBA);
        messageBB.setThreadingMessageID( "BB" );
        tm.addMessage(rootMessageB, messageBB);

        Enumeration tmEnumeration = tm.getEnumeration();
        MessageHeader messageHeader;

        try {
            while ( true ) {
                messageHeader = (MessageHeader)tmEnumeration.nextElement(); // exception will be thrown here
                ++i;
                  if (DEBUG) System.out.println( "message header: " + messageHeader );
            }
        } catch (Throwable t) {
            //t.printStackTrace(); // we expect that exception will be thrown
            assertEquals(6, i); // number of  messages is 4
        }
    }

    public void testRemove() {
          if (DEBUG) System.out.println("testRemove");
        ThreadedEmails te = new ThreadedEmails();

          if (DEBUG) System.out.println("adding message A");
        MessageHeader messageA = new MessageHeader(null );
//        messageA.setThreadingMessageID( "A" );
        messageA.setSubject( "A" );
        te.addMessage( messageA );

          if (DEBUG) System.out.println("adding message B");
        MessageHeader messageB = new MessageHeader(null );
//        messageB.setThreadingMessageID( "B" );
        messageB.setSubject( "B" );
        te.addMessage( messageB );
        
          if (DEBUG) System.out.println("removing message 1");
        te.removeMessageAt( 1 );
          if (DEBUG) System.out.println("message 1 removed");
        
          if (DEBUG) System.out.println( te.getSize() );
        assertEquals( 1, te.getSize() );
    }

    private static void printMessageSubjects( final ThreadedEmails te ) {
        final Enumeration enumeration = te.getEnumeration();
        MessageHeader header;
        while ( enumeration.hasMoreElements() ) {
            header = (MessageHeader)enumeration.nextElement();
            System.out.print( " " + header.getSubject() );
        }
        System.out.println();
    }

    private void check( final ThreadedEmails te, final String[] subjects ) {
        assertEquals( te.getSize(), subjects.length );

        final Enumeration e = te.getEnumeration();
        MessageHeader messageHeader;
        int i = 0;
        while ( e.hasMoreElements() ) {
            messageHeader = (MessageHeader)e.nextElement();
            assertEquals( subjects[i], messageHeader.getSubject() );
            ++i;
        }
    }

    public void testRemoveAt() {
          if (DEBUG) System.out.println("testRemoveAt()");
        ThreadedEmails te = new ThreadedEmails();
        MessageHeader header = new MessageHeader(null);
        header.setSubject( "1" );
        header.setThreadingMessageID( "1" );
        te.addMessage( header );
        te.sort( ComparatorStrategy.getStrategy().getComparator( Ordering.NATURAL, Criterion.SUBJECT ) );
          if (DEBUG) printMessageSubjects( te );
        //----------------------------------------------------------------------
        header = new MessageHeader(null);
        header.setSubject( "2" );
        header.setThreadingMessageID( "2" );
        te.addMessage( header );
        te.sort( ComparatorStrategy.getStrategy().getComparator( Ordering.NATURAL, Criterion.SUBJECT ) );
          if (DEBUG) printMessageSubjects( te );
        //----------------------------------------------------------------------
        header = new MessageHeader(null);
        header.setSubject( "3" );
        header.setThreadingMessageID( "3" );
        te.addMessage( header );
        te.sort( ComparatorStrategy.getStrategy().getComparator( Ordering.NATURAL, Criterion.SUBJECT ) );
          if (DEBUG) printMessageSubjects( te );
        //----------------------------------------------------------------------
        header = new MessageHeader(null);
        header.setSubject( "4" );
        header.setThreadingMessageID( "4" );
        te.addMessage( header );
        te.sort( ComparatorStrategy.getStrategy().getComparator( Ordering.NATURAL, Criterion.SUBJECT ) );
          if (DEBUG) printMessageSubjects( te );
        //----------------------------------------------------------------------
        header = new MessageHeader(null);
        header.setSubject( "5" );
        header.setThreadingMessageID( "5" );
        te.addMessage( header );
        te.sort( ComparatorStrategy.getStrategy().getComparator( Ordering.NATURAL, Criterion.SUBJECT ) );
          if (DEBUG) printMessageSubjects( te );
        //----------------------------------------------------------------------
        header = new MessageHeader(null, header);
        header.setSubject( "6" );
        header.setThreadingMessageID( "6" );
        te.addMessage( header );
        te.sort( ComparatorStrategy.getStrategy().getComparator( Ordering.NATURAL, Criterion.SUBJECT ) );
          if (DEBUG) printMessageSubjects( te );
        //----------------------------------------------------------------------

          if (DEBUG) System.out.println("removing 5. message");
        te.removeMessageAt( 4 ); // originally 5. message
        te.sort( ComparatorStrategy.getStrategy().getComparator( Ordering.NATURAL, Criterion.SUBJECT ) );
        check(te, new String[] {"1", "2", "3", "4", "6"});
          if (DEBUG) printMessageSubjects( te );
          if (DEBUG) System.out.println("removing 1. message");
        te.removeMessageAt( 0 ); // originally 1. message
        te.sort( ComparatorStrategy.getStrategy().getComparator( Ordering.NATURAL, Criterion.SUBJECT ) );
        check(te, new String[] {"2", "3", "4", "6"});
          if (DEBUG) printMessageSubjects( te );
          if (DEBUG) System.out.println("removing 3. message");
        te.removeMessageAt( 1 ); // originally 3. message
        te.sort( ComparatorStrategy.getStrategy().getComparator( Ordering.NATURAL, Criterion.SUBJECT ) );
        check(te, new String[] {"2", "4", "6"});
          if (DEBUG) printMessageSubjects( te );
    }

    public void testBackwardIterating() {
        //#ifdef MUJMAIL_TEST_BACKWARD_ITERATING
//#         Settings.threading = true;
//#
//#           /*
//#            * --root
//#            *   |--1
//#            *   |  |--2
//#            *   |  \--3
//#            *   |     |--4
//#            *   |     \--5
//#            *   \--6
//#            * --root2
//#            * --root3
//#            */
//#         ThreadedEmails te = new ThreadedEmails();
//#
//#         MessageHeader root = new MessageHeader(null);
//#         root.setSubject( "root" );
//#         root.setThreadingMessageID( "root" );
//#         te.addRoot( root );
//#
//#         MessageHeader root2 = new MessageHeader(null);
//#         root2.setSubject( "root2" );
//#         root2.setThreadingMessageID( "root2" );
//#         te.addRoot( root2 );
//#
//#         MessageHeader root3 = new MessageHeader(null);
//#         root3.setSubject( "root3" );
//#         root3.setThreadingMessageID( "root3" );
//#         te.addRoot( root3 );
//#
//#         MessageHeader m1 = new MessageHeader(null);
//#         m1.setSubject( "1" );
//#         m1.setThreadingMessageID( "1" );
//#         m1.setParentID( "root" );
//#         Vector parents = new Vector();
//#         parents.addElement( "root" );
//#         m1.setParentIDs( parents );
//#         te.addMessage( root, m1 );
//#
//#         MessageHeader m2 = new MessageHeader(null);
//#         m2.setSubject( "2" );
//#         m2.setThreadingMessageID( "2" );
//#         m2.setParentID( "1" );
//#         parents = new Vector();
//#         parents.addElement( "root" );
//#         parents.addElement( "m1" );
//#         m2.setParentIDs( parents );
//#         te.addMessage( m1, m2 );
//#
//#         MessageHeader m3 = new MessageHeader(null);
//#         m3.setSubject( "3" );
//#         m3.setThreadingMessageID( "3" );
//#         m3.setParentID( "1" );
//#         parents = new Vector();
//#         parents.addElement( "root" );
//#         parents.addElement( "m1" );
//#         m3.setParentIDs( parents );
//#         te.addMessage( m1, m3 );
//#
//#         MessageHeader m4 = new MessageHeader(null);
//#         m4.setSubject( "4" );
//#         m4.setThreadingMessageID( "4" );
//#         m4.setParentID( "3" );
//#         parents = new Vector();
//#         parents.addElement( "root" );
//#         parents.addElement( "m1" );
//#         parents.addElement( "m3" );
//#         m4.setParentIDs( parents );
//#         te.addMessage( m3, m4 );
//#
//#         MessageHeader m5 = new MessageHeader(null);
//#         m5.setSubject( "5" );
//#         m5.setThreadingMessageID( "5" );
//#         m5.setParentID( "3" );
//#         parents = new Vector();
//#         parents.addElement( "root" );
//#         parents.addElement( "m1" );
//#         parents.addElement( "m3" );
//#         m5.setParentIDs( parents );
//#         te.addMessage( m3, m5 );
//#
//#         MessageHeader m6 = new MessageHeader(null);
//#         m6.setSubject( "6" );
//#         m6.setThreadingMessageID( "6" );
//#         m6.setParentID( "root" );
//#         parents = new Vector();
//#         parents.addElement( "root" );
//#         m6.setParentIDs( parents );
//#         te.addMessage( root, m6 );
//#
//#           // sorting is not needed, but for sure
//#         te.sort( ComparatorStrategy.getStrategy().getComparator( Ordering.NATURAL, Criterion.THREADING_MESSAGE_ID ) );
//#
//#         try {
//#             final Enumerator fe = te.new Enumerator();
//#             int i = 0;
//#             MessageHeader messageHeader;
//#             String subject;
//#             while ( fe.hasNextElement() ) {
//#                 messageHeader = (MessageHeader)fe.nextElement();
//#                 subject = messageHeader.getSubject();
//#                 switch (i) {
//#                 case 0: assertEquals("root", subject); break;
//#                 case 1: assertEquals("1", subject); break;
//#                 case 2: assertEquals("2", subject); break;
//#                 case 3: assertEquals("3", subject); break;
//#                 case 4: assertEquals("4", subject); break;
//#                 case 5: assertEquals("5", subject); break;
//#                 case 6: assertEquals("6", subject); break;
//#                 case 7: assertEquals("root2", subject); break;
//#                 case 8: assertEquals("root3", subject); break;
//#                 default: fail( "unexpected message (i=" + i + ", subject='" + subject + "')" );
//#                 }
//#                 ++i;
//#             }
//#
//#             --i;
//#             while ( fe.hasPreviousElement() ) {
//#                 messageHeader = (MessageHeader)fe.previousElement();
//#                 subject = messageHeader.getSubject();
//#                 --i;
//#                 switch (i) {
//#                 case 7: assertEquals("root2", subject); break;
//#                 case 6: assertEquals("6", subject); break;
//#                 case 5: assertEquals("5", subject); break;
//#                 case 4: assertEquals("4", subject); break;
//#                 case 3: assertEquals("3", subject); break;
//#                 case 2: assertEquals("2", subject); break;
//#                 case 1: assertEquals("1", subject); break;
//#                 case 0: assertEquals("root", subject); break;
//#                 default: fail( "unexpected message (i=" + i + ", subject='" + subject + "')" );
//#                 }
//#             }
//#         } catch (Throwable t) {
//#             t.printStackTrace();
//#         }
        //#else
       System.err.println( "This test requires that sources are built with preprocessing option\n" +
             "MUJMAIL_TEST_BACKWARD_ITERATING enabled." );
       fail();
        //#endif
    }

    //#ifdef MUJMAIL_TEST_GET_MESSAGE_AT
//#     private void check(int simpleNext, int simplePrevious, int nextFromZero ) {
//#         assertTrue( "simpleNext=" + simpleNext + ", simplePrevious=" + simplePrevious + ", nextFromZero=" + nextFromZero,
//#             ThreadedEmails.simpleNext == simpleNext
//#             && ThreadedEmails.simplePrevious == simplePrevious
//#             && ThreadedEmails.nextFromZero == nextFromZero
//#         );
//#     }
    //#endif

    public void testGetMessageAtFunction() {
        //#ifdef MUJMAIL_TEST_GET_MESSAGE_AT
//#         Settings.threading = true;
//#
//#         ThreadedEmails te = new ThreadedEmails();
//#
//#         MessageHeader root = new MessageHeader(null);
//#         root.setSubject( "root" );
//#         root.setThreadingMessageID( "root" );
//#         te.addRoot( root );
//#
//#         MessageHeader root2 = new MessageHeader(null);
//#         root2.setSubject( "root2" );
//#         root2.setThreadingMessageID( "root2" );
//#         te.addRoot( root2 );
//#
//#         MessageHeader root3 = new MessageHeader(null);
//#         root3.setSubject( "root3" );
//#         root3.setThreadingMessageID( "root3" );
//#         te.addRoot( root3 );
//#
//#         MessageHeader m1 = new MessageHeader(null);
//#         m1.setSubject( "1" );
//#         m1.setThreadingMessageID( "1" );
//#         m1.setParentID( "root" );
//#         Vector parents = new Vector();
//#         parents.addElement( "root" );
//#         m1.setParentIDs( parents );
//#         te.addMessage( root, m1 );
//#
//#         MessageHeader m2 = new MessageHeader(null);
//#         m2.setSubject( "2" );
//#         m2.setThreadingMessageID( "2" );
//#         m2.setParentID( "1" );
//#         parents = new Vector();
//#         parents.addElement( "root" );
//#         parents.addElement( "m1" );
//#         m2.setParentIDs( parents );
//#         te.addMessage( m1, m2 );
//#
//#         MessageHeader m3 = new MessageHeader(null);
//#         m3.setSubject( "3" );
//#         m3.setThreadingMessageID( "3" );
//#         m3.setParentID( "1" );
//#         parents = new Vector();
//#         parents.addElement( "root" );
//#         parents.addElement( "m1" );
//#         m3.setParentIDs( parents );
//#         te.addMessage( m1, m3 );
//#
//#         MessageHeader m4 = new MessageHeader(null);
//#         m4.setSubject( "4" );
//#         m4.setThreadingMessageID( "4" );
//#         m4.setParentID( "3" );
//#         parents = new Vector();
//#         parents.addElement( "root" );
//#         parents.addElement( "m1" );
//#         parents.addElement( "m3" );
//#         m4.setParentIDs( parents );
//#         te.addMessage( m3, m4 );
//#
//#         MessageHeader m5 = new MessageHeader(null);
//#         m5.setSubject( "5" );
//#         m5.setThreadingMessageID( "5" );
//#         m5.setParentID( "3" );
//#         parents = new Vector();
//#         parents.addElement( "root" );
//#         parents.addElement( "m1" );
//#         parents.addElement( "m3" );
//#         m5.setParentIDs( parents );
//#         te.addMessage( m3, m5 );
//#
//#         MessageHeader m6 = new MessageHeader(null);
//#         m6.setSubject( "6" );
//#         m6.setThreadingMessageID( "6" );
//#         m6.setParentID( "root" );
//#         parents = new Vector();
//#         parents.addElement( "root" );
//#         m6.setParentIDs( parents );
//#         te.addMessage( root, m6 );
//#
//#           // sorting is not needed, but for sure
//#         te.sort( ComparatorStrategy.getStrategy().getComparator( Ordering.NATURAL, Criterion.THREADING_MESSAGE_ID ) );
//#
//#         MessageHeader messageHeader;
//#         messageHeader = te.getMessageAt( 0 );
//#         assertEquals( root, messageHeader);
//#         check(1, 0, 0);
//#
//#         messageHeader = te.getMessageAt( 1 );
//#         assertEquals( m1, messageHeader);
//#         check(2, 0, 0);
//#
//#         messageHeader = te.getMessageAt( 2 );
//#         assertEquals( m2, messageHeader);
//#         check(3, 0, 0);
//#
//#         messageHeader = te.getMessageAt( 3 );
//#         assertEquals( m3, messageHeader);
//#         check(4, 0, 0);
//#
//#         messageHeader = te.getMessageAt( 4 );
//#         assertEquals( m4, messageHeader);
//#         check(5, 0, 0);
//#
//#         messageHeader = te.getMessageAt( 5 );
//#         assertEquals( m5, messageHeader);
//#         check(6, 0, 0);
//#
//#         messageHeader = te.getMessageAt( 6 );
//#         assertEquals( m6, messageHeader);
//#         check(7, 0, 0);
//#
//#         messageHeader = te.getMessageAt( 7 );
//#         assertEquals( root2, messageHeader);
//#         check(8, 0, 0);
//#
//#         messageHeader = te.getMessageAt( 8 );
//#         assertEquals( root3, messageHeader);
//#         check(9, 0, 0);
//#
//#         messageHeader = te.getMessageAt( 7 );
//#         assertEquals( root2, messageHeader);
//#         check(9, 1, 0);
//#
//#         messageHeader = te.getMessageAt( 6 );
//#         assertEquals( m6, messageHeader);
//#         check(9, 2, 0);
//#
//#         messageHeader = te.getMessageAt( 5 );
//#         assertEquals( m5, messageHeader);
//#         check(9, 3, 0);
//#
//#         messageHeader = te.getMessageAt( 4 );
//#         assertEquals( m4, messageHeader);
//#         check(9, 4, 0);
//#
//#         messageHeader = te.getMessageAt( 3 );
//#         assertEquals( m3, messageHeader);
//#         check(9, 5, 0);
//#
//#         messageHeader = te.getMessageAt( 2 );
//#         assertEquals( m2, messageHeader);
//#         check(9, 6, 0);
//#
//#         messageHeader = te.getMessageAt( 1 );
//#         assertEquals( m1, messageHeader);
//#         check(9, 7, 0);
//#
//#         messageHeader = te.getMessageAt( 0 );
//#         assertEquals( root, messageHeader);
//#         check(9, 7, 1);
//#
//#         messageHeader = te.getMessageAt( 2 );
//#         assertEquals( m2, messageHeader);
//#         check(10, 7, 1);
//#
//#         messageHeader = te.getMessageAt( 4 );
//#         assertEquals( m4, messageHeader);
//#         check(11, 7, 1);
//#
//#         messageHeader = te.getMessageAt( 6 );
//#         assertEquals( m6, messageHeader);
//#         check(12, 7, 1);
//#
//#         messageHeader = te.getMessageAt( 3 );
//#         assertEquals( m3, messageHeader);
//#         check(12, 8, 1);
//#
//#         messageHeader = te.getMessageAt( 0 );
//#         assertEquals( root, messageHeader);
//#         check(12, 8, 2);
        //#else
       System.err.println( "This test requires that sources are built with preprocessing option\n" +
             "MUJMAIL_TEST_GET_MESSAGE_AT enabled." );
       fail();
        //#endif
    }


    public Test suite() {
        TestSuite suite = new TestSuite();
        String finishedTestName = "none";
        try {
            suite.addTest( new ThreadedEmailTest("testEnumeration", new TestMethod() {
                public void run(TestCase testCase) throws Throwable {
                    ((ThreadedEmailTest)testCase).testEnumeration();
                }
            }) );

            finishedTestName = "testEnumeration";

            suite.addTest( new ThreadedEmailTest("testEnumerationFail", new TestMethod() {
                public void run(TestCase testCase) throws Throwable {
                    ((ThreadedEmailTest)testCase).testEnumerationFail();
                }
            }) );
    
            finishedTestName = "testEnumerationFail";

            suite.addTest( new ThreadedEmailTest("testRemove", new TestMethod() {
                public void run(TestCase testCase) throws Throwable {
                    ((ThreadedEmailTest)testCase).testRemove();
                }
            }) );

            finishedTestName = "testRemove";

          suite.addTest( new ThreadedEmailTest("testBackwardIterating", new TestMethod() {
              public void run(TestCase testCase) throws Throwable {
                  ((ThreadedEmailTest)testCase).testBackwardIterating();
              }
          }) );

          finishedTestName = "testBackwardIterating";

          suite.addTest( new ThreadedEmailTest("testGetMessageAtFunction", new TestMethod() {
              public void run(TestCase testCase) throws Throwable {
                  ((ThreadedEmailTest)testCase).testGetMessageAtFunction();
              }
          }) );

          finishedTestName = "testGetMessageAtFunction";
        } catch (Throwable t) {
            System.err.println("Last finished test '" + finishedTestName + "'");
            t.printStackTrace();
        }
        return suite;
    }
}
