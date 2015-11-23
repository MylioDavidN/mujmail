package test.mujmail.threading;

import j2meunit.framework.Test;
import j2meunit.framework.TestCase;
import j2meunit.framework.TestMethod;
import j2meunit.framework.TestSuite;

import java.util.Enumeration;
import java.util.Vector;

import mujmail.MessageHeader;
import mujmail.Settings;
import mujmail.ordering.ComparatorStrategy;
import mujmail.ordering.Criterion;
import mujmail.ordering.Ordering;
import mujmail.threading.Algorithm;
import mujmail.threading.ThreadedEmails;

public class ThreadingTest extends TestCase {

    private static final boolean DEBUG = false;

    public ThreadingTest() {
        super();
    }

    public ThreadingTest(String name, TestMethod testMethod) {
        super(name, testMethod);
    }



    public void testBasicFunctionality() {
        new BasicFunctionalityData("testBasicFunctionality").test();
    }

    public void testEmptyContainers() {
        new EmptyContainerData("testEmptyContainers").test();
    }

    public void testEmptyContainersWithC() {
        new EmptyContainerDataWithC("testEmptyContainersWithC").test();
    }

    public void testEmptyContainersDifferent() {
        new EmptyContainerDataDifferent("testEmptyContainersDifferent").test();
    }

    public void testIncompleteParentPath() {
        new IncompletePathData("testIncompleteParentPath").test();
    }

    public void testIncompleteParentPathSwitched() {
        new IncompletePathDataSwitched("testIncompleteParentPathSwitched").test();
    }

    public void testTester1mujmailorg() {
        new Tester1mujmailorgData("testTester1mujmailorg").test();
    }

    public void testTester1mujmailorg090308() {
        new Tester1mujmailOrgData090308("testTester1mujmailorg090308").test();
    }

    public void testDataFromDavidsAccount090308() {
    	new DavidsData( "testDataFromDavidsAccount090308" ).test();
    }

    public void testOneMail() {
    	new OneMailWithoutParent( "testOneMail" ).test();
    }

    public void testRemovingEmailProblem() {
    	new RemovingEmailsProblemData( "testRemovingEmailProblem" ).test();
    }

    public void testNewThreadingRealData() {
        new NewThreadingRealData( "testNewThreadingRealData" ).test();
    }


    public Test suite() {
    	Settings.threading = true;

        TestSuite suite = new TestSuite();

        suite.addTest( new ThreadingTest("testBasicFunctionality", new TestMethod() {
            public void run(TestCase testCase) throws Throwable {
                ((ThreadingTest)testCase).testBasicFunctionality();
            }
        }) );

        suite.addTest( new ThreadingTest("testEmptyContainers", new TestMethod() {
            public void run(TestCase testCase) throws Throwable {
                ((ThreadingTest)testCase).testEmptyContainers();
            }
        }) );

        suite.addTest( new ThreadingTest("testEmptyContainersWithC", new TestMethod() {
            public void run(TestCase testCase) throws Throwable {
                ((ThreadingTest)testCase).testEmptyContainersWithC();
            }
        }) );

        suite.addTest( new ThreadingTest("testEmptyContainersDifferent", new TestMethod() {
            public void run(TestCase testCase) throws Throwable {
                ((ThreadingTest)testCase).testEmptyContainersDifferent();
            }
        }) );

        suite.addTest( new ThreadingTest("testIncompleteParentPath", new TestMethod() {
            public void run(TestCase testCase) throws Throwable {
                ((ThreadingTest)testCase).testIncompleteParentPath();
            }
        }) );

        suite.addTest( new ThreadingTest("testIncompleteParentPathSwitched", new TestMethod() {
            public void run(TestCase testCase) throws Throwable {
                ((ThreadingTest)testCase).testIncompleteParentPathSwitched();
            }
        }) );

        suite.addTest( new ThreadingTest("testTester1mujmail.org", new TestMethod() {
            public void run(TestCase testCase) throws Throwable {
                ((ThreadingTest)testCase).testTester1mujmailorg();
            }
        }) );

        suite.addTest( new ThreadingTest("testTester1mujmailorg090308", new TestMethod() {
            public void run(TestCase testCase) throws Throwable {
                ((ThreadingTest)testCase).testTester1mujmailorg090308();
            }
        }) );

        suite.addTest( new ThreadingTest("testDataFromDavidsAccount090308", new TestMethod() {
            public void run(TestCase testCase) throws Throwable {
                ((ThreadingTest)testCase).testDataFromDavidsAccount090308();
            }
        }) );

        suite.addTest( new ThreadingTest("testOneMail", new TestMethod() {
            public void run(TestCase testCase) throws Throwable {
                ((ThreadingTest)testCase).testOneMail();
            }
        }) );

        suite.addTest( new ThreadingTest("testRemovingEmailProblem", new TestMethod() {
            public void run(TestCase testCase) throws Throwable {
                ((ThreadingTest)testCase).testRemovingEmailProblem();
            }
        }) );

        suite.addTest( new ThreadingTest("testNewThreadingRealData", new TestMethod() {
            public void run(TestCase testCase) throws Throwable {
                ((ThreadingTest)testCase).testNewThreadingRealData();
            }
        }) );

        return suite;
    }

    abstract class AData {
    	/**
    	 * Vector of message headers.
    	 */
        protected Vector messages;

        /** Test name. It's printed to console when test starts. */
        private String testName;

        /**
         * Constructor that sets test name.
         * 
         * @see #testName
         */
        public AData( final String testName ) {
            messages = new Vector();
            this.testName = testName;
        }

        /**
         * Getter for vector of messages.
         * 
         * @return vector of messages
         */
        public Vector getMessages() {
            return messages;
        }

        /**
         * Method invokes threading algorithm on prepared data and at the end
         * checks if the result is the expected result.
         */
        public void test() {
            try {
            	System.out.println( "=== " + testName + " ===");
                ThreadedEmails threadedEmails = Algorithm.getAlgorithm().invoke( this.getMessages() );
                threadedEmails.sort( ComparatorStrategy.getStrategy().getComparator( Ordering.NATURAL, Criterion.THREADING_MESSAGE_ID ) );
                  //#ifdef MUJMAIL_DEVELOPMENT
//#                   if (DEBUG) threadedEmails.printToConsole();
                  //#endif
                if ( ! this.checkResult( threadedEmails ) ) {
                	fail();
                }
            } catch (Throwable t) {
                t.printStackTrace();
                assertTrue(false);
            }
        }

        /**
         * Array of expected threaded message IDs.
         */
        protected String[] threadedMessageIDs;
        /**
         * Array of flags whether the expected message have to be root message.
         */
        protected boolean[] isRoot;
        /**
         * Method prepares expected data for test.
         * 
         * @see #threadedMessageIDs
         * @see #isRoot
         */
        abstract protected void prepareDataForCheck();

        /**
         * This method makes two checks for e-mails:
         * <ul>
         * <li>checks e-mail threading message ID against </li>
         * <li>checks whether the e-mail have to be root message or not</li>
         * </ul>
         * @param threadedEmails structure to be checked
         * @return return if the result is correct (<code>true</code>) or not
         * @see #prepareDataForCheck()
         */
        private boolean checkResult( ThreadedEmails threadedEmails ) {
        	  // check if some e-mails are missing
        	if ( messages.size() != threadedEmails.getSize() - threadedEmails.getEmptyRootsNumber() ) {
        		System.err.println( "Number of messages in result is not correct - expected: " + messages.size() + ", but got " + (threadedEmails.getSize() - threadedEmails.getEmptyRootsNumber()) );
        		return false;
        	}
        	prepareDataForCheck();

        	final Enumeration enum1 = threadedEmails.getEnumeration();

        	String threadedMessageID;
        	int i = 0;
        	MessageHeader header;
            while ( enum1.hasMoreElements() ) {
            	header = (MessageHeader)enum1.nextElement();
            	if ( header == null ) {
            		fail( "Header is null" );
            	}
            	if ( i >= threadedMessageIDs.length) {
            		System.err.println( "Too many messages" );
            		return false;
            	}
            	threadedMessageID = threadedMessageIDs[i];
            	if ( ! threadedMessageID.equals( header.getThreadingMessageID() ) ) {
    				System.err.println( "Message i: " + i + " have to have threaded message ID: '" + threadedMessageID + "', is '" + header.getThreadingMessageID() + "'" );
    				return false;
    			}
    			if ( isRoot[i] && ! threadedEmails.isRootMessage( header ) ) {
    				System.err.println( "Message i: " + i + " have to be root message (threaded message ID: '" + threadedMessageID + "')" );
    				return false;
    			}

            	++i;
            }

            Enumeration e = threadedEmails.getEnumeration();
            i = 0;
            while (e.hasMoreElements()) {
                MessageHeader mh = (MessageHeader)e.nextElement();
                System.out.println( i++ + ". ID: " + mh.getThreadingMessageID() );
            }

        	return true;
        }
    }
    /**
     * Class contains data for test and result should be:
     * <pre>
     * --A
     *   |--AA
     *   |  \--AAA
     *   \--AB
     * --B
     * </pre>
     * When converted to 2 levels:
     * <pre>
     * --A
     *   |--AA
     *   |--AAA
     *   \--AB
     * --B
     * </pre>
     * 
     * @author Betlista
     */
    private class BasicFunctionalityData extends AData {

        public BasicFunctionalityData( final String testName ) {
        	super(testName);
            Vector firstLevelParentIDs = new Vector();
            firstLevelParentIDs.addElement( "A" );

            Vector secondLevelParentIDs = new Vector();
            secondLevelParentIDs.addElement( "A" );
            secondLevelParentIDs.addElement( "AA" );

            MessageHeader messageA = new MessageHeader(null);
            messageA.setSubject( "test" );
            messageA.setThreadingMessageID( "A" );

            MessageHeader messageAA = new MessageHeader(null);
            messageAA.setSubject( "Re: test" );
            messageAA.setThreadingMessageID( "AA" );
            messageAA.setParentID("A");
            messageAA.setParentIDs( firstLevelParentIDs );

            MessageHeader messageAAA = new MessageHeader(null);
            messageAAA.setSubject( "RE: Re: test" );
            messageAAA.setThreadingMessageID( "AAA" );
            messageAAA.setParentID("AA");
            messageAAA.setParentIDs( secondLevelParentIDs );

            MessageHeader messageAB = new MessageHeader(null);
            messageAB.setSubject( "RE: test" );
            messageAB.setThreadingMessageID( "AB" );
            messageAB.setParentIDs( firstLevelParentIDs );

            MessageHeader messageB = new MessageHeader(null);
            messageB.setSubject( "subject" );
            messageB.setThreadingMessageID( "B" );

            messages.addElement( messageA );
            messages.addElement( messageAA );
            messages.addElement( messageAAA );
            messages.addElement( messageAB );
            messages.addElement( messageB );
        }

        protected void prepareDataForCheck() {
        	threadedMessageIDs = new String[] {
        		"A",
        		"AA",
        		"AAA",
        		"AB",
        		"B"
        	};
        	isRoot = new boolean[] {
        		true,
        		false,
        		false,
        		false,
        		true
        	};
        }
    }

    /**
     * Class contains data for test and result should be:
     * <pre>
     * --1
     *   \--10
     *      \--100
     *         |--A
     *         \--B
     * </pre>
     * (or something similar - siblings could be switched)
     * When converted to 2 levels:
     * <pre>
     * --100
     *   |--A
     *   \--B
     * </pre>
     * 
     * @author Betlista
     */
    class EmptyContainerData extends AData {

        public EmptyContainerData( final String testName ) {
        	super(testName);
            Vector parentIDs = new Vector();
            parentIDs.addElement( "1" );
            parentIDs.addElement( "10" );
            parentIDs.addElement( "100" );

              // create A message
            MessageHeader messageA = new MessageHeader(null);
            messageA.setSubject( "A" );
            messageA.setThreadingMessageID( "A" );
            messageA.setParentID( "100" );
            messageA.setParentIDs( parentIDs );
            messages.addElement( messageA );

              // create message B
            MessageHeader messageB = new MessageHeader(null);
            messageB.setSubject( "B" );
            messageB.setThreadingMessageID( "B" );
            messageB.setParentID( "100" );
            messageB.setParentIDs( parentIDs );
            messages.addElement( messageB );
        }

        protected void prepareDataForCheck() {
        	threadedMessageIDs = new String[] {
        		"100",
        		"A",
        		"B"
        	};
        	isRoot = new boolean[] {
        		true,
        		false,
        		false
        	};
        }
    }

    /**
     * Class contains data for test and result should be:
     * <pre>
     * --1
     *   \--10
     *      |--100
     *      |  |--A
     *      |  \--B
     *      \--C
     * </pre>
     * (or something similar - siblings could be switched)
     * When converted to 2 levels:
     * <pre>
     *   --10
     *     |--A
     *     |--B
     *     \--C
     * </pre>
     * 
     * @author Betlista
     */
    class EmptyContainerDataWithC extends AData {
        public EmptyContainerDataWithC( final String testName ) {
        	super(testName);
            Vector longParentList = new Vector();
            longParentList.addElement( "1" );
            longParentList.addElement( "10" );
            longParentList.addElement( "100" );

            Vector shortParentList = new Vector();
            shortParentList.addElement( "1" );
            shortParentList.addElement( "10" );

              // create A message
            MessageHeader messageA = new MessageHeader(null);
            messageA.setSubject( "A" );
            messageA.setThreadingMessageID( "A" );
            messageA.setParentID( "100" );
            messageA.setParentIDs( longParentList );
            messages.addElement( messageA );

              // create message B
            MessageHeader messageB = new MessageHeader(null);
            messageB.setSubject( "B" );
            messageB.setThreadingMessageID( "B" );
            messageB.setParentID( "100" );
            messageB.setParentIDs( longParentList );
            messages.addElement( messageB );

              // create message C
            MessageHeader messageC = new MessageHeader(null);
            messageC.setSubject( "C" );
            messageC.setThreadingMessageID( "C" );
            messageC.setParentID( "10" );
            messageC.setParentIDs( shortParentList );
            messages.addElement( messageC );
        }
        protected void prepareDataForCheck() {
        	threadedMessageIDs = new String[] {
    			"10",
        		"A",
        		"B",
        		"C"
        	};
        	isRoot = new boolean[] {
        		true,
        		false,
        		false,
        		false
        	};
        }
    }

    /**
     * Class contains data for test and result should be:
     * <pre>
     * --1
     *   |--10
     *   |  \--A
     *   \--11
     *      \--B
     * </pre>
     * (or something similar - siblings could be switched)
     * When converted to 2 levels:
     * <pre>
     * --1
     *   |--A
     *   \--B
     * </pre>
     * 
     * @author Betlista
     */
    class EmptyContainerDataDifferent extends AData {
        public EmptyContainerDataDifferent( final String testName ) {
        	super(testName);
            Vector parentIDsForA = new Vector();
            parentIDsForA.addElement( "1" );
            parentIDsForA.addElement( "10" );

            Vector parentIDsForB = new Vector();
            parentIDsForB.addElement( "1" );
            parentIDsForB.addElement( "11" );

              // create A message
            MessageHeader messageA = new MessageHeader(null);
            messageA.setSubject( "A" );
            messageA.setThreadingMessageID( "A" );
            messageA.setParentID( "10" );
            messageA.setParentIDs( parentIDsForA );
            messages.addElement( messageA );

              // create message B
            MessageHeader messageB = new MessageHeader(null);
            messageB.setSubject( "B" );
            messageB.setThreadingMessageID( "B" );
            messageB.setParentID( "11" );
            messageB.setParentIDs( parentIDsForB );
            messages.addElement( messageB );
        }
        protected void prepareDataForCheck() {
        	threadedMessageIDs = new String[] {
    			"1",
        		"A",
        		"B"
        	};
        	isRoot = new boolean[] {
        		true,
        		false,
        		false
        	};
        }
    }

    /**
     * Class contains data for test and result should be:
     * <pre>
     * --1
     *   \--10
     *      \--100
     *         \--1000
     *            \--10000
     *               |--A
     *               \--B
     * </pre>
     * (or something similar - siblings could be switched)
     * When converted to 2 levels:
     * <pre>
     * --10000
     *   |--A
     *   \--B
     * </pre>
     * 
     * @author Betlista
     */
    class IncompletePathData extends AData {
        public IncompletePathData( final String testName ) {
        	super(testName);
            Vector completePath = new Vector();
            completePath.addElement("1");
            completePath.addElement("10");
            completePath.addElement("100");
            completePath.addElement("1000");
            completePath.addElement("10000");

            Vector incompletePath = new Vector();
            incompletePath.addElement("10");
            incompletePath.addElement("10000");

            MessageHeader messageA = new MessageHeader(null);
            messageA.setSubject( "A" );
            messageA.setThreadingMessageID( "A" );
            messageA.setParentID( "10000" );
            messageA.setParentIDs( completePath );
            MessageHeader messageB = new MessageHeader(null);
            messageB.setSubject( "B" );
            messageB.setThreadingMessageID( "B" );
            messageB.setParentID( "10000" );
            messageB.setParentIDs( incompletePath );

            messages.addElement( messageA );
            messages.addElement( messageB );
        }
        protected void prepareDataForCheck() {
        	threadedMessageIDs = new String[] {
    			"10000",
        		"A",
        		"B"
        	};
        	isRoot = new boolean[] {
        		true,
        		false,
        		false
        	};
        }
    }

    /**
     * Class contains data for test and result should be:
     * <pre>
     * --1
     *   \--10
     *      \--100
     *         \--1000
     *            \--10000
     *               |--A
     *               \--B
     * </pre>
     * When converted to 2 levels:
     * <pre>
     * --10000
     *   |--A
     *   \--B
     * </pre>
     * 
     * @author Betlista
     */
    class IncompletePathDataSwitched extends AData {
        public IncompletePathDataSwitched( final String testName ) {
        	super(testName);
            Vector completePath = new Vector();
            completePath.addElement("1");
            completePath.addElement("10");
            completePath.addElement("100");
            completePath.addElement("1000");
            completePath.addElement("10000");

            Vector incompletePath = new Vector();
            incompletePath.addElement("10");
            incompletePath.addElement("10000");

            MessageHeader messageA = new MessageHeader(null);
            messageA.setSubject( "A" );
            messageA.setThreadingMessageID( "A" );
            messageA.setParentID( "10000" );
            messageA.setParentIDs( incompletePath );
            MessageHeader messageB = new MessageHeader(null);
            messageB.setSubject( "B" );
            messageB.setThreadingMessageID( "B" );
            messageB.setParentID( "10000" );
            messageB.setParentIDs( completePath );

            messages.addElement( messageA );
            messages.addElement( messageB );
        }
        protected void prepareDataForCheck() {
        	threadedMessageIDs = new String[] {
    			"10", // this is not the same result as in previous test, but
    			      //   it is 0K when we realize thet both messages
    			      //   (10 and 10000) are empty ones
        		"A",
        		"B"
        	};
        	isRoot = new boolean[] {
        		true,
        		false,
        		false
        	};
        }
    }

    /**
     * Class contains real data from tester1@mujmail.org account actual
     * to 13.1.2009.
     * 
     * Result should be:
     * <pre>
     * --3b94c0010810041056i19959738s2170d35b7398df1b@mail.gmail.com
     *   |--189acd030810041058u338d48damac81a5f4b58df277@mail.gmail.com
     *   |  \--2a17c6820810041100r13f30f7cya12598f8c932c662@mail.gmail.com
     *   |--2a17c6820810041057i2552fc5u14d48d306e627199@mail.gmail.com
     *   \--71dd3840810041059o3c8fd64p27c0fdd13a43670d@mail.gmail.com
     * --48E6327C.1090609@mujmail.org
     * --4941BDA5.6060504@mujmail.org
     * --8.10-2192-1018511217-1229047423@seznam.cz
     * </pre>
     * When converted to 2 levels:
     * <pre>
     * --3b94c0010810041056i19959738s2170d35b7398df1b@mail.gmail.com
     *   |--189acd030810041058u338d48damac81a5f4b58df277@mail.gmail.com
     *   |--2a17c6820810041057i2552fc5u14d48d306e627199@mail.gmail.com
     *   |--2a17c6820810041100r13f30f7cya12598f8c932c662@mail.gmail.com
     *   \--71dd3840810041059o3c8fd64p27c0fdd13a43670d@mail.gmail.com
     * --48E6327C.1090609@mujmail.org
     * --4941BDA5.6060504@mujmail.org
     * --8.10-2192-1018511217-1229047423@seznam.cz
     * </pre> 
     * 
     * @author Betlista
     */
    class Tester1mujmailorgData extends AData {
        public Tester1mujmailorgData( final String testName ) {
        	super(testName);
            MessageHeader messageHeader;
            Vector parentIDs;

            messageHeader = new MessageHeader(null);
            messageHeader.setMessageID( "GmailId11cc3358968ff63d" );
            messageHeader.setThreadingMessageID( "48E6327C.1090609@mujmail.org" );
            messageHeader.setParentID( "" );
            messageHeader.setSubject( "simple thunderbird test" );
            // parentIDs=[]
            parentIDs = new Vector();
            messageHeader.setParentIDs( parentIDs );
            messages.addElement(messageHeader);

            messageHeader = new MessageHeader(null);
            messageHeader.setMessageID( "GmailId11cc901287529933" );
            messageHeader.setThreadingMessageID( "3b94c0010810041056i19959738s2170d35b7398df1b@mail.gmail.com" );
            messageHeader.setParentID( "" );
            messageHeader.setSubject( "thread1" );
            // parentIDs=[]
            parentIDs = new Vector();
            messageHeader.setParentIDs( parentIDs );
            messages.addElement(messageHeader);

            messageHeader = new MessageHeader(null);
            messageHeader.setMessageID( "GmailId11cc9026e3f97cae" );
            messageHeader.setThreadingMessageID( "2a17c6820810041057i2552fc5u14d48d306e627199@mail.gmail.com" );
            messageHeader.setParentID( "3b94c0010810041056i19959738s2170d35b7398df1b@mail.gmail.com" );
            messageHeader.setSubject( "Re: thread1" );
            // parentIDs=[3b94c0010810041056i19959738s2170d35b7398df1b@mail.gmail.com]
            parentIDs = new Vector();
            parentIDs.addElement( "3b94c0010810041056i19959738s2170d35b7398df1b@mail.gmail.com" );
            messageHeader.setParentIDs( parentIDs );
            messages.addElement(messageHeader);

            messageHeader = new MessageHeader(null);
            messageHeader.setMessageID( "GmailId11cc902ebd7851df" );
            messageHeader.setThreadingMessageID( "189acd030810041058u338d48damac81a5f4b58df277@mail.gmail.com" );
            messageHeader.setParentID( "3b94c0010810041056i19959738s2170d35b7398df1b@mail.gmail.com" );
            messageHeader.setSubject( "Re: thread1" );
            // parentIDs=[3b94c0010810041056i19959738s2170d35b7398df1b@mail.gmail.com]
            parentIDs = new Vector();
            parentIDs.addElement( "3b94c0010810041056i19959738s2170d35b7398df1b@mail.gmail.com" );
            messageHeader.setParentIDs( parentIDs );
            messages.addElement(messageHeader);

            messageHeader = new MessageHeader(null);
            messageHeader.setMessageID( "GmailId11cc9040ba23c95b" );
            messageHeader.setThreadingMessageID( "71dd3840810041059o3c8fd64p27c0fdd13a43670d@mail.gmail.com" );
            messageHeader.setParentID( "3b94c0010810041056i19959738s2170d35b7398df1b@mail.gmail.com" );
            messageHeader.setSubject( "Re: thread1" );
            // parentIDs=[3b94c0010810041056i19959738s2170d35b7398df1b@mail.gmail.com]
            parentIDs = new Vector();
            parentIDs.addElement( "3b94c0010810041056i19959738s2170d35b7398df1b@mail.gmail.com" );
            messageHeader.setParentIDs( parentIDs );
            messages.addElement(messageHeader);

            messageHeader = new MessageHeader(null);
            messageHeader.setMessageID( "GmailId11cc9048b6203bc2" );
            messageHeader.setThreadingMessageID( "2a17c6820810041100r13f30f7cya12598f8c932c662@mail.gmail.com" );
            messageHeader.setParentID( "189acd030810041058u338d48damac81a5f4b58df277@mail.gmail.com" );
            messageHeader.setSubject( "Re: thread1" );
            // parentIDs=[189acd030810041058u338d48damac81a5f4b58df277@mail.gmail.com, 3b94c0010810041056i19959738s2170d35b7398df1b@mail.gmail.com]
            parentIDs = new Vector();
            parentIDs.addElement( "189acd030810041058u338d48damac81a5f4b58df277@mail.gmail.com" );
            parentIDs.addElement( "3b94c0010810041056i19959738s2170d35b7398df1b@mail.gmail.com" );
            messageHeader.setParentIDs( parentIDs );
            messages.addElement(messageHeader);

            messageHeader = new MessageHeader(null);
            messageHeader.setMessageID( "GmailId11e28ccdf301b571" );
            messageHeader.setThreadingMessageID( "4941BDA5.6060504@mujmail.org" );
            messageHeader.setParentID( "" );
            messageHeader.setSubject( "simple test" );
            // parentIDs=[]
            parentIDs = new Vector();
            messageHeader.setParentIDs( parentIDs );
            messages.addElement(messageHeader);

            messageHeader = new MessageHeader(null);
            messageHeader.setMessageID( "GmailId11e28ef6436b4e4d" );
            messageHeader.setThreadingMessageID( "8.10-2192-1018511217-1229047423@seznam.cz" );
            messageHeader.setParentID( "4941C65A.10609@mujmail.org" );
            messageHeader.setSubject( "Re:Message ID test (final)" );
            // parentIDs=[4941C65A.10609@mujmail.org]
            parentIDs = new Vector();
            parentIDs.addElement( "4941C65A.10609@mujmail.org" );
            messageHeader.setParentIDs( parentIDs );
            messages.addElement(messageHeader);
        }
        protected void prepareDataForCheck() {
/*
     * --3b94c0010810041056i19959738s2170d35b7398df1b@mail.gmail.com
     *   |--189acd030810041058u338d48damac81a5f4b58df277@mail.gmail.com
     *   |--2a17c6820810041057i2552fc5u14d48d306e627199@mail.gmail.com
     *   |--2a17c6820810041100r13f30f7cya12598f8c932c662@mail.gmail.com
     *   \--71dd3840810041059o3c8fd64p27c0fdd13a43670d@mail.gmail.com
     * --48E6327C.1090609@mujmail.org
     * --4941BDA5.6060504@mujmail.org
     *   \--8.10-2192-1018511217-1229047423@seznam.cz
 */
        	threadedMessageIDs = new String[] {
    			"3b94c0010810041056i19959738s2170d35b7398df1b@mail.gmail.com",
        		"189acd030810041058u338d48damac81a5f4b58df277@mail.gmail.com",
                "2a17c6820810041100r13f30f7cya12598f8c932c662@mail.gmail.com",
        		"2a17c6820810041057i2552fc5u14d48d306e627199@mail.gmail.com",
        		"71dd3840810041059o3c8fd64p27c0fdd13a43670d@mail.gmail.com",
        		
        		"48E6327C.1090609@mujmail.org",
        		"4941BDA5.6060504@mujmail.org",
        		"8.10-2192-1018511217-1229047423@seznam.cz"
        	};
        	isRoot = new boolean[] {
        		true,
        		false,
        		false,
        		false,
        		false,

        		true,
        		true,
        		true
        	};
        }
    }
    
    class Tester1mujmailOrgData090308 extends AData {
    	public Tester1mujmailOrgData090308(final String testName) {
    		super(testName);
			
    		MessageHeader messageHeader;
    		Vector parentIDs;

    		messageHeader = new MessageHeader(null);
    		messageHeader.setMessageID( "GmailId11fb9f56528cfb1b" );
    		messageHeader.setThreadingMessageID( "49a86ff7.03eb300a.4698.ffffefad@mx.google.com" );
    		messageHeader.setParentID( "" );
    		messageHeader.setSubject( "move to trash 2" );
    		// parentIDs=[]
    		parentIDs = new Vector();
    		messageHeader.setParentIDs( parentIDs );
    		messages.addElement(messageHeader);

    		messageHeader = new MessageHeader(null);
    		messageHeader.setMessageID( "GmailId11fb9f7111d598d5" );
    		messageHeader.setThreadingMessageID( "49a87065.03eb300a.4698.fffff080@mx.google.com" );
    		messageHeader.setParentID( "" );
    		messageHeader.setSubject( "preview" );
    		// parentIDs=[]
    		parentIDs = new Vector();
    		messageHeader.setParentIDs( parentIDs );
    		messages.addElement(messageHeader);

    		messageHeader = new MessageHeader(null);
    		messageHeader.setMessageID( "GmailId11fba08cb3a28b0f" );
    		messageHeader.setThreadingMessageID( "49a874ee.1ae7300a.7045.ffffc957@mx.google.com" );
    		messageHeader.setParentID( "" );
    		messageHeader.setSubject( "delete/undelete" );
    		// parentIDs=[]
    		parentIDs = new Vector();
    		messageHeader.setParentIDs( parentIDs );
    		messages.addElement(messageHeader);

    		messageHeader = new MessageHeader(null);
    		messageHeader.setMessageID( "GmailId11fbbe79fe82dc00" );
    		messageHeader.setThreadingMessageID( "49a8ef83.25e2660a.21d1.6983@mx.google.com" );
    		messageHeader.setParentID( "" );
    		messageHeader.setSubject( "send 28/1" );
    		// parentIDs=[]
    		parentIDs = new Vector();
    		messageHeader.setParentIDs( parentIDs );
    		messages.addElement(messageHeader);

    		messageHeader = new MessageHeader(null);
    		messageHeader.setMessageID( "GmailId11fbc80a7358292d" );
    		messageHeader.setThreadingMessageID( "49a916b0.09cc660a.76dc.ffffa206@mx.google.com" );
    		messageHeader.setParentID( "" );
    		messageHeader.setSubject( "wrong" );
    		// parentIDs=[]
    		parentIDs = new Vector();
    		messageHeader.setParentIDs( parentIDs );
    		messages.addElement(messageHeader);

    		messageHeader = new MessageHeader(null);
    		messageHeader.setMessageID( "GmailId11fbc81c999e6437" );
    		messageHeader.setThreadingMessageID( "49a916fb.07a0660a.516c.ffffb98d@mx.google.com" );
    		messageHeader.setParentID( "" );
    		messageHeader.setSubject( "wrong 3" );
    		// parentIDs=[]
    		parentIDs = new Vector();
    		messageHeader.setParentIDs( parentIDs );
    		messages.addElement(messageHeader);

    		messageHeader = new MessageHeader(null);
    		messageHeader.setMessageID( "GmailId11fbd14282e97d2c" );
    		messageHeader.setThreadingMessageID( "49a93c73.0aa5660a.7354.2dd3@mx.google.com" );
    		messageHeader.setParentID( "" );
    		messageHeader.setSubject( "NB send test" );
    		// parentIDs=[]
    		parentIDs = new Vector();
    		messageHeader.setParentIDs( parentIDs );
    		messages.addElement(messageHeader);

    		messageHeader = new MessageHeader(null);
    		messageHeader.setMessageID( "GmailId11fbd142dd3e4d0e" );
    		messageHeader.setThreadingMessageID( "49a93c74.0aa5660a.7354.2dd7@mx.google.com" );
    		messageHeader.setParentID( "" );
    		messageHeader.setSubject( "send rev 600" );
    		// parentIDs=[]
    		parentIDs = new Vector();
    		messageHeader.setParentIDs( parentIDs );
    		messages.addElement(messageHeader);

    		messageHeader = new MessageHeader(null);
    		messageHeader.setMessageID( "GmailId11fc736ea762c646" );
    		messageHeader.setThreadingMessageID( "49abd4b4.0707d00a.4b45.122b@mx.google.com" );
    		messageHeader.setParentID( "" );
    		messageHeader.setSubject( "NB" );
    		// parentIDs=[]
    		parentIDs = new Vector();
    		messageHeader.setParentIDs( parentIDs );
    		messages.addElement(messageHeader);

    		messageHeader = new MessageHeader(null);
    		messageHeader.setMessageID( "GmailId11fd3280636800c6" );
    		messageHeader.setThreadingMessageID( "49AEE32E.20908@mujmail.org" );
    		messageHeader.setParentID( "" );
    		messageHeader.setSubject( "simple HTML TB" );
    		// parentIDs=[]
    		parentIDs = new Vector();
    		messageHeader.setParentIDs( parentIDs );
    		messages.addElement(messageHeader);

    		messageHeader = new MessageHeader(null);
    		messageHeader.setMessageID( "GmailId11fd8414907c9cc4" );
    		messageHeader.setThreadingMessageID( "49B03179.4010301@mujmail.org" );
    		messageHeader.setParentID( "49B03152.2090000@mujmail.org" );
    		messageHeader.setSubject( "Re: root message test (tester1)" );
    		// parentIDs=[49B03152.2090000@mujmail.org]
    		parentIDs = new Vector();
    		parentIDs.addElement( "49B03152.2090000@mujmail.org" );
    		messageHeader.setParentIDs( parentIDs );
    		messages.addElement(messageHeader);

    		messageHeader = new MessageHeader(null);
    		messageHeader.setMessageID( "GmailId11fd843438c95efc" );
    		messageHeader.setThreadingMessageID( "49B031FA.3030403@mujmail.org" );
    		messageHeader.setParentID( "49B03152.2090000@mujmail.org" );
    		messageHeader.setSubject( "Re: root message test" );
    		// parentIDs=[49B03152.2090000@mujmail.org]
    		parentIDs = new Vector();
    		parentIDs.addElement( "49B03152.2090000@mujmail.org" );
    		messageHeader.setParentIDs( parentIDs );
    		messages.addElement(messageHeader);

    		messageHeader = new MessageHeader(null);
    		messageHeader.setMessageID( "GmailId11fde4e8025fe18e" );
    		messageHeader.setThreadingMessageID( "49b1be01.0437560a.3bd7.1a9c@mx.google.com" );
    		messageHeader.setParentID( "" );
    		messageHeader.setSubject( "send 7/1" );
    		// parentIDs=[]
    		parentIDs = new Vector();
    		messageHeader.setParentIDs( parentIDs );
    		messages.addElement(messageHeader);
		}
    	protected void prepareDataForCheck() {
        	threadedMessageIDs = new String[] {
    			"49AEE32E.20908@mujmail.org",

    			"49B03152.2090000@mujmail.org",
    			  "49B03179.4010301@mujmail.org",
    			  "49B031FA.3030403@mujmail.org",

    			"49a86ff7.03eb300a.4698.ffffefad@mx.google.com",
    			"49a87065.03eb300a.4698.fffff080@mx.google.com",
    			"49a874ee.1ae7300a.7045.ffffc957@mx.google.com",
    			"49a8ef83.25e2660a.21d1.6983@mx.google.com",
    			"49a916b0.09cc660a.76dc.ffffa206@mx.google.com",
    			"49a916fb.07a0660a.516c.ffffb98d@mx.google.com",
    			"49a93c73.0aa5660a.7354.2dd3@mx.google.com",
    			"49a93c74.0aa5660a.7354.2dd7@mx.google.com",
    			"49abd4b4.0707d00a.4b45.122b@mx.google.com",
    			"49b1be01.0437560a.3bd7.1a9c@mx.google.com"
        	};
        	isRoot = new boolean[] {
        		true,

        		true,
        		false,
        		false,

        		true,
        		true,
        		true,
        		true,
        		true,
        		true,
        		true,
        		true,
        		true,
        		true,
        	};
    	}
    }

	/**
	 * Result should be:
	 * <pre>
	 * --117839.58882.qm@web65515.mail.ac4.yahoo.com",
	 * --4981c81f.0405560a.112a.ffffa906@mx.google.com",
	 * --49a9d6ee.09c5660a.3144.31dd@mx.google.com",
	 * --49aa8a24.09cc660a.65c9.4125@mx.google.com",
	 * --691953.8731.qm@web65503.mail.ac4.yahoo.com",
	 * --6f4858d80811121247n2eca3852gf083bb0985e30cee@mail.gmail.com",
	 * --6f4858d80811121252v21fd516dh12d61deef6278da7@mail.gmail.com",
	 *   \--6f4858d80901170531j76fef731w5d99aab103673944@mail.gmail.com",
	 * --6f4858d80902101030o5a8beef2ic1f6409be5ff8879@mail.gmail.com",
	 * --729861.54352.qm@web65501.mail.ac4.yahoo.com",
	 * --798064.91440.qm@web65513.mail.ac4.yahoo.com",
	 * --837d98840901100421o69398087i1ce1dd79422113bd@mail.gmail.com",
	 * --86014.51801.qm@web65505.mail.ac4.yahoo.com"
	 * </pre>
	 * 
	 * @param testName test method name
	 */
    class DavidsData extends AData {

		public DavidsData(String testName) {
			super(testName);

			MessageHeader messageHeader;
			Vector parentIDs;

			messageHeader = new MessageHeader(null);
			messageHeader.setMessageID( "INBOX&612821038&20" );
			messageHeader.setThreadingMessageID( "49aa8a24.09cc660a.65c9.4125@mx.google.com" );
			messageHeader.setParentID( "" );
			messageHeader.setSubject( "test send all" );
			// parentIDs=[]
			parentIDs = new Vector();
			messageHeader.setParentIDs( parentIDs );
			messages.addElement(messageHeader);

			messageHeader = new MessageHeader(null);
			messageHeader.setMessageID( "INBOX&612821038&19" );
			messageHeader.setThreadingMessageID( "49a9d6ee.09c5660a.3144.31dd@mx.google.com" );
			messageHeader.setParentID( "" );
			messageHeader.setSubject( "test max lines" );
			// parentIDs=[]
			parentIDs = new Vector();
			messageHeader.setParentIDs( parentIDs );
			messages.addElement(messageHeader);

			messageHeader = new MessageHeader(null);
			messageHeader.setMessageID( "INBOX&612821038&18" );
			messageHeader.setThreadingMessageID( "6f4858d80902101030o5a8beef2ic1f6409be5ff8879@mail.gmail.com" );
			messageHeader.setParentID( "" );
			messageHeader.setSubject( "html attachment" );
			// parentIDs=[]
			parentIDs = new Vector();
			messageHeader.setParentIDs( parentIDs );
			messages.addElement(messageHeader);

			messageHeader = new MessageHeader(null);
			messageHeader.setMessageID( "INBOX&612821038&17" );
			messageHeader.setThreadingMessageID( "4981c81f.0405560a.112a.ffffa906@mx.google.com" );
			messageHeader.setParentID( "" );
			messageHeader.setSubject( "test posilani" );
			// parentIDs=[]
			parentIDs = new Vector();
			messageHeader.setParentIDs( parentIDs );
			messages.addElement(messageHeader);

			messageHeader = new MessageHeader(null);
			messageHeader.setMessageID( "INBOX&612821038&16" );
			messageHeader.setThreadingMessageID( "6f4858d80901170531j76fef731w5d99aab103673944@mail.gmail.com" );
			messageHeader.setParentID( "6f4858d80811121252v21fd516dh12d61deef6278da7@mail.gmail.com" );
			messageHeader.setSubject( "Re: test flaggggs" );
			// parentIDs=[6f4858d80811121252v21fd516dh12d61deef6278da7@mail.gmail.com]
			parentIDs = new Vector();
			parentIDs.addElement( "6f4858d80811121252v21fd516dh12d61deef6278da7@mail.gmail.com" );
			messageHeader.setParentIDs( parentIDs );
			messages.addElement(messageHeader);

			messageHeader = new MessageHeader(null);
			messageHeader.setMessageID( "INBOX&612821038&15" );
			messageHeader.setThreadingMessageID( "837d98840901100421o69398087i1ce1dd79422113bd@mail.gmail.com" );
			messageHeader.setParentID( "837d98840901100418u250e7983y47a67ddf2c9543b8@mail.gmail.com" );
			messageHeader.setSubject( "=?KOI8-R?B?RndkOiD+1M/Uzw==?=" );
			// parentIDs=[837d98840901100418u250e7983y47a67ddf2c9543b8@mail.gmail.com]
			parentIDs = new Vector();
			parentIDs.addElement( "837d98840901100418u250e7983y47a67ddf2c9543b8@mail.gmail.com" );
			messageHeader.setParentIDs( parentIDs );
			messages.addElement(messageHeader);

			messageHeader = new MessageHeader(null);
			messageHeader.setMessageID( "INBOX&612821038&14" );
			messageHeader.setThreadingMessageID( "86014.51801.qm@web65505.mail.ac4.yahoo.com" );
			messageHeader.setParentID( "" );
			messageHeader.setSubject( "test pdf conversion" );
			// parentIDs=[]
			parentIDs = new Vector();
			messageHeader.setParentIDs( parentIDs );
			messages.addElement(messageHeader);

			messageHeader = new MessageHeader(null);
			messageHeader.setMessageID( "INBOX&612821038&13" );
			messageHeader.setThreadingMessageID( "798064.91440.qm@web65513.mail.ac4.yahoo.com" );
			messageHeader.setParentID( "" );
			messageHeader.setSubject( "test attachment conversion" );
			// parentIDs=[]
			parentIDs = new Vector();
			messageHeader.setParentIDs( parentIDs );
			messages.addElement(messageHeader);

			messageHeader = new MessageHeader(null);
			messageHeader.setMessageID( "INBOX&612821038&12" );
			messageHeader.setThreadingMessageID( "117839.58882.qm@web65515.mail.ac4.yahoo.com" );
			messageHeader.setParentID( "" );
			messageHeader.setSubject( "test bin" );
			// parentIDs=[]
			parentIDs = new Vector();
			messageHeader.setParentIDs( parentIDs );
			messages.addElement(messageHeader);

			messageHeader = new MessageHeader(null);
			messageHeader.setMessageID( "INBOX&612821038&11" );
			messageHeader.setThreadingMessageID( "729861.54352.qm@web65501.mail.ac4.yahoo.com" );
			messageHeader.setParentID( "" );
			messageHeader.setSubject( "test html view" );
			// parentIDs=[]
			parentIDs = new Vector();
			messageHeader.setParentIDs( parentIDs );
			messages.addElement(messageHeader);

			messageHeader = new MessageHeader(null);
			messageHeader.setMessageID( "INBOX&612821038&10" );
			messageHeader.setThreadingMessageID( "691953.8731.qm@web65503.mail.ac4.yahoo.com" );
			messageHeader.setParentID( "" );
			messageHeader.setSubject( "test conversion" );
			// parentIDs=[]
			parentIDs = new Vector();
			messageHeader.setParentIDs( parentIDs );
			messages.addElement(messageHeader);

			messageHeader = new MessageHeader(null);
			messageHeader.setMessageID( "INBOX&612821038&9" );
			messageHeader.setThreadingMessageID( "6f4858d80811121252v21fd516dh12d61deef6278da7@mail.gmail.com" );
			messageHeader.setParentID( "" );
			messageHeader.setSubject( "test flaggggs" );
			// parentIDs=[]
			parentIDs = new Vector();
			messageHeader.setParentIDs( parentIDs );
			messages.addElement(messageHeader);

			messageHeader = new MessageHeader(null);
			messageHeader.setMessageID( "INBOX&612821038&8" );
			messageHeader.setThreadingMessageID( "6f4858d80811121247n2eca3852gf083bb0985e30cee@mail.gmail.com" );
			messageHeader.setParentID( "" );
			messageHeader.setSubject( "ffff" );
			// parentIDs=[]
			parentIDs = new Vector();
			messageHeader.setParentIDs( parentIDs );
			messages.addElement(messageHeader);
		}
    	protected void prepareDataForCheck() {
        	threadedMessageIDs = new String[] {
    			"117839.58882.qm@web65515.mail.ac4.yahoo.com",
    			"4981c81f.0405560a.112a.ffffa906@mx.google.com",
    			"49a9d6ee.09c5660a.3144.31dd@mx.google.com",
    			"49aa8a24.09cc660a.65c9.4125@mx.google.com",
    			"691953.8731.qm@web65503.mail.ac4.yahoo.com",
    			"6f4858d80811121247n2eca3852gf083bb0985e30cee@mail.gmail.com",

    			"6f4858d80811121252v21fd516dh12d61deef6278da7@mail.gmail.com",
    				"6f4858d80901170531j76fef731w5d99aab103673944@mail.gmail.com",

    			"6f4858d80902101030o5a8beef2ic1f6409be5ff8879@mail.gmail.com",
    			"729861.54352.qm@web65501.mail.ac4.yahoo.com",
    			"798064.91440.qm@web65513.mail.ac4.yahoo.com",

    			//"837d98840901100418u250e7983y47a67ddf2c9543b8@mail.gmail.com", // empty parent - have just one child -> was removed
    				"837d98840901100421o69398087i1ce1dd79422113bd@mail.gmail.com",

    			"86014.51801.qm@web65505.mail.ac4.yahoo.com"
        	};
        	isRoot = new boolean[] {
        		true,
        		true,
        		true,
        		true,
        		true,
        		true,

        		true,
        		false,

        		true,
        		true,
        		true,

        		//true,
        		true,

        		true
        	};
    	}
    }
    
    class OneMailWithoutParent extends AData {

		public OneMailWithoutParent(String testName) {
			super(testName);
			//[mujmail.MessageHeader[messageID='GmailId11fe8998984d3c96', threadedMessageID='49B460A3.6080407@mujmail.org', parentID='', parentIDs=[], subject='retrieve test', isEmpty=false, deleted=false]]
			MessageHeader messageHeader;
			Vector parentIDs;

			messageHeader = new MessageHeader(null);
			messageHeader.setMessageID( "GmailId11fe8998984d3c96" );
			messageHeader.setThreadingMessageID( "49B460A3.6080407@mujmail.org" );
			messageHeader.setParentID( "" );
			messageHeader.setSubject( "retrieve test" );
			// parentIDs=[]
			parentIDs = new Vector();
			messageHeader.setParentIDs( parentIDs );
			messages.addElement(messageHeader);
		}

		protected void prepareDataForCheck() {
			threadedMessageIDs = new String[] {
				"49B460A3.6080407@mujmail.org"
			};
			isRoot = new boolean[] { true };
		}
    }

    /**
     * Result should be:
     * <pre>
     * --000e0cd2981a3cc5900464ee844c@googlemail.com
     * --000e0cd2981a8712e4046516d97a@googlemail.com
     * --000e0cd2981ab933b904650079c7@googlemail.com
     * --117839.58882.qm@web65515.mail.ac4.yahoo.com
     * --4981c81f.0405560a.112a.ffffa906@mx.google.com
     * --49a9d52e.0ab6660a.1af5.ffff95cc@mx.google.com
     * --49a9d6ee.09c5660a.3144.31dd@mx.google.com
     * --49a9d6ee.09c5660a.3144.31dd@mx.google.com
     * --49aa8a24.09cc660a.65c9.4125@mx.google.com
     * --49bb7322.06a1660a.69a7.ffffba17@mx.google.com
     * --49bb7256.06a1660a.69a7.ffffb711@mx.google.com
     * --49a7cfe0.0b38560a.5f8a.ffffc6ce@mx.google.com
     * --49a8659b.0aaa660a.7457.ffff8c9b@mx.google.com
     * --49a865e9.0aec660a.2913.2e3e@mx.google.com
     * --49a86736.0aec660a.2913.3329@mx.google.com
     * --49a8f7fd.0637560a.67fe.010c@mx.google.com
     * --49a914db.0c07560a.04b2.ffffe06d@mx.google.com
     * --49aa8a22.09cc660a.65c9.4118@mx.google.com
     * --49aaa8af.09cc660a.65c9.ffffabf0@mx.google.com
     * --49bb6fda.0e0f660a.17b1.ffff90ef@mx.google.com
     * --49bb72bd.06a1660a.69a7.ffffb878@mx.google.com
     * --49bb7322.06a1660a.69a7.ffffba17@mx.google.com
     * --691953.8731.qm@web65503.mail.ac4.yahoo.com
     * --6f4858d80811121247n2eca3852gf083bb0985e30cee@mail.gmail.com
     * --6f4858d80811121252v21fd516dh12d61deef6278da7@mail.gmail.com
     *   \--6f4858d80901170531j76fef731w5d99aab103673944@mail.gmail.com
     * --6f4858d80902101030o5a8beef2ic1f6409be5ff8879@mail.gmail.com
     * --6f4858d80903110 747i5b88f23ekc5af0404259b02cd@mail.gmail.com
     * --729861.54352.qm@web65501.mail.ac4.yahoo.com
     * --798064.91440.qm@web65513.mail.ac4.yahoo.com
     * --837d98840901100421o69398087i1ce1dd79422113bd@mail.gmail.com
     * --86014.51801.qm@web65505.mail.ac4.yahoo.com
     * --cbca5e80020b08a5999eef67e25dae10.1233152119.72382@dbas.hide.vol.cz
     * </pre>
     * 
     * @author Betlista
     *
     */
    class RemovingEmailsProblemData extends AData {
    	
    	public RemovingEmailsProblemData(String testName) {
    		super(testName);
    		
    		MessageHeader messageHeader;
    		Vector parentIDs;

    		messageHeader = new MessageHeader(null);
    		messageHeader.setMessageID( "INBOX&612821038&28" );
    		messageHeader.setThreadingMessageID( "000e0cd2981a8712e4046516d97a@googlemail.com" );
    		messageHeader.setParentID( "" );
    		messageHeader.setSubject( "Delivery Status Notification (Failure)" );
    		// parentIDs=[]
    		parentIDs = new Vector();
    		messageHeader.setParentIDs( parentIDs );
    		messages.addElement(messageHeader);

    		messageHeader = new MessageHeader(null);
    		messageHeader.setMessageID( "INBOX&612821038&27" );
    		messageHeader.setThreadingMessageID( "49bb7322.06a1660a.69a7.ffffba17@mx.google.com" );
    		messageHeader.setParentID( "" );
    		messageHeader.setSubject( "test hromadne posilani 705" );
    		// parentIDs=[]
    		parentIDs = new Vector();
    		messageHeader.setParentIDs( parentIDs );
    		messages.addElement(messageHeader);

    		messageHeader = new MessageHeader(null);
    		messageHeader.setMessageID( "INBOX&612821038&26" );
    		messageHeader.setThreadingMessageID( "49bb7256.06a1660a.69a7.ffffb711@mx.google.com" );
    		messageHeader.setParentID( "" );
    		messageHeader.setSubject( "test posilani 705" );
    		// parentIDs=[]
    		parentIDs = new Vector();
    		messageHeader.setParentIDs( parentIDs );
    		messages.addElement(messageHeader);

    		messageHeader = new MessageHeader(null);
    		messageHeader.setMessageID( "INBOX&612821038&25" );
    		messageHeader.setThreadingMessageID( "000e0cd2981ab933b904650079c7@googlemail.com" );
    		messageHeader.setParentID( "" );
    		messageHeader.setSubject( "Delivery Status Notification (Delay)" );
    		// parentIDs=[]
    		parentIDs = new Vector();
    		messageHeader.setParentIDs( parentIDs );
    		messages.addElement(messageHeader);

    		messageHeader = new MessageHeader(null);
    		messageHeader.setMessageID( "INBOX&612821038&24" );
    		messageHeader.setThreadingMessageID( "000e0cd2981a3cc5900464ee844c@googlemail.com" );
    		messageHeader.setParentID( "" );
    		messageHeader.setSubject( "Delivery Status Notification (Delay)" );
    		// parentIDs=[]
    		parentIDs = new Vector();
    		messageHeader.setParentIDs( parentIDs );
    		messages.addElement(messageHeader);

    		messageHeader = new MessageHeader(null);
    		messageHeader.setMessageID( "INBOX&612821038&20" );
    		messageHeader.setThreadingMessageID( "49aa8a24.09cc660a.65c9.4125@mx.google.com" );
    		messageHeader.setParentID( "" );
    		messageHeader.setSubject( "test send all" );
    		// parentIDs=[]
    		parentIDs = new Vector();
    		messageHeader.setParentIDs( parentIDs );
    		messages.addElement(messageHeader);

    		messageHeader = new MessageHeader(null);
    		messageHeader.setMessageID( "INBOX&612821038&19" );
    		messageHeader.setThreadingMessageID( "49a9d6ee.09c5660a.3144.31dd@mx.google.com" );
    		messageHeader.setParentID( "" );
    		messageHeader.setSubject( "test max lines" );
    		// parentIDs=[]
    		parentIDs = new Vector();
    		messageHeader.setParentIDs( parentIDs );
    		messages.addElement(messageHeader);

    		messageHeader = new MessageHeader(null);
    		messageHeader.setMessageID( "INBOX&612821038&18" );
    		messageHeader.setThreadingMessageID( "6f4858d80902101030o5a8beef2ic1f6409be5ff8879@mail.gmail.com" );
    		messageHeader.setParentID( "" );
    		messageHeader.setSubject( "html attachment" );
    		// parentIDs=[]
    		parentIDs = new Vector();
    		messageHeader.setParentIDs( parentIDs );
    		messages.addElement(messageHeader);

    		messageHeader = new MessageHeader(null);
    		messageHeader.setMessageID( "INBOX&612821038&17" );
    		messageHeader.setThreadingMessageID( "4981c81f.0405560a.112a.ffffa906@mx.google.com" );
    		messageHeader.setParentID( "" );
    		messageHeader.setSubject( "test posilani" );
    		// parentIDs=[]
    		parentIDs = new Vector();
    		messageHeader.setParentIDs( parentIDs );
    		messages.addElement(messageHeader);

    		messageHeader = new MessageHeader(null);
    		messageHeader.setMessageID( "INBOX&612821038&16" );
    		messageHeader.setThreadingMessageID( "6f4858d80901170531j76fef731w5d99aab103673944@mail.gmail.com" );
    		messageHeader.setParentID( "6f4858d80811121252v21fd516dh12d61deef6278da7@mail.gmail.com" );
    		messageHeader.setSubject( "Re: test flaggggs" );
    		// parentIDs=[6f4858d80811121252v21fd516dh12d61deef6278da7@mail.gmail.com]
    		parentIDs = new Vector();
    		parentIDs.addElement( "6f4858d80811121252v21fd516dh12d61deef6278da7@mail.gmail.com" );
    		messageHeader.setParentIDs( parentIDs );
    		messages.addElement(messageHeader);

    		messageHeader = new MessageHeader(null);
    		messageHeader.setMessageID( "INBOX&612821038&15" );
    		messageHeader.setThreadingMessageID( "837d98840901100421o69398087i1ce1dd79422113bd@mail.gmail.com" );
    		messageHeader.setParentID( "837d98840901100418u250e7983y47a67ddf2c9543b8@mail.gmail.com" );
    		messageHeader.setSubject( "=?KOI8-R?B?RndkOiD+1M/Uzw==?=" );
    		// parentIDs=[837d98840901100418u250e7983y47a67ddf2c9543b8@mail.gmail.com]
    		parentIDs = new Vector();
    		parentIDs.addElement( "837d98840901100418u250e7983y47a67ddf2c9543b8@mail.gmail.com" );
    		messageHeader.setParentIDs( parentIDs );
    		messages.addElement(messageHeader);

    		messageHeader = new MessageHeader(null);
    		messageHeader.setMessageID( "INBOX&612821038&14" );
    		messageHeader.setThreadingMessageID( "86014.51801.qm@web65505.mail.ac4.yahoo.com" );
    		messageHeader.setParentID( "" );
    		messageHeader.setSubject( "test pdf conversion" );
    		// parentIDs=[]
    		parentIDs = new Vector();
    		messageHeader.setParentIDs( parentIDs );
    		messages.addElement(messageHeader);

    		messageHeader = new MessageHeader(null);
    		messageHeader.setMessageID( "INBOX&612821038&13" );
    		messageHeader.setThreadingMessageID( "798064.91440.qm@web65513.mail.ac4.yahoo.com" );
    		messageHeader.setParentID( "" );
    		messageHeader.setSubject( "test attachment conversion" );
    		// parentIDs=[]
    		parentIDs = new Vector();
    		messageHeader.setParentIDs( parentIDs );
    		messages.addElement(messageHeader);

    		messageHeader = new MessageHeader(null);
    		messageHeader.setMessageID( "INBOX&612821038&12" );
    		messageHeader.setThreadingMessageID( "117839.58882.qm@web65515.mail.ac4.yahoo.com" );
    		messageHeader.setParentID( "" );
    		messageHeader.setSubject( "test bin" );
    		// parentIDs=[]
    		parentIDs = new Vector();
    		messageHeader.setParentIDs( parentIDs );
    		messages.addElement(messageHeader);

    		messageHeader = new MessageHeader(null);
    		messageHeader.setMessageID( "INBOX&612821038&11" );
    		messageHeader.setThreadingMessageID( "729861.54352.qm@web65501.mail.ac4.yahoo.com" );
    		messageHeader.setParentID( "" );
    		messageHeader.setSubject( "test html view" );
    		// parentIDs=[]
    		parentIDs = new Vector();
    		messageHeader.setParentIDs( parentIDs );
    		messages.addElement(messageHeader);

    		messageHeader = new MessageHeader(null);
    		messageHeader.setMessageID( "INBOX&612821038&10" );
    		messageHeader.setThreadingMessageID( "691953.8731.qm@web65503.mail.ac4.yahoo.com" );
    		messageHeader.setParentID( "" );
    		messageHeader.setSubject( "test conversion" );
    		// parentIDs=[]
    		parentIDs = new Vector();
    		messageHeader.setParentIDs( parentIDs );
    		messages.addElement(messageHeader);

    		messageHeader = new MessageHeader(null);
    		messageHeader.setMessageID( "INBOX&612821038&9" );
    		messageHeader.setThreadingMessageID( "6f4858d80811121252v21fd516dh12d61deef6278da7@mail.gmail.com" );
    		messageHeader.setParentID( "" );
    		messageHeader.setSubject( "test flaggggs" );
    		// parentIDs=[]
    		parentIDs = new Vector();
    		messageHeader.setParentIDs( parentIDs );
    		messages.addElement(messageHeader);

    		messageHeader = new MessageHeader(null);
    		messageHeader.setMessageID( "INBOX&612821038&8" );
    		messageHeader.setThreadingMessageID( "6f4858d80811121247n2eca3852gf083bb0985e30cee@mail.gmail.com" );
    		messageHeader.setParentID( "" );
    		messageHeader.setSubject( "ffff" );
    		// parentIDs=[]
    		parentIDs = new Vector();
    		messageHeader.setParentIDs( parentIDs );
    		messages.addElement(messageHeader);

    		messageHeader = new MessageHeader(null);
    		messageHeader.setMessageID( "1235866927.486787P80477I48328969_uuIA.imap,S=2232" );
    		messageHeader.setThreadingMessageID( "49a9d52e.0ab6660a.1af5.ffff95cc@mx.google.com" );
    		messageHeader.setParentID( "" );
    		messageHeader.setSubject( "test send again and again" );
    		// parentIDs=[]
    		parentIDs = new Vector();
    		messageHeader.setParentIDs( parentIDs );
    		messages.addElement(messageHeader);

    		messageHeader = new MessageHeader(null);
    		messageHeader.setMessageID( "1235867375.487278P80477I48328997_o0vi.imap,S=2424" );
    		messageHeader.setThreadingMessageID( "49a9d6ee.09c5660a.3144.31dd@mx.google.com" );
    		messageHeader.setParentID( "" );
    		messageHeader.setSubject( "test max lines" );
    		// parentIDs=[]
    		parentIDs = new Vector();
    		messageHeader.setParentIDs( parentIDs );
    		messages.addElement(messageHeader);

    		messageHeader = new MessageHeader(null);
    		messageHeader.setMessageID( "1233152118.713750P30116I48329226_13ZZ.imap,S=11740" );
    		messageHeader.setThreadingMessageID( "cbca5e80020b08a5999eef67e25dae10.1233152119.72382@dbas.hide.vol.cz" );
    		messageHeader.setParentID( "" );
    		messageHeader.setSubject( "Informace o vasem novem uctu VOLNY" );
    		// parentIDs=[]
    		parentIDs = new Vector();
    		messageHeader.setParentIDs( parentIDs );
    		messages.addElement(messageHeader);

    		messageHeader = new MessageHeader(null);
    		messageHeader.setMessageID( "1235810302.543831P23156I0_U1HI0Eel.m3,S=2210" );
    		messageHeader.setThreadingMessageID( "49a8f7fd.0637560a.67fe.010c@mx.google.com" );
    		messageHeader.setParentID( "" );
    		messageHeader.setSubject( "test send mobile" );
    		// parentIDs=[]
    		parentIDs = new Vector();
    		messageHeader.setParentIDs( parentIDs );
    		messages.addElement(messageHeader);

    		messageHeader = new MessageHeader(null);
    		messageHeader.setMessageID( "1235913251.741914P50253I48328974_A5fe.imap,S=2168" );
    		messageHeader.setThreadingMessageID( "49aa8a22.09cc660a.65c9.4118@mx.google.com" );
    		messageHeader.setParentID( "" );
    		messageHeader.setSubject( "test send all" );
    		// parentIDs=[]
    		parentIDs = new Vector();
    		messageHeader.setParentIDs( parentIDs );
    		messages.addElement(messageHeader);

    		messageHeader = new MessageHeader(null);
    		messageHeader.setMessageID( "1235772828.769699P9336I0_rdPKT6b9.m3,S=2152" );
    		messageHeader.setThreadingMessageID( "49a8659b.0aaa660a.7457.ffff8c9b@mx.google.com" );
    		messageHeader.setParentID( "" );
    		messageHeader.setSubject( "test send hanging" );
    		// parentIDs=[]
    		parentIDs = new Vector();
    		messageHeader.setParentIDs( parentIDs );
    		messages.addElement(messageHeader);

    		messageHeader = new MessageHeader(null);
    		messageHeader.setMessageID( "1237021375.583379P15351I0_Ywhwi4qX.m3,S=2144" );
    		messageHeader.setThreadingMessageID( "49bb72bd.06a1660a.69a7.ffffb878@mx.google.com" );
    		messageHeader.setParentID( "" );
    		messageHeader.setSubject( "test posilani 705" );
    		// parentIDs=[]
    		parentIDs = new Vector();
    		messageHeader.setParentIDs( parentIDs );
    		messages.addElement(messageHeader);

    		messageHeader = new MessageHeader(null);
    		messageHeader.setMessageID( "1237020636.181676P4526I0_5IvkQLc5.m3,S=2158" );
    		messageHeader.setThreadingMessageID( "49bb6fda.0e0f660a.17b1.ffff90ef@mx.google.com" );
    		messageHeader.setParentID( "" );
    		messageHeader.setSubject( "test posilani na volny" );
    		// parentIDs=[]
    		parentIDs = new Vector();
    		messageHeader.setParentIDs( parentIDs );
    		messages.addElement(messageHeader);

    		messageHeader = new MessageHeader(null);
    		messageHeader.setMessageID( "1236782879.99715P91080I0_YfnTaSxr.m3,S=18896" );
    		messageHeader.setThreadingMessageID( "6f4858d80903110747i5b88f23ekc5af0404259b02cd@mail.gmail.com" );
    		messageHeader.setParentID( "" );
    		messageHeader.setSubject( "testing max lines (plaintext)" );
    		// parentIDs=[]
    		parentIDs = new Vector();
    		messageHeader.setParentIDs( parentIDs );
    		messages.addElement(messageHeader);

    		messageHeader = new MessageHeader(null);
    		messageHeader.setMessageID( "1235734504.528084P64503I0_8qbfvuTa.m3,S=2199" );
    		messageHeader.setThreadingMessageID( "49a7cfe0.0b38560a.5f8a.ffffc6ce@mx.google.com" );
    		messageHeader.setParentID( "" );
    		messageHeader.setSubject( "test send later" );
    		// parentIDs=[]
    		parentIDs = new Vector();
    		messageHeader.setParentIDs( parentIDs );
    		messages.addElement(messageHeader);

    		messageHeader = new MessageHeader(null);
    		messageHeader.setMessageID( "1237021476.373699P16633I0_8wiUztiW.m3,S=2245" );
    		messageHeader.setThreadingMessageID( "49bb7322.06a1660a.69a7.ffffba17@mx.google.com" );
    		messageHeader.setParentID( "" );
    		messageHeader.setSubject( "test hromadne posilani 705" );
    		// parentIDs=[]
    		parentIDs = new Vector();
    		messageHeader.setParentIDs( parentIDs );
    		messages.addElement(messageHeader);

    		messageHeader = new MessageHeader(null);
    		messageHeader.setMessageID( "1235921082.742608P50253I48329038_f7xX.imap,S=2207" );
    		messageHeader.setThreadingMessageID( "49aaa8af.09cc660a.65c9.ffffabf0@mx.google.com" );
    		messageHeader.setParentID( "" );
    		messageHeader.setSubject( "test sleeping" );
    		// parentIDs=[]
    		parentIDs = new Vector();
    		messageHeader.setParentIDs( parentIDs );
    		messages.addElement(messageHeader);

    		messageHeader = new MessageHeader(null);
    		messageHeader.setMessageID( "1235817691.599403P62348I48328990_2FXO.imap,S=2180" );
    		messageHeader.setThreadingMessageID( "49a914db.0c07560a.04b2.ffffe06d@mx.google.com" );
    		messageHeader.setParentID( "" );
    		messageHeader.setSubject( "test" );
    		// parentIDs=[]
    		parentIDs = new Vector();
    		messageHeader.setParentIDs( parentIDs );
    		messages.addElement(messageHeader);

    		messageHeader = new MessageHeader(null);
    		messageHeader.setMessageID( "1235773240.205105P15490I0_FRzU6viL.m3,S=2167" );
    		messageHeader.setThreadingMessageID( "49a86736.0aec660a.2913.3329@mx.google.com" );
    		messageHeader.setParentID( "" );
    		messageHeader.setSubject( "test send all 3" );
    		// parentIDs=[]
    		parentIDs = new Vector();
    		messageHeader.setParentIDs( parentIDs );
    		messages.addElement(messageHeader);

    		messageHeader = new MessageHeader(null);
    		messageHeader.setMessageID( "1235772906.675320P11629I0_HFKI2J9M.m3,S=2151" );
    		messageHeader.setThreadingMessageID( "49a865e9.0aec660a.2913.2e3e@mx.google.com" );
    		messageHeader.setParentID( "" );
    		messageHeader.setSubject( "test again" );
    		// parentIDs=[]
    		parentIDs = new Vector();
    		messageHeader.setParentIDs( parentIDs );
    		messages.addElement(messageHeader);
    	}
    	
    	protected void prepareDataForCheck() {
    		threadedMessageIDs = new String[] {
				"000e0cd2981a3cc5900464ee844c@googlemail.com",
				"000e0cd2981a8712e4046516d97a@googlemail.com",
				"000e0cd2981ab933b904650079c7@googlemail.com",
				"117839.58882.qm@web65515.mail.ac4.yahoo.com",
				"4981c81f.0405560a.112a.ffffa906@mx.google.com", // 5
				"49a7cfe0.0b38560a.5f8a.ffffc6ce@mx.google.com",
				"49a8659b.0aaa660a.7457.ffff8c9b@mx.google.com",
				"49a865e9.0aec660a.2913.2e3e@mx.google.com",
				"49a86736.0aec660a.2913.3329@mx.google.com",
				"49a8f7fd.0637560a.67fe.010c@mx.google.com", // 10
				"49a914db.0c07560a.04b2.ffffe06d@mx.google.com",
				"49a9d52e.0ab6660a.1af5.ffff95cc@mx.google.com",
				"49a9d6ee.09c5660a.3144.31dd@mx.google.com",
				"49a9d6ee.09c5660a.3144.31dd@mx.google.com",
				"49aa8a22.09cc660a.65c9.4118@mx.google.com", // 15
				"49aa8a24.09cc660a.65c9.4125@mx.google.com",
				"49aaa8af.09cc660a.65c9.ffffabf0@mx.google.com",
				"49bb6fda.0e0f660a.17b1.ffff90ef@mx.google.com",
				"49bb7256.06a1660a.69a7.ffffb711@mx.google.com",
				"49bb72bd.06a1660a.69a7.ffffb878@mx.google.com", // 20
				"49bb7322.06a1660a.69a7.ffffba17@mx.google.com",
				"49bb7322.06a1660a.69a7.ffffba17@mx.google.com",
				"691953.8731.qm@web65503.mail.ac4.yahoo.com",
				"6f4858d80811121247n2eca3852gf083bb0985e30cee@mail.gmail.com", //24

				"6f4858d80811121252v21fd516dh12d61deef6278da7@mail.gmail.com",
					"6f4858d80901170531j76fef731w5d99aab103673944@mail.gmail.com",

				"6f4858d80902101030o5a8beef2ic1f6409be5ff8879@mail.gmail.com",
				"6f4858d80903110747i5b88f23ekc5af0404259b02cd@mail.gmail.com",
				"729861.54352.qm@web65501.mail.ac4.yahoo.com",
				"798064.91440.qm@web65513.mail.ac4.yahoo.com",

					"837d98840901100421o69398087i1ce1dd79422113bd@mail.gmail.com",

				"86014.51801.qm@web65505.mail.ac4.yahoo.com",
				"cbca5e80020b08a5999eef67e25dae10.1233152119.72382@dbas.hide.vol.cz"
    		};
    		isRoot = new boolean[] {
    			true,
    			true,
    			true,
    			true,
    			true, // 5
    			true,
    			true,
    			true,
    			true,
    			true, // 10
    			true,
    			true,
    			true,
    			true,
    			true, // 15
    			true,
    			true,
    			true,
    			true,
    			true, // 20
    			true,
    			true,
    			true,
    			true,

    			true,
    			false,

    			true,
    			true,
    			true,
    			true,

    			true,

    			true,
    			true
    		};
    	}
    }

    class NewThreadingRealData extends AData {
        /**
         * Result should be:
         * <pre>
         * --3b94c0010903181218g7dad81e3vd6fc56723dd88562@mail.gmail.com
         * --49A55FDE.5000701@mujmail.org (empty)
         *   \--3b94c0010903181235u5b5387cfm68c4ef972eae480f@mail.gmail.com
         * --49AEE32E.20908@mujmail.org
         * --49B03152.2090000@mujmail.org (empty)
         *   \--49B03179.4010301@mujmail.org
         * --49a86ff7.03eb300a.4698.ffffefad@mx.google.com
         * 49a87065.03eb300a.4698.fffff080@mx.google.com
         * 49a874ee.1ae7300a.7045.ffffc957@mx.google.com
         * 49a8ef83.25e2660a.21d1.6983@mx.google.com
         * 49a93c73.0aa5660a.7354.2dd3@mx.google.com
         * 49a93c74.0aa5660a.7354.2dd7@mx.google.com
         * 49abd4b4.0707d00a.4b45.122b@mx.google.com
         * 49b1be01.0437560a.3bd7.1a9c@mx.google.com
         * 49beb070.170d660a.66c9.ffffcb50@mx.google.com
         * 49beb326.170d660a.66c9.ffffdbeb@mx.google.com
         * 49beb426.170d660a.66c9.ffffe304@mx.google.com
         * 49beb744.08b6660a.6e44.6ec7@mx.google.com
         * 71dd3840903310807l702f4eb2j92147e5de81d1583@mail.gmail.com
         * 71dd3840903310807r5943d46bw74fbcd1bfe271044@mail.gmail.com
         * --df7c56410902250653m6abab365t608e4ae0f5bb5ca8@mail.gmail.com (empty)
         *   |--3b94c0010903180851v74780bbcs971e677d36b0b517@mail.gmail.com
         *   \--3b94c0010903181215u264dc852v886ef569f76e9fed@mail.gmail.com
         * --f5a0da050903111116v7c50bb9fla85b49f62a61e3a4@mail.gmail.com
         *   \--3b94c0010903111118y615c4ce4v20e057366981c681@mail.gmail.com
         * </pre>
         * @param testName
         */
        public NewThreadingRealData(String testName) {
            super(testName);
            MessageHeader messageHeader;
            Vector parentIDs;

            messageHeader = new MessageHeader(null);
            messageHeader.setMessageID( "GmailId1205d124c0144dd9" );
            messageHeader.setThreadingMessageID( "71dd3840903310807r5943d46bw74fbcd1bfe271044@mail.gmail.com" );
            messageHeader.setParentID( "" );
            messageHeader.setSubject( "Re: Threading test (3. reply, 2. level)" );
            // parentIDs=[]
            parentIDs = new Vector();
            messageHeader.setParentIDs( parentIDs );
            messages.addElement(messageHeader);

            messageHeader = new MessageHeader(null);
            messageHeader.setMessageID( "GmailId1205d11fd9356ce2" );
            messageHeader.setThreadingMessageID( "71dd3840903310807l702f4eb2j92147e5de81d1583@mail.gmail.com" );
            messageHeader.setParentID( "" );
            messageHeader.setSubject( "Re: Threading test (2. reply, 1. level)" );
            // parentIDs=[]
            parentIDs = new Vector();
            messageHeader.setParentIDs( parentIDs );
            messages.addElement(messageHeader);

            messageHeader = new MessageHeader(null);
            messageHeader.setMessageID( "GmailId11fb9f56528cfb1b" );
            messageHeader.setThreadingMessageID( "49a86ff7.03eb300a.4698.ffffefad@mx.google.com" );
            messageHeader.setParentID( "" );
            messageHeader.setSubject( "move to trash 2" );
            // parentIDs=[]
            parentIDs = new Vector();
            messageHeader.setParentIDs( parentIDs );
            messages.addElement(messageHeader);

            messageHeader = new MessageHeader(null);
            messageHeader.setMessageID( "GmailId11fb9f7111d598d5" );
            messageHeader.setThreadingMessageID( "49a87065.03eb300a.4698.fffff080@mx.google.com" );
            messageHeader.setParentID( "" );
            messageHeader.setSubject( "preview" );
            // parentIDs=[]
            parentIDs = new Vector();
            messageHeader.setParentIDs( parentIDs );
            messages.addElement(messageHeader);

            messageHeader = new MessageHeader(null);
            messageHeader.setMessageID( "GmailId11fba08cb3a28b0f" );
            messageHeader.setThreadingMessageID( "49a874ee.1ae7300a.7045.ffffc957@mx.google.com" );
            messageHeader.setParentID( "" );
            messageHeader.setSubject( "delete/undelete" );
            // parentIDs=[]
            parentIDs = new Vector();
            messageHeader.setParentIDs( parentIDs );
            messages.addElement(messageHeader);

            messageHeader = new MessageHeader(null);
            messageHeader.setMessageID( "GmailId11fbbe79fe82dc00" );
            messageHeader.setThreadingMessageID( "49a8ef83.25e2660a.21d1.6983@mx.google.com" );
            messageHeader.setParentID( "" );
            messageHeader.setSubject( "send 28/1" );
            // parentIDs=[]
            parentIDs = new Vector();
            messageHeader.setParentIDs( parentIDs );
            messages.addElement(messageHeader);

            messageHeader = new MessageHeader(null);
            messageHeader.setMessageID( "GmailId11fbd14282e97d2c" );
            messageHeader.setThreadingMessageID( "49a93c73.0aa5660a.7354.2dd3@mx.google.com" );
            messageHeader.setParentID( "" );
            messageHeader.setSubject( "NB send test" );
            // parentIDs=[]
            parentIDs = new Vector();
            messageHeader.setParentIDs( parentIDs );
            messages.addElement(messageHeader);

            messageHeader = new MessageHeader(null);
            messageHeader.setMessageID( "GmailId11fbd142dd3e4d0e" );
            messageHeader.setThreadingMessageID( "49a93c74.0aa5660a.7354.2dd7@mx.google.com" );
            messageHeader.setParentID( "" );
            messageHeader.setSubject( "send rev 600" );
            // parentIDs=[]
            parentIDs = new Vector();
            messageHeader.setParentIDs( parentIDs );
            messages.addElement(messageHeader);

            messageHeader = new MessageHeader(null);
            messageHeader.setMessageID( "GmailId11fc736ea762c646" );
            messageHeader.setThreadingMessageID( "49abd4b4.0707d00a.4b45.122b@mx.google.com" );
            messageHeader.setParentID( "" );
            messageHeader.setSubject( "NB" );
            // parentIDs=[]
            parentIDs = new Vector();
            messageHeader.setParentIDs( parentIDs );
            messages.addElement(messageHeader);

            messageHeader = new MessageHeader(null);
            messageHeader.setMessageID( "GmailId11fd3280636800c6" );
            messageHeader.setThreadingMessageID( "49AEE32E.20908@mujmail.org" );
            messageHeader.setParentID( "" );
            messageHeader.setSubject( "simple HTML TB" );
            // parentIDs=[]
            parentIDs = new Vector();
            messageHeader.setParentIDs( parentIDs );
            messages.addElement(messageHeader);

            messageHeader = new MessageHeader(null);
            messageHeader.setMessageID( "GmailId11fd8414907c9cc4" );
            messageHeader.setThreadingMessageID( "49B03179.4010301@mujmail.org" );
            messageHeader.setParentID( "49B03152.2090000@mujmail.org" );
            messageHeader.setSubject( "Re: root message test (tester1)" );
            // parentIDs=[49B03152.2090000@mujmail.org]
            parentIDs = new Vector();
            parentIDs.addElement( "49B03152.2090000@mujmail.org" );
            messageHeader.setParentIDs( parentIDs );
            messages.addElement(messageHeader);

            messageHeader = new MessageHeader(null);
            messageHeader.setMessageID( "GmailId11fde4e8025fe18e" );
            messageHeader.setThreadingMessageID( "49b1be01.0437560a.3bd7.1a9c@mx.google.com" );
            messageHeader.setParentID( "" );
            messageHeader.setSubject( "send 7/1" );
            // parentIDs=[]
            parentIDs = new Vector();
            messageHeader.setParentIDs( parentIDs );
            messages.addElement(messageHeader);

            messageHeader = new MessageHeader(null);
            messageHeader.setMessageID( "GmailId11ff6c0770839b26" );
            messageHeader.setThreadingMessageID( "f5a0da050903111116v7c50bb9fla85b49f62a61e3a4@mail.gmail.com" );
            messageHeader.setParentID( "" );
            messageHeader.setSubject( "three responses" );
            // parentIDs=[]
            parentIDs = new Vector();
            messageHeader.setParentIDs( parentIDs );
            messages.addElement(messageHeader);

            messageHeader = new MessageHeader(null);
            messageHeader.setMessageID( "GmailId11ff6c17f5a46174" );
            messageHeader.setThreadingMessageID( "3b94c0010903111118y615c4ce4v20e057366981c681@mail.gmail.com" );
            messageHeader.setParentID( "f5a0da050903111116v7c50bb9fla85b49f62a61e3a4@mail.gmail.com" );
            messageHeader.setSubject( "Re: three responses" );
            // parentIDs=[f5a0da050903111116v7c50bb9fla85b49f62a61e3a4@mail.gmail.com]
            parentIDs = new Vector();
            parentIDs.addElement( "f5a0da050903111116v7c50bb9fla85b49f62a61e3a4@mail.gmail.com" );
            messageHeader.setParentIDs( parentIDs );
            messages.addElement(messageHeader);

            messageHeader = new MessageHeader(null);
            messageHeader.setMessageID( "GmailId12010e13fa11bd51" );
            messageHeader.setThreadingMessageID( "49beb070.170d660a.66c9.ffffcb50@mx.google.com" );
            messageHeader.setParentID( "" );
            messageHeader.setSubject( "test" );
            // parentIDs=[]
            parentIDs = new Vector();
            messageHeader.setParentIDs( parentIDs );
            messages.addElement(messageHeader);

            messageHeader = new MessageHeader(null);
            messageHeader.setMessageID( "GmailId12010ebd74edc218" );
            messageHeader.setThreadingMessageID( "49beb326.170d660a.66c9.ffffdbeb@mx.google.com" );
            messageHeader.setParentID( "" );
            messageHeader.setSubject( "test 3 recipients" );
            // parentIDs=[]
            parentIDs = new Vector();
            messageHeader.setParentIDs( parentIDs );
            messages.addElement(messageHeader);

            messageHeader = new MessageHeader(null);
            messageHeader.setMessageID( "GmailId12010efba8488e98" );
            messageHeader.setThreadingMessageID( "49beb426.170d660a.66c9.ffffe304@mx.google.com" );
            messageHeader.setParentID( "" );
            messageHeader.setSubject( "batch" );
            // parentIDs=[]
            parentIDs = new Vector();
            messageHeader.setParentIDs( parentIDs );
            messages.addElement(messageHeader);

            messageHeader = new MessageHeader(null);
            messageHeader.setMessageID( "GmailId12010fbea0fb56c8" );
            messageHeader.setThreadingMessageID( "49beb744.08b6660a.6e44.6ec7@mx.google.com" );
            messageHeader.setParentID( "" );
            messageHeader.setSubject( "test" );
            // parentIDs=[]
            parentIDs = new Vector();
            messageHeader.setParentIDs( parentIDs );
            messages.addElement(messageHeader);

            messageHeader = new MessageHeader(null);
            messageHeader.setMessageID( "GmailId1201a475cb183e7a" );
            messageHeader.setThreadingMessageID( "3b94c0010903180851v74780bbcs971e677d36b0b517@mail.gmail.com" );
            messageHeader.setParentID( "df7c56410902250653m6abab365t608e4ae0f5bb5ca8@mail.gmail.com" );
            messageHeader.setSubject( "Fwd: html test" );
            // parentIDs=[df7c56410902250653m6abab365t608e4ae0f5bb5ca8@mail.gmail.com]
            parentIDs = new Vector();
            parentIDs.addElement( "df7c56410902250653m6abab365t608e4ae0f5bb5ca8@mail.gmail.com" );
            messageHeader.setParentIDs( parentIDs );
            messages.addElement(messageHeader);

            messageHeader = new MessageHeader(null);
            messageHeader.setMessageID( "GmailId1201b028c3677f2c" );
            messageHeader.setThreadingMessageID( "3b94c0010903181215u264dc852v886ef569f76e9fed@mail.gmail.com" );
            messageHeader.setParentID( "df7c56410902250653m6abab365t608e4ae0f5bb5ca8@mail.gmail.com" );
            messageHeader.setSubject( "Fwd: html test" );
            // parentIDs=[df7c56410902250653m6abab365t608e4ae0f5bb5ca8@mail.gmail.com]
            parentIDs = new Vector();
            parentIDs.addElement( "df7c56410902250653m6abab365t608e4ae0f5bb5ca8@mail.gmail.com" );
            messageHeader.setParentIDs( parentIDs );
            messages.addElement(messageHeader);

            messageHeader = new MessageHeader(null);
            messageHeader.setMessageID( "GmailId1201b053d904d145" );
            messageHeader.setThreadingMessageID( "3b94c0010903181218g7dad81e3vd6fc56723dd88562@mail.gmail.com" );
            messageHeader.setParentID( "" );
            messageHeader.setSubject( "test 20:18" );
            // parentIDs=[]
            parentIDs = new Vector();
            messageHeader.setParentIDs( parentIDs );
            messages.addElement(messageHeader);

            messageHeader = new MessageHeader(null);
            messageHeader.setMessageID( "GmailId1201b15243063ca9" );
            messageHeader.setThreadingMessageID( "3b94c0010903181235u5b5387cfm68c4ef972eae480f@mail.gmail.com" );
            messageHeader.setParentID( "49A55FDE.5000701@mujmail.org" );
            messageHeader.setSubject( "Fwd: html test (thunderbird)" );
            // parentIDs=[49A55FDE.5000701@mujmail.org]
            parentIDs = new Vector();
            parentIDs.addElement( "49A55FDE.5000701@mujmail.org" );
            messageHeader.setParentIDs( parentIDs );
            messages.addElement(messageHeader);
        }
        protected void prepareDataForCheck() {
            threadedMessageIDs = new String[] {
                    "3b94c0010903181218g7dad81e3vd6fc56723dd88562@mail.gmail.com",

                    //"49A55FDE.5000701@mujmail.org (empty)",
                    "3b94c0010903181235u5b5387cfm68c4ef972eae480f@mail.gmail.com",

                    "49AEE32E.20908@mujmail.org",

                    //"49B03152.2090000@mujmail.org (empty)",
                    "49B03179.4010301@mujmail.org",

                    "49a86ff7.03eb300a.4698.ffffefad@mx.google.com", // 5
                    "49a87065.03eb300a.4698.fffff080@mx.google.com",
                    "49a874ee.1ae7300a.7045.ffffc957@mx.google.com",
                    "49a8ef83.25e2660a.21d1.6983@mx.google.com",
                    "49a93c73.0aa5660a.7354.2dd3@mx.google.com",
                    "49a93c74.0aa5660a.7354.2dd7@mx.google.com", // 10
                    "49abd4b4.0707d00a.4b45.122b@mx.google.com",
                    "49b1be01.0437560a.3bd7.1a9c@mx.google.com",
                    "49beb070.170d660a.66c9.ffffcb50@mx.google.com",
                    "49beb326.170d660a.66c9.ffffdbeb@mx.google.com",
                    "49beb426.170d660a.66c9.ffffe304@mx.google.com", // 15
                    "49beb744.08b6660a.6e44.6ec7@mx.google.com",
                    "71dd3840903310807l702f4eb2j92147e5de81d1583@mail.gmail.com",
                    "71dd3840903310807r5943d46bw74fbcd1bfe271044@mail.gmail.com",

                    "df7c56410902250653m6abab365t608e4ae0f5bb5ca8@mail.gmail.com",// (empty)
                      "3b94c0010903180851v74780bbcs971e677d36b0b517@mail.gmail.com",
                      "3b94c0010903181215u264dc852v886ef569f76e9fed@mail.gmail.com",

                    "f5a0da050903111116v7c50bb9fla85b49f62a61e3a4@mail.gmail.com",
                      "3b94c0010903111118y615c4ce4v20e057366981c681@mail.gmail.com",
            };
            isRoot = new boolean[] {
                true,
                true,
                true,
                true,
                
                true, // 5
                true,
                true,
                true,
                true,
                true, // 10
                true,
                true,
                true,
                true,
                true, // 15
                true,
                true,
                true,

                true,
                false,
                false,

                true,
                false
            };
        }
    }
}
