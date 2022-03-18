package MeamDB;

import java.util.ArrayList;
import java.util.Scanner;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class UnfollowUser extends Command {

    private State state = State.Initial;
    private ArrayList<User> followedUsers = new ArrayList<>(10);
    private User selectedUser;
    private int uid;

    public UnfollowUser(int uid) {
        this.uid = uid;
    }

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
                            .append("\nPlease select a user to unfollow by typing the number next to their name, or type \"quit\" to quit")
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

    private StringBuilder getMenu() {
        StringBuilder menu = new StringBuilder(this.followedUsers.size() * 20 + 140);
        menu = menu.append("\n\nID | username\n-------------------------\n");
        for(int i = 0; i != followedUsers.size(); i++) {
            if(i < 9)
                menu = menu.append('0');
            menu = menu.append(i)
                .append(" | ")
                .append(followedUsers.get(i).username)
                .append('\n');
        }
        return menu;
    }

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

    private void unfollow(Connection c) throws SQLException {
        // Unfollow the user
        PreparedStatement s = c.prepareStatement("DELETE FROM p320_12.follower WHERE followed = ? AND follower = ?;");
        s.setInt(1, this.selectedUser.uid);
        s.setInt(2, this.uid);
        s.executeUpdate();
        this.followedUsers.removeIf(u -> u.uid == this.selectedUser.uid);
        this.state = State.Success;
    }

    protected void processInput(String input) {
        if(input.equalsIgnoreCase("quit")) {
            this.state = State.Quit;
        } else {
            try {
                this.selectedUser = this.followedUsers.get(Integer.parseInt(input));
                this.state = State.Unfollow;
            } catch(NumberFormatException | IndexOutOfBoundsException e) {
                this.state = State.BadInput;
            }
        }
    }

	private static enum State { Initial, Loaded, BadInput, Unfollow, Success, Quit, NoFollowees }
    private static class User {
        int uid;
        String username;
        User(int uid, String username) {
            this.uid = uid;
            this.username = username;
        }
    }
}
