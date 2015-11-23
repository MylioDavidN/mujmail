/*
MujMail - Simple mail client for J2ME
Copyright (C) 2008 Jan Gregor

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

package mujmail.util;

/**
 * Represents an iterator.
 * @author Jan Gregor
 */
public interface Iterator {
    /**
     * Gets true if this iterator has next element.
     * @return true if this iterator has next element.
     */
    public boolean hasNext();
    /**
     * Next element in this iterator.
     * @return next element in this iterator.
     */
    public Object next();
}
