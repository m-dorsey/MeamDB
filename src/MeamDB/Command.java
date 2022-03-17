package MeamDB;

import java.util.Scanner;
import java.sql.Connection;
import java.sql.SQLException;

public abstract class Command {

    public void run(Connection c, Scanner s) throws SQLException {
        switch(this.action()) {
            case Prompt:
                System.out.print(this.getPrompt() + "\n~> ");
                this.processInput(s.nextLine());
                this.run(c, s);
                return;
            case Sql:
                this.runSql(c);
                this.run(c, s);
                return;
            case Exit:
                String message = this.getPrompt();
                if(message != null)
                    System.out.println(message);
        }
    }

    protected enum Action { Exit, Prompt, Sql}
    protected abstract Action action();

    protected abstract String getPrompt();
    protected abstract void runSql(Connection c) throws SQLException;
    protected abstract void processInput(String input);
}
