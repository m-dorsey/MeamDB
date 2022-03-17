package MeamDB;

import java.util.Scanner;
import java.sql.Connection;
import java.sql.Statement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class FollowUser {

	private State state = State.Initial;
	private String last_user = null;
	private int uid;

	public static void run_command(Connection c, Scanner s, int uid) throws SQLException {
		new FollowUser(uid).follow_inner(c, s);
	}

	public FollowUser(int uid) {
		this.uid = uid;
	}

	private void follow_inner(Connection c, Scanner s) throws SQLException {
		System.out.println(this.get_prompt());
		if(!this.should_exit()) {
			System.out.print("~> ");
			this.process_input(s.nextLine());
			if(this.should_try_lookup())
				this.try_follow(c);
			this.follow_inner(c, s);
		}
	}

	private void process_input(String input) {
		if(input.equalsIgnoreCase("quit")) {
			this.state = State.Quit;
		} else {
			this.last_user = input;
		}
	}

	private String get_prompt() {
		switch(this.state) {
			case Initial:
				return "Please enter a username to follow";
			case Failed:
				return "We couldn't find that user!  Try another name, or type \"quit\"";
			case Success:
				return "You're now following " + last_user;
			case Quit:
				return "Returning to main menu";
		}
		throw new RuntimeException("unreachable");
	}

	private void try_follow(Connection c) throws SQLException {
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

	private boolean should_exit() {
		switch(this.state) {
			case Initial:
			case Failed:
				return false;
			case Success:
			case Quit:
				return true;
		}
		throw new RuntimeException("unreachable");
	}

	private boolean should_try_lookup() {
		switch(this.state) {
			case Initial:
			case Failed:
				return true;
			case Success:
			case Quit:
				return false;
		}
		throw new RuntimeException("unreachable");
	}

	private static enum State { Initial, Failed, Success, Quit }

}
