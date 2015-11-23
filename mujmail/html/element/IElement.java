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

/**
 * Represents &lt;i&gt; tag.
 * 
 * @author Betlista
 */
public class IElement extends AElement {

    public IElement() {
        super("i");
    }

    public Point draw(Graphics g, int x, int y) {
        Font f = g.getFont();
        Font newFont;
        if ( type == START ) {
            newFont = Font.getFont(f.getFace(), f.getStyle() | Font.STYLE_ITALIC, f.getSize());
        } else {
            newFont = Font.getFont(f.getFace(), f.getStyle() ^ Font.STYLE_ITALIC, f.getSize());
        }
        g.setFont( newFont );
        return new Point(x, y);
    }

}
