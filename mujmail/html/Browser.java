//#condition MUJMAIL_HTML
package mujmail.html;

/*
MujMail - Simple mail client for J2ME
Copyright (C) 2006 Nguyen Son Tung <n.sontung@gmail.com>
Copyright (C) 2008 David Hauzar <david.hauzar.mujmail@gmail.com>
 
This program is free software; you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation; either version 2 of the License, or
(at your option) any later version.
This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.
You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */

import java.util.Stack;
import java.util.Vector;

import javax.microedition.io.ConnectionNotFoundException;
import javax.microedition.lcdui.AlertType;
import javax.microedition.lcdui.Canvas;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Display;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Graphics;

import mujmail.MailForm;
import mujmail.MujMail;
import mujmail.html.Drawable.Point;

/**
 * <p>
 * In context of HTML drawing browser is really simple class, when parsing is
 * done and vector of {@link Drawable} objects is returned this class iterate
 * over all these elements and calls {@link Drawable#draw(Graphics, int, int)}
 * method.</p>
 * 
 * <p>
 * While Browser extends canvas, it's in fact Displayable and so it's used
 * to show the results. In Browser it is available to follow HTML links, in such
 * case the new HTML page is downloaded, parsed and shown. Browser manages
 * the event handling between different instances of Browser.
 * </p>
 * 
 * @author Betlista
 */
public class Browser extends Canvas implements CommandListener {

    private static final boolean DEBUG = false;

    /** Stack of Browser instances to enable link following. */
    private static final Stack/*Browser*/ browsers = new Stack();

    /** Number of links at the actual page */
    private int linksAtPage = 0;
    /** Represents index of active (selected) link in <u>page</u>. */
    private int activeLink = -1;
    /** Link (a href) source */
    private String link = null;

    /**
     * Is used to enable access to Displayable instance what is used for
     * example when text is displayed to get screen width, for correct line
     * breaking.
     */
    public static Browser getActualBrowser() {
        return (Browser)browsers.peek();
    }

    /** Contains elements (Drawables) returned from parsing. */
    private Vector/*Drawable*/ elements;

    private int startingX = 1;
    private int startingY = 1;

    private int lastY;

    /**
     * Constructor for Browser instance creation.
     * Side-efect of calling this constructor is that this new instance is added
     * to the stack of browsers - at the top.
     * 
     * @param elements vector of {@link Drawable Drawables}
     * @see #browsers - stack
     */
    public Browser( final Vector/*Drawable*/ elements ) {
        this.elements = elements;
        browsers.push( this );

        Command backCommand = new Command("back", Command.BACK, 0);
        addCommand( backCommand );
        setCommandListener( this );
    }

    public int increaseLinksNumber() {
          //System.out.println( "linksAtPage: " + linksAtPage );
        return linksAtPage++;
    }

    public int getActiveLink() {
        return activeLink;
    }

    public void setLink(String link) {
        this.link = link;
    }

    /**
     * Responsible for all elements painting.
     * Responsibility for correct drawing is delegated to the elements
     * implementing {@link Drawable} interface, the only one thing that method
     * is responsible for is that when element is drawn it returns the position
     * where the drawing ended and this position is consequently passed
     * as parameter to {@link Drawable#draw(Graphics, int, int)} method.
     */
    protected void paint(Graphics g) {
        linksAtPage = 0;
        g.setColor( 0xFFFFFF );
        g.fillRect(0, 0, getWidth(), getHeight());
        g.setColor( 0 );
        final int size = elements.size();
        Drawable drawable;
        Point point = null;
        if ( size > 0 ) {
            drawable = (Drawable)elements.elementAt( 0 );
              if (DEBUG) System.out.println( "DEBUG Browser.paint(Graphics) - startingX: " + startingX + ", startingY: " + startingY );
            point = drawable.draw(g, startingX, startingY);
        }
        for ( int i = 1; i < size; ++i ) {
            drawable = (Drawable)elements.elementAt( i );
              // point variable cannot be null here ;-)
              //   if size is >= 1, than it's > 0 so it was initialized in
              //   if statement above
            point = drawable.draw(g, point.x, point.y);
        }
        if ( point != null ) {
            lastY = point.y;
              if (DEBUG) System.out.println( "DEBUG Browser.paint(Graphics) - lastY: " + lastY );
        }
    }

    protected void keyPressed(int keyCode) {
          if (DEBUG) System.out.println( "DEBUG Browser.keyPressed(keyCode=" + keyCode + ")" );
        final int height = getHeight();
        final int gameAction = getGameAction(keyCode);
          if (DEBUG) System.out.println( "DEBUG Browser.keyPressed(int) - gameAction: " + gameAction );
        switch ( gameAction ) {
            case DOWN:
                if ( lastY > height ) {
                    startingY -= height;
                }
                activeLink = -1;
                repaint();
                break;
            case UP:
                if ( startingY < 0 ) {
                    startingY += height;
                }
                activeLink = -1;
                repaint();
                break;

            case RIGHT:
                  // if there are some links at page
                if ( linksAtPage > 0 ) {
                      // if the index is lower than the index of the last link
                    if ( activeLink < linksAtPage - 1 ) {
                        ++activeLink;
                    } else {
                          // otherwise move to first link
                        activeLink = 0;
                    }
                }
                  System.out.println( "linksAtPage: " + linksAtPage + ", activeLink: " + activeLink );
                repaint();
                break;
            case LEFT:
                if ( linksAtPage > 0 ) {
                    if ( activeLink <= 0 ) {
                        activeLink = linksAtPage - 1;
                    } else {
                        --activeLink;
                    }
                }
                  System.out.println( "linksAtPage: " + linksAtPage + ", activeLink: " + activeLink );
                repaint();
                break;
            case FIRE:
                try {
                      System.out.println( "fire, link='" + link + "'" );
                    MujMail.mujmail.platformRequest( link );
                } catch (ConnectionNotFoundException cnfe ) {
                    MujMail.mujmail.alert.setAlert("Unable to visit the link '" + link + "'", AlertType.ERROR );
                    cnfe.printStackTrace();
                }
                break;
        }
    }

    /* **************************
     *     interface methods    *
     ****************************/

    /*
     * (non-Javadoc)
     * @see javax.microedition.lcdui.CommandListener#commandAction(javax.microedition.lcdui.Command, javax.microedition.lcdui.Displayable)
     */
    public void commandAction(Command c, Displayable d) {
          if (DEBUG) System.out.println( "DEBUG Browser.commandAction(Command, Displayable)" );
        if ( d == this ) {
            if ( c.getCommandType() == Command.BACK ) {
                  if (DEBUG) {
                      System.out.println( "DEBUG Browser.commandAction(Command, Displayable) - back command" );
                      System.out.println( "DEBUG Browser.commandAction(Command, Displayable) - number of browsers: " + browsers.size() );
                  }
                if ( browsers.size() > 1 ) {
                      if (DEBUG) System.out.println( "DEBUG Browser.commandAction(Command, Displayable) - moving to previous browser" );
                    Display display = Display.getDisplay( MujMail.mujmail );
                    display.setCurrent( (Displayable)browsers.pop() );
                } else {
                      if (DEBUG) System.out.println( "DEBUG Browser.commandAction(Command, Displayable) - moving back to mail form" );
                    browsers.pop();
                    MujMail.mujmail.mailForm.setContext( MailForm.MODE_LIST );
                    MujMail.mujmail.mailForm.showPreviousScreen();
                }
            }
        }
    }
}
