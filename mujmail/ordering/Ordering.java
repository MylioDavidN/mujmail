package mujmail.ordering;

import mujmail.util.Functions;

/** 
 * Represents if type of sorting, whether increasing({@link #NATURAL}) or decreasing({@link #UNNATURAL})
 */
public final class Ordering {
    /** Sort mode. Increasing order */
    public static final Ordering NATURAL = new Ordering(0, "natural");
    /** Sort mode. Descreasing order */
    public static final Ordering UNNATURAL = new Ordering(1, "unnatural");

    /** Identification of sorting mode */
    public final int ordinal;
    /** Human readable name of sorting mode */
    private final String name; 

    /** 
     * Private constructor to prevent instances creation
     * @param ordinal index of ordering type
     * @param name Sorting mode internal name
     */
    private Ordering(int ordinal, String name) {
        this.ordinal = ordinal;
        this.name = name;
    }

    /** 
     * For given ordering constant from {@link mujmail.util.Functions} SRT_ORDER_ entries gives Ordering object.
     *
     * @param ordering Header sorting mode from {@link mujmail.util.Functions}
     * @return Ordering for given sorting mode
     */
    public static Ordering valueOf(int ordering) throws IllegalArgumentException {
        if (ordering == Functions.SRT_ORDER_DEC) {
            return UNNATURAL;
        } else if ( ordering == Functions.SRT_ORDER_INC ) {
            return NATURAL;
        } else {
            throw new IllegalArgumentException("Expected value in (" + Functions.SRT_ORDER_DEC + ", " + Functions.SRT_ORDER_INC + ") but got " + ordering);
        }
    }

    /* *********************
     *    Object methods   *
     ***********************/
    public String toString() {
        return name;
    }

    public boolean equals(Object obj) {
        return this == obj;
    }

    public int hashCode() {
        if ( this == NATURAL ) {
            return Functions.SRT_ORDER_INC;
        } else { // there are just two possibilities now
            return Functions.SRT_ORDER_DEC;
        }
    }
}
