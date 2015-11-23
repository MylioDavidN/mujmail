package mujmail;

/*
MujMail - Simple mail client for J2ME
Copyright (C) 2006 Nguyen Son Tung <n.sontung@gmail.com>
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

import javax.microedition.lcdui.*;
import java.util.*;

/**
 * Used to get confirmations and alerting user.
 * 
 * Implemented using the timer which periodically inspects whether there is
 * some alert to be displayed.
 */
public class MyAlert extends Canvas {

    private class alertTask extends TimerTask { //to check if there any alert job to alert

        MyAlert myAlert;

        public alertTask(MyAlert myAlert) {
            this.myAlert = myAlert;
        }

        synchronized public void run() {
            //if there's something to display. and we have lost focus. take focus back and display the alert
            if (!jobQueue.isEmpty() && !(mujMail.getDisplay().getCurrent() == alertWindow || mujMail.getDisplay().getCurrent() == myAlert)) {
                mujMail.getDisplay().setCurrent(myAlert);
            }
            if (jobQueue.isEmpty() && timer != null) { //nothing to display. stop controlling jobQueue
                timer.cancel();
                timer = null;
            }
        }
    }

    public static class AlertJob { //must be public static to let the other classes check its mode

        Object callObject; //object that caused alert
        Displayable nextDisplay; //next display after the alert window disappears
        String text;
        byte mode;
        AlertType type;
        long invokeTime;

        public AlertJob(Object callObject, Displayable display, String text, byte mode, AlertType type) {
            this.callObject = callObject;
            nextDisplay = display;
            this.text = text;
            this.mode = mode;
            this.type = type;
        }
    }
    public static final byte DEFAULT = 0;
    public static final byte DB_CLEAR_CONFIRM = 1;
    public static final byte EXIT_BUSY_CONFIRM = 2;
    public static final byte DEL_CONFIRM = 4;
    public static final byte NEW_MAILS = 3;
    Vector jobQueue = new Vector();
    MujMail mujMail;
    Command OK, cancel;
    Alert alertWindow; //the alert window that is displayed to the user 
    AlertJob lastJob;
    Timer timer;

    public MyAlert(MujMail mujMail) {
        alertWindow = new Alert("MujMail");
        this.mujMail = mujMail;
        alertWindow.setCommandListener(mujMail);
        OK = new Command(Lang.get(Lang.BTN_OK), Command.OK, 0);
        cancel = new Command(Lang.get(Lang.BTN_CANCEL), Command.CANCEL, 0);
        alertWindow.addCommand(OK);
    }

    protected void paint(Graphics g) {
    //do nothing
    }

    //we've got the focus. display next alerts
    protected synchronized void showNotify() {
        if (!jobQueue.isEmpty()) {
            AlertJob job = (AlertJob) jobQueue.firstElement();
            jobQueue.removeElementAt(0);
            invokeAlert(job);
        } else {
            mujMail.getDisplay().setCurrent(lastJob.nextDisplay);
        }
    }

    /**
     * Adds the alert to the queue of alerts. The timer periodically controls
     * it and displays the alerts.
     * 
     * Example: when a object wants to alert or get confirmation it calls
     * mujMail.alert.setAlert(this,this,some_text,MyAlert.DEFAULT,AlertType.ERROR)
     * 
     * @param callObject the object that wants to display an alert (can be 
     *  null)
     * @param display the next display (can be null)
     * @param text 
     * @param mode defines the action that follows after displaying the alert 
     *  (by default it's MyAlert.default, which does nothing)	
     *  then MujMail.commandAction will do the rest, see the code in action 
     *  in Mujmail.java
     * @param type
     */
    public synchronized void setAlert(Object callObject, Displayable display, String text, byte mode, AlertType type) {
        Displayable nextDisplay = display == null ? mujMail.getDisplay().getCurrent() : display;
        if (nextDisplay == alertWindow) //this alert was trigged by Menu and param display is null
        {
            nextDisplay = mujMail.getMenu();
        }
        jobQueue.addElement(new AlertJob(callObject, nextDisplay, text, mode, type));
        //there's some job but we have lost focus			
        if (timer == null && !jobQueue.isEmpty() && !(mujMail.getDisplay().getCurrent() == alertWindow || mujMail.getDisplay().getCurrent() == this)) {
            timer = new Timer();
            //then we have to take focus back and do all the jobs		
            timer.schedule(new alertTask(this), 0, 500);
        }
    }
    
    /**
     * Adds the alert to the queue of alerts. The timer periodically controls
     * it and displays the alerts.
     * 
     * Example: when a object wants to alert or get confirmation it calls
     * mujMail.alert.setAlert(some_text,MyAlert.DEFAULT,AlertType.ERROR)
     * 
     * @param text 
     * @param mode defines the action that follows after displaying the alert 
     *  (by default it's MyAlert.default, which does nothing)	
     *  then MujMail.commandAction will do the rest, see the code in action 
     *  in Mujmail.java
     * @param type
     */
    public void setAlert(String text, byte mode, AlertType type) {
        setAlert(null, null, text, mode, type);
    }
    
    /**
     * Adds the alert to the queue of alerts. The timer periodically controls
     * it and displays the alerts.
     * 
     * Example: when a object wants to alert or get confirmation it calls
     * mujMail.alert.setAlert(some_text,AlertType.ERROR)
     * 
     * @param text 
     * @param type
     */
    public void setAlert(String text, AlertType type) {
        setAlert(null, null, text, DEFAULT, type);
    }

    private void invokeAlert(AlertJob job) {
        if (Settings.debug) {
            System.out.println(job.text);
        }
        //no need to alerts about new mails in several servers simultaneously
        if (job.mode == NEW_MAILS && lastJob != null && lastJob.mode == NEW_MAILS && lastJob.invokeTime - System.currentTimeMillis() <= 5000) {
            mujMail.getDisplay().setCurrent(this);
            return;
        }

        lastJob = job;
        alertWindow.setString(job.text);
        alertWindow.setType(job.type);
        //do we need to display a confirming window?
        if (job.mode == DB_CLEAR_CONFIRM || job.mode == EXIT_BUSY_CONFIRM || job.mode == DEL_CONFIRM) {
            alertWindow.addCommand(cancel);
            alertWindow.setTimeout(Alert.FOREVER);
        } else {
            alertWindow.removeCommand(cancel);
            alertWindow.setTimeout(3000);
        }
        lastJob.invokeTime = System.currentTimeMillis();
        mujMail.getDisplay().setCurrent(alertWindow, this);
    }
}
