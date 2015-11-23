//#condition MUJMAIL_FS
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

import mujmail.util.StartupModes;
import mujmail.*;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.TextBox;

/**
 * Extends the functionality of TextBox with the possibility of starting in new
 * thread.
 * 
 * @see TextInputDialog
 * 
 * @author David Hauzar
 */
public class MyTextBox extends TextBox {
    private Displayable tempDisplayable;
    
    /**
     * Creates a new TextBox object with the given title string, initial 
     * contents, maximum size in characters, and constraints. If the text 
     * parameter is null, the TextBox is created empty. The maxSize parameter 
     * must be greater than zero. An IllegalArgumentException is thrown if the 
     * length of the initial contents string exceeds maxSize. However, the 
     * implementation may assign a maximum size smaller than the application had 
     * requested. If this occurs, and if the length of the contents exceeds the 
     * newly assigned maximum size, the contents are truncated from the end in 
     * order to fit, and no exception is thrown.
     * 
     * @param title the title text to be shown with the display
     * @param text the initial contents of the text editing area, null may be 
     *  used to indicate no initial content
     * @param maxSize the maximum capacity in characters. The implementation may limit 
     *  boundary maximum capacity and the actually assigned capacity may me 
     *  smaller than requested. A defensive application will test the actually 
     *  given capacity with getMaxSize().
     * @param constraints @see TextField.constraints
     * 
     * @throws IllegalArgumentException if maxSize is zero or less
     * @throws IllegalArgumentException if the constraints  parameter is invalid
     * @throws IllegalArgumentException if text is illegal for the specified 
     *  constraints
     * @throws IllegalArgumentException - if the length of the string exceeds the requested maximum capacity


     */
    public MyTextBox(String title, String text, int maxSize, int constraints) {
        super(title, text, maxSize, constraints);
    }
    
    /**
     * Starts the displaying of the text box.
     * @param runMode startup mode
     */
    public void start(StartupModes runMode) {
        if (runMode == StartupModes.IN_THE_SAME_THREAD) {
            MujMail.mujmail.getDisplay().setCurrent(this);
        } else {
            this.tempDisplayable = this;
            // start in the new thread
            Runnable r = new Runnable() {

                public void run() {
                    MujMail.mujmail.getDisplay().setCurrent(tempDisplayable);
                }
            };
            Thread t = new Thread(r);
            t.start();
        }
        
    }

}
