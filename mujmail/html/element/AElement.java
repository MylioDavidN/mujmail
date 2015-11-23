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

import mujmail.html.Drawable;

/**
 * Represents abstract class, parent for all elements. It doesn't represent
 * &lt;a&gt; tag/element.
 * 
 * @author Betlista
 */
public abstract class AElement implements Drawable {

      // constants representing if the element is first/last one (from paired
      //   element), to avoid discussions about if the unpaired elements are the
      //   same as the start or ending tags, there is separate constant
    /** Represents that the element is opening/start/first element */
    public static final int START  = 1;
    /** Represents that the element is closing/end/last element */
    public static final int END    = 2;
    /** Represents that the element is unpaired (not opening nor closing) */
    public static final int UNPAIR = 3;

    /**
     * Represents type of element in meaning of following possibilities:
     * <ul>
     * <li>{@link #START}</li>
     * <li>{@link #END}</li>
     * <li>{@link #UNPAIR}</li>
     * </ul>
     */
    protected int type;

    /**
     * Represents tag name of the element.
     * Exception is text element - it's name is "text".
     */
    protected String name;

    /**
     * Constructor that requires the name of the element.
     * Type of the element is set to {@link #START}.
     * 
     * @param name element name (see {@link #name})
     */
    public AElement(String name) {
        this.name = name;
        this.type = START;
    }

    /**
     * Constructor that enables programmer to define name and type without
     * calling {@link #setType(int)}.
     *  
     * @param name element name (see {@link #name})
     * @param type element type (see {@link #type})
     */
    public AElement(String name, int type) {
        this(name);
        this.type = type;
    }

    /**
     * Getter for type.
     * 
     * @return element type
     * @see #type
     */
    public int getType() {
        return type;
    }

    /**
     * Setter for type.
     * 
     * @param type new type
     * @see #type
     */
    public void setType(int type) {
        this.type = type;
    }

    /**
     * Getter for name.
     * Note: {@link #name} field have no setter because is set in constructor
     * and cannot be changed.
     * 
     * @return element name
     * @see #name
     */
    public String getName() {
        return name;
    }

}
