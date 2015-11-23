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

import javax.microedition.lcdui.Canvas;

/**
 * Calls appropriate methods on listener when a mujMail pointer event occurs.
 * 
 * Transforms java pointer events to mujMail pointer events, calls method
 * {@link mujmail.pointer.MujMailPointerEvent#handleEvent(mujmail.pointer.MujMailPointerEventListener)}
 * on the event. This method hereafter should call appropriate methods on
 * listener.
 *
 * User of this class must ensure calling of methods corresponding to java pointer
 * events (pointerPressed etc.) on this object.
 * 
 * @see MujMailPointerEvent
 * 
 * @author David Hauzar
 */
public class MujMailPointerEventProducer {
    private final MujMailPointerEventListener listener;
    private final int screenWidth;
    private final int screenHeight;

    /**
     * Creates new instance of MujMailPointerEventProducer.
     * 
     * @param listener the object that's methods will be called when mujmail
     *  event occurrs
     * @param screenWidth the width of the screen of the display of the phone
     * @param screenHeight the height of the screen of the display of the phone
     */
    public MujMailPointerEventProducer(MujMailPointerEventListener listener, int screenWidth, int screenHeight) {
        this.listener = listener;
        this.screenHeight = screenHeight;
        this.screenWidth = screenWidth;
    }
    
    /**
     * Transforms pointer pressed java event to mujmail pointer event and handle
     * this event. Typically calls appropriate method on listener.
     * 
     * This method must be called by owner by this object when pointerPressed
     * java pointer event occurrs.
     * @param x the x coordinate of the pointer
     * @param y the y coordinate of the pointer
     * 
     * @see Canvas#pointerPressed
     */
    public void pointerPressed(int x, int y) {
        transformPointerPressed(x, y).handleEvent(listener);
    }
    
    private boolean isOnLeftSide(int x) {
        if (x <= screenWidth / 4) {
            return true;
        } else {
            return false;
        }
    }
    
    private boolean isOnRightSide(int x) {
        if (x >= (screenWidth / 4)*3) {
            return true;
        } else {
            return false;
        }
    }
    
    private boolean isDown(int y) {
        if (y >= (screenHeight / 4)*3) {
            return true;
        } else {
            return false;
        }
    }
    
    private boolean isUP(int y) {
        if (y <= screenHeight / 4) {
            return true;
        } else {
            return false;
        }
    }
    
    /**
     * Transforms pointer pressed java event to mujmail pointer event.
     * 
     * @param x the x coordinate of the pointer.
     * @param y the y coordinate of the pointer.
     * @return mujmail event transformed from pointer pressed event.
     */
    private MujMailPointerEvent transformPointerPressed(int x, int y) {
        if (isOnLeftSide(x) && !isDown(y) && !isUP(y)) {
            // on the left but not in the corners
            return MujMailPointerEvent.LEFT;
        }
        
        if (isOnRightSide(x) && !isDown(y) && !isUP(y)) {
            // on the right but not in the corners
            return MujMailPointerEvent.RIGHT;
        }
        
        if (isUP(y) && !isOnLeftSide(x) && !isOnRightSide(x)) {
            // up but not in the corners
            System.out.println("up");
            return MujMailPointerEvent.UP;
        }
        
        if (isDown(y) && !isOnLeftSide(x) && !isOnRightSide(x)) {
            // down but not in the corners
            return MujMailPointerEvent.DOWN;
        }
        
        if (!isDown(y) && !isUP(y) && !isOnLeftSide(x) && !isOnRightSide(x)) {
            // on the center
            return MujMailPointerEvent.FIRE;
        }
        
        
        if (isDown(y) && (isOnLeftSide(x) || isOnRightSide(x)) ) {
            // bottom corners
            return MujMailPointerEvent.DOWN_QUARTERS_STAR;
        }
        
        if (isUP(y) && (isOnLeftSide(x) || isOnRightSide(x)) ) {
            // up corners
            return MujMailPointerEvent.UP_QUARTERS_SLASH;
        }
        
        return MujMailPointerEvent.NO_ACTION;
    }
}
