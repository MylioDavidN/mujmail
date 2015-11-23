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
 * The interface using that tasks can publish the information about their
 * progress and that is used to update progress.
 * 
 * @author David Hauzar
 */
public interface Progress {
    
    /**
     * Sets the title on the progress bar.
     * @param title the title to be displayed in the progress bar.
     */
    public void setTitle(String title);
    
    /**
     * Updates the progress bar.
     * 
     * @param actual actual progress of the operation.
     * @param total the progress when the operation is finished.
     */
    public void updateProgress( int total,int actual);
    
    /**
     * Returns true if this progress is actually displayed.
     * @return true if this progress is actually displayed
     */
    public boolean isDisplayed();

    /**
     * Increments actual progress of the operation.
     * @param increment the number that will be added to actual progress
     */
    public void incActual(int increment);

    /**
     * Gets the value of actual progres of this progress.
     * @return the value of actual progress.
     */
    public int getActual();

    /**
     * Gets the value of progress when the operation is finished.
     * @return the value of toal progress.
     */
    public int getTotal();

    /**
     * Gets title of this progress.
     * @return the title of this progress.
     */
    public String getTitle();

}
