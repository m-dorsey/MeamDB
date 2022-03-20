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

        System.out.println("How would you like to modify " + collectionName + "? Add song | Remove song");
        String modification = scan.nextLine();
        modification = modification.toLowerCase();
        if(modification.equals("add song")){
            System.out.println("Which song would you like to add?");
            ResultSet songToAdd = searchSong(conn, scan);
            stmt.executeQuery("insert into p320_12.song_collection "
                + "(" + rs.getInt("s.sid") +", " + collection.getInt("cid") + ")");
        }
        else if(modification.equals("remove song")){
            System.out.println("Which song would you like to remove?");
            ResultSet songToRemove = searchSong(conn, scan);
            stmt.executeQuery("delete from p320_12.song_collection where p320_12.song_collection.sid = "
                 + rs.getInt("s.sid") + " and p320_12.song_collection.cid = "+ collection.getInt("cid"));
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
                    Statement stmt2 = conn.createStatement();
                    ResultSet data = stmt2.executeQuery("select count(s.sid) as num, sum(s.length) as time from p320_12.song s, p320_12.collection c where s.sid = c.cid and c.cid = " + rs.getInt("cid"));

                    data.next();
                    System.out.println(rs.getString("name") + " | " + data.getInt(1)  + " | " + data.getInt(2));


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

            System.out.println("Sort by 'song name','artist name','genre', or 'release year'");
            String orderingBy = scan.nextLine();
            orderingBy = orderingBy.toLowerCase();
            String orderAppend1 = "";

            System.out.println("Sort 'ascending' or 'descending'");
            String sortDirection = scan.nextLine();
            sortDirection = sortDirection.toLowerCase();
            String orderAppend2 = "";
            String orderRunoff = "";

            if( ( searchingBy.equals("song name") || searchingBy.equals("artist") || searchingBy.equals("album") || searchingBy.equals("genre"))){
                if( orderingBy.equals("song name") || orderingBy.equals("artist name") || orderingBy.equals("genre") || orderingBy.equals("release year")){
                    if( sortDirection.equals("ascending") || sortDirection.equals("descending")) {
                        //we're good. all inputs are successful
                        validSearch = true;
                    }
                }
            }

            if( orderingBy.equals("song name")){
                //orderingBy = "s.title";// asc";
                orderAppend1 = "s.title";
                orderRunoff = "a.name asc";
            }
            if( orderingBy.equals("artist name")){
                //orderingBy = "a.name";// asc";
                orderAppend1 = "a.name";
                orderRunoff = "s.title asc";
            }
            if( orderingBy.equals("genre")){
                orderAppend1 = "s.genre";
                orderRunoff = "s.title asc, a.name asc";
            }
            if( orderingBy.equals("release year")){
                orderAppend1 = "releaseYear";
                orderRunoff = "s.title asc, a.name asc";
            }

            if( sortDirection.equals( "ascending" )){
                orderAppend2 = "asc";
            }else{
                orderAppend2 = "desc";
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



                     alright. back to the chalk board.
                     what do we need in this query?
                        song name
                        artist name
                        album
                        length
                        listen count
                    What tables to we need to query?
                        p320_12.song s
                        p320_12.artist a
                        p320_12.song_artist s_a
                        p320_12.album alb
                        p320_12.album_song alb_s
                        p320_12.play p
                    what columns do we need?
                        s.title
                        s.sid
                        s.length
                        s.genre
                        YEAR(s.release_date) as year
                        a.name
                        a.artist_id
                        s_a.sid
                        s_a.artist_id
                        alb.name
                        alb.album_id
                        alb_s.album_id
                        alb_s.sid
                        p.sid    //FIXME want an outer join on plays. It doesn't necessarily exist
                        count(p) as totalPlay
                    group by sid

                    what comparisons need to be made
                        s.sid = s_a.sid
                        s.sid = alb_s.sid
                        s.sid = p.sid   //FIXME how will this work with the outer join?
                        a.artist_id = s_a.artist_id
                        alb.album_id = alb_s.album_id

                    what comparisons are chosen by the user?
                        s.title = 'input';
                        a.name = 'input';
                        alb.name = 'input';
                        genre.name = 'input';

                    what do we do for sorting?
                        FIRST
                        get sorting from user
                        asc/desc on
                        s.title
                        a.name
                        s.genre
                        s.release_date

                     then assuming no contradictions
                        s.title asc
                        a.name asc



                     select s.title, s.sid, s.length, s.genre, YEAR(s.release_date) as year, a.name, a.artist_id, s_a.sid, s_a.artist_id, alb.name, alb.album_id, alb_s.album_id, alb_s.sid
                     from p320_12.song s, p320_12.artist a, p320_12.song_artist s_a, p320_12.album alb, p320_12.album_song alb_s, p320_12.play p
                     where s.sid = s_a.sid and s.sid = alb_s.sid and a.artist_id = s_a.artist_id and alb.album_id = alb_s.album_id
                      and INSERT CUSTOM THING
                     order by INSERT CUSTOM THING, s.title asc, a.name asc






                     */


                    switch (searchingBy) {
                        case "song name":

                            System.out.println("Input song name:");
                            String songName = scan.nextLine();

                            stmt = conn.createStatement();
                            rs = stmt.executeQuery(
                                "select s.title, s.sid, s.length, s.genre, extract(year from s.release_date) as releaseYear, a.name, alb.name " +
                                    "from p320_12.song s, p320_12.artist a, p320_12.song_artist s_a, p320_12.album alb, p320_12.album_song alb_s " +
                                    "where s.sid = s_a.sid and s.sid = alb_s.sid and a.artist_id = s_a.artist_id and alb.album_id = alb_s.album_id " +
                                    "and s.title = '" + songName + "' " +
                                    "order by " + orderAppend1 + " " + orderAppend2 + ", " + orderRunoff
                            );


                            /*
                            stmt = conn.createStatement();
                            rs = stmt.executeQuery(
                                "select s.title, s.genre, a.name, count(p.timestamp), alb.name " +
                                    "from p320_12.song s, p320_12.artist a, song_artist sa, play p, album alb, album_song alb_s " +
                                    "where s.sid = sa.sid and sa.artist_id = a.artist_id and p.sid = s.sid and alb.album_id = alb_s.album_id and s.sid = alb_s.sid " +
                                    "and s.title = '" + songName + "'" + //NOTE: this last part is what specifies that it's the song name
                                    "order by " + orderAppend + ";"
                            );

                             */


                            break;
                        case "artist":

                            System.out.println("Input artist name:");
                            String artistName = scan.nextLine();

                            stmt = conn.createStatement();
                            rs = stmt.executeQuery(
                                "select s.title, s.sid, s.length, s.genre, extract(year from s.release_date) as releaseYear, a.name, alb.name " +
                                    "from p320_12.song s, p320_12.artist a, p320_12.song_artist s_a, p320_12.album alb, p320_12.album_song alb_s " +
                                    "where s.sid = s_a.sid and s.sid = alb_s.sid and a.artist_id = s_a.artist_id and alb.album_id = alb_s.album_id " +
                                    "and a.name = '" + artistName + "' " +
                                    "order by " + orderAppend1 + " " + orderAppend2 + ", " + orderRunoff
                            );

                            /*
                            stmt = conn.createStatement();
                            rs = stmt.executeQuery(
                                "select s.title, s.genre, a.name, count(p.timestamp), alb.name " +
                                    "from p320_12.song s, p320_12.artist a, song_artist sa, play p, album alb, album_song alb_s " +
                                    "where s.sid = sa.sid and sa.artist_id = a.artist_id and p.sid = s.sid and alb.album_id = alb_s.album_id and s.sid = alb_s.sid " +
                                    "and a.name = '" + artistName + "'" +//NOTE: this last part is what specifies that it's the song name
                                    "order by " + orderAppend + ";"
                            );

                             */


                            break;
                        case "album":

                            System.out.println("Input album name:");
                            String albumName = scan.nextLine();

                            stmt = conn.createStatement();
                            rs = stmt.executeQuery(
                                "select s.title, s.sid, s.length, s.genre, extract(year from s.release_date) as releaseYear, a.name, alb.name " +
                                    "from p320_12.song s, p320_12.artist a, p320_12.song_artist s_a, p320_12.album alb, p320_12.album_song alb_s " +
                                    "where s.sid = s_a.sid and s.sid = alb_s.sid and a.artist_id = s_a.artist_id and alb.album_id = alb_s.album_id " +
                                    "and alb.name = '" + albumName + "' " +
                                    "order by " + orderAppend1 + " " + orderAppend2 + ", " + orderRunoff
                            );

                            /*
                            stmt = conn.createStatement();
                            rs = stmt.executeQuery(
                                "select s.title, s.genre, a.name, count(p.timestamp), alb.name " +
                                    "from p320_12.song s, p320_12.artist a, song_artist sa, play p, album alb, album_song alb_s " +
                                    "where s.sid = sa.sid and sa.artist_id = a.artist_id and p.sid = s.sid and alb.album_id = alb_s.album_id and s.sid = alb_s.sid " +
                                    "and alb.name = '" + albumName + "'" + //NOTE: this last part is what specifies that it's the song name
                                    "order by " + orderAppend + ";"
                            );

                             */

                            break;
                        case "genre":

                            System.out.println("Input genre name:");
                            String genreName = scan.nextLine();

                            stmt = conn.createStatement();
                            rs = stmt.executeQuery(
                                "select s.title, s.sid, s.length, s.genre, extract(year from s.release_date) as releaseYear, a.name, alb.name " +
                                    "from p320_12.song s, p320_12.artist a, p320_12.song_artist s_a, p320_12.album alb, p320_12.album_song alb_s " +
                                    "where s.sid = s_a.sid and s.sid = alb_s.sid and a.artist_id = s_a.artist_id and alb.album_id = alb_s.album_id " +
                                    "and s.genre = '" + genreName + "' " +
                                    //"group by s.sid " +
                                    "order by " + orderAppend1 + " " + orderAppend2 + ", " + orderRunoff
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
                        combined += rs.getString("title");
                        combined += " | " + rs.getString(6);
                        combined += " | " + rs.getString(7);
                        combined += " | " + rs.getString("genre");
                        //combined += " | " + rs.getInt("count(p.timestamp)");
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
                    searchSong(conn, scan);

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

