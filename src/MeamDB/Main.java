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
                    if( rs.getString("password").equals(password)){
                        //valid login. the username matches the password
                        validLogin = true;
                        break;
                    }
                }
            }catch (Exception e){
                e.printStackTrace();
            }




            if( validLogin ){
                //FIXME probably want to return the uid instead, because it's what we reference
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
                    if( rs.getString("username").equals(username)){
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
                    if( rs.getString("email").equals(username)){
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





        //FIXME we should instead return the uid, because that's what's
        // always referenced
        return username;
    }

    public static boolean createNewCollection( Connection conn, Scanner scan, int uid ){

        //FIXME in the database, we need to make the name and uid combined unique, or else a
        // user wouldn't be able to specify which collection to modify.
        // Actually, we might be able to avoid that if we make it so they
        // can't input a new name or change a name to something that already exists

        System.out.println("Input a new name for the collection.");

        boolean validName = false;
        String collectionName = scan.nextLine();

        while( !validName ) {

            boolean breakLoop = false;
            try {
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery("select * from p320_12.collection where p320_12.collection.uid = " + uid);

                while( rs.next() ){
                    if( rs.getString("name").equals(collectionName)){
                        System.out.println("There already exists a collection with that name.");
                        System.out.println("Input 'y' to try again with another name. ");
                        String input = scan.nextLine();
                        if( input.toLowerCase().equals("y")){
                            breakLoop = true;
                            break;
                        }else{
                            return false;
                        }
                    }
                }

                if( !breakLoop ){
                    validName = true;
                    //valid collection.
                    //TODO write the SQL to add the collection to the database.
                    return true;

                }

            }catch ( Exception e){
                e.printStackTrace();
            }


        }

        //this return should be unreachable.
        return false;
    }

    public static boolean modifyCollection( Connection conn, Scanner scan, int uid ) throws SQLException {
        System.out.println("Which collection would you like to modify?");
        Statement stmt = conn.createStatement();
        ResultSet rs = stmt.executeQuery("select name from p320_12.collection where p320_12.collection.uid = " + uid);
        while( rs.next() ){
            System.out.println(rs.getString("name"));
        }

        String collectionName = scan.nextLine();
        ResultSet collection = stmt.executeQuery("select * from p320_12.collection where p320_12.collection.uid = "
            + uid + " and p320_12.collection.name = " + collectionName);

        System.out.println("How would you like to modify " + collectionName + "? Add song | Remove song");
        String modification = scan.nextLine();

        if(modification.equals("Add song")){
            System.out.println("Which song would you like to add?");
            ResultSet songToAdd = searchSong(conn, scan);
            stmt.executeQuery("insert into p320_12.song_collection "
                + "(" + rs.getString("s.sid") +", " + collection.getString("cid") + ")");
        }
        else if(modification.equals("Remove song")){
            System.out.println("Which song would you like to remove?");
            ResultSet songToRemove = searchSong(conn, scan);
        }
        else{
            System.out.println("Invalid modification. Please try again");
            return false;
        }
        return true;
    }


    public static void viewCollections( Connection conn, int uid ){
        //want to the the full result set and print out everything
        // FIXME we should check to see if we just need to print out all collectoins
        //  or collections *with* their songs, or if we print all then
        //  allow a user to expand a selected one.

        try {
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("select name from p320_12.collection where p320_12.collection.uid = " + uid);


            while( rs.next() ){
                System.out.println(rs.getString("name"));
            }


        }catch (Exception e){
            e.printStackTrace();
        }


    }

    public static void renameCollection( Connection conn, Scanner scan, int uid ) {
        boolean validCollection = false;
        int collectionID = -1;
        while (!validCollection){
            System.out.println("Input a collection to rename.");
            String chosen = scan.nextLine();

            try {
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery("select name from p320_12.collection where p320_12.collection.uid = " + uid + " and p320_12.collection.name = " + chosen);

                //if it has something, then that means there is a colleciton with the chosen name,
                // so we can't use it
                if( rs.next() ){
                    collectionID = rs.getInt("cid");
                    validCollection = true;
                }else{
                    System.out.println("That is not a collection.");
                    System.out.println("Check to see if you typed it wrong.");
                    System.out.println("Input 'y' to try again.");
                    String input = scan.nextLine();
                    if( input.toLowerCase().equals("y")){
                        //we go through again
                    }else{
                        //we're giving up.
                        return;
                    }
                }

            }catch (Exception e){
                e.printStackTrace();
            }
        }

        boolean validNewName = false;
        String newName = null;
        while( !validNewName ){
            System.out.println("Input a new name for this collection.");
            newName = scan.nextLine();

            try{
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery("select name from p320_12.collection where p320_12.collection.uid = " + uid + " and p320_12.collection.name = " + newName);

                //if the result set is not null, then there is a collection with that name already
                if( rs.next()){
                    System.out.println("There already exists a collection with that name.");
                    System.out.println("Input 'y' to try again.");
                    String input = scan.nextLine();
                    if( input.toLowerCase().equals("y")){
                        //we're going again
                    }else{
                        //we're giving up.
                        return;
                    }
                }else{
                    //the name isn't already being used
                    validNewName = true;
                    //TODO write the sql to modify the name of the collection
                    // to what the user input. We've got all the data, we just need
                    // to write the SQl (which I can't remember. oops.)

                    //we're leaving, because we're done
                    return;
                }
            }catch (Exception e){
                e.printStackTrace();
            }
        }


        //this should be unreachable. Eitherway, it won't affect anything.
    }

    public static boolean playSong( Connection conn, Scanner scan, int uid ) throws SQLException {
        ResultSet rs = searchSong(conn, scan);

        if (rs.next()) {
            System.out.println("Now playing" + rs.getString("s.title") + " by " + rs.getString("a.name"));
            Statement stmt = conn.createStatement();
            stmt.executeQuery("insert into p320_12.play (" + uid + ", " + rs.getString("s.sid") +
                ", Current TIME CURRENT_TIMESTAMP)");
            stmt.executeQuery("update p320_12.song set p320_12.song.count = p320_12.song.count + 1 where p320_12.song.sid = "
                + rs.getString("s.sid"));
        }
        else{
            System.out.println("No song available");
            return false;
        }
        return true;
    }

    public static ResultSet searchSong( Connection conn, Scanner scan ){

        boolean validSearch = false;
        while( !validSearch ) {
            System.out.println("Search by 'song name', 'artist', 'album', or 'genre'");
            String searchingBy = scan.nextLine();
            searchingBy = searchingBy.toLowerCase();

            System.out.println("Sort alphabetically by 'song name' or 'artist name'");
            String orderingBy = scan.nextLine();
            orderingBy = orderingBy.toLowerCase();


            if( ( searchingBy.equals("song name") || searchingBy.equals("artist") || searchingBy.equals("album") || searchingBy.equals("genre"))){
                if( orderingBy.equals("song name") || orderingBy.equals("artist name")){
                    //we're good. Both inputs are successful
                    validSearch = true;
                }
            }

            if( orderingBy.equals("song name")){
                orderingBy = "s.title asc";
            }
            if( orderingBy.equals("artist name")){
                orderingBy = "a.name asc";
            }

            if( validSearch ) {
                try {
                    Statement stmt;
                    ResultSet rs = null;

                    /*
                    //FIXME I feel like it won't get the count of total
                    // plays right. We'll see, I guess.

                    alright. Let's get a handle on this query
                    select s.title, s.genre, a.name, count(p.timestamp), alb.name
                    from p320_12.song s, p320_12.artist a, song_artist sa, play p, album alb, album_song alb_s
                    where s.sid = sa.sid and sa.artist_id = a.artist_id and p.sid = s.sid and alb.album_id = alb_s.album_id and s.sid = alb_s.sid
                     */


                    switch (searchingBy) {
                        case "song name":

                            System.out.println("Input song name:");
                            String songName = scan.nextLine();

                            stmt = conn.createStatement();
                            rs = stmt.executeQuery(
                                "select s.title, s.genre, a.name, count(p.timestamp), alb.name " +
                                    "from p320_12.song s, p320_12.artist a, song_artist sa, play p, album alb, album_song alb_s " +
                                    "where s.sid = sa.sid and sa.artist_id = a.artist_id and p.sid = s.sid and alb.album_id = alb_s.album_id and s.sid = alb_s.sid " +
                                    "and s.title = " + songName + //NOTE: this last part is what specifies that it's the song name
                                    "order by " + orderingBy
                            );


                            break;
                        case "artist":

                            System.out.println("Input artist name:");
                            String artistName = scan.nextLine();

                            stmt = conn.createStatement();
                            rs = stmt.executeQuery(
                                "select s.title, s.genre, a.name, count(p.timestamp), alb.name " +
                                    "from p320_12.song s, p320_12.artist a, song_artist sa, play p, album alb, album_song alb_s " +
                                    "where s.sid = sa.sid and sa.artist_id = a.artist_id and p.sid = s.sid and alb.album_id = alb_s.album_id and s.sid = alb_s.sid " +
                                    "and a.name = " + artistName + //NOTE: this last part is what specifies that it's the song name
                                    "order by " + orderingBy
                            );


                            break;
                        case "album":

                            System.out.println("Input album name:");
                            String albumName = scan.nextLine();

                            stmt = conn.createStatement();
                            rs = stmt.executeQuery(
                                "select s.title, s.genre, a.name, count(p.timestamp), alb.name " +
                                    "from p320_12.song s, p320_12.artist a, song_artist sa, play p, album alb, album_song alb_s " +
                                    "where s.sid = sa.sid and sa.artist_id = a.artist_id and p.sid = s.sid and alb.album_id = alb_s.album_id and s.sid = alb_s.sid " +
                                    "and alb.name = " + albumName + //NOTE: this last part is what specifies that it's the song name
                                    "order by " + orderingBy
                            );

                            break;
                        case "genre":

                            System.out.println("Input genre name:");
                            String genreName = scan.nextLine();

                            stmt = conn.createStatement();
                            rs = stmt.executeQuery(
                                "select s.title, s.genre, a.name, count(p.timestamp), alb.name " +
                                    "from p320_12.song s, p320_12.artist a, song_artist sa, play p, album alb, album_song alb_s " +
                                    "where s.sid = sa.sid and sa.artist_id = a.artist_id and p.sid = s.sid and alb.album_id = alb_s.album_id and s.sid = alb_s.sid " +
                                    "and s.genre = " + genreName + //NOTE: this last part is what specifies that it's the song name
                                    "order by " + orderingBy
                            );
                            break;
                    }

                    //We've already got the result set from the switch
                    // so now we can print everything out

                    //TODO if we can guarantee a max length of any title or anything,
                    // we can control the white space to be pretty like.

                    System.out.println("Song | Artist | Album | Song Genre | Total Plays");
                    while( rs.next() ){
                        String combined = "";
                        combined += rs.getString("s.title");
                        combined += " | " + rs.getString("a.name");
                        combined += " | " + rs.getString("alb.name");
                        combined += " | " + rs.getString("s.genre");
                        combined += " | " + rs.getInt("count(p.timestamp)");
                        System.out.println(combined);
                    }
                    return rs;

                } catch (Exception e) {
                    e.printStackTrace();
                }

            }else{
                System.out.println("That is not a valid input. Input 'y' to try again.");
                String input = scan.nextLine();
                input = input.toLowerCase();
                if( input.equals("y")){
                    //we're going again
                }else{
                    //guess they're giving up
                    return null;
                }
            }
        }
        return null;
    }

    public static void main(String[] args) throws SQLException {

        int lport = 5431;
        String rhost = "starbug.cs.rit.edu";
        int rport = 5432;
        String user = "CS_USER"; //change to your username
        String password = "CS_PASSWORD"; //change to your password
        String databaseName = "p320_12"; //change to your database name

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

                //NOTE: we might want to keep it in this series of if else statements, because
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

                }
                else if (input.equals("exit")){
                    break;
                }
                else if (input.equals("create new account")) {
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

