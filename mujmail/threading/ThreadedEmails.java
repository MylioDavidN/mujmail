package mujmail.threading;

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.NoSuchElementException;
import java.util.Vector;

import mujmail.util.Functions;
import mujmail.IStorage;
import mujmail.MessageHeader;
import mujmail.Settings;
import mujmail.ordering.Comparator;

/**
 * This class represents structure for threaded e-mail.
 * Structure contains vector of root messages and for each root message
 * it contains {@link Vector} of child messages. Child messages vectors
 * are stored in hash map, in which root message ID is the key.
 * 
 * {@link ThreadedEmails} structure is used also for other boxes too
 * (f.e. Outbox, Draft, ...). In these boxes we do not need threads, so
 * in these cases only root messages are used and, for memory saving,
 * there are not empty vectors, but nulls.
 * 
 * @author Betlista
 */
public class ThreadedEmails implements IStorage {

    /** Flag signals if we want to print debug prints */
    private static final boolean DEBUG = false;

    /* *
     * While Hashtable does not support adding nulls, we use this
     * constant to represent that there is nothing in hashtable stored under
     * some key.
     */
//    private static final Object NULL = new Object(); // why not Vector? Because Object is the simplest class.

    /** Root messages, threads (with or without children). */
    private Vector/*<MessageHeader>*/ rootMessages;

    /**
     * Child vectors mapped in map, in which parent message ID is key under
     * which child vector is stored.
     */
    private Hashtable/*< String (parentID - threadedMessageID), Vector<MessageHeader> >*/ children;

    /**
     * Mapping for getting parent for child ID.
     */
    private Hashtable/*< String (childID - threadedMessageID), MessageHeader>*/ parents;

    /**
     * Number of e-mail headers stored in structure.
     */
    int size;

    /**
     * Creates empty structure.
     */
    public ThreadedEmails() {
        children = new Hashtable();
        parents = new Hashtable();
        rootMessages = new Vector();
        size = 0;
        lastReturnedHeader = null;
        lastReturnedIndex = 0;
    }

    /**
     * <p>Returns the index at which the message is stored in vector.</p>
     * <p>
     * Invariant of this method is, that this condition have to be true:<br>
     * <code>
     *   // index is the index returned by this method<br>
     *   // messages and threadingMessageID are parameters<br>
     * threadingMessageID.equals( messages.elementAt(index).getThreadingMessageID() )
     * </code>
     * </p>
     * @param messages vector to be searched in
     * @param threadingMessageID message ID we are looking message for
     * @return index of the message in the vectors
     */
    private int indexOf(final Vector/*<MessageHeaders>*/ messages, final String threadingMessageID) {
        final int size = messages.size();
        MessageHeader header;
        for (int i = 0; i < size; ++i) {
            header = (MessageHeader)messages.elementAt( i );
            if ( threadingMessageID.equals( header.getThreadingMessageID() ) ) {
                return i;
            }
        }
        return -1;
    }
    /**
     * Adds root (new thread) to vector of root messages.
     * 
     * @param messageHeader e-mail header that represents thread parent
     *        message
     */
    public void addRoot(final MessageHeader messageHeader) {
          if (DEBUG) System.out.println("DEBUG ThreadedEmails.addRoot(MessageHeader) - message header: " + messageHeader );
          if (DEBUG) System.out.println("DEBUG ThreadedEmails.addRoot(MessageHeader) - size (start): " + size);
          // if the message is the empty root message and showing of these 
          //   messages is turned off, such message is skipped
//        if ( !Settings.rootsVisible && messageHeader.isEmpty() ) {
//        	return;
//        }
        final int index = indexOf(rootMessages, messageHeader.getThreadingMessageID() );
          if (DEBUG) System.out.println("DEBUG ThreadedEmails.addMessage(MessageHeader, MessageHeader) - index: " + index);
          // if there is not this message in root messages
        if ( index == -1 ) {
            rootMessages.addElement( messageHeader );
            //String messageID = messageHeader.getThreadingMessageID();
            //children.put(messageID, NULL); // have no children yet
            //parents.put(messageID, NULL); // have no parent too (it's root message)
            ++size;
        } else {
            MessageHeader header = (MessageHeader)rootMessages.elementAt( index );
            if ( header.isEmpty() ) {
                rootMessages.setElementAt( messageHeader, index);
            } else {
                  // this is here because we support multiple messages with same
                  //   threading message ID
                rootMessages.addElement( messageHeader );
                ++size;
            }
        }
          if (DEBUG) System.out.println("DEBUG ThreadedEmails.addRoot(MessageHeader) - size (end): " + size);
    }

    /**
     * Adds message to thread. rootMessageID could be different from
     * messageHeaders parent ID.
     * 
     * @param parentMessage e-mail header message ID of the parent message
     * @param messageHeader e-mail header to be added to the thread
     */
    public void addMessage( final MessageHeader parentMessage, final MessageHeader messageHeader ) {
          if (DEBUG) System.out.println("DEBUG ThreadedEmails.addMessage(MessageHeader, MessageHeader) - rootMessage: " + parentMessage + ", messageHeader: " + messageHeader);
          if (DEBUG) System.out.println("DEBUG ThreadedEmails.addMessage(MessageHeader, MessageHeader) - size (start): " + size);
          // when threading is not enabled - all messages are added to the root messages
        if ( ! Settings.threading ) {
        	addRoot( messageHeader );
        	return;
        }
          // roots are added yet
          // add to parents
        parents.put( messageHeader.getThreadingMessageID(), parentMessage );
          // add to children
        Object childVector = children.get( parentMessage.getThreadingMessageID() );
        if ( childVector instanceof Vector ) {
            ((Vector)childVector).addElement( messageHeader );
            ++size;
        } else {
            Vector v = new Vector();
            v.addElement( messageHeader );
            children.put( parentMessage.getThreadingMessageID(), v );
            ++size;
        }
    }

    /* ************************
     *    Interface methods   *
     **************************/

    /*
     * (non-Javadoc)
     * @see mujmail.IStorage#getEnumeration()
     */
    public Enumeration getEnumeration() {
        return new Enumerator();
    }

    /**
     * Simply returns number of e-mail headers in structure.
     * Number of messages is increased when
     * {@link #addMessage(MessageHeader, MessageHeader)} is called.
     * 
     * @return number of e-mail headers in structure.
     */
    public int getSize() {
        return size;
    }

    /**
     * Returns whether structure is empty.
     * 
     * @return true if structure is empty (size == 0), false when size > 0
     */
    public boolean isEmpty() {
        return size == 0;
    }

    private int lastReturnedIndex = 0;
    private MessageHeader lastReturnedHeader = null;

    //#ifdef MUJMAIL_TEST_GET_MESSAGE_AT
//#     public static int simpleNext = 0;
//#     public static int simplePrevious = 0;
//#     public static int nextFromZero = 0;
    //#endif

    /*
     * (non-Javadoc)
     * @see mujmail.IStorage#getMessageAt(int)
     */
    public MessageHeader getMessageAt(final int index) {
          if (DEBUG) { System.out.println("DEBUG ThreadedEmails.getMessageAt(" + index + ")" ); }
          //System.out.println("  lastReturnedIndex = " + lastReturnedIndex);
          //System.out.println("  lastReturnedHeader= " + lastReturnedHeader);

        /* TODO (Betlista): this implementation is really stupid, but
           *   this method should not be used, I want to remove usage of this
           *   method from the code
           */

          /* Betlista:
           * Previous comment is correct, but we can assume that when user uses
           * this method he wants with quite good possibility the next message
           * he wanted before - on this is based next solution:
           * 
           * I remember previously returned message and it's index and when user
           * request message with index i than:
           *   when i > lastReturnedIndex
           *     than it's called nextElement() until lastReturned == i
           *   when i < lastReturnedIndex && i > (lastReturnedIndex - i)
           *     than it's called previousElement() until lastReturned == i
           *   when i < lastReturnedIndex && i < (lastReturnedIndex - i)
           *     than it's called nextElement() for i times (from zero)
           * 
           * Note: lastReturnedIndex is shifted by one again current position
           *  lastReturnedIndex=5 means that we returned 4th position (position are conted from 0)
           */
        final Enumerator messages = new Enumerator();
        MessageHeader messageHeader = null;

        // Note: This guard has to be here because other 3 possibilities (noted above)
        //  moves index immediately
        // We want to know same position as in last call
        if (index == lastReturnedIndex - 1) { // -1 is shift between lastReturedIndex and real position
            return lastReturnedHeader;
        }
        
        if (index >= lastReturnedIndex) {
            //#ifdef MUJMAIL_TEST_GET_MESSAGE_AT
//#             ++simpleNext;
            //#endif
            messages.actual = lastReturnedHeader;
            messages.index = lastReturnedIndex;
            while ( messages.hasNextElement() ) {
                messageHeader = (MessageHeader)messages.nextElement();
                  //if (DEBUG) { System.out.println(" (" + (messages.index) + ") " + messageHeader); }
                if ( messages.index - 1 == index ) {
                    lastReturnedHeader = messages.actual;
                    lastReturnedIndex = messages.index;
                    break;
                }
            }
        } else if ( index < lastReturnedIndex ) {
            if ( index < lastReturnedIndex - index ) {
                //#ifdef MUJMAIL_TEST_GET_MESSAGE_AT
//#                 ++nextFromZero;
                //#endif
                while ( messages.hasNextElement() ) {
                    messageHeader = (MessageHeader)messages.nextElement();
                    //if (DEBUG) { System.out.println(" (" + (messages.index) + ") " + messageHeader); }
                    if ( messages.index - 1 == index ) {
                        lastReturnedHeader = messages.actual;
                        lastReturnedIndex = messages.index;
                        break;
                    }
                }
            } else {
                //#ifdef MUJMAIL_TEST_GET_MESSAGE_AT
//#                 ++simplePrevious;
                //#endif
                messages.actual = lastReturnedHeader;
                messages.index = lastReturnedIndex;
                while ( messages.hasPreviousElement() ) {
                    messageHeader = (MessageHeader)messages.previousElement();
                    //if (DEBUG) { System.out.println(" (" + (messages.index) + ") " + messageHeader); }
                    if ( messages.index - 1 == index ) {
                        lastReturnedHeader = messages.actual;
                        lastReturnedIndex = messages.index;
                        break;
                    }
                }
            }
        }
          //System.out.println("Result (" + index + ")=" + messageHeader);
          //System.out.println("  lastReturnedIndex = " + lastReturnedIndex);
          //System.out.println("  lastReturnedHeader= " + lastReturnedHeader);
          //System.out.println();
        return messageHeader;
    }

    /**
     * Removes all messages from structure.
     */
    public void removeAllMessages() {
        children.clear();
        parents.clear();
        rootMessages.removeAllElements();
        size = 0;
    }

    /**
     * <p>Removes message at the i-th position.</p>
     * <p>
     * Note: This structure is not designed to add/remove messages one by one.
     *   This method is a little bit more difficult than just removing message
     *   from {@link Vector}. Message have to be removed first and than
     *   messages in structure have to be resorted (new threads created).
     * </p>
     */
    public void removeMessageAt(final int index) {
        if (DEBUG) { System.out.println("DEBUG ThreadedEmails.removeMessageAt(int index="+ index + ")"); }
        final Enumeration messages = getEnumeration();
        final Vector remainingMessages = new Vector();
        MessageHeader messageHeader;
        int i = 0;
        while ( messages.hasMoreElements() ) {
            messageHeader = (MessageHeader)messages.nextElement();
            if ( i != index ) {
                remainingMessages.addElement( messageHeader );
            } else {
                if (DEBUG) { System.out.println("DEBUG ThreadedEmails.removeMessageAt  removing MH=" + messageHeader); }
            }
            ++i;
        }

        createNewStructure(remainingMessages);
    }

    /**
     * <p>Removes message equal to passed message.</p>
     * <p>
     * Note: This structure is not designed to add/remove messages one by one.
     *   This method is a little bit more difficult than just removing message
     *   from {@link Vector}. Message have to be removed first and than
     *   messages in structure have to be resorted (new threads created).
     * </p>
     */
    public void removeMessage(final MessageHeader messageToRemove) {
          if (DEBUG) System.out.println("DEBUG ThreadedEmails.removeMessage(MessageHeader=" + messageToRemove + ")");
        final Enumeration messages = getEnumeration();
        final Vector remainingMessages = new Vector();
        MessageHeader messageHeader;
        while ( messages.hasMoreElements() ) {
            messageHeader = (MessageHeader)messages.nextElement();
            if ( messageToRemove.equals( messageHeader ) ) {
                continue;
            }
            remainingMessages.addElement(messageHeader);
        }

        createNewStructure(remainingMessages);
          if (DEBUG) System.out.println("removeMessage(MessageHeader) - end");
    }

    /**
     * Method recreates structure from passed messages.
     * 
     * @param messages messages to build this structure from
     */
    private void createNewStructure(final Vector messages) {
        ThreadedEmails newStructure = Algorithm.getAlgorithm().invoke( messages );
        this.rootMessages = newStructure.rootMessages;
        this.children = newStructure.children;
        this.parents = newStructure.parents;
        this.size = newStructure.size;
    }

    /**
     * <p>Adds message to structure.</p>
     * 
     * <p>
     * Note: This structure is not designed to add/remove messages one by one.
     *   This method is a little bit more difficult than just adding message
     *   to {@link Vector}. Message have to be added first and than
     *   messages in structure have to be resorted (new threads created).
     * </p>
     */
    public void addMessage(final MessageHeader newMessageHeader) {
        if ( newMessageHeader.getThreadingMessageID() == null ) {
            throw new IllegalArgumentException( "message have to have threading ID set" );
        }
        final Enumeration messages = getEnumeration();
        final Vector newMessages = new Vector();
        MessageHeader messageHeader;
        while ( messages.hasMoreElements() ) {
            messageHeader = (MessageHeader)messages.nextElement();
            newMessages.addElement( messageHeader );
        }
        newMessages.addElement( newMessageHeader );

        createNewStructure( newMessages );
    }

    public void sort(Comparator comparator) {
        if (DEBUG) {
            //#ifdef MUJMAIL_DEVELOPMENT
//#             printToConsole();
            //#endif
            System.out.println("comparator: " + comparator);
            System.out.println("sorting rootMessages...");
        }
        Functions.sort(rootMessages, comparator);
        MessageHeader messageHeader;
        //Vector/*<MessageHeader>*/ childMessages;
        for ( int i = 0; i < rootMessages.size(); ++i ) {
            messageHeader = (MessageHeader)rootMessages.elementAt( i );
            sortChildren( (Vector)children.get( messageHeader.getThreadingMessageID() ), comparator );
        }
    }

    private void sortChildren( Vector v, Comparator c ) {
        if ( v == null ) {
            return;
        }
        Functions.sort(v, c);
        for ( int i = 0; i < v.size(); ++i ) {
            sortChildren( (Vector)children.get( v.elementAt(i) ), c);
        }
    }

    public boolean isRootMessage(final MessageHeader messageHeader) {
        return rootMessages.contains( messageHeader );
    }

    /**
     * Returns level of the message in message tree.
     * 
     * @param messageThreadingID threading message ID for which find the level
     * @return level n which the message is stored in tree
     */
    public int getLevel(final String messageThreadingID ) {
        int level = 0;

        String childMessageThreadingID = messageThreadingID;
        MessageHeader parentMessage = (MessageHeader)parents.get( childMessageThreadingID );
        while ( parentMessage != null ) {
              // if there is parent increment level
            ++level;
              // find grant parent - parent of previous parent message
            parentMessage = (MessageHeader)parents.get( parentMessage.getThreadingMessageID() );
        }

        return level;
    }

    public boolean hasChildren(final MessageHeader messageHeader) {
        if ( !isRootMessage(messageHeader) ) {
            return false;
        }
        Object o = children.get( messageHeader.getThreadingMessageID() );
        if (o instanceof Vector) {
            Vector v = (Vector)o;
            return !v.isEmpty();
        } else {
            if (DEBUG) { System.out.println( "DEBUG ThreadedEmails.hasChildren(MessageHeader) " + o.getClass() ); } // TODO: remove 
        }
        return false;
    }

    /**
     * Returns number of empty root messages.
     * In ThreadedEmails structure only root messages can be empty. This method
     * iterate over these headers and return number of the empy ones.
     * 
     * @return number of empty root messages
     */
    public int getEmptyRootsNumber() {
    	int result = 0;
    	final int rootMessagesCount = rootMessages.size();
    	for ( int i = 0; i < rootMessagesCount; ++i) {
    		if ( ((MessageHeader)rootMessages.elementAt( i )).isEmpty() ) {
    			++result;
    		}
    	}
    	return result;
    }

    /**
     * When empty message have child which is empty, it moves children
     * of this child to the empty parent message.<br>
     * Example:
     * <pre>
     * --1 (empty)
     *   \--10 (empty)
     *      |--A (not empty)
     *      \--B (not empty)
     * </pre> 
     * Result should be:
     * <pre>
     * --1
     *   |--A
     *   \--B
     * </pre>
     * At the end it removes all empty root messages without no children.
     */
    public void removeUnnecessaryEmptyMessages() {
    	  if ( DEBUG ) {
    		  System.out.println( "DEBUG ThreadedEmails.removeUnnecessaryEmptyMessages()" );
              //#ifdef MUJMAIL_DEVELOPMENT
//#     		  printToConsole();
    		  //#endif
    	  }
    	final int rootsCount = rootMessages.size();
        MessageHeader messageHeader;
        Vector childVector;
    	for (int i = 0; i < rootsCount; ++i ) {
            messageHeader = (MessageHeader)rootMessages.elementAt( i );
            childVector = (Vector)children.get( messageHeader.getThreadingMessageID() );
            removeEmptyMessages( childVector );
    	}

    	for ( int i = rootsCount - 1; i >= 0; --i ) {
    	    messageHeader = (MessageHeader)rootMessages.elementAt( i );
    	    if ( messageHeader.isEmpty() ) {
    	        childVector = (Vector)children.get( messageHeader.getThreadingMessageID() );
    	        if ( childVector == null || childVector.size() == 0 ) {
    	            rootMessages.removeElementAt( i );
    	            --size;
    	        }
    	        if ( childVector != null && childVector.size() == 1 ) {
    	            children.remove( messageHeader.getThreadingMessageID() );
    	            rootMessages.removeElementAt( i );
    	            MessageHeader child =  (MessageHeader)childVector.elementAt( 0 );
    	            child.setParentID(null);
    	            parents.remove( child.getThreadingMessageID() );
    	            rootMessages.addElement( child );
    	            --size;
    	        }
    	    }
    	}
    }

    private void removeEmptyMessages(final Vector messages) {
        if ( messages == null ) {
            return;
        }
        final int vectorSize = messages.size();
        MessageHeader messageHeader;
        MessageHeader messageParent;
        Vector childVector;
        for ( int i = vectorSize - 1; i >= 0; --i ) {
            messageHeader = (MessageHeader)messages.elementAt( i );
            messageParent = (MessageHeader)parents.get( messageHeader.getThreadingMessageID() );
            childVector = (Vector)children.get( messageHeader.getThreadingMessageID() );
            removeEmptyMessages( childVector );
            if ( messageHeader.isEmpty() ) {
                final int childCount = (childVector == null)?0:childVector.size();
                MessageHeader child;
                for ( int j = childCount - 1; j >= 0; --j ) {
                    child = (MessageHeader)childVector.elementAt(j); // child vector cannot be null here because size is greater than zero
                    childVector.removeElement( child );
                    child.setParentID( messageHeader.getParentID() );
                    parents.put( child.getThreadingMessageID(), messageParent );
                    messages.addElement( child );
                }
                children.remove( messageHeader.getThreadingMessageID() );
                parents.remove( messageHeader.getThreadingMessageID() );
                messages.removeElementAt( i );
                --size;
            }
        }
    }

    //#ifdef MUJMAIL_DEVELOPMENT
//#     public void printToConsole() {
//#           if (!DEBUG) return;
//#         System.out.println("ThreadedEmails.printConsole()");
//#         System.out.println("size: " + size );
//#         System.out.println("=== alternative ===");
//#         Enumeration enumeration = this.getEnumeration();
//#         while ( enumeration.hasMoreElements() ) {
//#             MessageHeader mh = (MessageHeader)enumeration.nextElement();
//#             System.out.println( "msg: " + mh.toString() );
//#         }
//#     }
    //#endif


    /* *******************
     *    inner class    *
     *********************/

    /* *
     * Constant represents that message at this "position" is root message.
     * 
     * @see Enumerator#actualRootChild
     */
//    private static final int ROOT_MESSAGE = -1;

    /**
     * Class enables iterating over the structure.
     * There are two indexes:<ul>
     * <li>root index - index to the root messages vector</li>
     * <li>child index - index to the vector of children for root message</li>
     * </ul>
     * When child index is {@link ThreadedEmails#ROOT_MESSAGE}, that means that
     * the message to retrieve is the one from root message vector.
     * When root index is size of root messages vector, taht means that we are
     * at the end of all messages (there are no more elements).
     * 
     * @author Betlista
     */
    //#ifdef MUJMAIL_TEST_BACKWARD_ITERATING
//#     public
    //#else
       private
    //#endif
    class Enumerator implements Enumeration {

        /**
         * <p>
         * Index of next message, used for simple implementation
         * of {@link #hasMoreElements()} method.
         * </p>
         * <p>
         * When index value is for example 3 that means that next message will
         * be returned the 4th message in structure (index starts with 0).
         * </p>
         */
        private int index;

        /**
         * Reference to actual message.
         */
        private MessageHeader actual;

        /**
         * Idea of this enumeration is that it points to next element, this is
         * the invariant.
         */
        public Enumerator() {
            index = 0;
            actual = null;
        }

        /**
         * Returns <code>true</code> if there are more elements.
         * 
         * @return <code>true</code> if there are more elements in enumeration,
         *         <code>false</code> otherwise
         */
        public boolean hasMoreElements() {
            return index < size;
        }

        /**
         * Method is just synonym for {@link #hasMoreElements()} method.
         */
        //#ifdef MUJMAIL_TEST_BACKWARD_ITERATING
//#         public
        //#else
       private
        //#endif
        boolean hasNextElement() {
            return hasMoreElements();
        }

        /**
         * Returns next element in enumeration.
         * 
         * @return MessageHeader next message
         * @throws NoSuchElementException if there are no more elements.
         * @see #hasMoreElements()
         */
        public Object nextElement() throws NoSuchElementException {
            if ( !hasNextElement() ) {
                throw new NoSuchElementException();
            }
            MessageHeader messageHeader = null;
            
            if ( actual == null ) {
                messageHeader = (MessageHeader)rootMessages.elementAt( 0 );
            } else {
                boolean actualChanged = false;
                while ( messageHeader == null ) {
                    if ( !actualChanged ) {
                        messageHeader = getFirstChild();
                    }

                    if ( messageHeader == null ) {
                        messageHeader = getSibling(true);
                    }

                    if ( messageHeader == null ) {
                          // actual cannot be null here
                          // setting of
                        actual = (MessageHeader)parents.get( actual.getThreadingMessageID() );
                        actualChanged = true;
                        messageHeader = getSibling(true);
                    }
                }
            }

            actual = messageHeader;
            ++index;
            return messageHeader;
        }

        //#ifdef MUJMAIL_TEST_BACKWARD_ITERATING
//#         public
        //#else
       private
        //#endif
        boolean hasPreviousElement() {
              /* If index is 0 that means that next returned message will be
               * the 1st message, so there was no message returned yet and
               * that why it has no previous message. 
               * 
               * If index is 1 the actual message is the 1st one and it has
               * no previous message too.
               */
            return index > 1;
        }

        /**
         * Returns the previous message for actual one.
         *  
         * @return previous message for actual ones
         */
        //#ifdef MUJMAIL_TEST_BACKWARD_ITERATING
//#         public
        //#else
       private
        //#endif
        Object previousElement() {
            if ( !hasPreviousElement() ) {
                throw new NoSuchElementException();
            }
            MessageHeader messageHeader = null;

              /* First of all we need to retrieve the last child message
               * of the previous sibling tree.
               * Example:
               * --root
               *   |--1
               *   |  |--2
               *   |  \--3
               *   |     |--4
               *   |     \--5 <- previous
               *   \--6       <- actual
               * If actual message is 6, than previous message is 5
               * => previous sibling of 6 is 1 and last child is 5
               */
            MessageHeader parent = (MessageHeader)parents.get( actual.getThreadingMessageID() );
            if ( parent == null ) {
                MessageHeader sibling = getSibling( false );
                MessageHeader lastChild = getLastChild( sibling.getThreadingMessageID() );
                if ( lastChild == null ) {
                    messageHeader = sibling;
                } else {
                    messageHeader = lastChild;
                }
            } else {
                Vector childVector = (Vector)children.get( parent.getThreadingMessageID() );
                if ( childVector != null ) {
                    final int index = childVector.indexOf( actual );
                    if ( index > 0 ) {
                        MessageHeader previousSibling = (MessageHeader)childVector.elementAt( index - 1 );
                        MessageHeader lastChild = getLastChild( previousSibling.getThreadingMessageID() );
                        if ( lastChild == null ) {
                            messageHeader = previousSibling;
                        } else {
                            messageHeader = lastChild;
                        }
                    } else {
                        messageHeader = parent;
                    }
                }
            }

            actual = messageHeader;
            --index;
            return messageHeader;
        }

        /**
         * Method returns last child of the subtree with message identified
         * by rootThreadingID as root of this subtree.
         *  
         * @param rootThreadingID identification of the message that represents
         *        subtree root
         * @return last child of subtree or null if root message have no child.
         */
        private MessageHeader getLastChild( final String rootThreadingID ) {
            MessageHeader lastChild = null;

            Vector childVector = (Vector)children.get( rootThreadingID );
            if ( childVector != null ) {
                lastChild = (MessageHeader)childVector.elementAt( childVector.size() - 1 );
                MessageHeader newLastChild = getLastChild( lastChild.getThreadingMessageID() );
                if ( newLastChild != null ) {
                    lastChild = newLastChild;
                }
            }
            return lastChild;
        }

        /**
         * Returns first child for actual message.<br>
         * Example:
         * <pre>
         * -- Msg1
         *    |-- Msg3
         *    \-- Msg4
         * -- Msg2
         * </pre>
         * For Msg1 returns Msg3, for other message in this example returns null.
         * 
         * @return first child for actual message or null
         */
        private MessageHeader getFirstChild() {
            Object o = children.get( actual.getThreadingMessageID() );
            if ( o == null ) {
                return null;
            }
            Vector childVector = (Vector)o;
            return (MessageHeader)childVector.elementAt( 0 );
        }

        /**
         * Returns sibling for actual message.
         * Example:
         * <pre>
         * -- Msg1
         *    |-- Msg3
         *    \-- Msg4
         * -- Msg2
         * </pre>
         * For Msg1 returns Msg2, for Msg3 returns Msg4, for all other messages
         * returns null (have no sibling).
         * 
         * @param next specifies if we want next (<code>true</code>) sibling
         *        or the previous one (<code>false</code>)
         * @return sibling for actual message or null
         */
        private MessageHeader getSibling( final boolean next ) {
            final int shift = next?1:-1;
            final MessageHeader messageHeader;
              // get parent
            final String actualMessageID = actual.getThreadingMessageID();
            final int index;
            MessageHeader parentMessage = (MessageHeader)parents.get( actualMessageID );
            if ( parentMessage == null ) {
                  // have no parent, get next root message or first child (if some exist)
                index = rootMessages.indexOf( actual );
                  // how we know that there is such message?
                  //   it's simple: this method is called only from nextElement()
                  //   or previousElement() methods, so there was check if such
                  //   element exist
                messageHeader = (MessageHeader)rootMessages.elementAt( index + shift );
            } else {
                  // in this case we are not operating with root messages,
                  //   so there is no guarantee that sibling exists

                  // child vector cannot be null here, because actual message
                  //   is one of the children
                Vector childVector = (Vector)children.get( parentMessage.getThreadingMessageID() );
                index = childVector.indexOf( actual );
                if ( next && index == childVector.size() - 1 ) {
                      // when we want next message, but actual is last one
                      //   in child vector so there is no sibling
                    messageHeader = null;
                } else if ( !next && index == 0 ) {
                      // when we want previous message, but actual is first one
                      //   in child vector so there is no sibling
                    messageHeader = null;
                } else {
                    messageHeader = (MessageHeader)childVector.elementAt( index + shift );
                }
            }
            return messageHeader;
        }
    }
}
