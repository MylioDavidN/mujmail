//#condition MUJMAIL_TOUCH_SCR
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
 * Strategy enumeration class containing all possible events that are transformed
 * from pointer events produced by java by MujMailPointerEventTransformer.
 * 
 * @author David Hauzar
 */
abstract class MujMailPointerEvent {
    private MujMailPointerEvent() {};
    
    /**
     * Handles this pointer event.
     * Typycally calls appropriate method on listener.
     * @param listener object listening for this event.
     */
    abstract void handleEvent(MujMailPointerEventListener listener);

    ///////////////////////////////////////
    // Events used to emulate keyboard.
    //////////////////////////////////////
    /** Event corresponding up key on keypad. */
    static final MujMailPointerEvent UP = new UP();
    /** Event corresponding down key on keypad. */
    static final MujMailPointerEvent DOWN = new DOWN();
    /** Event corresponding left key on keypad. */
    static final MujMailPointerEvent LEFT = new LEFT();
    /** Event corresponding right key on keypad. */
    static final MujMailPointerEvent RIGHT = new RIGHT();
    /** Event corresponding fire (enter) key on keypad. */
    static final MujMailPointerEvent FIRE = new FIRE();
    /** Event corresponding downQuartersStar key on keypad. */
    static final MujMailPointerEvent DOWN_QUARTERS_STAR = new DOWN_QUARTERS_STAR();
    /** Event corresponding upQuartersSlash key on keypad. */
    static final MujMailPointerEvent UP_QUARTERS_SLASH = new UP_QUARTERS_SLASH();
    /** Dummy event corresponding no action. */
    static final MujMailPointerEvent NO_ACTION = new NoAction();
    
    
    private static class NoAction extends MujMailPointerEvent {
        private NoAction() {};

        public void handleEvent(MujMailPointerEventListener listener) {
        }
    }
    private static class UP extends MujMailPointerEvent {
        private UP() {};

        public void handleEvent(MujMailPointerEventListener listener) {
            listener.up();
        }
    }
    private static class DOWN extends MujMailPointerEvent {
        private DOWN() {};

        public void handleEvent(MujMailPointerEventListener listener) {
            listener.down();
        }
    }
    private static class LEFT extends MujMailPointerEvent {
        private LEFT() {};

        public void handleEvent(MujMailPointerEventListener listener) {
            listener.left();
        }
    }
    private static class RIGHT extends MujMailPointerEvent {
        private RIGHT() {};

        public void handleEvent(MujMailPointerEventListener listener) {
            listener.right();
        }
    }
    private static class FIRE extends MujMailPointerEvent {
        private FIRE() {};

        public void handleEvent(MujMailPointerEventListener listener) {
            listener.fire();
        }
    }
    private static class DOWN_QUARTERS_STAR extends MujMailPointerEvent {
        private DOWN_QUARTERS_STAR() {};

        public void handleEvent(MujMailPointerEventListener listener) {
            listener.downQuartersStar();
        }
    }
    private static class UP_QUARTERS_SLASH extends MujMailPointerEvent {
        private UP_QUARTERS_SLASH() {};

        public void handleEvent(MujMailPointerEventListener listener) {
            listener.upQuartersSlash();
        }
    }
    

}
