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
import javax.microedition.lcdui.Displayable;
import mujmail.util.Observable;
import mujmail.util.Observer;

/**
 * Static class for managing background tasks and getting information about them.
 *
 * 
 * @author David Hauzar
 */
public class TasksManager {
    static ChangableObservable endTaskObservable = new ChangableObservable();
    static ChangableObservable startTaskObservable = new ChangableObservable();
    static final int MAXIMUM_NUMBER_OF_RUNNING_TASKS = 2;

    private static class ChangableObservable extends Observable {

        public void setChanged() {
            super.setChanged();
        }

    }

    /**
     * Checks whether there are some tasks running and if not, exits the
     * application.
     * If there are some tasks running, exits the application when all tasks
     * will terminate. In this case it alsodisplays the dialog to user in that
     * user can cancel exiting or confirm exiting despite of some tasks are
     * running.
     */
    public static void conditionallyExit() {
        ConditionalActionRunner exitActionRunner = new ConditionalActionRunner(
                ConditionalActionRunner.ExitApplicationCondition.CONDITION,
                ConditionalActionRunner.ExitApplicationAction.ACTION);
        exitActionRunner.startAction(
                new ConditionalActionRunnerUI(
                    exitActionRunner,
                    "Some tasks are still running",
                    "There are incompleted tasks that are still running. The application will exit automatically when all will terminate.",
                    "Exit anyway"));
    }

    /**
     * Returns true if some task is running or waiting to start.
     * @return true if some task is running or waiting to start.
     */
    public static boolean isSomeTaskRunningOrWaitingToStart() {
        return !BackgroundTask.getRunningTasks().isEmpty() || ConditionalActionRunner.ConditionalTaskRunner.getNumberOfTasksWaitingForStart() != 0;
    }

    /**
     * Gets the number of tasks either running or waiting to be started.
     * @return the number of tasks either running or waiting to be started.
     */
    public static int numTasks() {
        return BackgroundTask.getRunningTasks().size() + ConditionalActionRunner.ConditionalTaskRunner.getNumberOfTasksWaitingForStart();
    }

    /**
     * Adds observer that will be notified whenever some task will end.
     * @param endTaskObserver the object that will be notified whenever the some
     *  task will end.
     *  The argument of function notify will be the task that actually ended.
     *  That is the object of type BackgroundTask.
     * @see Observable#addObserver(mujmail.util.Observer) 
     */
    public static void addEndTaskObserver(Observer endTaskObserver) {
        endTaskObservable.addObserver(endTaskObserver);
    }

    /**
     * Deletes observer that is notified whenever some task ends. This observer
     * will be not notified any more.
     * @param endTaskObserver the observer to be deleted.
     * @see Observable#deleteObserver(mujmail.util.Observer)
     */
    public static void deleteEndTaskObserver(Observer endTaskObserver) {
        endTaskObservable.deleteObserver(endTaskObserver);
    }

    /**
     * Deletes all observers that are notified whenever some task ends. This
     * observers will be not notified any more.
     * @see Observable#deleteObservers()
     */
    public static void deleteEndTaskObservers() {
        endTaskObservable.deleteObservers();
    }

    /**
     * Adds observer that will be notified whenever some task will start.
     * @param startTaskObserver the object that will be notified whenever the some
     *  task will start.
     *  The argument of function notify will be the task that actually started.
     *  That is the object of type BackgroundTask.
     * @see Observable#addObserver(mujmail.util.Observer)
     */
    public static void addStartTaskObserver(Observer startTaskObserver) {
        startTaskObservable.addObserver(startTaskObserver);
    }

    /**
     * Deletes observer that is notified whenever some task starts. This observer
     * will be not notified any more.
     * @param startTaskObserver the observer to be deleted.
     * @see Observable#deleteObserver(mujmail.util.Observer)
     */
    public static void deleteStartTaskObserver(Observer startTaskObserver) {
        startTaskObservable.deleteObserver(startTaskObserver);
    }

    /**
     * Deletes all observers that are notified whenever some task starts. This
     * observers will be not notified any more.
     * @see Observable#deleteObservers()
     */
    public static void deleteStartTaskObservers() {
        startTaskObservable.deleteObservers();
    }

    /**
     * Do all task manager actions with task that actually ended.
     * This method is called by BackgroundTask whenever the task ends.
     * @param backgroundTask the task that ended
     */
    static void taskEnded(BackgroundTask backgroundTask) {
        endTaskObservable.setChanged();
        endTaskObservable.notifyObservers(backgroundTask);
    }

    /**
     * Do all task manager action with task that actually started.
     * This method is called by BackgroundTask whenever the task starts.
     * @param backgroundTask the task that starts.
     */
    static void taskStarted(BackgroundTask backgroundTask) {
        startTaskObservable.setChanged();
        startTaskObservable.notifyObservers(backgroundTask);
    }

    /**
     * Sets given screen as screen that will be displayed after termination
     * of the task or pressing back button to all running tasks.
     * @param screen the screen to be set.
     */
    public static void setScreenToAllTasks(Displayable screen) {
        Vector allTasks = BackgroundTask.getRunningTasks();
        for (int i = 0; i < allTasks.size(); i++) {
            BackgroundTask task;
            try {
                task = (BackgroundTask) allTasks.elementAt(i);
            } catch (Exception e) {
                break;
            }
            task.setNextScreen(screen);
            task.getProgressManager().setPrevScreen(screen);
        }
    }

    /**
     * Gets the Vector of running tasks.
     * @return Vector of running tasks.
     */
    public static Vector getRunningTasks() {
        return BackgroundTask.getRunningTasks();
    }

}
