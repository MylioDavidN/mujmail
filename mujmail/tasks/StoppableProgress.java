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
 * progress and that is used to update progress and observe whether thy should
 * terminate theirself.
 * 
 * @author David Hauzar
 */
public interface StoppableProgress extends Progress {
    /**
     * Returns true if user has called the stop method. This means that the thread
     * that progress is displayed by this progress bar should be stopped.
     * The task should monitor the return value of this method and if it is true,
     * it should stop itself.
     * 
     * @return true if user has called the stop method.
     */
    public boolean stopped();

}
