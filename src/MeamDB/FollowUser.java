package MeamDB;

import java.util.Scanner;
import java.sql.Connection;
import java.sql.Statement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class FollowUser extends Command {

	private State state = State.Initial;
	private String last_user = null;
	private int uid;

	public FollowUser(int uid) {
		this.uid = uid;
	}

    protected Command.Action action() {
		switch(this.state) {
			case Initial:
			case Failed:
				return Command.Action.Prompt;
			case Fetch:
				return Command.Action.Sql;
			case Success:
			case Quit:
				return Command.Action.Exit;
		}
		throw new RuntimeException("unreachable");
    }

	protected void processInput(String input) {
		if(input.equalsIgnoreCase("quit")) {
			this.state = State.Quit;
		} else {
			this.last_user = input;
			this.state = State.Fetch;
		}
	}

	protected String getPrompt() {
		switch(this.state) {
			case Initial:
				return "Please enter a username to follow";
			case Failed:
				return "We couldn't find that user!  Try another name, or type \"quit\"";
			case Success:
				return "You're now following " + last_user;
			case Quit:
				return "Returning to main menu";
            default:
                throw new RuntimeException("Internal error:  State shouldn't prompt");
		}
	}

	protected void runSql(Connection c) throws SQLException {
		Statement stmt = c.createStatement();
		ResultSet maybe_user = stmt.executeQuery("select uid from p320_12.user where username = '" + this.last_user + "';");
		if(maybe_user.next()) {
			int followee_id = maybe_user.getInt(1);
			int nRows = stmt.executeUpdate("insert into p320_12.follower values (" + this.uid + ", " + followee_id + ");");
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

	private static enum State { Initial, Failed, Fetch, Success, Quit }
}
