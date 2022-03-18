package MeamDB;

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;

import java.sql.*;
import java.util.Properties;
import java.util.Scanner;

public class Main {


    /**
     *
     * @return -1 if invalid, userid otherwise.
     */
    private static int login( Connection conn, Scanner scan ){

        String username = "";
        String password = "";
		    String input;

        do {
            System.out.println("input the username for the account");
            username = scan.nextLine();
            System.out.println("input the password for the account.");
            password = scan.nextLine();


            try {
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery("Select password, uid from p320_12.user where p320_12.user.username = '" + username + "'");

                while (rs.next()) {
                    if( rs.getString(1).equals(password)){
						return rs.getInt(2);
                    }
                }
            }catch (Exception e){
                e.printStackTrace();
            }

			System.out.println("invalid login. 'y' to try again. anything else to exit.");
			input = scan.nextLine();
        } while (!input.equalsIgnoreCase("y"));

		// User chose not to log in
        return -1;
    }



    public static int createNewAccount(Connection conn, Scanner scan) throws SQLException {


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
                            return -1;
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
            } else {
				validPassword = true;
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
                            return -1;
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

		return createUser(conn, username, password, fname, lname, email);
    }

	public static int createUser(
		Connection c,
		String username,
		String password,
		String fname,
		String lname,
		String email
	) throws SQLException {
		PreparedStatement creationStatement = c.prepareStatement("INSERT INTO p320_12.user (username, password, fname, lname, email) VALUES (?, ?, ?, ?, ?) RETURNING uid");

		creationStatement.setString(1, username);
		creationStatement.setString(2, password);
		creationStatement.setString(3, fname);
		creationStatement.setString(4, lname);
		creationStatement.setString(5, email);

		ResultSet maybeUserId = creationStatement.executeQuery();
		if(maybeUserId.next()) {
			return maybeUserId.getInt(1);
		} else {
			throw new RuntimeException("Failed to create user???");
		}
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
                    stmt.executeUpdate("insert into p320_12.collection (name, uid) "
                        + "values ('" + collectionName + "', " + uid + ")");
                    System.out.println("Collection: " + collectionName + " has been created");
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
            stmt.executeQuery("delete from p320_12.song_collection where p320_12.song_collection.sid = "
                 + rs.getString("s.sid") + " and p320_12.song_collection.cid = "+ collection.getString("cid"));
        }
        else{
            System.out.println("Invalid modification. Please try again");
            return false;
        }
        return true;
    }

    public static ResultSet viewCollections( Connection conn, int uid, boolean printing ){
        //want to the the full result set and print out everything
        // FIXME we should check to see if we just need to print out all collectoins
        //  or collections *with* their songs, or if we print all then
        //  allow a user to expand a selected one.

        try {
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("select name, cid from p320_12.collection where p320_12.collection.uid = " + uid);

            while( rs.next() ){
                if( printing ) {
                    System.out.println(rs.getString("name"));
                }else{
                    //we just don't print. This exists, so we can
                    // easily get all the collections for the
                    // play collection function.
                }
            }


            return rs;

        }catch (Exception e){
            e.printStackTrace();
        }

        //should be unreachable.
        return null;
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


        //this should be unreachable. Either way, it won't affect anything.
    }

    public static boolean playCollection( Connection conn, Scanner scan, int uid ){
        ResultSet rs = viewCollections(conn, uid, false);

        boolean collectionFound = false;
        int collectionID = -1; //-1 is a temporary value that never actually happens.

        while (!collectionFound) {

            System.out.println("Input a collection to play.");
            String collection = scan.nextLine();


            try {
                while (rs.next()) {
                    if (rs.getString("name").equals(collection)) {
                        collectionFound = true;
                        collectionID = rs.getInt("cid");
                    }
                }

                if( !collectionFound ){
                    System.out.println("That is not a valid name. Input 'y' to try again");
                    String input = scan.nextLine();
                    if( input.toLowerCase().equals("y")){
                        //we're going again
                    }else{
                        //we're quitting
                        return false;
                    }
                }else{
                    //otherwise, we can mark everything as played

                    //get all the songs in that collection
                    Statement stmt = conn.createStatement();
                    ResultSet songs = stmt.executeQuery("select sid from song_collection where cid = " + collectionID);

                    //mark all the songs as played
                    while( songs.next() ){
                        Statement stmt2 = conn.createStatement();
                        stmt2.executeQuery("insert into p320_12.play (" + uid + ", " + rs.getInt("sid") + ", Current TIME CURRENT_TIMESTAMP)" );
                    }


                    return true;

                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }


        return true;
    }

    public static boolean playSong( Connection conn, Scanner scan, int uid ) throws SQLException {
        ResultSet rs = searchSong(conn, scan);

        if (rs.next()) {
            System.out.println("Now playing" + rs.getString("s.title") + " by " + rs.getString("a.name"));
            Statement stmt = conn.createStatement();
            stmt.executeQuery("insert into p320_12.play (" + uid + ", " + rs.getInt("s.sid") +
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

    /**
     *
     */
    public static void usage() {

        System.out.println("HELP ------------------------------------------------------------------------------------------------------------\n" +
                "'login'                   \t\t Login to an existing account.\n" +
                "                          \t\t *You must login before executing other commands*\n" +
                "'create new account'      \t\t Create a new account\n" +
                "'create new collection'   \t\t Create a new collection\n" +
                "'view collections'        \t\t View all of your created collections\n" +
                "'search song'             \t\t Search for songs\n" +
                "'rename collection'       \t\t Rename one of your collections\n" +
                "'modify collection'       \t\t Modify one of your collections\n" +
                "'delete collection'       \t\t Delete one of your collections\n" +
                "'play song'               \t\t Play a song\n" +
                "'play collection'         \t\t Play a collection\n" +
                "'follow friend'           \t\t Follow a friend\n" + //FIXME it isn't specified, but we probably also want to be able to list the follows.
                "'unfollow friend'         \t\t Unfollow a friend\n" +
                "'exit'                    \t\t Exit the program\n"
        );

    }

    /**
     *
     * @param args
     * @throws SQLException
     */
    public static void main(String[] args) throws SQLException {

        int lport = 5431;
        String rhost = "starbug.cs.rit.edu";
        int rport = 5432;
        String user = ""; //change to your username
        String password = ""; //change to your password
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

            System.out.println(
                    "*****************************************\n" +
                    "-----------------------------------------\n" +
                    "            Welcome to MeamDB\n" +
                    "-----------------------------------------\n" +
                    "*****************************************\n" +
                    "Getting Started \n" +
                    "--input one of the following commands:\n" +
                    "  'login'             \t Login\n" +
                    "  'create new account'\t Create new account\n" +
                    "  'help'              \t List all commands\n" +
                    "-----------------------------------------"
            );
//            System.out.println("Input 'login' to log in with your credentials or 'create new account' to create a new account");



            /*
            a loop to constantly get input from users
            while true
             */

            Scanner scan = new Scanner(System.in);
            String input = "";
            String username = null;

            boolean loggedIn = false;
			int userId = -1;

            //this loop runs forever to get input from the user
            while( true ) {
                System.out.print("what would you like to do: ");

                input = scan.nextLine();
                input = input.toLowerCase();

                //TODO we probably want to have a function for each command

                //NOTE: we might want to keep it in this series of if else statements, because
                //      the 4th if checks to see if the user is logged in, not the input.

                if (input.equals("login")) {
                    //in here, we need to handle logging in
                    userId = login( conn, scan );
                    if( userId != -1 ){
                        loggedIn = true;
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

                    userId = createNewAccount( conn, scan );
                    if( userId != -1 ){
                        loggedIn = true;
                    }else {
                        loggedIn = false;
                    }

                } else if (input.equals("help")) {

                    usage();

                } else if (!loggedIn) {
                    System.out.println("Error: you must be logged in before executing that command.");
                } else if (input.equals("create new collection")) {
                    createNewCollection(conn, scan, userId);

                } else if (input.equals("view collections")) {
                    viewCollections(conn, userId, true);

                } else if (input.equals("search song")) {
                    searchSong(conn, scan);

                } else if (input.equals("rename collection")) {
                    renameCollection(conn, scan, userId);

                } else if (input.equals("modify collection")) {
                    modifyCollection(conn, scan, userId);

                } else if (input.equals("delete collection")) {
                    new DeleteCollection(userId).run(conn, scan);
                } else if (input.equals("play song")) {
                    playSong(conn, scan, userId);

                } else if (input.equals("play collection")) {
                    playCollection(conn,scan, userId);
                } else if (input.equals("follow friend")) {
					new FollowUser(userId).run(conn, scan);
                } else if (input.equals("unfollow friend")) {
                    new UnfollowUser(userId).run(conn, scan);
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

