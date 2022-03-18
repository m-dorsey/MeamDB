package MeamDB;

public class Menu {

    private int nEntries;
    private StringBuilder builder;

    public Menu(String entryKind, int guessNumberEntries, int guessAverageNameLength) {
        this.builder = new StringBuilder(entryKind.length() + 34 + (guessAverageNameLength + 6) * guessNumberEntries)
            .append("\n\nID | ")
            .append(entryKind)
            .append("\n---|---------------------\n");
    }

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

    public StringBuilder display() {
        StringBuilder ourBuilder = this.builder;
        this.builder = null;
        return ourBuilder;
    }
}
