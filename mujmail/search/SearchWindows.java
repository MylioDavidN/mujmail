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

import mujmail.util.SaveableBooleanValue;
import java.util.Enumeration;
import java.util.Vector;
import javax.microedition.lcdui.ChoiceGroup;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.DateField;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Form;
import javax.microedition.lcdui.StringItem;
import javax.microedition.lcdui.TextField;
import mujmail.Lang;
import mujmail.MujMail;
import mujmail.PersistentBox;

/**
 * The collection of forms that enables user to enter search settings and to
 * run search.
 * @author David Hauzar
 */
public class SearchWindows {
    private final MujMail mujmail;
    private SearchSettings searchSettings;
    /** true for original user interface. */
    private static final boolean ORIGINAL_UI = false;

    public SearchWindows(MujMail mujmail) {
        this.mujmail = mujmail;
    }
    
    /**
     * Displays the dialog where user can enter search settings and start the
     * search.
     */
    public void newSearch() {
        searchSettings = new SearchSettings();
        searchSettings.addAllSearchableBoxes();
        startAddSearchPhraseWindow();
    }
    
    /**
     * If the search box is not empty, displays it. If it is empty, displays
     * the window with search settings.
     */
    public void displaySearchWindow() {
        if (!mujmail.getSearchBox().isEmpty()) {
            displaySearchBox();
        } else {
            newSearch();
        }
    }
    
    private class MyChoiceGroup extends ChoiceGroup {

        public MyChoiceGroup(String header, int mode) {
            super(header, mode);
        }
        
        /**
         * Saves selected state of items corresponding to items in this choice
         * group.
         * Note that vector items must have the same length or must be longer
         * than the number of items in this choice group.
         * @param items the vector of object implementing SaveableBooleanValue
         *  interface.
         */
        public void saveSelectedStateOfCorrespondingItems(Vector items) {
            for (int i = 0; i < size(); i++) {
                SaveableBooleanValue item = (SaveableBooleanValue) items.elementAt(i);
                item.saveBoolean(isSelected(i));
            }
        }
        
        /**
         * Gets elements from vector items that have the same index as items that
         * are selected in this choice group.
         * Note that vector items must have the same length or must be longer
         * than the number of items in this choice group.
         * @param items the items to be filtered.
         * @return elements from vector items that correspond to selected items
         *  in this choice group.
         */
        public Vector getItemsCorrespondingToSelected(Vector items) {
            Vector correspondingToSelected = new Vector();
            for (int i = 0; i < size(); i++) {
                if (isSelected(i)) {
                    correspondingToSelected.addElement(items.elementAt(i));
                }
            }
            
            return correspondingToSelected;
        }
        
        /**
         * Gets elements from enumeration items that have the same position as 
         * items that are selected in this choice group.
         * Note that enumeration items must have the same length or must be longer
         * than the number of items in this choice group.
         * @param items the items to be filtered.
         * @return elements from enumeration items that correspond to selected items
         *  in this choice group.
         */
        public Vector getItemsCorrespondingToSelected(Enumeration items) {
            Vector correspondingToSelected = new Vector();
            int i = 0;
            while (items.hasMoreElements()) {
                Object item = items.nextElement();
                if (isSelected(i)) {
                    correspondingToSelected.addElement(item);
                }
                
                i++;
            }
            
            return correspondingToSelected;
        }
        
    }
    
    /**
     * Shows windows where user can append new serching phrase.
     * 
     * Let user enter search settings and returns it.
     * search settings entered by the user.
     *  null if user presses cancel button
     *  
     */
    private void startAddSearchPhraseWindow() {
        mujmail.getDisplay().setCurrent(new AddSearchPhraseForm());
    }
    
    /**
     * Do search and display search results.
     */
    private void doSearchDisplayResults() {
        mujmail.getSearchBox().startSearchInNewThreadDisplayResults(searchSettings);
    }
    
    private void displaySearchBox() {
        mujmail.getDisplay().setCurrent( mujmail.getSearchBox() );
    }
    
    /**
     * Form that displays search settings.
     */
    private class SearchSettingsForm extends Form {
        private final Command ok;
        private final MyChoiceGroup boxes;
        private final DateField dateFrom;
        private final DateField dateTo;
        private final StringItem dateHeader;

        public SearchSettingsForm(AddSearchPhraseForm previousPhrase) {
            super(Lang.get(Lang.SEA_ADVANCED_SETTINGS));
            
            dateHeader = new StringItem(Lang.get(Lang.SEA_DATE_INTERVAL), "");
            append(dateHeader);
            dateFrom = new DateField(Lang.get(Lang.SEA_DATE_FROM), DateField.DATE);
            append(dateFrom);
            dateTo = new DateField(Lang.get(Lang.SEA_DATE_TO), DateField.DATE);
            append(dateTo);
            
            boxes = new MyChoiceGroup(Lang.get(Lang.SEA_SEARCH_IN_MAILBOXES), ChoiceGroup.MULTIPLE);
            addBoxesInWhichSearch();
            append(boxes);
            
            ok = new Command(Lang.get(Lang.BTN_OK), Command.OK, 1);
            addCommand(ok);
            setCommandListener(new SearchSettingsCommandListener(this, previousPhrase));
        }
        
        private void addBoxesInWhichSearch() {
            Vector searchableBoxes = PersistentBox.getPersistentBoxes();
            for (int i = 0; i < searchableBoxes.size(); i++) {
                PersistentBox searchableBox = (PersistentBox) searchableBoxes.elementAt(i);
                boxes.append(searchableBox.getName(), null);
                boxes.setSelectedIndex(i, searchableBox.loadBoolean());
            }
        }
        
    }
    
    /**
     * Listens for events of given SearchSettingsForm
     */
    private class SearchSettingsCommandListener implements CommandListener {
        private final SearchSettingsForm searchSettingsForm;
        private final AddSearchPhraseForm previousPhrase;

        public SearchSettingsCommandListener(SearchSettingsForm searchSettingsForm, AddSearchPhraseForm previusPhrase) {
            this.searchSettingsForm = searchSettingsForm;
            this.previousPhrase = previusPhrase;
        }
        

        public void commandAction(Command command, Displayable arg1) {
            if (command == searchSettingsForm.ok) {
                saveAddPhraseSettings();
                addSelectedBoxesToSearchSettings();
                setDateIntervalInSearchSettings();
                
                mujmail.getDisplay().setCurrent(previousPhrase);
            }
        }

        private Date getDateFromForm(java.util.Date date, Date dateIfNull) {
            if (date == null) {
                return dateIfNull;
            }

            return new Date(date.getTime());
        }
        
        private void saveAddPhraseSettings() {
            searchSettingsForm.boxes.saveSelectedStateOfCorrespondingItems(
                    PersistentBox.getPersistentBoxes());
        }

        private void addSelectedBoxesToSearchSettings() {
            searchSettings.removeBoxes();
            searchSettings.addBoxes(searchSettingsForm.boxes.getItemsCorrespondingToSelected(PersistentBox.getPersistentBoxes()));
        }

        private void setDateIntervalInSearchSettings() {
            Date from = getDateFromForm(searchSettingsForm.dateFrom.getDate(), Date.MINIMUM_DATE);
            Date to = getDateFromForm(searchSettingsForm.dateTo.getDate(), Date.MAXIMUM_DATE);
            searchSettings.setDateInterval(DateInterval.createDateInterval(from, to));            
        }
        
    }
    
    
    
    /**
     * Form that enables to enter the search phrase.
     */
    private class AddSearchPhraseForm extends Form {
        private final Command search;
        private final Command back;
        private final Command addNewSearchPhrase;
        private final Command searchSettings;
        private final TextField searchPhrase;
        private final ChoiceGroup requiredChG;
        private final ChoiceGroup wholeWordsChG;
        private final MyChoiceGroup messagePartsChG;

        public AddSearchPhraseForm() {
            super(Lang.get(Lang.SEA_ENTER_SEARCH_PHRASE));
            
            searchPhrase = new TextField(Lang.get(Lang.SEA_SEARCH_PHRASE), "", 256, TextField.ANY);
            append(searchPhrase);
            
            requiredChG = new ChoiceGroup("", ChoiceGroup.MULTIPLE);
            if (ORIGINAL_UI) requiredChG.append(Lang.get(Lang.SEA_REQUIRED), null);
            else requiredChG.append("All phrases must match", null);
            append(requiredChG);
            
            wholeWordsChG = new ChoiceGroup("", ChoiceGroup.MULTIPLE);
            wholeWordsChG.append(Lang.get(Lang.SEA_WHOLE_WORDS), null);
            append(wholeWordsChG);
            
            messagePartsChG = new MyChoiceGroup(Lang.get(Lang.SEA_SEARCH_IN_MSG_PARTS), ChoiceGroup.MULTIPLE);
            addMessagePartsInWhichSearch();
            append(messagePartsChG);
            
            search = new Command(Lang.get(Lang.BTN_SEA_SEARCH), Command.OK, 4);
            back = new Command(Lang.get(Lang.BTN_BACK), Command.BACK, 3);
            addNewSearchPhrase = new Command(Lang.get(Lang.BTN_SEA_ADD_NEW_PHRASE), Command.ITEM, 2);
            searchSettings = new Command(Lang.get(Lang.SEA_ADVANCED_SETTINGS), Command.ITEM, 1);
            addCommand(search);
            addCommand(back);
            if (ORIGINAL_UI) addCommand(addNewSearchPhrase);
            addCommand(searchSettings);
            
            setCommandListener(new AddSearchPhraseCommandListener(this));
        }

        /**
         * Gets the array of string search phrases contained in this form.
         * @return String array with phrases entered into this windows.
         */
        public String[] getSearchPhrases() {
            if (ORIGINAL_UI) {
                String[] phrases = {searchPhrase.getString()};
                return phrases;
            }

            Vector phrases = new Vector(1);
            String phrasesString = searchPhrase.getString();
            int start = 0;
            while (start < phrasesString.length()) {
                int end;
                if (phrasesString.charAt(start) == '"') {
                    start++;
                    end = phrasesString.indexOf("\"", start);
                } else {
                    end = phrasesString.indexOf(" ", start);
                }
                if (end == -1) end = phrasesString.length();
                String phrase = phrasesString.substring(start, end);
                phrases.addElement(phrase);

                if (start != 0 && phrasesString.charAt(start-1) == '"') {
                    start = end + 2;
                } else {
                    start = end + 1;
                }
            }

            String[] phrasesArray = new String[phrases.size()];
            phrases.copyInto(phrasesArray);

            return phrasesArray;
        }
        
        /**
         * Returns the importance of the phrase that user marked.
         * @return the importance of the phrase that user marked.
         */
        public SearchPhrase.PhraseImportance getPhraseImportance() {
            if (requiredChG.isSelected(0)) {
                return SearchPhrase.PhraseImportance.REQUIRED;
            } else {
                return SearchPhrase.PhraseImportance.OPTIONAL;
            }
        }
        
        public FulltextSearchModes getSearchMode() {
            if (wholeWordsChG.isSelected(0)) {
                return FulltextSearchModes.WHOLE_WORDS;
            } else {
                return FulltextSearchModes.NOT_WHOLE_WORDS;
            }
        }
        
        private void addMessagePartsInWhichSearch() {
            Vector messageParts = SearchMessagePart.getAllMessageParts();
            for (int i = 0; i < messageParts.size(); i++) {
                SearchMessagePart messagePart = (SearchMessagePart) messageParts.elementAt(i);
                messagePartsChG.append(messagePart.toString(), null);
                messagePartsChG.setSelectedIndex(i, messagePart.loadBoolean());
            }
        }
        
    }
    
    /**
     * Listenes for events of AddSearchPhraseForm.
     */
    private class AddSearchPhraseCommandListener implements CommandListener {
        private final AddSearchPhraseForm searchPhraseForm;

        public AddSearchPhraseCommandListener(AddSearchPhraseForm searchPhraseForm) {
            this.searchPhraseForm = searchPhraseForm;
        }
        

        public void commandAction(Command command, Displayable displayable) {
            if (command == searchPhraseForm.search) {
                addSearchPhrasesToSettings();
                doSearchDisplayResults();
            } else if (command == searchPhraseForm.back) {
                mujmail.mainMenu();
            } else if (command == searchPhraseForm.addNewSearchPhrase) {
                addSearchPhrasesToSettings();
                startAddSearchPhraseWindow();
            } else if (command == searchPhraseForm.searchSettings) {
                startSearchSettingsWindow();
            }
        }

        /**
         * Adds search phrases contained in edit box to search settings.
         */
        private void addSearchPhrasesToSettings() {
            saveAddPhraseSettings();
            String[] phrases = searchPhraseForm.getSearchPhrases();
            for (int i = 0; i < phrases.length; i++) {
                searchSettings.addSearchPhrase(
                    new SearchPhrase(
                    phrases[i],
                    searchPhraseForm.getSearchMode(),
                    searchPhraseForm.getPhraseImportance(),
                    getSelectedMessageParts()));
            }
        }
        
        private void startSearchSettingsWindow() {
            mujmail.getDisplay().setCurrent(new SearchSettingsForm(searchPhraseForm));
        }
        
        private void saveAddPhraseSettings() {
            searchPhraseForm.messagePartsChG.saveSelectedStateOfCorrespondingItems(
                    SearchMessagePart.getAllMessageParts());
        }
        
        /**
         * Gets selected message parts in searchPhraseForm.
         * @return vector of selected message parts in searchPhraseForm.
         */
        private Vector getSelectedMessageParts() {
            return searchPhraseForm.messagePartsChG.getItemsCorrespondingToSelected(
                    SearchMessagePart.getAllMessageParts());
        }
        
    }
    
}
