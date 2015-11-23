//#condition MUJMAIL_DEBUG_CONSOLE
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

package mujmail.debug;

import java.util.Vector;

import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Form;

import mujmail.Lang;
import mujmail.MujMail;
import mujmail.ui.MVCComponent;

/**
 * Displays the strings printed to debug console.
 * 
 * @author David Hauzar
 */
public class DebugConsoleUI extends MVCComponent {
    private Form view;
    private Command back = new Command(Lang.get(Lang.BTN_BACK), Command.BACK, 0);
    private Command clear = new Command(Lang.get(Lang.BTN_CLR), Command.ITEM, 0);
    

    public void commandAction(Command arg0, Displayable arg1) {
        if (arg0 == back) {
            getDisplay().setCurrent(MujMail.mujmail.getMenu());
            return;
        } 
        
        if (arg0 == clear) {
            DebugConsole.deleteRecords( true);
            showScreen();
            return;
        }
    }

    protected void createView() {
        view = new Form("Debug console");
        view.addCommand(back);
        view.addCommand(clear);
        Vector records = DebugConsole.getRecords();
        for (int i = 0; i < records.size(); i++) {
            view.append((String) records.elementAt(i) + "\n");
        }
    }

    protected void initModel() {
    }

    protected void updateView() {
        createView();
    }

    protected Displayable getView() {
        return view;
    }
    
    

    
}
