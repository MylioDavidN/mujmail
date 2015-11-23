//#condition MUJMAIL_SEARCH
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

package mujmail.search;

import java.util.Vector;

import javax.microedition.lcdui.Command;

import javax.microedition.lcdui.Displayable;
import mujmail.Lang;
import mujmail.MessageHeader;
import mujmail.MujMail;
import mujmail.NonpersistentBox;
import mujmail.tasks.StoppableBackgroundTask;

/**
 * Displays results of searching.
 *
 * Used in class {@link mujmail.search.SearchWindows} to start the search.
 * Messages that matches search criteria are than adding to this box immediately
 * when they aer matched.
 * Provides button to stop the search when searching is still in progress and to
 * start new search.
 * 
 * @author David Hauzar
 */
public class SearchBox extends NonpersistentBox {
    private final Command stopSearch = new Command(Lang.get(Lang.BTN_SEA_STOP_SEARCH), Command.ITEM, 10);
    private boolean isBusy = true;
    private StoppableBackgroundTask searchTask;

    private Command newSearch;

    /**
     * Creates new search box.
     * @param mujMail
     */
    public SearchBox(MujMail mujMail) {
        super(mujMail, Lang.get(Lang.SEA_SEARCH_RESULTS));
                
        newSearch = new Command(Lang.get(Lang.BTN_SEA_NEW_SEARCH), Command.ITEM, 0);
        addCommand(newSearch);
    }

    /**
     * Background task that runs the search.
     */
    private class SearchTask extends StoppableBackgroundTask {
        private final SearchSettings searchSettings;

        public SearchTask(SearchSettings searchSettings) {
            super(Lang.get(Lang.SEA_SEARCHING));
            this.searchSettings = searchSettings;
        }

        public void doWork() {
            disableTicker();
            SearchCore.search(searchSettings, SearchBox.this, this);
            resort();
            isBusy = false;
            removeCommand(stopSearch);
            enableTicker();
            repaint();
            searchTask = null;
        }
        
    }

    public boolean isBusy() {
        return isBusy;
    }
    
    void stopSearch() {
        searchTask.stopTask();
    }
    
    /**
     * Starts search in new thread and displays search results in this box.
     * @param searchSettings the settings used to search.
     */
    void startSearchInNewThreadDisplayResults(SearchSettings searchSettings) {
        storage.removeAllMessages();
        isBusy = true;
        addCommand(stopSearch);
        getMujMail().getDisplay().setCurrent(this);
        
        searchTask = new SearchTask(searchSettings);
        searchTask.setTitle(Lang.get(Lang.SEA_SEARCHING));
        searchTask.updateProgress(100, 0);
        searchTask.start(this);
    }

    public MessageHeader storeMail(MessageHeader header) {
        isBusy = false;
        return super.storeMail(header);
    }
    
    public void setMessages(Vector messages) {
        super.setMessages(messages);
        isBusy = false;
    }

    public void commandAction(Command c, Displayable d) {
        super.commandAction(c, d);

        if (c == newSearch)  {
            SearchWindows searchWindows = new SearchWindows(mujMail);
            searchWindows.newSearch();
        }
    }

}
