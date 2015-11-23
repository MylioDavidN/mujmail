/*
MujMail - Simple mail client for J2ME
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

package mujmail.util;

/**
 * The interface used when the calling is assynchronous (some action is started 
 * in new thread) and callback method is needed.
 * Caller passes the object implementing this interface to asynchronously called 
 * object and this object later invokes the method callback() of this interface.
 * 
 * @author David Hauzar
 */
public interface Callback {
    /**
     * The method which is invoked by (asynchronously) called object.
     * 
     * @param called the object which is called.
     * @param message the object which can hold the message from called object;
     *  null when no message is needed
     */
    public void callback(Object called, Object message);
    
    
    /** This object should be passed if no callback is needed. */
    public static final Callback NO_ACTION = new NoAction();
    
    /**
     * The class with implementation of callback method that does nothing.
     */
    static class NoAction implements Callback {
        /** It is not possible to make any instance of this class or make 
         descendant of this class outside this interface. */
        private NoAction() {};
        
        public void callback(Object called, Object message) {
            // no action
        }
        
    }
}
