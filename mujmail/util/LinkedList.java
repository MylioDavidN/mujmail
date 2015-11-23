package mujmail.util;

/**
 * Represents list in which elements have references to it's neighbours
 * (previous and next element).
 * 
 * @author Jan Gregor
 */
public class LinkedList {

	/** Reference to the first {@link Element element} in list. */
    protected Element first = null;
	/** Reference to the last {@link Element element} in list. */
    protected Element last  = null;

    /** List size. */
    protected int size = 0;

    /**
     * Adds element at the end.
     * 
     * @param value
     */
    public void add(Object value) {
    	  // create new element
    	  //   it's added to the end, so next is set to null and previous
    	  //   is set to actually last 
        Element newLast = new Element(value, last, null);
          // if it's the first element in list
        if ( size == 0 ) {
            this.first = newLast;
        }
          // if there is at least one element in list
        if ( size > 0) {
        	  // define references between last element in list and new element
            this.last.next = newLast;
            newLast.previous = this.last;
        }
          // new element is last one in list
        this.last = newLast;
        ++this.size;
    }

    /**
     * Concatenates list from parameter with this list.
     * 
     * @param list list to be added to this list
     */
    public void concatenateWith(LinkedList list) {
        if (list.isEmpty()) {
            return;
        }

        this.size += list.size();

        Element oldLast = this.last;
        if (oldLast != null) {
            oldLast.next = list.first;
        }

        list.first.previous = oldLast;
        this.last = list.last;

        if (this.first == null) {
            this.first = list.first;
        }
    }

    /**
     * Returns true if there is no element in list yet.
     * 
     * @return <code>true</code> if there is no element (size == 0),
     *         <code>false</code> otherwise
     */
    public boolean isEmpty() {
        return this.first == null;
    }

    /**
     * Returns the value of the first element.
     * 
     * @return value of the first {@link Element element} in the list
     */
    public Object getFirst() {
        if (isEmpty()) {
            throw new RuntimeException("LinkedList is empty");
        }
        return this.first.value;
    }
    
    /**
     * Returns content of the last {@link Element element}.
     * 
     * @return last {@link Element element} content
     */
    public Object getLast() {
        if (isEmpty()) {
            throw new RuntimeException("LinkedList is empty");
        }
        return this.last.value;
    }

    /**
     * Removes first {@link Element element} from the list.
     * 
     * @return the value of the removed {@link Element element}
     * @see #removeLast()
     */
    public Object removeFirst() {
        if (isEmpty()) {
            throw new RuntimeException("LinkedList is empty");
        }
        Element oldFirst = this.first;
        this.first = oldFirst.next;
        if (this.first != null) {
            this.first.previous = null;
        }
        
        oldFirst.previous = null;
        oldFirst.next = null;
        
        --this.size;
        
        return oldFirst.value;
    }

    /**
     * Removes last {@link Element element} from the list.
     * 
     * @return the value of the removed {@link Element element}
     * @see #removeFirst()
     */
    public Object removeLast() {
        if (isEmpty()) {
            throw new RuntimeException("LinkedList is empty");
        }
        Element oldLast = this.last;

        this.last = oldLast.previous;
        if (this.last != null) {
            this.last.next = null;
        }

        oldLast.previous = null;
        oldLast.next = null;

        --this.size;

        return oldLast.value;
    }

    /**
     * Returns the number of elements in list.
     * 
     * @return elements number in list
     */
    public int size() {
        return this.size;
    }

    /**
     * Returns iterator - structure for iterating over the elements in list.
     * 
     * @return {@link Iterator iterator}
     * @see Iterator
     */
    public Iterator getIterator() {
        return new ListIterator();
    }

    /* ************************
     *    Object's methods    *
     **************************/

    //#ifdef MUJMAIL_DEVELOPMENT
//#     /**
//#      * Returns string representation for the list in format:<br>
//#      * LinkedList[1.value, 2.value, ...]<br>
//#      * Values are result from calling:<br>
//#      * <code>{@link Element#getValue() element.getValue()}.toString()</code>
//#      * 
//#      * Note: available only in development version (preprocessing used)
//#      */
//#     public String toString() {
//#         StringBuffer buff = new StringBuffer("LinkedList[");
//#         Element element = this.first;
//#         if ( element != null ) {
//#             buff.append( element.value );
//#         }
//#         while ( element.getNext() != null ) {
//#             element = element.getNext();
//#             buff.append(", ").append( element.value );
//#         }
//#         return buff.append(']').toString();
//#     }
    //#endif

    /* *********************
     *    inner classes    *
     ***********************/

    /**
     * Represents one item in linked list.
     * It contains references to previous and next {@link Element element}
     * and primarily the value which we wanted to store in list.
     * 
     * @author Jan Gregor
     */
    protected static class Element {
        /** Value we wanted to store in list */
        protected Object  value;
        /** Reference to the previous {@link Element element} */
        protected Element previous;
        /** Reference to the next {@link Element element} */
        protected Element next;

        /**
         * Constructor for new element instance creation.
         * 
         * @param value value to be stored in list (see {@link #value})
         * @param previous reference to the previous element (can be null)
         * @param next reference to the next element (can be null)
         */
        public Element(Object value, Element previous, Element next) {
            this.value   = value;
            this.previous = previous;
            this.next    = next;
        }

        /**
         * Getter for the {@link #value} field.
         * 
         * @return value
         */
        public Object getValue() {
            return this.value;
        }
        
        /**
         * Getter for the {@link #previous} field.
         * 
         * @return reference to previous {@link Element element}
         */
        public Element getPrevious() {
            return this.previous;
        }
        
        /**
         * Getter for the {@link #next} field.
         * 
         * @return reference to next {@link Element element}
         */
        public Element getNext() {
            return this.next;
        }
    }

    /**
     * Implements {@link Iterator iterator} interface for {@link LinkedList}
     * class.
     */
    protected class ListIterator implements Iterator {

    	/** Reference to the actual {@link Element element} in list */
        protected Element actual;

        /**
         * Constructor that sets {@link #actual} to the first element in list.
         */
        public ListIterator() {
			this.actual = first;
		}

        /*
         * (non-Javadoc)
         * @see mujmail.util.Iterator#hasNext()
         */
        public boolean hasNext() {
            return this.actual != null;
        }

        /*
         * (non-Javadoc)
         * @see mujmail.util.Iterator#next()
         */
        public Object next() {
            if (this.actual != null) {
                Object result = this.actual.getValue();
                this.actual = this.actual.getNext();
                return result;
            } else {
                return null;
            }
        }
    }
}
