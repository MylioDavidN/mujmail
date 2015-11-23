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

import mujmail.util.Callback;
import mujmail.util.StartupModes;
import mujmail.*;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.Displayable;

/**
 * The dialog that enables to get text user input from user.
 *
 * @author David Hauzar
 */
public class TextInputDialog implements CommandListener {
    private MyTextBox myTextBox;
    private Command cmdConfirm;
    private Command cmdBack;
    private Callback callback;
    private Displayable backScreen;
    
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
    public TextInputDialog(String title, String text, int maxSize, int constraints) {
        cmdConfirm = new Command(Lang.get(Lang.BTN_OK), Command.OK, 1);
        cmdBack = new Command(Lang.get(Lang.BTN_BACK), Command.BACK, 0);
        myTextBox = new MyTextBox(title, text, maxSize, constraints);
    }
    
    /**
     * Starts the dialog in new thread. Switches the display to this dialog.
     * After user enters the text and confirms it, calls method callback() 
     * of the parameter callback. The text which user entered will be passed
     * as object of type String in parameter Message of the method callback()
     * 
     * @param display the main Display object of the midlet; used to display
     *  the dialog on the screen
     * @param backScreen  the screen to which the display should be switched if
     *  user presses back button
     * @param callback the object which method callback is called when user
     *  confirms the dialog
     *  the text which user entered is passed as String object as parameter
     *  Message of the method callback
     *  note that the focus should be switched back from the TextInputDialog
     *  in the callback() method
     */
    public void start(Displayable backScreen, Callback callback) {
        this.callback = callback;
        this.backScreen = backScreen;
        myTextBox.addCommand(cmdConfirm);
        myTextBox.addCommand(cmdBack);
        myTextBox.setCommandListener(this);
        myTextBox.start(StartupModes.IN_NEW_THREAD);
    }

    public void commandAction(Command command, Displayable displayable) {
        if (command == cmdConfirm) {
            callback.callback(this, myTextBox.getString());
            return;
        }
        
        if (command == cmdBack) {
            MujMail.mujmail.getDisplay().setCurrent(backScreen);
        }
    }
    
     /**
     * Returns the maximum size (number of characters) that can be stored in 
     * this TextBox.
     *
     * @return the maximum size in characters
     */
    public int getMaxSize() {
        return myTextBox.getMaxSize();
    }
    
    /**
     * Sets the maximum size (number of characters) that can be contained in 
     * this TextBox. If the current contents of the TextBox are larger than 
     * maxSize, the contents are truncated to fit.
     * @param maxSize the new maximum size
     * 
     * @return assigned maximum capacity - may be smaller than requested.
     * @throws IllegalArgumentException - if maxSize is zero or less.
     * @throws     IllegalArgumentException - if the contents after truncation 
     *  would be illegal for the current input constraints
     */
    public int setMaxSize(int maxSize) {
        return myTextBox.setMaxSize(maxSize);
    }
    
    
    
    
}
