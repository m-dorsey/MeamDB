package MeamDB;

import java.util.Scanner;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * A command to run the user interface for following a user
 *
 * Once instantiated with the constructor, the `run(Command, Scanner)` method should be
 * used to start the command
 */
public class FollowUser extends Command {

    /** Tracks the current overall state of the command */
	private State state = State.Initial;
    /** The last username/email the user provided */
	private String last_user = null;
    /** The UID of the active user */
	private int uid;

    /**
     * Instantiate the command
     *
     * @param uid The UID of the active user
     */
	public FollowUser(int uid) {
		this.uid = uid;
	}

    // Inherited
    protected Command.Action action() {
		switch(this.state) {
			case Initial:
				return Command.Action.Prompt("Please enter a username or email to follow!");
			case Failed:
				return Command.Action.Prompt("We couldn't find that user!  Try another name, or type \"quit\"");
			case Fetch:
				return Command.Action.Query(c -> this.addFollower(c));
			case Success:
				return Command.Action.Exit("You're now following " + last_user);
			case Quit:
				return Command.Action.Exit(null);
		}
		throw new RuntimeException("unreachable");
    }

    // Inherited
	protected void processInput(String input) {
		if(input.equalsIgnoreCase("quit")) {
			this.state = State.Quit;
		} else {
			this.last_user = input;
			this.state = State.Fetch;
		}
	}

    /**
     * A query to search for and follow the user's last input
     *
     * If the input contains an '@', this will search by email instead of username
     *
     * If the user doesn't exist, transition to Failed, otherwise, transition to Success
     */
	private void addFollower(Connection c) throws SQLException {
        String usernameOrEmail;
        if(this.last_user.indexOf('@') == -1)
            usernameOrEmail = "username";
        else
            usernameOrEmail = "email";

		PreparedStatement stmt = c.prepareStatement(String.format("select uid from p320_12.user where %s = ?;", usernameOrEmail));
        stmt.setString(1, this.last_user);
		ResultSet maybe_user = stmt.executeQuery();
		if(maybe_user.next()) {
			int followee_id = maybe_user.getInt(1);
			stmt = c.prepareStatement("insert into p320_12.follower values (?, ?);");
            stmt.setInt(1, this.uid);
            stmt.setInt(2, followee_id);
            int nRows = stmt.executeUpdate();
			System.out.println(nRows);
			if(nRows == 1) {
				this.state = State.Success;
			} else {
				throw new RuntimeException("I don't know what it means that this was raised");
			}
		} else {
			this.state = State.Failed;
		}
	}

    /** The available states for the command to be in */
	private static enum State { Initial, Failed, Fetch, Success, Quit }
}
