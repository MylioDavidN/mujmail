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

import java.util.Vector;

import mujmail.html.element.AHrefElement;
import mujmail.html.element.AElement;
import mujmail.html.element.BElement;
import mujmail.html.element.BRElement;
import mujmail.html.element.HRElement;
import mujmail.html.element.IElement;
import mujmail.html.element.PElement;
import mujmail.html.element.TextElement;
import mujmail.html.element.UElement;

/**
 * Class represents HTML parser. It's implemented as state machine.
 * 
 * @author Betlista
 */
public class Parser {

    /** HTML source to be parsed */
    private String html;

    /** Creates new instance of parser with HTML to be parsed. */
    public Parser( final String html ) {
        this.html = html;
    }

    /**
     * Parses the HTML source passed to constructor and returns parsed elements.
     * 
     * @return parsed elements
     */
    public Vector parse() {
        final Vector result = new Vector();

        final char[] htmlChars = new char[html.length()];
        html.getChars(0, html.length(), htmlChars, 0);
        char c;
        StringBuffer text = new StringBuffer();
        for (int i = 0; i < html.length(); ++i) {
            c = htmlChars[i];
            switch (c) {
                case '<':
                    if ( text.length() > 0 ) {
                        result.addElement( new TextElement( text.toString() ) );
                        text.setLength( 0 );
                    }
                    i += processElement(i, htmlChars, result);
                    break;
                  // process white spaces, add to text just first one
                case ' ':
                case '\r':
                case '\n':
                case '\t':
                    text.append(' ');
                    break;
                default:
                    text.append(c);
            }
        }

        removeUnnecessaryElements( result );
        return result;
    }

    /**
	 * Gets stream (array of characters) and position in stream that marks where
	 * the '<' character is. Reads the element name and skips agruments
	 * to the closing sign '>' and depending on element name it adds new element
	 * to the vector of elements.
	 * 
	 * @param i actual position in stream
	 * @param htmlChars stream to be processed
	 * @return number of characters that were read from stream
	 */
    private int processElement(final int i, final char[] htmlChars, final Vector elements) {
        final StringBuffer elementName = new StringBuffer();
        // move to the next position
        int pos = i + 1;
        int elementType = AElement.START;
        if ( htmlChars[i + 1] == '/') {
            elementType = AElement.END;
            ++pos;
        }
        if (!isLetter(htmlChars[pos])) {
            elements.addElement( new TextElement("<") );
            return 1;
        }
        while (pos < htmlChars.length && isLetter(htmlChars[pos])) {
            if ( htmlChars[pos] != '/' ) {
                elementName.append(htmlChars[pos]);
            }
            ++pos;
        }
        AElement element = getElementForName(elementName.toString());
        if (element != null) {
            if ( !"br".equals( element.getName() ) && !"hr".equals( element.getName() ) ) {
                element.setType( elementType );
            }
            if ( "a".equals( element.getName() ) ) {
                AHrefElement link = (AHrefElement)element;
                if ( htmlChars[i + 1] != '/' ) {
                    pos += getHrefValue( pos, htmlChars, link );
                }
            }
            elements.addElement(element);
        }
        while (pos < htmlChars.length && htmlChars[pos] != '>') {
            // do nothing, just skip element arguments
            ++pos;
        }
        return pos - i;
    }

    /**
     * Returns <code>true</code> if character is letter - 'a'..'z', 'A'..'Z'.
     * 
     * @param c
     *            character to find if it is letter
     * @return <code>true</code> if is letter, <code>false</code> otherwise
     */
    private boolean isLetter(char c) {
//        return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || c == '/';
        return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z');
    }

    /**
     * Returns element instance or null for requested name.<br>
     * Recognized names are:
     * <ul>
     * <li>br</li>
     * <li>p</li>
     * </ul>
     * 
     * @param elementName
     * @return
     */
    private AElement getElementForName( final String elementName ) {
        final String lower = elementName.toLowerCase();
        if ( "a".equals(lower) ) {
            return new AHrefElement();
        } else if ( "b".equals(lower) ) {
            return new BElement();
        } else if ( "br".equals(lower) ) {
            return new BRElement();
        } else if ( "i".equals(lower) ) {
            return new IElement();
        } else if ( "p".equals(lower) ) {
            return new PElement();
        } else if ( "u".equals(lower) ) {
            return new UElement();
        } else if ( "hr".equals(lower) ) {
            return new HRElement();
        } else {
            return null;
        }
    }

    /**
     * Removes from vector of elements the unnecessary ones.
     * Element is considered unnecessary when there are text elements only
     * with white spaces.
     * 
     * @param elements in which looking for unnecessary elements
     */
    private void removeUnnecessaryElements( Vector elements ) {
        Object element;
        Object previousElement;
        for ( int i = elements.size() - 1; i >= 0; --i ) {
            element = elements.elementAt( i );
            if ( i > 0 && element instanceof TextElement ) {
                previousElement = elements.elementAt( i - 1 );
                if ( previousElement instanceof TextElement ) {
                    TextElement textElement = (TextElement)element;
                    TextElement previousTextElement = (TextElement)previousElement;

                    if ( " ".equals( textElement.getText() ) && " ".equals( previousTextElement.getText() ) ) {
                        elements.removeElementAt( i );
                    }
                }
            }
        }
    }

    /**
     * Parses a tag and sets reference (href) part to {@link AElement}.
     * 
     * @return number of skipped elements
     */
    private int getHrefValue( final int i, final char[] htmlChars, final AHrefElement element ) {
        final char[] copy = new char[ htmlChars.length - i ];
        System.arraycopy(htmlChars, i, copy, 0, copy.length);
        String html = new String( copy );
        String lower = html.toLowerCase();
        int index = lower.indexOf( "href" );
        if ( index == -1 ) {
            return 0;
        }
          // we found "href" part
        int valueStart = index + 5; // "href=".length is 5
        final char c = lower.charAt( valueStart );
        final int valueEnd;
        if ( c == '\'' || c == '"' ) {
            ++valueStart;
            valueEnd = lower.indexOf( c, valueStart );
        } else {
            final int spaceIndex = lower.indexOf( ' ', valueStart );
            final int bracketIndex = lower.indexOf( '>', valueStart );
            valueEnd = Math.min( spaceIndex, bracketIndex );
        }
        final String value = html.substring( valueStart, valueEnd );
        element.setHref( value );
        return valueEnd;
    }
}
