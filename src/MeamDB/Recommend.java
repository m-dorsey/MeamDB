package MeamDB;

import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class Recommend extends Command {
    private State state = State.MainMenu;
    private RecType recType;
    private String selectedGenre;
    private int selectedSong;
    private int uid;
    private boolean badInput = false;

    private ArrayList<String> genres = new ArrayList(5);
    private ArrayList<Result> chart = new ArrayList(50);
    private DetailedResult trackDetails;

    private static String MAIN_MENU = new Menu("chart", 4, 8)
        .addEntry("Top this Month")
        .addEntry("Popular with Friends")
        .addEntry("Top Genres")
        .addEntry("Picks for You")
        .display()
        .append('\n')
        .toString();

    public Recommend(int uid) {
        this.uid = uid;
    }

    protected Action action() {
        switch(this.state) {
            case MainMenu:
                return Action.Prompt(
                    this.badInput
                    ? MAIN_MENU + "Oops, that wasn't a valid chart.  Please make a selection or type 'quit'"
                    : MAIN_MENU + "Pick a chart to view, or type 'quit' to return to the main menu"
                );
            case QueryGenres:
                return Action.Query(c -> this.queryGenres(c));
            case PickGenre:
                return Action.Prompt(
                    this.badInput
                    ? "\nHmm, that's not right.  Try entering a genre number, or typing 'back'"
                    : this.generateGenreMenu().append("\nPick a genre to view top tracks, or type 'back' to go back").toString()
                );
            case QueryRecs:
                return Action.Query(c -> this.queryRecs(c));
            case ListRecs:
                return Action.Prompt(
                    this.badInput
                    ? "\nHmm, that's not right.  Try entering a track number, or typing 'back'"
                    : this.generateChartMenu().append("\nPick a track to view details, or type 'back' to go back").toString()
                );
            case QueryTrackInfo:
                return Action.Query(c -> this.queryTrackInfo(c));
            case ShowTrackInfo:
                return Action.Prompt(
                    this.badInput
                    ? "Oops, that's not an option.  Please try typing a number between 1 and 3."
                    : this.renderTrackInfo()
                );
            case MarkListen:
                return Action.Query(c -> this.markListen(c));
            case ListenRecorded:
                return Action.Prompt(
                    this.badInput
                    ? "\nThat doesn't look right.  Please enter the number next to the option you want" :
                    "Listen recorded!\n\n" +

                    "What would you like to do?\n" +
                    "[1] Return to chart\n"        +
                    "[2] Pick a different chart\n" +
                    "[3] Return to main menu"
                );
            case Exit:
                return Action.Exit(null);
        }
        throw new RuntimeException("unreachable");
    }

    protected void processInput(String input) {
        this.badInput = false;
        switch(this.state) {
            case MainMenu:
                if(input.equalsIgnoreCase("quit")) {
                    this.state = State.Exit;
                    return;
                }
                try {
                    this.recType = RecType.values()[Integer.parseInt(input) - 1];
                    if(this.recType == RecType.Genres)
                        this.state = State.QueryGenres;
                    else
                        this.state = State.QueryRecs;
                } catch(NumberFormatException | ArrayIndexOutOfBoundsException e) {
                    this.badInput = true;
                }
                break;
            case PickGenre:
                if(input.equalsIgnoreCase("back")) {
                    this.state = State.MainMenu; return;
                }
                try {
                    this.selectedGenre = this.genres.get(Integer.parseInt(input) - 1);
                    this.state = State.QueryRecs;
                } catch(NumberFormatException | IndexOutOfBoundsException e) {
                    this.badInput = true;
                }
                break;
            case ListRecs:
                if(input.equalsIgnoreCase("back")) {
                    this.state =
                        this.recType == RecType.Genres
                        ? State.PickGenre
                        : State.MainMenu;
                    return;
                }
                try {
                    this.selectedSong = this.chart.get(Integer.parseInt(input) - 1).sid;
                    this.state = State.QueryTrackInfo;
                } catch(NumberFormatException | IndexOutOfBoundsException e) {
                    this.badInput = true;
                }
                break;
            case ShowTrackInfo:
                switch(input.toLowerCase()) {
                    case "1":
                    case "open":
                        String url = String.format(
                            "https://musicbrainz.org/release/%s",
                            this.trackDetails.mbid
                        );

                        // impure :(
                        // but also im too lazy to incorperate this into Command
                        if(Desktop.isDesktopSupported()) {
                            try {
                                Desktop.getDesktop().browse(new URI(url));
                                System.out.format("Opening %s...%n%n", url);
                            } catch(IOException | URISyntaxException e) {
                                e.printStackTrace();
                                System.out.println("[ERROR] Failed to open URI");
                            }
                        } else {
                            System.out.println("Desktop interface not supported!");
                            System.out.println("Try opening this URL:");
                            System.out.println(url);
                        }
                        return;
                    case "2":
                    case "listen":
                    case "mark":
                        this.state = State.MarkListen; return;
                    case "3":
                    case "back":
                        this.state = State.ListRecs; return;
                    case "quit":
                        this.state = State.Exit; return;
                    default:
                        this.badInput = true; return;
                }
            case ListenRecorded:
                switch(input.toLowerCase()) {
                    case "1":
                    case "return":
                    case "back":
                        this.state = State.ListRecs; return;
                    case "2":
                    case "pick":
                        this.state = State.MainMenu; return;
                    case "3":
                    case "quit":
                    case "exit":
                        this.state = State.Exit; return;
                    default:
                        this.badInput = true; return;
                }
            default:
                throw new RuntimeException("Unexpected input-type event");
        }
    }

    private void queryGenres(Connection c) throws SQLException {
        PreparedStatement stmt = c.prepareStatement(
            "SELECT genre " +
            "FROM p320_12.play " +
            "JOIN p320_12.song USING (sid) " +
            "WHERE age(timestamp) < INTERVAL '1 month' " +
            "GROUP BY genre " +
            "ORDER BY COUNT(*) DESC " +
            "LIMIT 5;"
        );
        System.out.println(stmt);
        ResultSet rs = stmt.executeQuery();
        this.genres.clear();
        while(rs.next())
            this.genres.add(rs.getString(1));
        this.state = State.PickGenre;
    }

    private void queryRecs(Connection c) throws SQLException {
        PreparedStatement stmt = c.prepareStatement(this.recType.makeStatement());
        switch(this.recType) {
            case Friends:
            case Personal:
                stmt.setInt(1, this.uid);
                break;
            case Genres:
                stmt.setString(1, this.selectedGenre);
                break;
            case Month:
                break; //no-op
        }
        ResultSet results = stmt.executeQuery();
        this.chart.clear();
        while(results.next()) {
            this.chart.add(new Result(
                results.getInt(1),
                results.getString(2)
            ));
        }
        this.state = State.ListRecs;
    }

    private StringBuilder generateChartMenu() {
        Menu menu = new Menu("track", 50, 25);
        for(Result r: this.chart)
            menu.addEntry(r.title);
        return menu.display();
    }

    private StringBuilder generateGenreMenu() {
        Menu menu = new Menu("genre", 20, 5);
        for(String g : this.genres)
            menu.addEntry(g);
        return menu.display();
    }

    private void queryTrackInfo(Connection c) throws SQLException {
        PreparedStatement stmt = c.prepareStatement(
            "SELECT title, length, genre, track_number, album.name, artist.name, mbid " +
            "FROM p320_12.song " +
            "JOIN p320_12.album_song USING (sid) " +
            "JOIN p320_12.album USING (album_id) " +
            "LEFT JOIN p320_12.song_artist USING (sid) " +
            "LEFT JOIN p320_12.artist USING (artist_id) " +
            "LEFT JOIN p320_12.album_mbid USING (album_id) " +
            "WHERE sid = ?;"
        );
        stmt.setInt(1, this.selectedSong);
        ResultSet result = stmt.executeQuery();
        if(result.next()) {
            this.trackDetails = new DetailedResult(
                result.getString(1),
                result.getInt(2),
                result.getString(3),
                result.getInt(4),
                result.getString(5),
                result.getString(6),
                result.getString(7)
            );
        } else {
            throw new RuntimeException("Couldn't find selected song??");
        }
        this.state = State.ShowTrackInfo;
    }

    private String renderTrackInfo() {
        return String.format(
            "Title:    %s%n" +
            "Artist:   %s%n" +
            "Length:   %d:%02d%n" +
            "Genre:    %s%n%n" +

            "Album:    %s%n" +
            "Track No: %s%n%n" +

            "What would you like to do?%n" +
            "[1] Open in web browser%n" +
            "[2] Mark as listened%n" +
            "[3] Go back",
            this.trackDetails.title,
            this.trackDetails.artist,
            this.trackDetails.length / 60,
            this.trackDetails.length % 60,
            this.trackDetails.genre,
            this.trackDetails.album,
            this.trackDetails.trackNumber
        );
    }

    private void markListen(Connection c) throws SQLException {
        PreparedStatement stmt = c.prepareStatement(
            "INSERT INTO p320_12.play VALUES (?, ?, CURRENT_TIMESTAMP);"
        );
        stmt.setInt(1, this.uid);
        stmt.setInt(2, this.selectedSong);
        stmt.execute();
        this.state = State.ListenRecorded;
    }

    private enum State {
        MainMenu, QueryGenres, PickGenre, QueryRecs, ListRecs, QueryTrackInfo,
        ShowTrackInfo, MarkListen, ListenRecorded, Exit
    }
    private enum RecType {
        Month, Friends, Genres, Personal;

        protected String makeStatement() {
            switch(this) {
                case Month:
                    // No args
                    return
                        "SELECT sid, title " +
                        "FROM p320_12.play " +
                        "JOIN p320_12.song USING (sid) " +
                        "WHERE timestamp > CURRENT_TIMESTAMP - INTERVAL '1 month' " +
                        "GROUP BY sid, title " +
                        "ORDER BY COUNT(*) DESC " +
                        "LIMIT 50;";
                case Friends:
                    // 1 arg: uid
                    return
                        "SELECT sid, title " +
                        "FROM p320_12.follower "+
                        "JOIN p320_12.play "+
                            "ON play.uid = followed " +
                        "JOIN p320_12.song " +
                            "USING (sid) " +
                        "WHERE follower = ? " +
                            "AND age(timestamp) < INTERVAL '1 month' " +
                        "GROUP BY sid, title " +
                        "ORDER BY COUNT(*) DESC " +
                        "LIMIT 50;";
                case Genres:
                    // 1 arg: genre
                    return
                        "SELECT sid, title " +
                        "FROM p320_12.play " +
                        "JOIN p320_12.song " +
                            "USING (sid) " +
                        "WHERE genre = ? " +
                            "AND age(timestamp) < interval '1 month' " +
                        "GROUP BY sid, title " +
                        "ORDER BY COUNT(*) DESC " +
                        "LIMIT 50;";
                case Personal:
                    // 1 arg: uid
                    return
                        "SELECT sid, title " +
                        "FROM p320_12.recommendations(?) " +
                        "JOIN p320_12.song " +
                            "USING (sid) " +
                        "LIMIT 50;";
            }
            throw new RuntimeException("unreachable nya");
        }
    }

    private class Result {
        int sid;
        String title;
        Result(int sid, String title)
            { this.sid = sid; this.title = title; }
    }

    private class DetailedResult {
        String title;
        int length;
        String genre;
        int trackNumber;
        String album;
        String artist;
        String mbid;

        DetailedResult(
            String title, int length, String genre, int trackNumber, String album,
            String artist, String mbid
        ) {
            this.title = title; this.length = length; this.genre = genre;
            this.trackNumber = trackNumber; this.album = album; this.artist = artist;
            this.mbid = mbid;
        }
    }
}
