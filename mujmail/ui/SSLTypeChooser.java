//#condition MUJMAIL_SSL
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

package mujmail.ui;

import javax.microedition.lcdui.ChoiceGroup;
import javax.microedition.lcdui.Form;
import javax.microedition.lcdui.Choice;

import mujmail.Lang;

/**
 * Serves to choose the type of used ssl implementation.
 * 
 * @author David Hauzar
 */
public class SSLTypeChooser {
    private final Form form;
    private final int positionInForm;
    private final ChoiceGroup choiceGroup;

    /**
     * Construct ssl type chooser.
     * @param form the form to that this chooser is inserted.
     * @param positionInForm the position in form in that the chooser is inserted
     */
    public SSLTypeChooser(Form form, int positionInForm) {
        this.form = form;
        this.positionInForm = positionInForm;
        
        choiceGroup = new ChoiceGroup(Lang.get(Lang.AS_SSL_TYPE), Choice.EXCLUSIVE);
        SSLType.MUJMAIL_SSL.setIndexInChoiceGroup( choiceGroup.append(Lang.get(Lang.AS_MUJMAIL_SSL), null) );
        SSLType.FIRMWARE_SSL.setIndexInChoiceGroup( choiceGroup.append(Lang.get(Lang.AS_FIRMWARE_SSL), null) );
        
    }

    /**
     * Gets choice group that enables choosing of ssl type.
     * @return the choice group that enables choosing of ssl type.
     */
    public ChoiceGroup getChoiceGroupObject() {
        return choiceGroup;
    }

    /**
     * Set the ssl type that will be selected in this <code>SSLTypeChooser</code>.
     *
     * @param sslType the ssl type that will be selected in this <code>SSLTypeChooser</code>.
     *  1 for mujMail SSL
     * `0 for firmware SSL.
     */
    public void setSelectedType(int sslType) {
        if (sslType == SSLType.FIRMWARE_SSL.typeNumber) {
            choiceGroup.setSelectedIndex(SSLType.FIRMWARE_SSL.getIndexInChoiceGroup(), true);
        }
        
        if (sslType == SSLType.MUJMAIL_SSL.typeNumber) {
            choiceGroup.setSelectedIndex(SSLType.MUJMAIL_SSL.getIndexInChoiceGroup(), true);
        }
    }
    
    
    /**
     * Deletes ssl type chooser to form to the form to that this ssl type chooser
     * belongs.
     */
    public void deleteFromForm() {
        form.delete(positionInForm);
    }
    
    /**
     * Appends the ssl type chooser to the form to that this ssl type chooser
     * belongs.
     */
    public void appendToForm() {
        form.append(choiceGroup);
    }
    
    /**
     * Inserts the ssl type chooser to the form to that this ssl type chooser
     * belongs.
     */
    public void insertToForm() {
        form.insert(positionInForm, choiceGroup);
    }
    
    /**
     * Get the type of ssl that is actually chosen.
     * 
     * @return the type of ssl that is actually chosen.
     */
    private SSLType getSSLTypeChosen() {
        if (choiceGroup.getSelectedIndex() == SSLType.FIRMWARE_SSL.indexInChoiceGroup) {
            return SSLType.FIRMWARE_SSL;
        } else {
            return SSLType.MUJMAIL_SSL;
        }
    }
    
    /**
     * Get the type of ssl that is actually chosen.
     * 
     * @return the type of ssl that is actually chosen.
     *  0 for firmware ssl choosen
     *  1 for mujMailssl choosen
     */
    public int getSSLTypeNumberChosen() {
        return getSSLTypeChosen().typeNumber;
    }
    
    
    private static class SSLType {
        private final int typeNumber;
        private int indexInChoiceGroup;
        
        private SSLType() { typeNumber = -1; };
        private SSLType(int typeNumber) {
            this.typeNumber = typeNumber;
        }
        
        private void setIndexInChoiceGroup(int indexInChoiceGroup) {
            this.indexInChoiceGroup = indexInChoiceGroup;
        }
        
        private int getIndexInChoiceGroup() {
            return indexInChoiceGroup;
        }
        
        public static final SSLType MUJMAIL_SSL = new SSLType(1);
        public static final SSLType FIRMWARE_SSL = new SSLType(0);
    }

}
