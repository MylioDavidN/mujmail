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

package mujmail.ui;

import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Displayable;
import mujmail.MujMail;
import mujmail.util.StartupModes;

/**
 * Class for creating user interface elements.
 *
 * Represents one screen. It assumes model, view and controller in one class so
 * it is good for not so complex screens with no so complex models and controllers.
 * For complex ones, use rather MVC design pattern.
 *  
 * @author David Hauzar
 */
public abstract class MVCComponent implements CommandListener {
    private boolean initialized = false;
    
    /**
     * Gets the display that should be used to paint to the screen.
     * @return the display that should be used to paint to the screen.
     */
    protected MujMail.MyDisplay getDisplay() {
        return MujMail.mujmail.getDisplay();
    }
    
    /**
     * Gets displayable screen.
     * @return the screen that should be displayed.
     * @throws java.lang.Exception
     */
    private Displayable getScreen() {
        if (!initialized) {
            initModel();
            createView();
            initialized = true;
        } else {
            updateView();
        }
        Displayable view = getView();
        view.setCommandListener(this);
        
        return view;
    }

    /**
     * Shows screen of this user interface element.
     * @param startupMode the mode in that this method should be executed.
     */
    public void showScreen(StartupModes startupMode) {
        if (startupMode == StartupModes.IN_THE_SAME_THREAD) {
            showScreen();
        } else {
            // start in the new thread
            Thread t = new Thread() {

                public void run() {
                    showScreen();
                }
            };
            t.start();
        }
    }
    
    
    /**
     * Shows screen of this element.
     */
    public void showScreen() {
        try {
            getDisplay().setCurrent(getScreen());
        } catch (Exception e) {
            e.printStackTrace();
            // TODO: show allert
        }
    }
    
    
    /**
     * Gets the screen object.
     * Before first calling of this method, method {@link mujmail.ui.MVCComponent#createView()}.
     * will is always called. Before furthre calling of this method, method
     * {@link mujmail.ui.MVCComponent#updateView() } is called.
     *
     * @return the screen object.
     */
    protected abstract Displayable getView();
    /**
     * Initializes the model. This method is called before first calling of
     * method {@link mujmail.ui.MVCComponent#getView() }
     */
    protected abstract void initModel();
    /**
     * Creates the view. This method is called before first calling of method
     * {@link mujmail.ui.MVCComponent#getView() } and after calling of method
     * {@link mujmail.ui.MVCComponent#initModel() }.
     */
    protected abstract void createView();
    /**
     * Updates the view. Called in method {@link mujmail.ui.MVCComponent#showScreen() }
     * before calling of method getView when this calling is not first time.
     */
    protected abstract void updateView();
    

}
