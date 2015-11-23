package test.mujmail.html;

import j2meunit.midletui.TestRunner;

import java.util.Vector;

import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Display;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Form;
import javax.microedition.lcdui.Item;
import javax.microedition.lcdui.ItemCommandListener;
import javax.microedition.lcdui.StringItem;
import javax.microedition.midlet.MIDletStateChangeException;

import mujmail.html.Browser;

public class HTMLDrawingTest extends TestRunner implements CommandListener, ItemCommandListener {

    private static final boolean DEBUG = false;

    Display display = Display.getDisplay(this);

    Browser browser;

    protected void startApp() throws MIDletStateChangeException {
        start(
            new String[] { 
                ParserTest.class.getName()
            }
        );
    }

    /**
     * Overwrites the method from parent to show different result screen
     * (not the j2meunit one).
     */
    public void showResult() {
        display.setCurrent( new ResultCanvas() );
    }

    /* *************************
     *    interface methods    *
     ***************************/

    /*
     * (non-Javadoc)
     * @see javax.microedition.lcdui.CommandListener#commandAction(javax.microedition.lcdui.Command, javax.microedition.lcdui.Displayable)
     */
    public void commandAction(Command c, Displayable d) {
        if (DEBUG) { System.out.println( "command: " + c + " displayable: " + d); }
        if ( Command.BACK == c.getCommandType() ) {
            if ( d == browser ) {
                display.setCurrent( new ResultCanvas() );
            }
        }
    }

    /*
     * (non-Javadoc)
     * @see javax.microedition.lcdui.ItemCommandListener#commandAction(javax.microedition.lcdui.Command, javax.microedition.lcdui.Item)
     */
    public void commandAction(Command c, Item item) {
          if (DEBUG) { System.out.println( "command: " + c + " item: " + item); }
        if ( item instanceof StringItem ) {
            final StringItem si = (StringItem)item;
            final String text = si.getLabel();
            browser = new Browser( (Vector)ParserTest.testResults.get(text) );
            browser.addCommand( new Command("back", Command.BACK, 0) );
            browser.setCommandListener( this );
            display.setCurrent( browser );
        }
    }

    /* *******************
     *    inner class    *
     *********************/

    /**
     * 
     * 
     * @author Betlista
     */
    class ResultCanvas extends Form {
        public ResultCanvas() {
            super("ResultCanvas");
//          addCommand( new Command("test", Command.OK, 10) );
//          setCommandListener( TestRunner.this );
            //append( new StringItem("test", "test") );
            final int size = ParserTest.testNames.size();
            String text;
            StringItem si;
            for (int i = 0; i < size; ++i) {
                text = (String)ParserTest.testNames.elementAt( i );
                si = new StringItem(text, "");
                si.setDefaultCommand( new Command("show", Command.ITEM, 0) );
                si.setItemCommandListener( HTMLDrawingTest.this );
                append( si );
            }
        }
    }
}
