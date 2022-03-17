package MeamDB;

import java.util.Scanner;
import java.sql.Connection;
import java.sql.SQLException;

public abstract class Command {

    public void run(Connection c, Scanner s) throws SQLException {
        System.out.println(this.getPrompt());
        if(!this.shouldExit()) {
            System.out.print("~> ");
            this.processInput(s.nextLine());
            if(this.shouldTrySql())
                this.trySql(c);
            this.run(c, s);
        }
    }

    protected abstract void processInput(String input);
    protected abstract String getPrompt();
    protected abstract boolean shouldTrySql();
    protected abstract void trySql(Connection c) throws SQLException;
    protected abstract boolean shouldExit();
}
