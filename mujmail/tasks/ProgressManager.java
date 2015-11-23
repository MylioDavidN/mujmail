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

import javax.microedition.lcdui.Canvas;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Font;
import javax.microedition.lcdui.Graphics;

import mujmail.Lang;
import mujmail.MujMail;
import mujmail.ui.MVCComponent;

/**
 * GUI class that displays the progress of background task. Enables to minimize
 * the task.
 * 
 * @author David Hauzar
 */
class ProgressManager extends MVCComponent implements Progress {

    protected ProgressManagerView view;
    protected final BackgroundTask task;
    ScreenContainer prevScreen = new ScreenContainer();
    private boolean minimized = false;
    private String title = Lang.get(Lang.UT_TASKS_PROGRESS_BAR);
    private Progress progress = new Progress();
    private final Command back = new Command(Lang.get(Lang.BTN_BACK), Command.BACK, 0);

    public ProgressManager(BackgroundTask thread) {
        this.task = thread;
        
        view = new ProgressManagerView();
    }
    
    /**
     * Sets the screen that will be displayed if back method is called.
     * @param prevScreen the screen that will be displayed if back method is
     *  called.
     */
    public void setPrevScreen(Displayable prevScreen) {
        this.prevScreen.setScreen(prevScreen);
    }
    
    public boolean minimized() {
        return minimized;
    }

    public void showScreen() {
        task.nextScreen.setCurrentScreenIfEmpty();
        prevScreen.setCurrentScreenIfEmpty();
        super.showScreen();
        task.progressDisplayed = true;
    }




    // optionally displays back button??
    
    public boolean stopped() {
        return false;
    }
    
    public void setTitle(String title) {
        this.title = title;
        updateView();
    }

    public int getActual() {
        return progress.getActual();
    }

    public String getTitle() {
        return title;
    }

    public int getTotal() {
        return progress.getTotal();
    }

    

    public void updateProgress(int actual, int total) {
        progress.setProgress(actual, total);
        updateView();
    }

    public void incActual(int increment) {
        progress.incActual(increment);
        updateView();
    }
    
    /**
     * Displays previous screen.
     */
    void back() {
        System.out.println("Back");
        MujMail.mujmail.getDisplay().setCurrent(prevScreen.getScreen());
        //MujMail.mujmail.getDisplay().setCurrent(getPrevScreenToDisplay());
        minimized = true;
    }

    protected void createView() {
    }

    protected Displayable getView() {
        return view;
    }

    protected void initModel() {
    }

    protected void updateView() {
        view.repaint();
    }

    public boolean isDisplayed() {
        return MujMail.mujmail.getDisplay().getCurrent() == view;
    }
    
    

    public void commandAction(Command arg0, Displayable arg1) {
        System.out.println("key pressed");
        if (arg0 == back) {
            back();
        }
    }

    //TODO HUH? Rewrite the comment below
    /**
     * Gets screen to be displayed when back button is pressed.
     * If previous screen is screen of some progress that was already
     * terminated, gets previous screen of this progress etc..
     * @return the screen that should be displayed when back button is pressed.
     *
     * TODO: remove this method if setting next screen while painting progress
     * of the task that already finished will prove to be working.
     */
    private Displayable getPrevScreenToDisplay() {
        Displayable actPrevScreen = prevScreen.getScreen();
        while (BackgroundTask.shouldNotDisplayScreen(actPrevScreen)) {
            ProgressManager.ProgressManagerView prevProgressView = (ProgressManager.ProgressManagerView) actPrevScreen;
            actPrevScreen = prevProgressView.getPrevScreen();
            // TODO: write assertion actPrevScreen != prevProgressView.getPrevScreen()
        }

        return actPrevScreen;
    }
    
    public String toString() {
        StringBuffer buff = new StringBuffer( "ProgressManager[" );
        buff.append("progress=").append( progress )
            .append(']')
            ;
        return buff.toString();
    }

    /**
     * Holds the information about progress.
     */
    private static class Progress {
        private int total;
        private int actual;
        
        public void setProgress(int actual, int total) {
            this.actual = actual;
            this.total = total;
            
            if (actual > total) {
                actual = total;
            }
            
        }

        public void incActual(int increment) {
            actual += increment;
        }
        
        public int getFractionOfDone() {
            return getActual() / getTotal();
        }
        
        public int getTotal() { return total; }
        public int getActual() { return actual; }
        
        public String toString() {
            StringBuffer buff = new StringBuffer("ProgressManager$Progress[");
            buff.append("actual=").append( actual )
                .append(", total=").append( total )
                .append(']')
                ;
            return buff.toString();
        }
    } // end of Progress class

    /**
     * Progress manager view. Will be painted only if the task is still running.
     * If not, next screen will be displayed.
     */
    protected class ProgressManagerView extends Canvas {

        public ProgressManagerView() {
            addCommand(back);
        }
        

        protected void paint(Graphics g) {
            if (!task.isRunning()) {
                // the task is not running, display next screen
                getDisplay().setCurrent(task.nextScreen.getScreen());
            }


            fillTheScreen(g);
            
            paintProgress(g);
        }

        /**
         * Gets the task that's progress this view displays.
         * @return the task that's progress this view displays.
         */
        public BackgroundTask getTask() {
            return task;
        }

        public Displayable getPrevScreen() {
            return prevScreen.getScreen();
        }
        
        public void paintProgress(Graphics g) {
            short fontHeight = (short) g.getFont().getHeight();
            short y = fontHeight;
            short offset = (short) (g.getFont().charWidth('o') * 2);
            short x = offset;
            
            String progressStr = title + " (" + progress.getActual() + "/" + progress.getTotal() + ") ", word;
            g.setFont(Font.getFont(Font.FACE_SYSTEM, Font.STYLE_PLAIN, Font.SIZE_SMALL));

            g.setColor(0, 0, 0);
            g.drawRect(x, y, getWidth() - 2 * x, 3 * fontHeight / 2); //the surrounding rectangular
            g.setColor(0);
            if (progress.getTotal() > 0) {
                g.fillRect(x, y, (getWidth() - 2 * x) * progress.getActual() / progress.getTotal(), 3 * fontHeight / 2); //the inner surrounding rec.
                g.setColor(204, 0, 0);
                g.fillRect(x + 2, y + 2, (getWidth() - 2 * x) * progress.getActual() / progress.getTotal() - 3, 3 * fontHeight / 2 - 3); //the progress rec.
            }
            g.setColor(0);
            y = (short) (3 * fontHeight);
            x = offset;
            while (progressStr.length() > 0) { //we separate every word from the string progress till the end of the string			
                word = progressStr.substring(0, progressStr.indexOf(' ') != -1 ? progressStr.indexOf(' ') + 1 : progressStr.length());
                if (x + g.getFont().stringWidth(word) > getWidth()) {
                    y += fontHeight;
                    x = offset;
                }
                g.drawString(word, x, y, Graphics.TOP | Graphics.LEFT);
                x += g.getFont().stringWidth(word);
                progressStr = progressStr.substring(word.length()); //shorten the string by the word
            }
        }

        private void fillTheScreen(Graphics g) {
            g.setColor(0x00ffffff);
            g.fillRect(0, 0, getWidth(), getHeight());
        }
    }

}
