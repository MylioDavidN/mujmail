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

package mujmail.tasks;

import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.Displayable;

import mujmail.Lang;

/**
 * Progress manager with Stop button.
 * @author David Hauzar
 */
class StoppableProgressManager extends ProgressManager implements Progress {
    private boolean stopped = false;
    private final Command stop = new Command(Lang.get(Lang.BTN_TB_STOP), Command.BACK, 0);

    public StoppableProgressManager(BackgroundTask thread) {
        super(thread);
    }
    
    public boolean stopped() {
        return stopped;
    }
    
    /**
     * Set the state of the task to stop.
     */
    private void stopAction() {
        stop();
        task.setMinPriority();
    }
    
    public void stop() {
        stopped = true;
    }
    

    public void commandAction(Command arg0, Displayable arg1) {
        super.commandAction(arg0, arg1);
        
        if (arg0 == stop) {
            stopAction();
        }
    }
    
    protected void createView() {
        view = new StoppableProgressManagerView();
    }
    
    private class StoppableProgressManagerView extends ProgressManagerView {
        public StoppableProgressManagerView() {
            addCommand(stop);
        }
        
    }
}
