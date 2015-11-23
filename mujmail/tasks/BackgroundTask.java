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

import java.util.Vector;

import javax.microedition.lcdui.Alert;
import javax.microedition.lcdui.AlertType;
import javax.microedition.lcdui.Displayable;

import mujmail.Lang;
import mujmail.MujMail;
import mujmail.util.Functions;
import mujmail.util.StartupModes;
import mujmail.util.Observable;

/**
 * Represents the task that runs in background thread and that's progress can
 * be displayed to the user.
 * 
 * New task is created by subclassing this class and defining method doWork().
 * In this method business logic of task should be defined. The task is started
 * by calling method <code>start()</code> of instance of such subclass.
 *
 * By default progress bar is automatically showed when the task is started.
 * User can than minimalize this progress bar. The progress bar have to be
 * updated by the implementation of the task using methods of Progress.
 *
 * If method <code>disableDisplayingProgress</code> is called before starting
 * the task, progress is not displayed.
 *
 * The class is Observable. Registered Observers will receive events when
 * the state of the task is changed. Types of events that can be received
 * are defined in class {@link TaskEvents}.
 * 
 * @author David Hauzar
 */
public abstract class BackgroundTask extends Observable implements Progress {
    /** Flag signals if we want to print debug prints */
    protected static final boolean DEBUG = false;

    private static final Object enableStartingTasksNotifier = new Object();
    private static final Object enableTerminatingTasksNotifier = new Object();
    private static boolean enableStartingTasks = true;
    private static boolean enableTerminatingTasks = true;

    private boolean checkBeforeStartingTasksDisabled = false;

    // TODO: hashmap would be better?
    private static Vector allTasks = new Vector();

    /**
     * @return the allTasks
     */
    static Vector getRunningTasks() {
        return allTasks;
    }
    private Thread thread;
    private String alertText = null;
    private boolean isRunning = false;
    
    ScreenContainer nextScreen = new ScreenContainer();
    private final ProgressManager progressManager;
    private String taskName;
    /** true if automatically display progress when starting the task. */
    private boolean automaticallyDisplayProgress = true;
    /** true when the progress was already displayed. */
    boolean progressDisplayed = false;
    /** The user interface for canceling staring of the task. */
    ConditionalActionRunnerUI conditionalActionRunnerUI = null;

     /**
     * Creates the instance of Background task.
     * @param taskName the name of background task that will be displayed to
     *  users in tasks manager user interface.
     */
    public BackgroundTask(String taskName) {
        this.taskName = taskName;
        progressManager = (ProgressManager) createProgressManager();
        setTitle(taskName);  // TODO: Teporary change ... remove in final .. to easyly found source task
    }

    public String toString() {
        return taskName + 
                "; actual state: " + getTitle() +
                "; object identifier" + super.toString();
    }

    /**
     * Enables starting tasks.
     */
    static void enableStartingTasks() {
        synchronized (enableStartingTasksNotifier) {
            enableStartingTasks = true;
            enableStartingTasksNotifier.notifyAll();
        }
    }

    /**
     * Disables starting tasks.
     */
    static void disableStartingTasks() {
        enableStartingTasks = false;
    }

    /**
     * Enables terminating tasks.
     */
    static void enableTerminatingTasks() {
        synchronized (enableTerminatingTasksNotifier) {
            enableTerminatingTasks = true;
            enableTerminatingTasksNotifier.notifyAll();
        }
    }

    /**
     * Disables terminating tasks.
     */
    static void disableTerminatingTasks() {
        enableTerminatingTasks = false;
    }



    /**
     * Disables displaying progress after start of the task.
     */
    public void disableDisplayingProgress() {
        automaticallyDisplayProgress = false;
    }

    /**
     * Disables displaying user interface to conditional action runner when
     * the action cannot be started immadiately.
     */
    public void disableDisplayingUserActionRunnerUI() {
        conditionalActionRunnerUI = ConditionalActionRunnerUI.NOT_DISPLAY_ACTION_RUNNER_UI;
    }

    /**
     * Disables checking whether there are less tasks of the same class than
     * given limit started whe starting the task.
     * This means that the will be started immediately after executing
     * <code>start</code> method.
     */
    public void disableCheckBeforeStarting() {
        checkBeforeStartingTasksDisabled = true;
    }

    /**
     * Gets the name of this task.
     * @return the name of this task.
     */
    public String getTaskName() {
        return taskName;
    }




    
    /**
     * Creates progress manager for this background task. Descendants can
     * redefine this method to return another progress manager.
     * Note, that returned object has to be of type ProgressManager so the
     * descendant redefining this method must be in the same package as 
     * BackgroundTask.
     * 
     * @return the ProgressManager instance.
     */
    protected Object createProgressManager() {
        return new ProgressManager(this);
    }

    public boolean isDisplayed() {
        return getProgressManager().isDisplayed();
    }

    public int getActual() {
        return progressManager.getActual();
    }

    public String getTitle() {
        return progressManager.getTitle();
    }

    public int getTotal() {
        return progressManager.getTotal();
    }


    
    
    
    /**
     * Starts the background task in new thread and displays progress manager.
     * 
     * @param nextScreen the screen that will be displayed when the task will
     *  finish the work.
     * @param prevScreen the screen that will be displayed when the task will
     *  be minimized.
     */
    public void start(Displayable nextScreen, Displayable prevScreen) {
        this.setNextScreen(nextScreen);
        getProgressManager().setPrevScreen(prevScreen);
        conditionallyStartTask();
    }

    /**
     * If this task is waiting to start, cancels starting this task.
     * If this task is not waiting to start, des nothing.
     */
    public void cancelStartingTask() {
        conditionalActionRunnerUI.actionRunner.cancelAction();
    }


    /**
     * Runs the task if all conditions when the task can be runned holds.
     * If it do not hold, runs it later.
     * Does not block.
     */
    private void conditionallyStartTask() {
        if (checkBeforeStartingTasksDisabled) {
            immediatelyStart();
            return;
        }

        ConditionalActionRunner startTaskActionRunner = new ConditionalActionRunner.ConditionalTaskRunner(this);
        if (conditionalActionRunnerUI == null) {
            conditionalActionRunnerUI = new ConditionalActionRunnerUI(
                    startTaskActionRunner,
                    "There is big number of tasks running",
                    "The task " + getTaskName() + " will be started automatically when some task will terminate.");
        }
        startTaskActionRunner.startAction(
                conditionalActionRunnerUI);
    }

    /**
     * Immediately starts the task. Does not check whether there is bigger than
     * allowed tasks running.
     *
     * Does not set prev or next screen.
     */
    void immediatelyStart() {
        registeringTaskWorkBeforeStartingThread();
        thread = new TaskThread(this);
        thread.start();
    }

    /**
     * Starts the background task in new thread and displays progress manager.
     * 
     * When the task will be minimized, it will be displayed the screen that is
     * current in the time of starting progress manager. When
     * the task will finished, it will be also displayed also this screen.
     */
    public void start() {
        conditionallyStartTask();
    }
    
    /**
     * Starts the background task in new thread and displays progress manager.
     * 
     * When the task will be minimized, it will be displayed the screen that is
     * current in the time of starting progress manager.
     *
     * @param nextScreen the screen that will be displayed when the task will
     *  finish the work.
     */
    public void start(Displayable nextScreen) {
        this.setNextScreen(nextScreen);
        conditionallyStartTask();
    }

    /**
     * Do part of work of registering the task that must be done before starting
     * task thread.
     * The rest of registering work is done in method {@link TaskThread#registerTask}.
     */
    private void registeringTaskWorkBeforeStartingThread() {
        getRunningTasks().addElement(this);
    }

    /**
     * Waits until starting tasks is enabled.
     */
    private void waitForStartingTasksIsEnabled() {
        try {
            synchronized(enableStartingTasksNotifier) {
                if (enableStartingTasks) return;
                enableStartingTasksNotifier.wait();
            }
            waitForStartingTasksIsEnabled();
        } catch (Exception e) {
            System.out.println(e.toString());
            e.printStackTrace();
        }
    }

    /**
     * Waits until starting tasks is enabled.
     */
    private void waitForTerminatingTasksIsEnabled() {
        try {
            synchronized(enableTerminatingTasksNotifier) {
                if (enableTerminatingTasks) return;
                enableTerminatingTasksNotifier.wait();
            }
            waitForTerminatingTasksIsEnabled();
        } catch (Exception e) {
            System.out.println(e.toString());
            e.printStackTrace();
        }
    }

    /**
     * Updates progress bar that displays the progress of this task.
     * 
     * @param actual actual progress
     * @param total the progress when the task is finished
     */
    public void updateProgress( int total,int actual) {
        getProgressManager().updateProgress(actual, total);
        setChanged();
        notifyObservers(TaskEvents.UPDATE_PROGRESS);
    }

    public void incActual(int increment) {
        getProgressManager().incActual(increment);
        setChanged();
        notifyObservers(TaskEvents.INC_ACTUAL);
    }



    /**
     * Set the title of the progress bar that displayes the progress of this task.
     * 
     * @param title the title of the progress bar.
     */
    public void setTitle(String title) {
        if (DEBUG) { System.out.println("BackgroundTask.setTile(" + title + ")"); }
        getProgressManager().setTitle(title);
        setChanged();
        notifyObservers(TaskEvents.SET_TITLE);
    }
    
    /**
     * Sets the text in the alert that will be displayed if the task was minimized
     * and finishes.
     * @param alertText
     */
    public void setAlert(String alertText) {
        this.alertText = alertText;
    }
    
    /**
     * Set minimum priority to this task.
     */
    void setMinPriority() {
        thread.setPriority(Thread.MIN_PRIORITY);
    }
    
    
    /**
     * In this method it should be specified what the background task will do.
     */
    public abstract void doWork();
    
    /**
     * Shows progress of the task. If the task is not running, shows next screen.
     */
    public void showProgress() {
        getProgressManager().showScreen();
    }

    /**
     * Shows the progress of the task if the task is running. Otherwise do
     * nothing.
     */
    public void showProgressIfRunning() {
        if (isRunning()) {
            showProgress();
        }
    }

    /**
     * Gets the screen that should be displayed after task termination.
     * TODO: this functionality is not needed now. See ProgressManager.ProgressManagerView.paint.
     * TODO: remove this method if setting next screen while painting progress
     * of the task that already finished will prove to be working.
     * @return the nextScreen
     */
    private Displayable getNextScreen() {
        if (shouldNotDisplayScreen(nextScreen.getScreen())) {
            ProgressManager.ProgressManagerView nextProgressView = (ProgressManager.ProgressManagerView) nextScreen.getScreen();
            return nextProgressView.getTask().getNextScreen();
        }
        
        return nextScreen.getScreen();
    }

    /**
     * Returns true if this task is still running.
     * @return the isRunning true if this task is still running.
     */
    public boolean isRunning() {
        return isRunning;
    }

    /**
     * Returns true if given screen should not be displayed.
     * @param screen
     * @return
     */
    static boolean shouldNotDisplayScreen(Displayable screen) {
        if (screen instanceof ProgressManager.ProgressManagerView) {
            return false;
            // TODO: following version of return leads to race condition!!
            // It can detect that the task is running. So the task will be
            // returned by method getNextScreen and its progress possibly setted
            // as current screen. The problem is than after testing that the
            // task is running and before setting it as current screen the task
            // can terminate. Than, the progrogress will be displayed "forewer"
            // - canceling of displaying the progress is done while terminating
            // the task
            // Sollution: a) synchronization: before testing whether the task
            // is running, some variable signalizing that the task can not
            // terminate will be setted. Than, ater displaying this task, this
            // variable will be unseted.
            // b) does not allow to return progress as next screen: recently
            // used
            //return !screenProgressView.getTask().isRunning();
        }

        return false;
    }

    /**
     * @param nextScreen the screen that will be displayd after the termination
     *  of the task.
     */
    void setNextScreen(Displayable nextScreen) {
        this.nextScreen.setScreen(nextScreen);
    }

    /**
     * Gets progress manager of this task.
     * @return the progressManager
     */
    ProgressManager getProgressManager() {
        return progressManager;
    }
    
    /**
     * The thread in which the task is executed.
     */
    private class TaskThread extends Thread {
        private final BackgroundTask task;

        public TaskThread(BackgroundTask task) {
            this.task = task;
        }
        
        public void run() {
            registerTask();
            if (automaticallyDisplayProgress) {
                showProgressManager();
            }
            
            try {
                doWork();
            } catch (RuntimeException rte) {
                System.err.println("ERROR BackgroundTask.run() unhandled runtime exception:");
                rte.printStackTrace();
                throw rte;
            } catch (Error e) {
                System.err.println("ERROR BackgroundTask.run() unhandled error:");
                e.printStackTrace();
                throw e;
            // } catch (Exception e) { // is missing because doWork is not declaring, that it is throwing exceptions...
            }

            waitToShowProgress();
            
            if (shouldDisplayNextScreen()) {
                MujMail.mujmail.getDisplay().setCurrent(nextScreen.getScreen());
                //MujMail.mujmail.getDisplay().setCurrent(getNextScreen());
            }
            if (shouldShowAlert()) {
                showAlert();
            }

            unRegisterTask();
        }

        /**
         * Registers the task as running. Done while starting task thread.
         * Note that some registering of task must be done before starting the
         * thread. See method {@link BackgroundTask#registerTaskWorkBeforeStartingThread}
         */
        private void registerTask() {
            waitForStartingTasksIsEnabled();
            isRunning = true;
            TasksManager.taskStarted(BackgroundTask.this);
        }
        
        private void unRegisterTask() {
            waitForTerminatingTasksIsEnabled();
            isRunning = false;
            getRunningTasks().removeElement(task);
            TasksManager.taskEnded(task);
        }

        private boolean shouldDisplayNextScreen() {
            return isDisplayed();
        }

        private boolean shouldShowAlert() {
            return !progressManager.stopped() && getProgressManager().minimized() && alertText != null;
        }

        private void showAlert() {
            Alert alert = new Alert(Lang.get(Lang.ALRT_SYS_TASK_FINISHED), alertText, null, AlertType.CONFIRMATION);
            alert.setTimeout(Alert.FOREVER);
            MujMail.mujmail.getDisplay().setCurrent(alert,nextScreen.getScreen());
        }

        /**
         * Shows progress of the task.
         */
        private void showProgressManager() {
            getProgressManager().showScreen(StartupModes.IN_NEW_THREAD);
        }

        /**
         * If the progress should be displayed, waits until the progress is
         * displayed. If not, returns immediately.
         *
         * This is needed because when the task ends next screen is displayed
         * using method <code>setCurrent</code>. When the task starts it is
         * started new thread to display progress screen by using the same
         * method. It must be ensured that calling <code>setCurrent</code> while
         * displaying progress bar will be before displaying next screen.
         */
        private void waitToShowProgress() {
            while (true) {
                if (automaticallyDisplayProgress && !progressDisplayed) {
                    yield();
                    //TODO remove this
                    Functions.sleep(100);
                } else {
                    break;
                }
            }
        }
        
    }
    

}
