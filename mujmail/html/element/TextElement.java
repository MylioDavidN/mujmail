//#condition MUJMAIL_HTML
package mujmail.html.element;

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

import javax.microedition.lcdui.Font;
import javax.microedition.lcdui.Graphics;

import mujmail.html.Browser;

/**
 * Represents text in HTML.
 * 
 * @author Betlista
 */
public class TextElement extends AElement {

    /** Flag determines whether we want to print debug prints to console. */
    private static final boolean DEBUG = false;

    /**
     * Contains text
     * 
     * @see #TextElement(String) for initial manipulation
     */
	private String text;

	/**
	 * Constructor for this class.
	 * Before storing text to instance, all multiple white spaces are replaced
	 * with one white space - space. Moreover the tabs, CR and LF are replaced
	 * with space too.
	 * 
	 * @param text without multiple white spaces in row
	 */
	public TextElement( final String text ) {
	    super(text, AElement.UNPAIR);
	    final StringBuffer buff = new StringBuffer();
	    char c;
	    boolean firstWhiteSpace = true;
	    for ( int i = 0; i < text.length(); ++i ) {
	        c = text.charAt(i);
	        if ( c == ' ' || c == '\n' || c == '\r' || c == '\t') {
	            if ( firstWhiteSpace ) {
	                firstWhiteSpace = false;
	                buff.append( c );
	            }
	        } else {
	            buff.append( c );
	            firstWhiteSpace = true;
	        }
	    }
		this.text = buff.toString();
	}

	/** 
	 * Getter for text.
	 * 
	 * @return text in element
	 * @see #text
	 */
    public String getText() {
        return text;
    }

	/* *************************
	 *    interface methods    *
	 ***************************/

    /**
     * <p>Draws the element.</p>
     * 
     * <p>
     * This is the most complicated draw method from all descendants
     * of {@link AElement}.<br>
     * Method, using font size - concretely width for some string and height
     * (=line height), draws the text:
     * <ul>
     * <li>when text has lower width as screen width it's OK and text is drawn
     * at the box</li>
     * <li>if not, the character where it could be split is found
     * {@link #findIndex(StringBuffer)} and string is split</li>
     * <li>in last case when there is no character to split string, it's split
     * at the position that fine for line or it's moved to next line</li>
     * </ul>
     * </p>
     * 
     * @param g used for drawing
     * @param x horizontal coordinate where to start drawing
     * @param x vertical coordinate where to start drawing
     */
    public Point draw( final Graphics g, final int x, final int y) {
        final Font font = g.getFont();
        final Browser browser = Browser.getActualBrowser();
        int screenWidth = browser.getWidth();
        int stringWidth = font.stringWidth( text );

        int newx = x;
        int newy = y;

        StringBuffer remaining = new StringBuffer( text );
        int index;
        char[] firstChars;
        int firstCharsWidth;
          if (DEBUG) System.out.println( "DEBUG TextElement.draw(...) - newx: " + newx + ", newy: " + newy + "stringWidth: " + stringWidth + ", screenWidth: " + screenWidth );
        while ( newx + stringWidth > screenWidth - 1) {
            // find the position where to split
            index = findIndex( remaining ) + 1;
            firstChars = new char[index];
            remaining.getChars(0, index, firstChars, 0);
              if (DEBUG) System.out.println( "DEBUG TextElement.draw(...) - firstChars: '" + new String(firstChars) + "'");
            firstCharsWidth = font.charsWidth( firstChars, 0, index);
              if (DEBUG) System.out.println( "DEBUG TextElement.draw(...) - firstCharsWidth: " + firstCharsWidth);
            if ( newx + firstCharsWidth <= screenWidth - 1 ) {
                g.drawChars( firstChars, 0, index, newx, newy, Graphics.LEFT | Graphics.TOP );
                newx += firstCharsWidth;
            } else {
                if ( firstCharsWidth <= screenWidth - 1 ) {
                    newy += font.getHeight();
                    g.drawChars( firstChars, 0, index, 1, newy, Graphics.LEFT | Graphics.TOP );
                    newx = firstCharsWidth;
                } else {
                    char c;
                    int charWidth;
                    while ( newx + firstCharsWidth >= screenWidth - 1 ) {
                        --index;
                        c = firstChars[index];
                        charWidth = font.charWidth( c );
                        firstCharsWidth -= charWidth;
                    }
                    g.drawChars( firstChars, 0, index, newx, newy, Graphics.LEFT | Graphics.TOP );
                    newx = 1;
                    newy += font.getHeight();
                }
            }
            remaining.delete( 0, index );
              if (DEBUG) System.out.println( "DEBUG TextElement.draw(...) - remaining: '" + remaining + "'");
            stringWidth = font.stringWidth( remaining.toString() );
              if (DEBUG) System.out.println( "DEBUG TextElement.draw(...) - newx: " + newx + ", stringWidth: " + stringWidth + ", screenWidth: " + screenWidth );
        }
        g.drawString( remaining.toString(), newx, newy, Graphics.LEFT | Graphics.TOP );
        newx += stringWidth;
		return new Point(newx, newy);
	}

    /**
     * Finds the first position in string where the string could be split.
     * Characters where the text could be split are - ' ', ',', '-', '/', ';', ':'.
     * 
     * @param text
     * @return
     */
    private int findIndex( StringBuffer text ) {
        char c;
        for ( int i = 0; i < text.length(); ++i ) {
            c = text.charAt( i );
            switch (c) {
                case ' ':
                case ',':
                case '-':
                case '.':
                case '/':
                case ';':
                case ':':
                    return i;
                default: continue;
            }
        }
        return text.length() - 1;
    }

    /* **********************
     *    Object methods    *
     ************************/
    //#ifdef MUJMAIL_DEVELOPMENT
//#     public String toString() {
//#         StringBuffer buff = new StringBuffer("Text[");
//#         buff.append("text='").append( text ).append( "']")
//#             ;
//#         return buff.toString();
//#     }
    //#endif
}
