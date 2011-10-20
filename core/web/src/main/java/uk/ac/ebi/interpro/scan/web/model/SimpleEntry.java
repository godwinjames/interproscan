package uk.ac.ebi.interpro.scan.web.model;

import java.util.*;

/**
* TODO: Add description
*
* @author Antony Quinn
* @version $Id$
*/
public final class SimpleEntry implements Comparable<SimpleEntry>  {

    private final String ac;
    private final String shortName;
    private final String name;
    private final String type;
    private List<SimpleLocation> locations = new ArrayList<SimpleLocation>(); // super matches
    private Map<String, SimpleSignature> signatures = new HashMap<String, SimpleSignature>();

    public SimpleEntry(String ac, String shortName, String name, String type) {
        this.ac         = ac;
        this.shortName  = shortName;
        this.name       = name;
        this.type       = type;
    }

    public String getAc() {
        return ac;
    }

    public String getShortName() {
        return shortName;
    }

    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }

    public List<SimpleLocation> getLocations() {
        return locations;
    }

    public void setLocations(List<SimpleLocation> locations) {
        this.locations = locations;
    }

    public Collection<SimpleSignature> getSignatures() {
        return signatures.values();
    }

    public Map<String, SimpleSignature> getSignaturesMap() {
        return signatures;
    }

    // Tautology: all entries are composed of integrated signatures. Better to *not* put integrated signatures here!!
    public boolean isIntegrated() {
        return (ac != null && !ac.equals(""));
    }

    @Override public int compareTo(SimpleEntry that) {

        if (this == that) {
            return 0;
        }

        // No supermatch locations (un-integrated signatures)
        if (this.ac == null || this.ac.equals("")) {
            return 1;
        }
        else if (that.ac == null || that.ac.equals("")) {
            return -1;
        }

        // Entry type
        final EntryType thisType = EntryType.parseName(this.getType());
        final EntryType thatType = EntryType.parseName(that.getType());
        if ( thisType != null && thatType != null) {
            final int compare = thisType.compareTo(thatType);
            if (compare != 0) {
                return compare;
            }
        }
        else if (thisType != null){
            return -1;
        }
        else if (thatType != null){
            return 1;
        }

        return Collections.min(this.locations).compareTo(Collections.min(that.locations));
    }

    @Override public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof SimpleEntry))
            return false;
        return this.ac.equals(((SimpleEntry)o).ac);
    }

}
