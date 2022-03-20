package MeamDB;

import java.util.Scanner;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * An abstract class to make writing new commans a little easier
 *
 * Using the Command class, commands are understood as a state machine, with each state
 * producing an Action, which serves as a state transition, as well as providing an
 * interface to do things such as access the database connection or read input.
 *
 * The general lifecycle of a Command is as follows:
 * - The Command subclass is instantiated by the caller with info such as the user's ID
 * - Command.run is called to start the Command engine
 * - The subclass is prompted for an action
 *   - If the action is a prompt, the engine prompts the user and calls back to the
 *     application with the result.  The callback allows the subclass to parse the input
 *     and move to the next state
 *   - If the action is an SQL query, the engine executes the provided function.  It is
 *     expected that the provided function will advance the subclass to the next state
 *   - If the action is an Exit, then the engine optionally displays a message before
 *     shutting down.
 * - Unless the engine exited, it again prompts the subclass for an action and loops until
 *   an exit is provided.
 *
 * To subclass a command, you will need to implement the action and processInput methods.
 * Action examines the current state of the subclass and produces the appropriate
 * Command.Action.  processInput serves as the callback for any prompt actions.
 *
 * @see Command.Action
 */
public abstract class Command {

    /**
     * Start the command engine
     *
     * @param c The database connection to run SQL queries on
     * @param s A scanner to read user input from
     * @throws SQLException if there is an exception throw in an inner command
     */
    public void run(Connection c, Scanner s) throws SQLException {
        while(this.action().run(this, c, s)) continue;
    }

    /**
     * The representation of actions which can be taken by subcommands
     *
     * The best way to create one of these is using the static methods Prompt, Exit, and
     * Query, which standin for constructors of the types of actions available.
     *
     * You should not subclass Action yourself.
     */
    protected abstract static class Action {

        /**
         * FOR INTERNAL USE ONLY  run this action on the given command
         *
         * @param cmd the command to call back into
         * @param c the database connection to use
         * @param s a scanner on stdin
         */
        abstract boolean run(Command cmd, Connection c, Scanner s) throws SQLException;

        /**
         * Create an Action to prompt the user for some input
         *
         * The user will be shown the provided message before being given a prompt, from
         * which exactly one line of input is read.  The input will then be fed back into
         * the Command subclass through the `processInput` method, which will presumably
         * trigger a state transition.
         *
         * @param message the message to show the user
         * @return an Action which prompts for input
         */
        public static Action Prompt(String message) { return new Action.Prompt(message); };

        /**
         * Create an action to exit the command
         *
         * If the provided message is not null, the user will be shown the message.
         * Then, the control flow simply yields back to whatever invoked the command
         *
         * @param message the message to show the user
         * @return an Action which exits the command
         */
        public static Action Exit(String message) { return new Action.Exit(message); };

        /**
         * Run an SQL query
         *
         * Run a query on the database.  This gives the subclass access to the database to
         * run exactly one function.  This doesn't have any callback to the subclass, so
         * it's assumed that the lambda passed to this method captures a reference to the
         * command subclass which can be used to trigger the state transition.
         *
         * The lambda will receive on parameter (a Connection object) and should return
         * nothing, but may throw an SQLException if it wants.
         *
         * @param query the method to run.  You should provide this one with a lambda.
         * @return an Action which runs the given query
         */
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

    /**
     * Determine the appropriate action to run for the current state
     *
     * See the class-level documentation and the documentation for the Action class for
     * more information.
     *
     * @return the Action to take
     * @see Command.Action
     * @see Command
     */
    protected abstract Action action();

    /**
     * Process some input from the user
     *
     * This is used as a callback from `Prompt` actions
     *
     * @param input the line of input provided by the user
     * @see Command.Action.Prompt
     */
    protected abstract void processInput(String input);

    /**
     * A lambda which is used for running queries
     *
     * @see Command.Action.Query
     */
    @FunctionalInterface
    protected interface QueryMethod {
        public void query(Connection c) throws SQLException;
    }
}
