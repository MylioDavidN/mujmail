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

package mujmail.tasks;

import javax.microedition.lcdui.Displayable;
import mujmail.MujMail;

/**
 * Contains information about screen
 * @author David Hauzar
 */
class ScreenContainer {
    private boolean setScreenCalled = false;
    private Displayable screen = null;

    /**
     * Gets the screen that is contained in this container.
     * @return the screen that is contained in this container.
     */
    public Displayable getScreen() {
        return screen;
    }

    /**
     * Saves the screen in this container.
     * @param screen the screen to be saved in this container.
     */
    public void setScreen(Displayable screen) {
        setScreenCalled = true;
        this.screen = screen;
    }


    /**
     * Sets current screen to this container if the container was not seted by
     * method <code>setScreen</code>.
     */
    public void setCurrentScreenIfEmpty() {
        if (!setScreenCalled) {
            screen = MujMail.mujmail.getDisplay().getCurrent();
        }
    }

}
