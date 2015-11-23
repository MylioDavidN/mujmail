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

import javax.microedition.lcdui.Alert;
import javax.microedition.lcdui.AlertType;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.Displayable;
import mujmail.MujMail;
import mujmail.ui.MVCComponent;
import mujmail.util.Callback;

/**
 * User interface for {@link ConditionalActionRunner}.
 *
 * It enables to cancel the action, to wait and test whether the condition
 * holds and to show the Task manager.
 *
 * To use it, pass object of this class to method
 * {@link ConditionalActionRunner#startAction(Callback)}. If the condition does
 * not hold, it shows this user interface to user. If the condition starts
 * holding this user interface is automatically canceled.
 * 
 * @author David Hauzar
 */
public class ConditionalActionRunnerUI implements Callback {
    private boolean wasDisplayed = false;
    final ConditionalActionRunner actionRunner;
    private final String alertTitle;
    private final String alertText;
    private ConditionalActionRunnerUIImpl uiScreen;
    private String doAnywayText = "";
    private Mode mode;
    private static Displayable lastNonActionRunnerScreen = null;

    /** Does not display action runner user interface. */
    public static final ConditionalActionRunnerUI NOT_DISPLAY_ACTION_RUNNER_UI = new NotDisplayConditionalActionRunnerUI();

    /**
     * Enumeration class where modes of ConditionalActionRunnerUI are declared.
     */
    public static class Mode {
        private Mode() {};

        /** Basic mode. Buttons Back, Cancel and Show progress manager will be
         displayed. */
        public static final Mode BASIC = new Mode();
        /** It will be displayed button force action without checking condition. */
        public static final Mode WITH_FORCE_ACTION = new Mode();
    }
    
    public ConditionalActionRunnerUI(ConditionalActionRunner actionRunner, String alertTitle, String alertText) {
        this.actionRunner = actionRunner;
        this.alertTitle = alertTitle;
        this.alertText = alertText;
        mode = Mode.BASIC;
    }

    public ConditionalActionRunnerUI(ConditionalActionRunner actionRunner, String alertTitle, String alertText, String doAnywayText) {
        this(actionRunner, alertTitle, alertText);
        this.doAnywayText = doAnywayText;
        mode = Mode.WITH_FORCE_ACTION;
    }
    
    public void callback(Object called, Object message) {
        uiScreen = new ConditionalActionRunnerUIImpl();
        uiScreen.showScreen();
    }

    /**
     * If current screen is not displayed by some instance of this class
     * sets it as last screen that was not shown by any instance of
     * ConditionalActionRunnerUI.
     */
    private static synchronized void setLastScreen() {
        Displayable current = MujMail.mujmail.getDisplay().getCurrent();
        if ( !(current instanceof ConditionalActionRunnerUIImpl.ConditionalActionRunnerAlert) ) {
            lastNonActionRunnerScreen = current;
        }
    }

    /**
     * Gets the screen that this conditional action runner user interface
     * displays to user.
     * @return the screen that this conditional action runner user interface
     *  displays to user.
     */
    Displayable getUIScreen() {
        return uiScreen.getView();
    }

    /**
     * Cancel displaying this conditional action runner user interface.
     * Sets last screen that was not shown by any instance of
     * ConditionalActionRunnerUI as current screen.
     *
     * This is not perfect sollution because there can be conditional action
     * runner screen of some task that was not yet started - and this screen
     * should be displayed. The sollution of this problem would be have the
     * list of conditional action runner screens of tasks that were not already
     * started and if this queue is not empty display some of this screen
     * instead of displaying last screen that was not shouwn like now.
     * However, this problem is not critical and this sollution would be
     * probably overcomplicated.
     */
    public void cancelDisplaying() {
        if (wasDisplayed) {
            MujMail.mujmail.getDisplay().setCurrent(lastNonActionRunnerScreen);
        }
        wasDisplayed = false;
    }

    /**
     * ConditionalActionRunnerUI that does not anything.
     */
    private static class NotDisplayConditionalActionRunnerUI extends ConditionalActionRunnerUI {

        public NotDisplayConditionalActionRunnerUI() {
            super(null, null, null);
        }


        public void callback(Object called, Object message) {
        }

        public void cancelDisplaying() {
        }


        
    }

    private class ConditionalActionRunnerUIImpl extends MVCComponent {
        private Displayable prevScreen;
        private Displayable view;
        private final Command BACK = new Command("Back", Command.BACK, 0);
        private final Command CANCEL_ACTION = new Command("Cancel", Command.CANCEL, 1);
        private final Command SHOW_TASK_MANAGER = new Command("Show tasks", Command.ITEM, 2);
        private final Command FORCE_ACTION = new Command(doAnywayText, Command.ITEM, 3);

        private class ConditionalActionRunnerAlert extends Alert {

            public ConditionalActionRunnerAlert() {
                super(alertTitle, alertText, null, AlertType.INFO);
            }

            public Displayable getPrevScreen() {
                return prevScreen;
            }

        }

        public void showScreen() {
            setLastScreen();
            wasDisplayed = true;
            super.showScreen();
        }



        protected void createView() {
            Alert alert = new ConditionalActionRunnerAlert();
            alert.setTimeout(Alert.FOREVER);

            alert.addCommand(BACK);
            alert.addCommand(CANCEL_ACTION);
            alert.addCommand(SHOW_TASK_MANAGER);
            if (mode == Mode.WITH_FORCE_ACTION) {
                alert.addCommand(FORCE_ACTION);
            }

            view = alert;
        }

        protected Displayable getView() {
            return view;
        }

        protected void initModel() {
            prevScreen = getDisplay().getCurrent();
        }

        protected void updateView() {
        }

        public void commandAction(Command c, Displayable d) {
            if (c == BACK) {
                getDisplay().setCurrent(prevScreen);
                return;
            }

            if (c == CANCEL_ACTION) {
                actionRunner.cancelAction();
                getDisplay().setCurrent(prevScreen);
                return;
            }

            if (c == SHOW_TASK_MANAGER) {
                TasksManagerUI taskManager = new TasksManagerUI();
                taskManager.showScreen();
                return;
            }

            if (c == FORCE_ACTION) {
                actionRunner.doTheAction();
                return;
            }
        }
    }
}
