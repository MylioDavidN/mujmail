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

import javax.microedition.lcdui.Graphics;

import mujmail.html.Browser;

/**
 * Represents &lt;hr&gt; tag - horizontal line.
 * 
 * @author Betlista
 */
public class HRElement extends AElement {

    /**
     * Constructor that simply calls super("hr");
     * 
     * @see AElement#AElement(String) ancestor constructor
     */
    public HRElement() {
        super("hr");
    }

    /**
     * Paints line.<br>
     * If position is not at the beginning of the screen (left border), than it
     * move position to next line and at the beginning of the line and prints
     * line.
     */
    public Point draw(Graphics g, int x, int y) {
          //System.out.println( "hr: x=" + x + ", y=" + y );
        Point point = new Point(x, y + 1);
        if (x > 1) {
            point.y += g.getFont().getHeight();
            point.x = 1;
        }
        final Browser browser = Browser.getActualBrowser();
        final int width = browser.getWidth();
        g.drawLine(point.x, point.y, point.x + width - 2, point.y);
        point.y++;
        return point;
    }

}
