package MeamDB;

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;

import java.sql.*;
import java.util.Properties;
import java.util.Scanner;
import java.util.Map;
import java.util.ArrayList;

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
                // Check if the username/password match & if they do, update the lastLogin
                // It's a little clumsy, but we can do it in one statement
                PreparedStatement stmt = conn.prepareStatement(
                    "UPDATE p320_12.user AS usr                   " +
                    "SET last_login = CURRENT_TIMESTAMP           " +
                    "FROM p320_12.user AS original                " +
                    "WHERE usr.username = ? AND usr.password = ?  " +
                    "AND original.uid = usr.uid                   " +
                    "RETURNING usr.uid, original.last_login       "
                );
                stmt.setString(1, username);
                stmt.setString(2, password);
                ResultSet loggedIn = stmt.executeQuery();

                if (loggedIn.next()) {
                    // If we got a match, the user was logged in
                    Timestamp lastLogin = loggedIn.getTimestamp(2);
                    System.out.format(
                        "Welcome back %s!  Your last login was %s%n",
                        username,
                        lastLogin.toString().substring(0,  16) // yyyy-mm-dd hh:mm
                    );
                    return loggedIn.getInt(1);
                }
            }catch (Exception e){
                e.printStackTrace();
            }

			System.out.println("invalid login. 'y' to try again. anything else to exit.");
			input = scan.nextLine();
        } while (input.equalsIgnoreCase("y"));

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
            if(username.indexOf('@') != -1) {
                // We don't want usernames that look like emails
                System.out.println("Usernames cannot contain an '@' sign.");
                System.out.println("Please try again.");
                continue;
            }


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
            + uid + " and p320_12.collection.name = '" + collectionName + "'");

        collection.next();

        int collectionID = collection.getInt("cid");

        System.out.println("How would you like to modify " + collectionName + "? Add song | Remove song");
        String modification = scan.nextLine();
        modification = modification.toLowerCase();
        if(modification.equals("add song")){
            System.out.println("Which song would you like to add?");
            ResultSet songToAdd = searchSong(conn, scan, false);
            while( songToAdd.next()){


                stmt.execute("insert into p320_12.song_collection(sid, cid) values "
                    + "('" + songToAdd.getInt("sid") + "','" + collectionID + "')");
            }
        }
        else if(modification.equals("remove song")){
            System.out.println("Which song would you like to remove?");
            ResultSet songToRemove = searchSong(conn, scan, false);
            while (songToRemove.next()){
                stmt.execute("delete from p320_12.song_collection where p320_12.song_collection.sid = "
                    + songToRemove.getInt("sid") + " and p320_12.song_collection.cid = " + collectionID);
            }
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
            if( printing ) {
                System.out.println("collection name | total songs | total duration"  );

                while (rs.next()) {

                    Statement stmt2 = conn.createStatement();
                    ResultSet data = stmt2.executeQuery("select count(s.sid) as num, sum(s.length) as time from p320_12.song s, p320_12.song_collection c where s.sid = c.sid and c.cid = " + rs.getInt("cid"));

                    data.next();

                    int totalseconds = data.getInt(2);

                    int minutes = totalseconds / 60;
                    int seconds = totalseconds % 60;
                    System.out.println(rs.getString("name") + " | " + data.getInt(1) + " | " + minutes + ":" + seconds);


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
                ResultSet rs = stmt.executeQuery("select name, cid from p320_12.collection where p320_12.collection.uid = " + uid + " and p320_12.collection.name = '" + chosen +"'");

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
                ResultSet rs = stmt.executeQuery("select name from p320_12.collection where p320_12.collection.uid = " + uid + " and p320_12.collection.name = '" + newName + "'");

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



                    Statement stmt2 = conn.createStatement();
                    stmt2.executeUpdate("update p320_12.collection set name = '" + newName + "' where cid = '" + collectionID+ "'");


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
                    ResultSet songs = stmt.executeQuery("select sid from p320_12.song_collection where cid = " + collectionID);

                    //mark all the songs as played
                    while( songs.next() ){
                        Statement stmt2 = conn.createStatement();
                        stmt2.execute("insert into p320_12.play(uid,sid,timestamp) values ('" + uid + "', '" + songs.getInt("sid") + "', CURRENT_TIMESTAMP)" );
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
        ResultSet rs = searchSong(conn, scan, false);

        if (rs.next()) {
            System.out.println("Now playing " + rs.getString("title") + " by " + rs.getString("artist_name"));
            Statement stmt = conn.createStatement();
            stmt.execute("insert into p320_12.play(uid,sid,timestamp) values (" + uid + ", " + rs.getInt("sid") +
                ", CURRENT_TIMESTAMP)");
        }
        else{
            System.out.println("No song available");
            return false;
        }
        return true;
    }

    private static enum SearchKind { Song, Artist, Album, Genre }
    private static enum SortKind { Song, Artist, Genre, Release }
    private static enum SortOrder { Asc, Desc }
    // These three maps are used for parsing input
    private static Map<String, SearchKind> searchKinds = Map.of(
            "song name", SearchKind.Song,
            "artist",    SearchKind.Artist,
            "album",     SearchKind.Album,
            "genre",     SearchKind.Genre
    );
    private static Map<String, SortKind> sortKinds = Map.of(
            "song name",   SortKind.Song,
            "artist name", SortKind.Artist,
            "genre",       SortKind.Genre,
            "release year", SortKind.Release
    );
    private static Map<String, SortOrder> sortOrders = Map.of(
            "ascending",   SortOrder.Asc,
            "descending",  SortOrder.Desc
    );
    // These four maps are used for generating SQL
    private static Map<SearchKind, String> searchKindsSql = Map.of(
            SearchKind.Song, "s.title",
            SearchKind.Artist, "a.name",
            SearchKind.Album, "alb.name",
            SearchKind.Genre, "s.genre"
    );
    private static Map<SortKind, String> sortKindsSql = Map.of(
            SortKind.Song,    "s.title",
            SortKind.Artist,  "a.name",
            SortKind.Genre,   "alb.name",
            SortKind.Release, "s.genre"
    );
    private static Map<SortKind, String> sortKindFallbacksSql = Map.of(
            SortKind.Song,    "a.name ASC",
            SortKind.Artist,  "s.title ASC",
            SortKind.Genre,   "s.title ASC, a.name ASC",
            SortKind.Release, "s.title ASC, a.name ASC"
    );
    private static Map<SortOrder, String> sortOrderSql = Map.of(
            SortOrder.Asc,  "ASC",
            SortOrder.Desc, "DESC"
    );
    // This class is used to represent the results
    private static class SearchResult {
        String song;
        String album;
        String artist;
        String genre;
        int plays;
        SearchResult(String song, String album, String artist, String genre, int plays) {
            // java give me a better way to do this, c'mon
            this.song = song; this.album = album; this.artist = artist;
            this.genre = genre; this.plays = plays;
        }
    }
    public static ResultSet searchSong( Connection conn, Scanner scan, boolean printing ) throws SQLException {

        // First we need to figure out what kind of search this is
        SearchKind searchKind = null;
        SortKind sortKind = null;
        SortOrder sortOrder = null;

        // We'll keep track of this so we know what to ask the user for later on
        // It's just a string version of searchKind though
        String searchKindInput = "this will be overwritten";

        while (searchKind == null) {
            // Keep asking the user until we get a good answer!
            System.out.println("Search by 'song name', 'artist', 'album', or 'genre'");
            searchKindInput = scan.nextLine().strip().toLowerCase();
            searchKind = searchKinds.get(searchKindInput);
            if (searchKind == null)
                System.out.println("Oops, please choose a valid option.\n");
        }
        while (sortKind == null) {
            // This is basically the exact same thing as the previous loop, but for sort
            System.out.println("Sort by 'song name','artist name','genre', or 'release year'");
            String input = scan.nextLine().strip().toLowerCase();
            sortKind = sortKinds.get(input);
            if (sortKind == null)
                System.out.println("Oops, please choose a valid option.\n");
        }
        while (sortOrder == null) {
            System.out.println("Sort 'ascending' or 'descending'");
            String input = scan.nextLine().strip().toLowerCase();
            sortOrder = sortOrders.get(input);
            if (sortOrder == null)
                System.out.println("Oops, please choose a valid option.\n");
        }

        // Now to get what the user actually wants to search up!
        System.out.format("Please enter the %s to search for%n", searchKindInput);
        // We surround it with percent signs so that we can match any string containing
        // this value.  We don't sanitize for pattern characters, but hopefully no songs
        // have weirdo characters
        String searchParameter = "%" + scan.nextLine().strip().toLowerCase() + "%";

        // Time to build the statement!
        //
        // We need to diplay:
        // - Song
        // - Artist
        // - Album
        // - Song Genre
        // - Total Plays
        //
        // To retrieve for other methods to use:
        // - s.sid
        // - s.length
        // - YEAR(s.release_date) as year
        // - a.artist_id
        // - alb.album_id
        // - number of plays as totalPlay
        //
        // Which requires joining
        // - song -> song_artist -> artist
        // - song -> album_song -> album
        // - song -> play
        //
        // Then apply the sort and search that the user specified
        //
        // (i think this query might duplicate results when there's multiple artists, but
        // uhhhh that's an edge case?)
        String query = String.format(
            // a very normal way of writing strings
            " SELECT                                            " +
                "     s.sid,                                        " +
                "     s.length,                                     " +
                "     s.title,                                      " +
                "     s.genre as song_genre,                        " +
                "     EXTRACT(year FROM s.release_date) as year,    " +
                "     a.artist_id,                                  " +
                "     a.name AS artist_name,                        " +
                "     alb.album_id,                                 " +
                "     alb.name AS album_name,                       " +
                "     COUNT(play.sid) as totalPlay                  " +
                " FROM p320_12.song AS s                            " +
                " JOIN p320_12.song_artist                          " +
                "     AS sa                                         " +
                "     ON sa.sid = s.sid                             " +
                " JOIN p320_12.artist                               " +
                "     AS a                                          " +
                "     ON sa.artist_id = a.artist_id                 " +
                " JOIN p320_12.album_song                           " +
                "     AS albs                                       " +
                "     ON s.sid = albs.sid                           " +
                " JOIN p320_12.album                                " +
                "     AS alb                                        " +
                "     ON albs.album_id = alb.album_id               " +
                " LEFT JOIN p320_12.play                            " +
                "     AS play                                       " +
                "     ON play.sid = s.sid                           " +
                " WHERE LOWER(%s) SIMILAR TO ?                      " +
                " GROUP BY                                          " +
                "     s.sid, sa.sid, sa.artist_id, a.artist_id,     " +
                "     albs.sid, albs.album_id, alb.album_id         " +
                " ORDER BY %s %s, %s                                " +
                " LIMIT 50;                                         "
            ,
            searchKindsSql.get(searchKind),
            sortKindsSql.get(sortKind),
            sortOrderSql.get(sortOrder),
            sortKindFallbacksSql.get(sortKind)
        );
        PreparedStatement statement = conn.prepareStatement(query);
        statement.setString(1, searchParameter);

        // and now we RUN!  THAT!  QUERY!!
        ResultSet rs = statement.executeQuery();

        // The first time iterating through we're just gonna store the data accessibly
        // But we also keep track of the max length of each of the fields (for display
        // purposes)
        int maxSong = 4;
        int maxArtist = 6;
        int maxAlbum = 5;
        int maxGenre = 5;
        ArrayList<SearchResult> results = new ArrayList(50);

        if (printing) {
            while (rs.next()) {
                SearchResult result = new SearchResult(
                    rs.getString("title"),
                    rs.getString("album_name"),
                    rs.getString("artist_name"),
                    rs.getString("song_genre"),
                    rs.getInt("totalPlay")
                );
                maxSong = Math.max(maxSong, result.song.length());
                maxArtist = Math.max(maxArtist, result.artist.length());
                maxAlbum = Math.max(maxAlbum, result.album.length());
                maxGenre = Math.max(maxGenre, result.genre.length());
                results.add(result);
            }


            // And now that we have all that, we can print it!
            String formatSpecifier = String.format(
                "%%-%ds | %%-%ds | %%-%ds | %%-%ds | %%d%n", // getting pretty meta here
                maxSong,
                maxArtist,
                maxAlbum,
                maxGenre
            );
            // Print the header first though
            System.out.format(
                String.format(
                    "%%-%ds | %%-%ds | %%-%ds | %%-%ds | %%s%n%s+%s+%s+%s+-------------%n",
                    maxSong,
                    maxArtist,
                    maxAlbum,
                    maxGenre,
                    "-".repeat(maxSong + 1),
                    "-".repeat(maxArtist + 2),
                    "-".repeat(maxAlbum + 2),
                    "-".repeat(maxGenre + 2)
                ),
                "Song", "Artist", "Album", "Genre", "Total Plays"
            );
            for (SearchResult result : results)
                System.out.format(formatSpecifier,
                    result.song,
                    result.artist,
                    result.album,
                    result.genre,
                    result.plays
                );
        }

        // Reset the result set so the next people can use it
        // rs.first(); //hmm yeah this doesn't work
        return rs;
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

        Scanner scan = new Scanner(System.in);

        int lport = 5431;
        String rhost = "starbug.cs.rit.edu";
        int rport = 5432;

        //for security reasons, I'm making it so we have to get user
        // and password from the user instead of just having it sit here.
        System.out.println("Input DB account username: ");
        String user = scan.nextLine();//""; //change to your username
        System.out.println("Input DB account password: ");
        String password = scan.nextLine();//""; //change to your password
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


            String input = "";
            String username = null;

            boolean loggedIn = false;
			int userId = -1;

            //this loop runs forever to get input from the user
            while( true ) {
                System.out.print("what would you like to do: ");

                input = scan.nextLine();
                input = input.toLowerCase();

                //NOTE: we might want to keep it in this series of if else statements, because
                //      the 5th if checks to see if the user is logged in, not the input.

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
                }
                else if (input.equals("help")) {
                    usage();
                }
                else if (!loggedIn) {
                    System.out.println("Error: you must be logged in before executing that command.");
                }
                else if (input.equals("create new collection")) {
                    createNewCollection(conn, scan, userId);

                }
                else if (input.equals("view collections")) {
                    viewCollections(conn, userId, true);

                }
                else if (input.equals("search song")) {
                    searchSong(conn, scan, true);

                }
                else if (input.equals("rename collection")) {
                    renameCollection(conn, scan, userId);

                }
                else if (input.equals("modify collection")) {
                    modifyCollection(conn, scan, userId);

                }
                else if (input.equals("delete collection")) {
                    new DeleteCollection(userId).run(conn, scan);
                }
                else if (input.equals("play song")) {
                    playSong(conn, scan, userId);
                }
                else if (input.equals("play collection")) {
                    playCollection(conn,scan, userId);
                }
                else if (input.equals("follow friend")) {
					/*
					note: we have searching for friends via username instead
					of email, because email isn't necesarily unique, where as
					username is unique.
					 */
                    new FollowUser(userId).run(conn, scan);
                }
                else if (input.equals("unfollow friend")) {
                    new UnfollowUser(userId).run(conn, scan);
                }
                else {
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

