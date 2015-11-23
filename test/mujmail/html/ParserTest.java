package test.mujmail.html;

import j2meunit.framework.Test;
import j2meunit.framework.TestCase;
import j2meunit.framework.TestMethod;
import j2meunit.framework.TestSuite;

import java.util.Hashtable;
import java.util.Vector;

import mujmail.html.Parser;
import mujmail.html.element.AHrefElement;
import mujmail.html.element.BElement;
import mujmail.html.element.BRElement;
import mujmail.html.element.IElement;
import mujmail.html.element.TextElement;
import mujmail.html.element.UElement;

public class ParserTest extends TestCase {

    public ParserTest() {
    }

    public ParserTest(String name, TestMethod testMethod) {
        super(name, testMethod);
    }

    public static Vector testNames = new Vector();
    public static Hashtable testResults = new Hashtable();

    public void testEmptyHTML() {
        new EmptyHTMLTestData("testEmptyHTML").test();
    }

    public void testEmptyHTMLWithSpaces() {
        new EmptyHTMLWithSpacesTestData("testEmptyHTMLWithSpaces").test();
    }

    public void testEmptyHTMLWithNewLines() {
        new EmptyHTMLWithNewLinesTestData("testEmptyHTMLWithNewLines").test();
    }

    public void testBreakElement() {
        new BreakElementTestData("testBreakElement").test();
    }

    public void testFormattedText() {
        new FormatedTextTestData("testFormattedText").test();
    }

    public void testLinks() {
        new LinkTestData("testLinks").test();
    }

    public void testLongLine() {
        new LongLineTestData("testLongLine").test();
    }



    public Test suite() {
        TestSuite suite = new TestSuite();

        suite.addTest( new ParserTest("testEmptyHTML", new TestMethod() {
            public void run(TestCase testCase) throws Throwable {
                ((ParserTest)testCase).testEmptyHTML();
            }
        }) );

        suite.addTest( new ParserTest("testEmptyHTMLWithSpaces", new TestMethod() {
            public void run(TestCase testCase) throws Throwable {
                ((ParserTest)testCase).testEmptyHTMLWithSpaces();
            }
        }) );

        suite.addTest( new ParserTest("testEmptyHTMLWithNewLines", new TestMethod() {
            public void run(TestCase testCase) throws Throwable {
                ((ParserTest)testCase).testEmptyHTMLWithNewLines();
            }
        }) );

        suite.addTest( new ParserTest("testBreakElement", new TestMethod() {
            public void run(TestCase testCase) throws Throwable {
                ((ParserTest)testCase).testBreakElement();
            }
        }) );

        suite.addTest( new ParserTest("testFormattedText", new TestMethod() {
            public void run(TestCase testCase) throws Throwable {
                ((ParserTest)testCase).testFormattedText();
            }
        }) );

        suite.addTest( new ParserTest("testLinks", new TestMethod() {
            public void run(TestCase testCase) throws Throwable {
                ((ParserTest)testCase).testLinks();
            }
        }) );

        suite.addTest( new ParserTest("testLongLine", new TestMethod() {
            public void run(TestCase testCase) throws Throwable {
                ((ParserTest)testCase).testLongLine();
            }
        }) );

        return suite;
    }

    abstract class TestData {
        String testName;
        String html;
        Vector expectedElemetns = new Vector();
        
        public TestData( final String testName ) {
            this.testName = testName;
            testNames.addElement( testName );
        }

        abstract void prepareExpected();

        void test() {
            try {
                prepareExpected();
                Parser parser = new Parser( html );
                Vector elements = parser.parse();
                assertEquals(expectedElemetns.size(), elements.size());
    
                Object expectedElement;
                Object parsedElement;
                for ( int i = 0; i < expectedElemetns.size(); ++i ) {
                    expectedElement = expectedElemetns.elementAt( i );
                    parsedElement = elements.elementAt( i );
    
                    assertEquals( expectedElement.getClass(), parsedElement.getClass() );
    
                    if ( expectedElement instanceof TextElement ) {
                        TextElement expectedText = (TextElement)expectedElement;
                        TextElement parsedText = (TextElement)parsedElement;
    
                        assertEquals( expectedText.getText(), parsedText.getText() );
                    }
                }
                ParserTest.testResults.put( testName, elements);
            } catch (Throwable t) {
                t.printStackTrace();
                
            }
        }
    }

    class EmptyHTMLTestData extends TestData {
        public EmptyHTMLTestData( final String testName ) {
            super( testName );
            StringBuffer buff = new StringBuffer();
            buff.append("<html><body></body></html>");
            html = buff.toString();
        }
        void prepareExpected() {
        }        
    }

    class EmptyHTMLWithSpacesTestData extends TestData {
        public EmptyHTMLWithSpacesTestData( final String testName ) {
            super( testName );
            StringBuffer buff = new StringBuffer();
            buff.append("<html> <body>  </body> </html>");
            html = buff.toString();
        }
        void prepareExpected() {
            expectedElemetns.addElement( new TextElement(" ") );
        }        
    }

    class EmptyHTMLWithNewLinesTestData extends TestData {
        public EmptyHTMLWithNewLinesTestData( final String testName ) {
            super( testName );
            StringBuffer buff = new StringBuffer();
            buff.append("<html>\n")
                .append("<body>\n")
                .append("</body>\n")
                .append("</html>");
            html = buff.toString();
        }
        void prepareExpected() {
            expectedElemetns.addElement( new TextElement(" ") );
        }        
    }

    class BreakElementTestData extends TestData {
        public BreakElementTestData( final String testName ) {
            super( testName );
            StringBuffer buff = new StringBuffer();
            buff.append("<html><body>first line<br>next line</body></html>");
            html = buff.toString();
        }
        void prepareExpected() {
            expectedElemetns.addElement( new TextElement("first line") );
            expectedElemetns.addElement( new BRElement() );
            expectedElemetns.addElement( new TextElement("next line") );
        }        
    }

    class FormatedTextTestData extends TestData {
        public FormatedTextTestData( final String testName ) {
            super( testName );
            StringBuffer buff = new StringBuffer();
            
            buff.append("<html>\n")
                .append("\t<body>\n")
                .append("\t\t<b>bold</b><br>\n")
                .append("\t\t<u>underlined</u><br>\n")
                .append("\t\t<i>italics</i><br>\n")
                .append("\t</body>\n")
                .append("</html>")
                ;

            html = buff.toString();
        }
        void prepareExpected() {
            expectedElemetns.addElement( new TextElement(" ") );

            expectedElemetns.addElement( new BElement() );
            expectedElemetns.addElement( new TextElement("bold") );
            expectedElemetns.addElement( new BElement() );
            expectedElemetns.addElement( new BRElement() );

            expectedElemetns.addElement( new TextElement(" ") );
            expectedElemetns.addElement( new UElement() );
            expectedElemetns.addElement( new TextElement("underlined") );
            expectedElemetns.addElement( new UElement() );
            expectedElemetns.addElement( new BRElement() );

            expectedElemetns.addElement( new TextElement(" ") );
            expectedElemetns.addElement( new IElement() );
            expectedElemetns.addElement( new TextElement("italics") );
            expectedElemetns.addElement( new IElement() );
            expectedElemetns.addElement( new BRElement() );

            expectedElemetns.addElement( new TextElement(" ") );
        }
    }

    class LinkTestData extends TestData {
        public LinkTestData( final String testName ) {
            super( testName );
            StringBuffer buff = new StringBuffer();
            buff.append("<a href=mujmail.org1>mujmail.org - 1</a><br>")
                .append("<a href=\"mujmail.org2\">mujmail.org - 2</a><br>")
                .append("<a href='mujmail.org3'>mujmail.org - 3</a><br>")
                ;
            html = buff.toString();
        }
        void prepareExpected() {
            AHrefElement link = new AHrefElement();
            link.setHref( "mujmail.org1" );
            expectedElemetns.addElement( link );
            expectedElemetns.addElement( new TextElement("mujmail.org - 1") );
            expectedElemetns.addElement( new AHrefElement() );
            expectedElemetns.addElement( new BRElement() );

            link = new AHrefElement();
            link.setHref( "mujmail.org2" );
            expectedElemetns.addElement( link );
            expectedElemetns.addElement( new TextElement("mujmail.org - 2") );
            expectedElemetns.addElement( new AHrefElement() );
            expectedElemetns.addElement( new BRElement() );

            link = new AHrefElement();
            link.setHref( "mujmail.org3" );
            expectedElemetns.addElement( link );
            expectedElemetns.addElement( new TextElement("mujmail.org - 3") );
            expectedElemetns.addElement( new AHrefElement() );
            expectedElemetns.addElement( new BRElement() );
        }
    }

    class LongLineTestData extends TestData {
        public LongLineTestData( final String testName ) {
            super(testName);
            StringBuffer buff = new StringBuffer();
            buff.append( "<html>" )
                .append( "<body>" )
                .append( "really, really really long long long long long long long line in HTML<br>" )
//                .append( "and" )
                .append( "and AnotherLongWordButNotContainingAnyCharacterToBeSplitAt" )
                .append( "</body>" )
                .append( "</html>" )
                ;
            html = buff.toString();
        }
        void prepareExpected() {
            expectedElemetns.addElement( new TextElement("really, really really long long long long long long long line in HTML") );
            expectedElemetns.addElement( new BRElement() );
//            expectedElemetns.addElement( new TextElement("and") );
            expectedElemetns.addElement( new TextElement("and AnotherLongWordButNotContainingAnyCharacterToBeSplitAt") );
        }
    }
}
