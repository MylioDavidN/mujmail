/*
MujMail - Simple mail client for J2ME
Copyright (C) 2009 David Hauzar <david.hauzar.mujmail@gmail.com>

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
 * Analogous to java.util.Observer from JSE. Javadoc comments are copyed from
 * original implementation in JSE.
 *
 * A class can implement the <code>Observer</code> interface when it wants to be
 * informed of changes in observable objects.
 *
 * @author David Hauzar
 */
public interface Observer {
    /**
     * This method is called whenever the observed object is changed. An
     * application calls an <code>Observable</code> object's
     * <code>notifyObservers</code> method to have all the object's observers
     * notified of the change.
     *
     * @param o the observable object.
     * @param arg an argument passed to the <code>notifyObservers</code>  method.
     */
    public void update(Observable o, Object arg);
}
