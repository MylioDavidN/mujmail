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

/**
 * The background task that can be stopped.
 *
 * The progress manager of this task will displays stop button. The task should
 * monitor the return value of method stopped. If it is true, it should end the 
 * task that is executing. If the task will not monitor return value of method 
 * stopped, it will be not stopped even if user presses stop button.
 * 
 * @author David Hauzar
 */
public abstract class StoppableBackgroundTask extends BackgroundTask 
        implements StoppableProgress {
    private StoppableProgressManager progressManager;

    public StoppableBackgroundTask(String taskName) {
        super(taskName);
    }

    

    protected Object createProgressManager() {
        progressManager = new StoppableProgressManager(this);
        return progressManager;
    }
    
    public boolean stopped() {
          if (DEBUG) { System.out.println("DEBUG StoppableBackgroundTask.stopped() - progress manager: " + progressManager); }
        return progressManager.stopped();
    }
    
    /**
     * Set the state of the task to stopped. That means that the method stopped
     * will return true and the priority of the task will be setted to minimum.
     */
    public void stopTask() {
        progressManager.stop();
        setMinPriority();
    }
 
}
