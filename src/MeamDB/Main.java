package MeamDB;

import com.jcraft.jsch.*;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

import java.util.Scanner;

public class Main {


    /**
     *
     * @return true if valid login, false if the user gives up on logging in.
     */
    private static boolean login(){
        //FIXME I feel like having 2 scanners will cause problems.
        // we'll see. Probably want to keep an eye out tho.
        Scanner scan = new Scanner(System.in);
        String username = "";
        String password = "";

        boolean validLogin = false;
        while (!validLogin) {
            System.out.println("input the username for the account");
            username = scan.nextLine();
            System.out.println("input the password for the account.");
            password = scan.nextLine();

            //now we check to see if this is valid credentials.
            //if so, we allow them in.
            //if not, then the following statement executes.
            System.out.println("invalid login. 'y' to try again. anything else to exit.");
            String input = scan.nextLine();
            input = input.toLowerCase();
            if( input.equals("y")){
                //we go through the loop again all easy like
            }else{
                return false;
            }
        }

        //FIXME probably want to actually return a string or string away
        // with the proper data to do things in the rest of the program.
        return true;
    }


    public static void main(String[] args) throws SQLException {

        int lport = 5432;
        String rhost = "starbug.cs.rit.edu";
        int rport = 5432;
        String user = "YOUR_CS_USERNAME"; //change to your username
        String password = "YOUR_CS_PASSWORD"; //change to your password
        String databaseName = "YOUR_DB_NAME"; //change to your database name

        String driverName = "org.postgresql.Driver";
        Connection conn = null;
        Session session = null;
        try {
            java.util.Properties config = new java.util.Properties();
            config.put("StrictHostKeyChecking", "no");
            JSch jsch = new JSch();
            session = jsch.getSession(user, rhost, 22);
            session.setPassword(password);
            session.setConfig(config);
            session.setConfig("PreferredAuthentications","publickey,keyboard-interactive,password");
            session.connect();
            System.out.println("Connected");
            int assigned_port = session.setPortForwardingL(lport, "localhost", rport);
            System.out.println("Port Forwarded");

            // Assigned port could be different from 5432 but rarely happens
            String url = "jdbc:postgresql://localhost:"+ assigned_port + "/" + databaseName;

            System.out.println("database Url: " + url);
            Properties props = new Properties();
            props.put("user", user);
            props.put("password", password);

            Class.forName(driverName);
            conn = DriverManager.getConnection(url, props);
            System.out.println("Database connection established");

            // Do something with the database....






            //the user should first be prompted to log in or create
            // an account

            System.out.println("Input 'login' to log in with your credentials or 'create new account' to create a new account");



            /*
            a loop to constantly get input from users
            while true
             */

            Scanner scan = new Scanner(System.in);
            String input = "";

            boolean loggedIn = false;

            //this loop runs forever to get input from the user
            while( true ) {
                System.out.println("type 'help' to get a list of commands.");

                input = scan.nextLine();
                input = input.toLowerCase();

                //TODO we probably want to have a function for each command
                if (input.equals("login")) {
                    //in here, we need to handle logging in
                    boolean successfulLogin = login();
                    if( successfulLogin ){
                        loggedIn = true;
                    }else{
                        loggedIn = false;
                    }
                } else if (input.equals("create new account")) {
                    //in here, we need to handle creating a new account
                    //and getting all that data.
                } else if (input.equals("help")) {
                    System.out.println("'login' to login to an existing account. You must log in before you execute other commands");
                    System.out.println("'create new account' to create a new account.");
                    System.out.println("'create new collection' to create a new collection of songs.");
                    System.out.println("'view collections' to view all your created collections");
                    System.out.println("'search song' to search for songs.");
                    System.out.println("'rename collection' to rename a collection");
                    System.out.println("'modify collection' to add or remove songs or albums from a collection");
                    System.out.println("'delete collection' to delete a collection.");
                    System.out.println("'play song' to play a song.");
                    System.out.println("'play collection' to play a collection.");
                    System.out.println("'follow friend' to follow a friend."); //FIXME it isn't specified, but we probably also want to be able to list the follows.
                    System.out.println("'unfollow friend' to unfollow a friend.");
                } else if (!loggedIn) {
                    System.out.println("Error: you must be logged in before executing that command.");
                } else if (input.equals("create new collection")) {

                } else if (input.equals("view collections")) {

                } else if (input.equals("search song")) {

                } else if (input.equals("rename collection")) {

                } else if (input.equals("modify collection")) {

                } else if (input.equals("delete collection")) {

                } else if (input.equals("play song")) {

                } else if (input.equals("play collection")) {

                } else if (input.equals("follow friend")) {

                } else if (input.equals("unfollow friend")) {

                } else {
                    System.out.println("That is not a valid command.");
                }


            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (conn != null && !conn.isClosed()) {
                System.out.println("Closing Database Connection");
                conn.close();
            }
            if (session != null && session.isConnected()) {
                System.out.println("Closing SSH Connection");
                session.disconnect();
            }
        }
    }
}

