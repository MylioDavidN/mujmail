package mujmail.threading;

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

import mujmail.MessageHeader;
import mujmail.Settings;

/**
 * Class represents algorithm for creating mail threads.
 * 
 * Algorithm is based on description written by Jamie Zawinski at
 * <a href="http://www.jwz.org/doc/threading.html">
 *   http://www.jwz.org/doc/threading.html
 * </a>
 * 
 * @author Betlista
 */
public class Algorithm {

	/** Subject for empty root messages. */
    private static final String EMPTY_MESSAGE_SUBJECT = "root";

    /** Flag signals if we want to print debug prints. */
	public static final boolean DEBUG = false; // used in MailDB too

	/** Instance of the singleton class. */
    private static final Algorithm algorithm = new Algorithm();

    /** Method to enable accessing the {@link #Algorithm()} instance. */
    public static Algorithm getAlgorithm() {
        return algorithm;
    }

    /**
     * Algorithm that group messages to so called threads - group of messages
     * that belongs together, this messages create discussion.
     * 
     * Algorithm assumes that all MessageHeaders have set parent IDs.
     * 
     * This method just call {@link #processMessage(Hashtable, MessageHeader)}
     * for each message in messageHeaders.
     * 
     * @param messageHeaders to be sorted
     * @see #processMessage(Hashtable, MessageHeader)
     */
    public ThreadedEmails invoke(final Vector/*<MessageHeaders>*/ messageHeaders) {
          if (DEBUG) System.out.println("DEBUG Algorithm.invoke(" + messageHeaders.toString() + ")");
        if ( !Settings.threading ) {
        	final ThreadedEmails threadedEmails = new ThreadedEmails();
        	final int size = messageHeaders.size();
        	for ( int i = 0; i < size; ++i) {
        		threadedEmails.addRoot( (MessageHeader)messageHeaders.elementAt(i) );
        	}
        	return threadedEmails;
        }
        try {
              //checkContent( messageHeaders );
              // idTable represents mapping, it maps message (represented by
              // message ID) to Container
            final Hashtable idTable2 = new Hashtable( messageHeaders.size() ); // Hashtable<String /*messageID*/, Vector<MessageHeader> >
              // for each message
            MessageHeader message;
            final int size = messageHeaders.size();
            for ( int i = 0; i < size; ++i) {
                message = (MessageHeader)messageHeaders.elementAt( i );
                processMessage( idTable2, message );
            }
              if (DEBUG) System.out.println("===   Creating threads   ===");
              // when all messages are processed, we have parent message for each one
            ThreadedEmails structure = createThreads( idTable2 );

              if (DEBUG) {
            	  System.out.println("DEBUG Algorithm.invoke(Vector) - end" );
                  //#ifdef MUJMAIL_DEVELOPMENT
//#             	  structure.printToConsole();
            	  //#endif
              }
            return structure;
        } catch (Throwable t) {
            System.err.println("ERROR Algorithm.invoke(Vector) - unexpected exception");
            t.printStackTrace();
              if (DEBUG) System.out.println("DEBUG Algorithm.invoke(Vector) - end, returning null");
            return null;
        }
    }

    /**
     * Checks if message ID is already in idTable.
     * <ul>
     * <li>if it's not we put the message header into ID table</li>
     * <li>when it is in idTable we check if the message header is empty (it
     *     was processed as reference in previous steps of algorithm) we
     *     simply mark it as not empty</li>
     *     <ul>
     *     <li>when the message is not empty that means we are processing same
     *         message again and that is error - exception is thrown</li>
     *     </ul>
     * </ul>
     * @param idTable
     * @param messageHeader
     * @exception IllegalStateException when message already processed wants to
     *            be processed again (or object in ID table is not instance
     *            of {@link MessageHeader message header})
     */
    private void processMessage(final Hashtable/*<String, MessageHeader>*/ idTable, final MessageHeader messageHeader) throws IllegalStateException {
          if (DEBUG) {
        	  System.out.println("DEBUG Algorithm.processMessage(..., " + messageHeader.toString() + ")");
              //#ifdef MUJMAIL_DEVELOPMENT
//#         	  dumpIDTable(idTable);
        	  //#endif
          }
        final String messageID = messageHeader.getThreadingMessageID();
          // 1. A.)
        Object o = idTable.get( messageID );
        Vector messageHeaders;
        if ( o == null ) {
              // message is not in idTable yet
        	messageHeaders = new Vector();
        	messageHeaders.addElement( messageHeader );
            idTable.put( messageID, messageHeaders );
        } else {
        	messageHeaders = (Vector)o;
            MessageHeader storedMessageHeader = (MessageHeader)messageHeaders.elementAt( 0 ); // get first element
            if ( storedMessageHeader.isEmpty() ) {
                  // container does not reference to any message
                  // this could happen when we processed message references
                  // and there was no container created for reference, so
                  // we created empty container
                //storedMessageHeader.setEmpty( false );
            	messageHeaders = new Vector();
            	messageHeaders.addElement( messageHeader );
                idTable.put( messageID, messageHeaders );
            } else {
            	messageHeaders.addElement( messageHeader );
            	// idTable3.put( messageID, messageHeader ); // not needed
            }
        }
        processMessageParentIDs(idTable, messageHeader);
    }

    /**
     * For each message loops over the list of message parents and adds the
     * parent (grandparent, ...) to ID table.
     * 
     * @param idTable
     * @param messageHeader
     * @throws IllegalStateException
     */
    private void processMessageParentIDs(final Hashtable idTable, final MessageHeader messageHeader) throws IllegalStateException {
          if (DEBUG) {
        	  System.out.println("DEBUG Algorithm.processMessageParentIDs(..., " + messageHeader.toString() + ")");
        	  //#ifdef MUJMAIL_DEVELOPMENT
//#         	  dumpIDTable(idTable);
        	  //#endif
          }
        //final Vector messageHeaders = (Vector)idTable.get( messageHeader.getThreadingMessageID() );
        final Vector parentIDs = messageHeader.getParentIDs(); // Vector<String>
        final int size = parentIDs.size();
          if (DEBUG) {
        	  System.out.println( "DEBUG Algorithm.processMessageParentIDs(..., ...) - size: " + size );
        	  System.out.println( "DEBUG Algorithm.processMessageParentIDs(..., ...) - parentIDs: " + parentIDs );
          }
        if (size == 0) return; // if there are no parent IDs we have nothing to do
        String previousParentID = null;
        String parentID = null;
        Vector parentIDVector;
        MessageHeader parentHeader = null;
        for ( int i = 0; i < size; ++i) {
              // parentID = message ID of parent message
            parentID = (String)parentIDs.elementAt( i );
            parentIDVector = (Vector)idTable.get( parentID );
            if ( parentIDVector == null ) {
            	parentHeader = null;
            } else {
            	parentHeader = (MessageHeader)parentIDVector.elementAt( 0 );
            }
            if ( parentHeader == null ) {
                  // if such message is not in idTable add it as a empty box
                parentHeader = new MessageHeader( messageHeader.getBox() );
                parentHeader.setThreadingMessageID( parentID );
                parentHeader.setSubject( EMPTY_MESSAGE_SUBJECT );
                parentHeader.setParentID( previousParentID );
                parentHeader.setEmpty( true );
                Vector vector = new Vector();
                vector.addElement( parentHeader );
                idTable.put(parentID, vector );
            }
//            parentHeader = messageHeader;
            previousParentID = parentID;
              //#ifdef MUJMAIL_DEVELOPMENT
//#               if (DEBUG) dumpIDTable(idTable);
              //#endif
        }
//      	final int parentIDVectorSize = messageHeaders.size();
          // for all messages in parent ID vector set parentID
//  		for ( int j = 0; j < parentIDVectorSize; ++j ) {
//  			((MessageHeader)messageHeaders.elementAt( j )).setParentID( parentID );
//  		}
//          break;
    }

    /**
     * idTable parameter contains e-mails threaded in threads with all levels
     * This method creates just one level  - there will be root messages and
     * in next level there will be children (and their children and so on)
     * of this root message
     * 
     * @param idTable maps messageID to {@link MessageHeader}
     */
    private ThreadedEmails createThreads( final Hashtable idTable ) {
        removeEmptyMessages( idTable );
          if (DEBUG) {
              System.out.println("DEBUG Algorithm.createThreads(Hashtable) - start:");
              //#ifdef MUJMAIL_DEVELOPMENT
//#               dumpIDTable( idTable );
              //#endif
          }
        final Enumeration messageIDs = idTable.keys();
        String messageID;
        MessageHeader actualMessageHeader;
        Vector actualMessageHeaders;
        int size;
        MessageHeader parentMessageHeader;
        Vector parentMessageHeaders;
        final ThreadedEmails threadedEmails = new ThreadedEmails();
          // for each message in idTable
          //   if it is root message (have no parentID) add it, if not skip
          //   it for now, that causes the problem
        final Vector childMessages = new Vector();
        while ( messageIDs.hasMoreElements() ) {
              // get parent message
            messageID = (String)messageIDs.nextElement();
            actualMessageHeaders = (Vector)idTable.get( messageID );
            size = actualMessageHeaders.size();
            for (int i = 0; i < size; ++i) {
	            actualMessageHeader = (MessageHeader)actualMessageHeaders.elementAt( i );
	            parentMessageHeaders = (Vector)idTable.get( actualMessageHeader.getParentID() ); // getParentId should always return at least ""
	            if ( parentMessageHeaders != null ) {
	            	parentMessageHeader = (MessageHeader)parentMessageHeaders.elementAt( 0 );
	            } else {
	            	parentMessageHeader = null;
	            }
	            if ( parentMessageHeader == null ) { // have no parent
	                threadedEmails.addRoot( actualMessageHeader );
	            } else {
	            	childMessages.addElement( actualMessageHeader.getThreadingMessageID() );
	            }
            }
        }

        final int children = childMessages.size();
//        MessageHeader rootMessage = null;
        for ( int i = 0; i < children; ++i ) {
              // get parent message
            messageID = (String)childMessages.elementAt( i );
            actualMessageHeaders = (Vector)idTable.get( messageID );
            size = actualMessageHeaders.size();
            for (int j = 0; j < size; ++j) {
	            actualMessageHeader = (MessageHeader)actualMessageHeaders.elementAt( j );
	            parentMessageHeaders = (Vector)idTable.get( actualMessageHeader.getParentID() ); // getParentId should always return at least ""
	            parentMessageHeader = (MessageHeader)parentMessageHeaders.elementAt( 0 );
	            threadedEmails.addMessage( parentMessageHeader, actualMessageHeader);
            }
        }

        threadedEmails.removeUnnecessaryEmptyMessages();
        return threadedEmails;
    }

    /**
     * Method removes not needed empty messages from structure. Not needed are
     * all empty messages.<br>
     * Example:
     * <pre>
     * -- Msg1
     *    |--Empty (not needed message)
     *    |  \-- Msg4
     *    \--Msg3
     * -- Msg2
     * </pre>
     * Should be:
     * <pre>
     * -- Msg1
     *    |--Msg4
     *    \--Msg3
     * -- Msg2
     * </pre>
     * 
     * @param idTable
     */
    private void removeEmptyMessages( final Hashtable/*<String, MessageHeader>*/ idTable ) {
          if (DEBUG) {
              System.out.println("DEBUG Algorithm.removeEmptyMessages(Hashtable) - start:");
              //#ifdef MUJMAIL_DEVELOPMENT
//#               dumpIDTable(idTable);
              //#endif
          }
          
          // first of all find messages that can be removed (have no non-empty child)
          //   find all empty messages
        final Vector/*<String>*/ emptyMessages = new Vector();
        Enumeration/*<String>*/ keys = idTable.keys();
        MessageHeader messageHeader;
        String key;
        while ( keys.hasMoreElements() ) {
            key = (String)keys.nextElement();
            messageHeader = (MessageHeader)((Vector)idTable.get( key )).elementAt( 0 );
            if ( messageHeader.isEmpty() ) {
                emptyMessages.addElement( key ); // key is threading message ID
            }
        }
          //   remove messages that are referenced
          //     find number of references for each message
        keys = idTable.keys();
        final Hashtable/*<String(messageID), Integer>*/ referenceCounts = new Hashtable( emptyMessages.size() );
        Object o;
        String parentMessageID;
        int count;
        while ( keys.hasMoreElements() ) {
            key = (String)keys.nextElement();
            messageHeader = (MessageHeader)((Vector)idTable.get( key )).elementAt( 0 );
            parentMessageID = messageHeader.getParentID();
            if ( ! "".equals( parentMessageID ) /*&& !emptyMessages.contains( messageHeader.getThreadingMessageID() )*/ ) {
                if ( ! messageHeader.isEmpty() ) { // if there is non-empty message that reference to message 
                    emptyMessages.removeElement( parentMessageID ); // remove message from empty messages
                } else {
                    o = referenceCounts.get( parentMessageID );
                    if ( o == null ) {
                        referenceCounts.put( parentMessageID, new Integer(1) );
                    } else {
                        count = ((Integer)o).intValue() + 1;
                        referenceCounts.put( parentMessageID, new Integer(count) );
                    }
                }
            }
        }
          if (DEBUG) System.out.println("DEBUG Algorithm.removeEmptyMessages(Hashtable) - empty messages (before removing referenced): " + emptyMessages );
          //     empty messages that are referenced more than 2 times are removed from empty messages
        String messageID;
        for ( int i = emptyMessages.size() - 1; i >= 0; --i ) {
            messageID = (String)emptyMessages.elementAt(i);
            o = referenceCounts.get( messageID );
            if ( o != null ) { // leaves have no references
                count = ((Integer)o).intValue();
                if ( count > 1) {
                    emptyMessages.removeElementAt( i );
                }
            }
        }
          if (DEBUG) System.out.println("DEBUG Algorithm.removeEmptyMessages(Hashtable) - empty messages: " + emptyMessages );
          // finally remove empty messages that can be removed
        keys = idTable.keys();
        Vector messageHeaders;
        int size;
        while ( keys.hasMoreElements() ) {
            key = (String)keys.nextElement();
            messageHeaders = (Vector)idTable.get( key );
            size = messageHeaders.size();
            for ( int i = 0; i < size; ++i ) {
            	messageHeader = (MessageHeader)messageHeaders.elementAt( i );
	            parentMessageID = messageHeader.getParentID();
	            while ( emptyMessages.contains( parentMessageID ) ) {
	            	parentMessageID = ((MessageHeader)((Vector)idTable.get( parentMessageID )).elementAt( 0 ) ).getParentID();
	            }
	            messageHeader.setParentID( parentMessageID );
            }
        }
        for ( int i = 0; i < emptyMessages.size(); ++i) {
            idTable.remove( emptyMessages.elementAt( i ) );
        }

          if (DEBUG) {
              System.out.println("DEBUG Algorithm.removeEmptyMessages(Hashtable) - end:");
              //#ifdef MUJMAIL_DEVELOPMENT
//#               dumpIDTable(idTable);
              //#endif
          }
    }

    //#ifdef MUJMAIL_DEVELOPMENT
//#     public void dumpIDTable( final Hashtable/*<String, Vector<MessageHeader> >*/ idTable) {
//#         if (!DEBUG) {
//#         	return;
//#         } else {
//# 	          System.out.println("DEBUG Algorithm.dumpIDTable()");
//# 	        final Enumeration keys = idTable.keys();
//# 	        String key;
//# 	        MessageHeader messageHeader;
//# 	        Vector/*MessageHeader*/ messageHeaderVector;
//# 	        int messageHeaderVectorSize;
//# 	        while ( keys.hasMoreElements() ) {
//# 	            key = (String)keys.nextElement();
//# 	            messageHeaderVector = (Vector)idTable.get( key );
//# 	            messageHeaderVectorSize = messageHeaderVector.size();
//# 	            for (int i = 0; i < messageHeaderVectorSize; ++i) {
//# 	            	messageHeader = (MessageHeader)messageHeaderVector.elementAt( i );
//# 	                System.out.println("  key: " + key + "\n    value: " + messageHeader.toString());            	
//# 	            }
//# 	        }
//#         }
//#     }
    //#endif
}
