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

import java.util.Vector;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.List;
import mujmail.MujMail;
import mujmail.ui.MVCComponent;
import mujmail.util.Observable;
import mujmail.util.Observer;

/**
 * User interface for managing tasks.
 * @author David Hauzar
 */
public class TasksManagerUI extends MVCComponent implements Observer {
    private Displayable prevScreen;
    private boolean showRunningTasks = true;
    private List tasksList;
    private Command back = new Command("Back", Command.BACK, 0);
    private Command select;
    private Command showRunning = new Command("Show running tasks", Command.ITEM, 2);
    private Command showWaiting = new Command("Show waiting tasks", Command.ITEM, 2);

    public void update(Observable o, Object arg) {
        showScreen();
    }

    protected void createView() {
    }

    protected Displayable getView() {
        createTasksList();
        appendTasks();
        return tasksList;
    }

    protected void initModel() {
        prevScreen = getDisplay().getCurrent();
        TasksManager.addEndTaskObserver(this);
        TasksManager.addStartTaskObserver(this);
    }

    protected void updateView() {
    }

    public void commandAction(Command c, Displayable d) {
        if (c == back) {
            TasksManager.deleteEndTaskObserver(this);
            TasksManager.deleteStartTaskObserver(this);
            getDisplay().setCurrent(prevScreen);
            return;
        }

        if (c == select) {
            int taskIndex = tasksList.getSelectedIndex();

            if (showRunningTasks) {
                // show progress of running task
                Vector tasks = TasksManager.getRunningTasks();
                BackgroundTask task;
                try {
                    task = (BackgroundTask) tasks.elementAt(taskIndex);
                } catch (Exception e) {
                    // given task is not yet active
                    return;
                }
                task.showProgress();
            } else {
                // shows the dialog where the start of the task can be canceled
                Vector tasks = ConditionalActionRunner.ConditionalTaskRunner.getWaitingTasks();
                BackgroundTask task;
                try {
                    task = (BackgroundTask) tasks.elementAt(taskIndex);
                } catch (Exception e) {
                    // given task is not yet active
                    return;
                }
                task.cancelStartingTask();
                showScreen();
            }


            return;
        }

        if (c == showRunning) {
            showRunningTasks = true;
            showScreen();
            return;
        }

        if (c == showWaiting) {
            showRunningTasks = false;
            showScreen();
            return;
        }
    }

    private void appendTasks() {
        // TODO: refactor
        Vector tasks;
        if (showRunningTasks) {
            tasks = TasksManager.getRunningTasks();
            for (int i = 0; i < tasks.size(); i++) {
                BackgroundTask task = null;
                try {
                    task = (BackgroundTask) tasks.elementAt(i);
                } catch (Exception e) {
                    break;
                }
                tasksList.append(task.getTaskName(), null);
            }
        } else {
            tasks = ConditionalActionRunner.ConditionalTaskRunner.getWaitingTasks();
            for (int i = 0; i < tasks.size(); i++) {
                BackgroundTask task = null;
                try {
                    task = (BackgroundTask) tasks.elementAt(i);
                } catch (Exception e) {
                    break;
                }
                tasksList.append(task.getTaskName() + " (waiting for the start)", null);
            }
        }

        if (tasks.size() > 0) {
            tasksList.addCommand(select);
        }
    }

    private void createTasksList() {
        if (showRunningTasks) {
            tasksList = new List("Running tasks", List.IMPLICIT);
            tasksList.addCommand(showWaiting);
            select = new Command("Progress", Command.OK, 1);
        } else {
            tasksList = new List("Waiting tasks", List.IMPLICIT);
            tasksList.addCommand(showRunning);
            select = new Command("Cancel starting", Command.OK, 1);
        }
        tasksList.addCommand(back);
    }

    

}
