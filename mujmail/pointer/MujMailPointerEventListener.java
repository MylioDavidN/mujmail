//condition MUJMAIL_TOUCH_SCR
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

package mujmail.pointer;

/**
 * The interface that should implement objects that listen to transformed mujmail
 * pointer events.
 * When object implementing this interface is passed to the constructor of the 
 * object of the class MujMailPointerEventProducer, that object calls appropriate 
 * method of this interface when some mujmail pointer event occurrs.
 * 
 * @author David Hauzar
 * @see MujMailPointerEventProducer
 */
public interface MujMailPointerEventListener {
    /**
     * Method called when event corresponding to pressing left key on keypad occurrs.
     */
    public void left();
    /**
     * Method called when event corresponding to pressing right key on keypad occurrs.
     */
    public void right();
    /**
     * Method called when event corresponding to pressing up key on keypad occurrs.
     */
    public void up();
    /**
     * Method called when event corresponding to pressing down key on keypad occurrs.
     */
    public void down();
    /**
     * Method called when event corresponding to pressing fire (enter) key on 
     * keypad occurrs.
     */
    public void fire();
    /**
     * Method called when event corresponding to pressing downQuartersStar key on keypad occurrs.
     */
    public void downQuartersStar();
    /**
     * Method called when event corresponding to pressing upQuartersSlash key on keypad occurrs.
     */
    public void upQuartersSlash();
    
    /**
     * Adapter class offering default implementations of methods from interface
     * MujMailPointerEventListener.
     */
    public static class MujMailPointerEventListenerAdapter implements MujMailPointerEventListener {

        public void left() {
        }

        public void right() {
        }

        public void down() {
        }

        public void up() {
        }

        public void fire() {
        }

        public void downQuartersStar() {
        }

        public void upQuartersSlash() {
        }

        
        
        
    }

}
