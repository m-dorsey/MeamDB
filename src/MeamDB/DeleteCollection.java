package MeamDB;

import java.util.ArrayList;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * A command to run the user interface for deleting a collection
 *
 * Once instantiated with the constructor, the `run(Command, Scanner)` method should be
 * used to start the command
 */
public class DeleteCollection extends Command {

    /** Tracks the current overall state of the command */
    private State state = State.Initial;
    /** A list of collections which have been retrieved from the database */
    private ArrayList<Collection> collections = new ArrayList<>(10);
    /** The collection that was last selected by the user */
    private Collection selected;
    /** The active user's ID */
    private int uid;

    /**
     * Instantiate the command
     *
     * @param uid The UID of the active user
     */
    public DeleteCollection(int uid) {
        this.uid = uid;
    }

    // Inherits from superclass
    protected Action action() {
        switch(this.state) {
            case Initial:
                return Command.Action.Query(c -> this.loadCollections(c));
            case Confirm:
                return Command.Action.Prompt("Are you sure you want to delete the collection \"" + this.selected.name + "\"? (y/n)");
            case Delete:
                return Command.Action.Query(c -> this.delete(c));
            case Loaded:
                if(this.collections.isEmpty())
                    return Command.Action.Exit("You don't have any collections!");
                else
                    return Command.Action.Prompt(
                        getMenu()
                            .append("\nPlease select a collection to remove by typing the number next to its name, or type \"quit\" to quit")
                            .toString()
                    );
            case BadInput:
                return Command.Action.Prompt("Oops, that's not a valid number.  Try again.\n");
            case Success:
                if(this.collections.isEmpty())
                    return Command.Action.Exit("Collection removed successfully.");
                else
                    return Command.Action.Prompt(
                        getMenu()
                            .append("\nCollection deleted!  You can delete another collection, or type \"quit\" to quit")
                            .toString()
                    );
            case Quit:
                return Command.Action.Exit(null);
        }
		throw new RuntimeException("unreachable");
    }

    /**
     * Generate a visual menu from the list of retrieved collections
     *
     * @returns a loaded StringBuilder of containing the menu
     */
    private StringBuilder getMenu() {
        Menu menu = new Menu("collection name", this.collections.size(), 30);
        for(Collection c : this.collections)
            menu.addEntry(c.name);
        return menu.display();
    }

    // Inherits
    protected void processInput(String input) {
        if(this.state == State.Confirm)
            this.state =
                input.substring(0, 1).equalsIgnoreCase("y")
                ? State.Delete
                : State.Loaded;
        else
            if(input.equalsIgnoreCase("quit")) {
                this.state = State.Quit;
            } else {
                try {
                    this.selected = this.collections.get(Integer.parseInt(input) - 1);
                    this.state = State.Confirm;
                } catch(NumberFormatException | IndexOutOfBoundsException e) {
                    this.state = State.BadInput;
                }
            }
    }

    /**
     * An SQL query to populate the `collections` attribute with the user's collections
     *
     * This is used as a Query Action
     *
     * @param c The database connection to use
     */
    private void loadCollections(Connection c) throws SQLException {
        // Retrieve a list of this user's collections
        PreparedStatement s = c.prepareStatement("SELECT cid, name FROM p320_12.collection WHERE uid = ?;");
        s.setInt(1, this.uid);
        ResultSet matchedCollections = s.executeQuery();

        while(matchedCollections.next()) {
            this.collections.add(new Collection(matchedCollections.getInt(1), matchedCollections.getString(2)));
        }

        this.state = State.Loaded;
    }

    /**
     * An SQL query to delete the currently selected collection
     *
     * This is used as a Query Action
     *
     * Requires that a collection is selected, and will raise a null pointer exception if
     * it's not
     *
     * @param c The database connection to use
     */
    private void delete(Connection c) throws SQLException {
        // Unfollow the user
        PreparedStatement s = c.prepareStatement("DELETE FROM p320_12.collection WHERE cid = ?;");
        s.setInt(1, this.selected.id);
        s.executeUpdate();
        this.collections.remove(this.selected);
        this.state = State.Success;
    }

    /** The available states for the command to be in */
	private static enum State { Initial, Loaded, BadInput, Confirm, Delete, Success, Quit }

    /** A convenient representation of a collection */
    private static class Collection {
        int id;
        String name;
        Collection(int id, String name) {
            this.id = id;
            this.name = name;
        }
    }
}
