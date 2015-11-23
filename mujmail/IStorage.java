package mujmail;

import java.util.Enumeration;

import mujmail.ordering.Comparator;

/**
 * Interface contains methods for accessing e-mail headers in storage.
 * Using interface makes later changes in storage implementation easier.
 * 
 * @author Betlista
 */
public interface IStorage {

    /**
     * Returns {@link Enumeration} that enables programmer to iterate over all
     * messages in structure and hides implementation details.
     * 
     * @return enumeration for iterating over e-mail headers
     */
    public Enumeration getEnumeration();

    /**
     * Returns number of e-mail headers in storage.
     * 
     * @return number of e-mail headers in storage.
     */
    public int getSize();

    /**
     * This method returns i-th e-mail message header in structure, but in more
     * complex structure other than Vector this could be expensive operation
     * and this method skouldn't be used.
     * 
     * @param index of the message to be returned
     */
    public MessageHeader getMessageAt(int index); // TODO: remove this method from interfaces

    /**
     * Returns information whether the storage is empty.
     * 
     * @return true if size of the storage == 0, false otherwise
     */
    public boolean isEmpty();

    /**
     * Removes all messages from storage.
     */
    public void removeAllMessages();

    /**
     * Sorts messages in structure in order defined by comparator.
     * 
     * @param comparator
     * @see Comparator
     * @see mujmail.ordering.comparator.MessageHeaderComparator
     */
    public void sort(Comparator comparator);
    /**
     * This method removes i-th e-mail message header in structure, but in more
     * complex structure other than Vector this could be expensive operation
     * and this method shouldn't be used.
     * 
     * @param index of the message to be removed
     */
    public void removeMessageAt(int index);
    
    /**
     * Method removes first occurrence message header from the storage.
     * 
     * @param messageHeader
     */
    public void removeMessage(MessageHeader messageHeader);

    /**
     * Adds messageheader to the storage.
     * 
     * @param messageHeader
     */
    public void addMessage(MessageHeader messageHeader);
}
