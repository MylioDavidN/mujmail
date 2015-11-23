
package mujmail;

import javax.microedition.lcdui.AlertType;

  // TODO: this class should be implement Singleton design pattern instead of static class
/**
 * Holds information about mobile phone, environment and limitations
 */
public class Properties {
    /** The maximum number of letters which can be used when creating new directory */
    public static final int directoryLength = 20;
    
    /** Name of mobile Model */
    public static String model = "!!undefined";
    /** Mobile phone manufacturer and type string */
    public static String platform = "!!undefined"; 
    
    /** If flag set than mail text fields have strange behavior, use generic field type instead */
   public static boolean textFieldMailIncorrect = false;
    
   /** If set BasicConnections successors have to write to standard output data which sending and receiving */ 
   public static final boolean debugConnections = false;

    /*
     * Here are defined features enabled/disabled in comliped mujMail 
     *
    //mujMail ssl .. cca 100kb
    //define MUJMAILS_SSL
    // compressed connections .. cca 10kb
    //define MUJMAIL_COMPRESSED_CONNECTION
    // Debug console .. cca 3kb
    //define MUJMAIL_DEBUG_CONSOLE
    // Search capability .. cca 20kb
    //define MUJMAIL_SEARCH
    // File system support .. cca 17kb
    //define MUJMAIL_FS
    // Synchronisation .. cca 10kb
    //Define MUJMAIL_SYNC
    // Touch screen support .. cca 5kb
    //define MUJMAIL_TOUCH_SCR
    // User folders .. cca 10kb
    //define MUJMAIL_USR_FOLDERS
    // HTML support .. cca 14kb
    //undefine MUJMAIL_HTML
    
    // For better debugging support
    //define MUJMAIL_DEVELOPMENT
     */
    
    //#ifdef MUJMAIL_FS
    /** Indicates whether there is jsr75 available (FileSystem). */
    private static boolean jsr75 = true;
    
    /** 
     * Indicates whether there is jsr75 available (FileSystem). 
     * @return true if JSR75 is available
     */
    public static boolean JSR75Available(){
        return jsr75;
    }
    //#endif
    
    /**
     * Initialize visible static fields of class
     */
    static { // Static constructor
        try {
            platform = System.getProperty( "microedition.platform");
        } catch(Exception ex) {}

        if ( Properties.platform != null ) {
            // Separate different manufacturers
            if (Properties.platform.startsWith("SonyEricsson")) {
                /*String */ model = Properties.platform.substring(12);
                Integer modelNumber = new Integer(0);
                try {
                    modelNumber = Integer.valueOf( platform.substring(13, 15)); //80
                    modelNumber = Integer.valueOf( platform.substring(13, 16)); //800
                    modelNumber = Integer.valueOf( platform.substring(13, 17)); //8000
                } catch (Exception e) {}
                        
                if (Properties.model.startsWith("K") && modelNumber.intValue()>=800) textFieldMailIncorrect = true;
                
            }
        } // Platform not null
        
        //#ifdef MUJMAIL_FS
        // JSR75
       try {
            //Class c = 
            Class.forName("javax.microedition.io.file.FileSystemRegistry");
        } catch (ClassNotFoundException e) {
            jsr75 = false;
        }
        //#endif

        // Another property
    }

    /**
     * Shows alert if functionality that is supported by mujMail 
     * isn't supported by mobile phone.
     * <p>Currently alerts missing filesystem support.
     *
     * @param alert Alert manager (use alert instance from mujMail)
     */
    public static void showStartupAlerts( MyAlert alert) {
        //#ifdef MUJMAIL_FS
        if (jsr75 == false) {
            alert.setAlert(Lang.get( Lang.ALRT_MOB_JSR75), AlertType.ERROR);
            try {
                Thread.sleep(3000); // if not stopped, alert is in short time, repainted by menu
            } catch (Exception e) {}
        }
        //#endif
    }

}
