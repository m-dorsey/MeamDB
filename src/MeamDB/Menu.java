package MeamDB;

/**
 * A wrapper around a StringBuilder for building nice looking menus of options
 *
 * Always takes the form a a two-column table, where the left column is a numeric
 * incremental ID and the right column is the name of the option
 *
 * Each time an entry is added, it is assigned the next ID and added to the menu
 *
 * Good for presenting the user with a list of options to choose from
 */
public class Menu {

    private int nEntries;
    private StringBuilder builder;

    /**
     * Instantiate an empty menu
     *
     * Until populated, this will only have the header row
     *
     * @param entryKind The label for the second column, e.g. "username"
     * @param guessNumberEntries A size hint for the number of entries to be created
     * @param guessAverageNameLength A size hint for the typical length of each entry name
     */
    public Menu(String entryKind, int guessNumberEntries, int guessAverageNameLength) {
        this.builder = new StringBuilder(entryKind.length() + 34 + (guessAverageNameLength + 6) * guessNumberEntries)
            .append("\n\nID | ")
            .append(entryKind)
            .append("\n---|---------------------\n");
    }

    /**
     * Add an entry to the menu
     *
     * The entry will be assigned the next integer index, starting at zero.
     *
     * @param entry the name of the entry
     * @return this
     * @throws RuntimeException if the menu has already been finalized
     */
    public Menu addEntry(String entry) {
        if(this.builder == null)
            throw new RuntimeException("Cannot add entries after calling .display()");
        this.nEntries++;
        if(nEntries < 10)
            this.builder.append('0');
        this.builder.append(nEntries)
            .append(" | ")
            .append(entry)
            .append('\n');
        return this;
    }

    /**
     * Finalize the menu
     *
     * Once you're done building the menu, use this method to display it.  Available as a
     * StringBuilder so that additional messages can be tacked on before it's converted
     * into a string.
     *
     * Once this method is called, no more entries can be added
     *
     * @return the textual representation of this menu
     */
    public StringBuilder display() {
        StringBuilder ourBuilder = this.builder;
        this.builder = null;
        return ourBuilder;
    }
}
