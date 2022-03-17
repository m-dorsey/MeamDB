package MeamDB;

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;

import java.sql.*;
import java.util.Properties;
import java.util.Scanner;

public class Main {


    /**
     *
     * @return null if invalid, username otherwise.
     */
    private static String login( Connection conn, Scanner scan ){

        String username = "";
        String password = "";

        boolean validLogin = false;
        while (!validLogin) {
            System.out.println("input the username for the account");
            username = scan.nextLine();
            System.out.println("input the password for the account.");
            password = scan.nextLine();


            try {
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery("Select * from p320_12.user where p320_12.user.username = " + username);

                while (rs.next()) {
                    if( rs.getString(password).equals(password)){
                        //valid login. the username matches the password
                        validLogin = true;
                        break;
                    }
                }
            }catch (Exception e){
                e.printStackTrace();
            }




            if( validLogin ){
                return username;
            }else if ( !validLogin ) {
                System.out.println("invalid login. 'y' to try again. anything else to exit.");
                String input = scan.nextLine();
                input = input.toLowerCase();
                if (input.equals("y")) {
                    //we go through the loop again all easy like
                } else {
                    return null;
                }
            }
        }

        //this should be unreachable, but compiler is complaining.
        return null;
    }



    public static String createNewAccount(Connection conn, Scanner scan){


        //get the username
        boolean validUsername = false;
        String username = null;


        while( !validUsername ) {
            System.out.println("Input username:");
            username = scan.nextLine();


            boolean copiedUser = false;

            try{
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery("select * from p320_12.user");
                while( rs.next() ){
                    if( rs.getString(username).equals(username)){
                        System.out.println("That user name is already in use.");
                        System.out.println("Input 'y' to try again. Anything else to stop.");

                        String input = scan.nextLine();
                        input = input.toLowerCase();
                        if( !input.equals("y") ){
                            return null;
                        }else{
                            copiedUser = true;
                        }
                        break;
                    }
                }

                if( !copiedUser ){
                    validUsername = true;
                }



            }catch (Exception e){
                e.printStackTrace();
            }

        }



        //get the password
        boolean validPassword = false;
        String password = null;
        while( !validPassword ){
            System.out.println("Input password:");

            password = scan.nextLine();

            if( !(password.length() > 0) ){
                System.out.println("Your password can't be blank.");
            }
        }



        boolean validEmail = false;
        String email = null;
        while( !validEmail ){
            System.out.println("Input email:");

            email = scan.nextLine();


            boolean copiedEmail = false;

            try{
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery("select * from p320_12.user");
                while( rs.next() ){
                    if( rs.getString(email).equals(username)){
                        System.out.println("That email is already in use.");
                        System.out.println("Input 'y' to try again. Anything else to stop.");

                        String input = scan.nextLine();
                        input = input.toLowerCase();
                        if( !input.equals("y") ){
                            return null;
                        }else{
                            copiedEmail = true;
                        }
                        break;
                    }
                }

                if( !copiedEmail ){
                    validEmail = true;
                }

            }catch (Exception e){
                e.printStackTrace();
            }
        }


        String fname = null;
        System.out.println("Input first name: ");
        fname = scan.nextLine();

        String lname = null;
        System.out.println("Input last name: ");
        lname = scan.nextLine();



        //TODO now we have to add the tuple to the database.
        // gotta figure that out later.





        return username;
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
            String username = null;

            boolean loggedIn = false;

            //this loop runs forever to get input from the user
            while( true ) {
                System.out.println("type 'help' to get a list of commands.");

                input = scan.nextLine();
                input = input.toLowerCase();

                //TODO we probably want to have a function for each command

                //NOTE: we mmight want to keep it in this series of if else statements, because
                //      the 4th if checks to see if the user is logged in, not the input.

                if (input.equals("login")) {
                    //in here, we need to handle logging in
                    String loginInfo = login( conn, scan );
                    if( loginInfo != null ){
                        loggedIn = true;
                        username = loginInfo;
                    }else{
                        loggedIn = false;
                    }
                } else if (input.equals("create new account")) {
                    //in here, we need to handle creating a new account
                    //and getting all that data.

                    username = createNewAccount( conn, scan );
                    if( username != null ){
                        loggedIn = true;
                    }else {
                        loggedIn = false;
                    }

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
                    System.out.println("'exit' to exit the program.");
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

                } else if (input.equals("exit")){
                    break;
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

