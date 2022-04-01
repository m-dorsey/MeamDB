package MeamDB;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

/**
 * A command to run the user interface for unfollowing a user
 *
 * Once instantiated with the constructor, the `run(Command, Scanner)` method should be
 * used to start the command
 */
public class UnfollowUser extends Command {

    /** Tracks the current overall state of the command */
    private State state = State.Initial;
    /** A list of users the active user is currently following */
    private ArrayList<User> followedUsers = new ArrayList<>(10);
    /** The user from the above list which the active user has selected */
    private User selectedUser;
    /** The UiD of the active user */
    private int uid;

    /**
     * Instantiate the command
     *
     * @param uid The UID of the active user
     */
    public UnfollowUser(int uid) {
        this.uid = uid;
    }

    // inherited
    protected Action action() {
        switch(this.state) {
            case Initial:
                return Command.Action.Query(c -> this.loadFollowers(c));
            case Unfollow:
                return Command.Action.Query(c -> this.unfollow(c));
            case Loaded:
                if(this.followedUsers.isEmpty())
                    return Command.Action.Exit("You aren't following any users!");
                else
                    return Command.Action.Prompt(
                        getMenu()
                            .append("\nYou are following " + this.followedUsers.size() + " users.\n" +
                                "Please select a user to unfollow by typing the number next to their name, or type \"quit\" to quit")
                            .toString()
                    );
            case BadInput:
                return Command.Action.Prompt("Oops, that's not a valid number.  Try again.\n");
            case Success:
                if(this.followedUsers.isEmpty())
                    return Command.Action.Exit("You're no longer following " + selectedUser.username);
                else
                    return Command.Action.Prompt(
                        getMenu()
                            .append("\nYou are no longer following ")
                            .append(selectedUser.username)
                            .append("!  Select another user to unfollow, or type \"quit\" to quit")
                            .toString()
                    );
            case Quit:
                return Command.Action.Exit(null);
        }
		throw new RuntimeException("unreachable");
    }

    /**
     * Generate a visual menu from the list of followed users
     *
     * @returns a loaded StringBuilder of containing the menu
     */
    private StringBuilder getMenu() {
        Menu menu = new Menu("username", this.followedUsers.size(), 20);
        for(User user : followedUsers)
            menu.addEntry(user.username);
        return menu.display();
    }

    /**
     * An SQL query to populate the `followers` attribute with the user's followees
     *
     * This is used as a Query Action
     *
     * @param c The database connection to use
     */
    private void loadFollowers(Connection c) throws SQLException {
        // Retrieve a list of usrs this person is following
        PreparedStatement s = c.prepareStatement("SELECT uid, username FROM p320_12.follower JOIN p320_12.user ON uid = followed WHERE follower = ?;");
        s.setInt(1, this.uid);
        ResultSet matchedUsers = s.executeQuery();

        while(matchedUsers.next()) {
            this.followedUsers.add(new User(matchedUsers.getInt(1), matchedUsers.getString(2)));
        }

        this.state = State.Loaded;
    }

    /**
     * An SQL query to unfollowe the currently selected followee
     *
     * This is used as a Query Action
     *
     * Requires that a followee is selected, and will raise a null pointer exception if
     * it's not
     *
     * @param c The database connection to use
     */
    private void unfollow(Connection c) throws SQLException {
        // Unfollow the user
        PreparedStatement s = c.prepareStatement("DELETE FROM p320_12.follower WHERE followed = ? AND follower = ?;");
        s.setInt(1, this.selectedUser.uid);
        s.setInt(2, this.uid);
        s.executeUpdate();
        this.followedUsers.removeIf(u -> u.uid == this.selectedUser.uid);
        this.state = State.Success;
    }

    // Inherited
    protected void processInput(String input) {
        if(input.equalsIgnoreCase("quit")) {
            this.state = State.Quit;
        } else {
            try {
                this.selectedUser = this.followedUsers.get(Integer.parseInt(input) - 1);
                this.state = State.Unfollow;
            } catch(NumberFormatException | IndexOutOfBoundsException e) {
                this.state = State.BadInput;
            }
        }
    }

    /** The available states for the command to be in */
	private static enum State { Initial, Loaded, BadInput, Unfollow, Success, Quit, NoFollowees }
    /** A convenient representation of a followee */
    private static class User {
        int uid;
        String username;
        User(int uid, String username) {
            this.uid = uid;
            this.username = username;
        }
    }
}
