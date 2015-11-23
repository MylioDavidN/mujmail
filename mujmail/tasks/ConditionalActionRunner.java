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

import java.util.Enumeration;
import java.util.Vector;
import mujmail.MujMail;
import mujmail.util.Callback;
import mujmail.util.Condition;
import mujmail.util.Functions;
import mujmail.util.Observable;
import mujmail.util.Observer;

/**
 * Runs given action when given condition is true. The condition is checked
 * every time some task is started or terminated.
 *
 * It is guaranteed that between testing the condition and doing the action
 * no task is started and no task is terminated.
 * Than, it is ensured that between testing the condition and doing the action
 * no condition of other instance of <code>ConditionalActionRunner</code> is
 * tested and no action of other instance of <code>ConditionalActionRunner</code>
 * is started.
 *
 * Ensures that the action is done only once.
 *
 * @author David Hauzar
 */
public class ConditionalActionRunner implements Observer {
    /** Flag signals if we want to print debug prints */
    protected static final boolean DEBUG = false;

    private final Callback action;
    private final Condition condition;
    private boolean actionWasDone = false;

    private Callback actionWhenConditionNotHolds;

    /**
     * Creates the instance of ConditionalActionRunner.
     * @param condition the condition that will be checked
     * @param action the action that will be runned if the condition is true.
     */
    public ConditionalActionRunner(Condition condition, Callback action) {
        this.condition = condition;
        this.action = action;

        
    }

    /**
     * Conditionally starts the action. That means that it checks whether the
     * condition holds. If it holds, starts the action. If not, runs the action
     * <code>actionWhenConditionNotHolds</code> and waits for the start or
     * termination of some task to check the condition again.
     *
     * @param actionWhenConditionNotHolds the action that will be runned if the
     *  condition does not hold before waiting for the next checking of the
     *  condition.
     */
    public void startAction(Callback actionWhenConditionNotHolds) {
        this.actionWhenConditionNotHolds = actionWhenConditionNotHolds;
        actionWasDone = false;
        registerObservers();
        if (!testConditionAndDoAction(this, new ConditionArgument(), actionWhenConditionNotHolds)) {
            actionWhenConditionNotHolds.callback(this, null);
            return;
        }
    }

    /**
     * Tests the condition and if the condition holds, do the action.
     * This method is synchronized so only one instance of this class can test
     * the condition and do the action in one time.
     * Ensures that no task is started and no task terminates between testing
     * the condiotion and doing the action.
     * 
     * @param actionRunner the action runner that condition will be tested and
     *  that action could be done.
     * @param conditionArgument the argument of the condition.
     * @return true if the condtion holded and the action was done.
     */
    private static synchronized boolean testConditionAndDoAction(ConditionalActionRunner actionRunner, ConditionArgument conditionArgument, Callback actionWhenConditionNotHolds) {
        BackgroundTask.disableStartingTasks();
        BackgroundTask.disableTerminatingTasks();

        if (actionRunner.actionWasDone) return true;

        boolean result = actionRunner.condition.condition(conditionArgument);
        if (result) {
            if (actionWhenConditionNotHolds instanceof ConditionalActionRunnerUI) {
                ((ConditionalActionRunnerUI)actionWhenConditionNotHolds).cancelDisplaying();
            }
            actionRunner.doTheAction();
        }

        BackgroundTask.enableStartingTasks();
        BackgroundTask.enableTerminatingTasks();

        return result;
    }

    /**
     * Do the action without checking the condition.
     * The action can be done only once so unregisters the this conditional
     * action runner to don't check the condition when some task is started
     * or terminated.
     * 
     */
    public void doTheAction() {
        cancelAction();
        if (DEBUG) System.out.println("do the action: " + this);
        if (!actionWasDone) {
            if (DEBUG) System.out.println("doing action");
            action.callback(this, null);
            actionWasDone = true;
        }
    }

    /**
     * Conditionally starts the action. That means that it checks whether the
     * condition holds. If it holds, starts the action. If not, waits for the
     * start or termination of some task to check the condition again.
     */
    public void startAction() {
        startAction(Callback.NO_ACTION);
    }

    public synchronized void update(Observable o, Object arg) {
        if (DEBUG) System.out.println("Update called");
        ConditionArgument conditionArgument = new ConditionArgument();
        conditionArgument.task = (BackgroundTask) arg;
        if (o == TasksManager.endTaskObservable) {
            conditionArgument.conditionCheckingReason = ReasonsForCheckingCondition.TERMINATED;
        } else {
            conditionArgument.conditionCheckingReason = ReasonsForCheckingCondition.STARTED;
        }

        testConditionAndDoAction(this, conditionArgument, actionWhenConditionNotHolds);
    }

    /**
     * Cancels the action.
     * 
     * This conditional action runner will not check the condition when some
     * task will started or terminated.
     */
    public void cancelAction() {
        if (DEBUG) System.out.println("cancel action" + this);
        unregisterObservers();
    }

    /**
     * Registers this object to update itself every time some task is started
     * or terminated.
     */
    protected void registerObservers() {
        TasksManager.addEndTaskObserver(this);
        TasksManager.addStartTaskObserver(this);
    }

    /**
     * Unregisters this object to not update itself every time some task is
     * started or terminated.
     */
    protected void unregisterObservers() {
        TasksManager.deleteEndTaskObserver(this);
        TasksManager.deleteStartTaskObserver(this);
    }

    /**
     * Object of this class is possed as argument to method
     * {@link mujmail.util.Condition#condition(java.lang.Object)}
     */
    public static class ConditionArgument {
        /** The task that caused checking of the condition. Null if this is initiall checking of the condition. */
        public BackgroundTask task = null;
        /** Why is the condition checked. */
        public ReasonsForCheckingCondition conditionCheckingReason = ReasonsForCheckingCondition.INITIAL;
    }

    /**
     * Enumeration class with reasons of checking of the condition.
     */
    public static class ReasonsForCheckingCondition {
        private ReasonsForCheckingCondition() {};
        /** Initial checking of the condition. */
        public static ReasonsForCheckingCondition INITIAL = new ReasonsForCheckingCondition();
        /** The condition is checked because some task started. */
        public static ReasonsForCheckingCondition STARTED = new ReasonsForCheckingCondition();
        /** The condition is checked because some task terminated. */
        public static ReasonsForCheckingCondition TERMINATED = new ReasonsForCheckingCondition();
    }

    /**
     * The condition for exiting application. The number of incompleted task
     * must be zero.
     * Singleton class with one instance condition.
     */
    public static class ExitApplicationCondition implements Condition {
        private ExitApplicationCondition() {};
        /** The condition when the application can be terminated. */
        public static final ExitApplicationCondition CONDITION = new ExitApplicationCondition();

        public boolean condition(Object argument) {
            if (TasksManager.isSomeTaskRunningOrWaitingToStart()) return false;
            return true;
        }
    }


    /**
     * The action for exiting the application.
     * Singleton class with one instance ACTION.
     */
    public static class ExitApplicationAction implements Callback {
        private ExitApplicationAction() {};
        /** The action that exits application. */
        public static final ExitApplicationAction ACTION  = new ExitApplicationAction();

        public void callback(Object called, Object message) {
            MujMail.mujmail.destroyApp(false);
        }

    }

    /**
     * The condition for starting new task. The number of started task must be
     * less then {@link TasksManager#MAXIMUM_NUMBER_OF_RUNNING_TASKS}.
     */
    public static class StartNewTaskCondition implements Condition {
        private final BackgroundTask taskToStart;
        //private boolean conditionalActionRunnerUIWasShown = false;

        public StartNewTaskCondition(BackgroundTask taskToStart) {
            this.taskToStart = taskToStart;
        }

        public synchronized boolean condition(Object argument) {            
            ConditionArgument condArgument = (ConditionArgument) argument;

            if (condArgument.conditionCheckingReason == ReasonsForCheckingCondition.STARTED) {
                return false;
            }
            if (testCondition()) {
                // TODO: not needed anymore because this funcitonality is done
                // by ConditionalActionRunner and ConditionalActionRunnerUI.
                //if (conditionalActionRunnerUIWasShown) {
                    //cancelDisplayingConditionalActionRunnerUI();
                //}
                return true;
            } else {
                //conditionalActionRunnerUIWasShown = true;
                return false;
            }
        }

        private boolean testCondition() {
            Enumeration tasks = TasksManager.getRunningTasks().elements();
            int numberOfTasksOfTheSameClass = 0;
            while (tasks.hasMoreElements()) {
                BackgroundTask task = (BackgroundTask) tasks.nextElement();
                if (DEBUG) System.out.println(task.getClass());
                if (DEBUG) System.out.println(taskToStart.getClass());
                if (task.getClass() == taskToStart.getClass()) {
                    numberOfTasksOfTheSameClass++;
                    if (numberOfTasksOfTheSameClass >= TasksManager.MAXIMUM_NUMBER_OF_RUNNING_TASKS) {
                        return false;
                    }
                }
            }
            
            return true;
        }

        //private void cancelDisplayingConditionalActionRunnerUI() {
            //taskToStart.conditionalActionRunnerUI.cancelDisplaying();
        //}
    }

    /**
     * The action for starting new tasks the application.
     */
    public static class StartNewTaskAction implements Callback {
        private final BackgroundTask taskToStart;
        public StartNewTaskAction(BackgroundTask taskToStart) {
            this.taskToStart = taskToStart;
        }

        public void callback(Object called, Object message) {
            taskToStart.immediatelyStart();
        }

    }


    /**
     * Used to start tasks. Contains all tasks that are actually waiting to
     * start.
     */
    public static class ConditionalTaskRunner extends ConditionalActionRunner {
        private BackgroundTask task;
        private static Vector waitingTasks = new Vector();

        public ConditionalTaskRunner(BackgroundTask task) {
            super(new StartNewTaskCondition(task),
                new StartNewTaskAction(task));
            this.task = task;
        }

        /**
         * Gets tasks that are waiting to start.
         * @return tasks that are waiting to start.
         */
        public static Vector getWaitingTasks() {
            return waitingTasks;
        }

        protected void registerObservers() {
             waitingTasks.addElement(task);
            super.registerObservers();
        }

        protected void unregisterObservers() {
            waitingTasks.removeElement(task);
            super.unregisterObservers();
        }

        /**
         * Gets the number of tasks waiting to start.
         * @return the number of tasks waiting to start.
         */
        public static int getNumberOfTasksWaitingForStart() {
            return waitingTasks.size();
        }


    }
    

    public static class SampleTestTask extends StoppableBackgroundTask {

        public SampleTestTask() {
            super("Sample task");
        }

        public void doWork() {
            while (true) {
                System.out.println("Do work");
                if (stopped()) {
                    System.out.println("Task was stopped.");
                    break;
                }
                Functions.sleep(500);
            }
            System.out.println("After while.");
        }
    }

    public static class SampleTestTask2 extends StoppableBackgroundTask {

        public SampleTestTask2() {
            super("Sample task 2");
        }

        public void doWork() {
            while (true) {
                System.out.println("Do work 2");
                if (stopped()) {
                    System.out.println("Task was stopped. 2");
                    break;
                }
                Functions.sleep(500);
            }
            System.out.println("After while. 2");
        }
    }


    
}
