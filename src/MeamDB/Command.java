package MeamDB;

import java.util.Scanner;
import java.sql.Connection;
import java.sql.SQLException;

public abstract class Command {

    public void run(Connection c, Scanner s) throws SQLException {
        while(this.action().run(this, c, s)) continue;
    }

    protected abstract static class Action {

        abstract boolean run(Command cmd, Connection c, Scanner s) throws SQLException;
        public static Action Prompt(String message) { return new Action.Prompt(message); };
        public static Action Exit(String message) { return new Action.Exit(message); };
        public static Action Query(QueryMethod query) { return new Action.Query(query); };

        private static class Prompt extends Action {
            private String message;
            private Prompt(String message) { this.message = message; }

            protected boolean run(Command cmd, Connection c, Scanner s) throws SQLException {
                System.out.print(this.message + "\n~> ");
                cmd.processInput(s.nextLine());
                return true;
            }
        }

        private static class Exit extends Action {
            private String message;
            private Exit(String message) { this.message = message; }

            protected boolean run(Command cmd, Connection c, Scanner s) throws SQLException {
                if(this.message != null)
                    System.out.println(this.message);
                return false;
            }
        }

        private static class Query extends Action {
            QueryMethod query;
            private Query(QueryMethod query) { this.query = query; }

            protected boolean run(Command cmd, Connection c, Scanner s) throws SQLException {
                this.query.query(c);
                return true;
            }
        }

    }

    protected abstract Action action();
    protected abstract void processInput(String input);

    @FunctionalInterface
    protected interface QueryMethod {
        public void query(Connection c) throws SQLException;
    }
}
