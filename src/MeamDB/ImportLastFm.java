package MeamDB;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.TreeMap;
import java.security.MessageDigest;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;


public class ImportLastFm extends Command {

    private static String apiKey = "538ff97b16827af7f7e03c6846c0c40b";
    private static String secret = "8ae0f74735a5d1b2e02d989d24bb5d81";
    private static String endpoint = "http://ws.audioscrobbler.com/2.0/";
    private static String confirmationMessage =
        "\n"                                                                       +
        "----------------------------------------------------------------------\n" +
        "If you have a last.fm account, you can take this moment to import your\n" +
        "scrobbles from the app.  To do this, you'll need to log in with your\n"   +
        "Last.fm account.  MeamDB will then dowload a list of all of your\n"       +
        "scrobbles, creating any missing songs/albums/artists in the process.\n"   +
        "Your scrobbles will be translated to plays in the database.\n"            +
        "\n"                                                                       +
        "If you don't have a last.fm account, but you do use Spotify, you may\n"   +
        "create a last.fm account and link your Spotify, which will import\n"      +
        "about a month's worth of play history as Scrobbles.  If you want to do\n" +
        "this, do so now before continuing.\n"                                     +
        "\n"                                                                       +
        "If you're ready to proceed, type 'y'.  If you've changed your mind,\n"    +
        "press 'n'\n"                                                              +
        "----------------------------------------------------------------------\n" ;

    private State state = State.LoadLastScan;
    private String error;
    private String token;
    private String lastUsername;
    private String session;
    private int currentPage;
    private int totalPages = 1;
    private int uid;
    private long lastScan = -1;
    private long scanStart;

    private ArrayList<Scrobble> scrobbles = new ArrayList(200);
    private ArrayList<String> missingAlbums = new ArrayList(150);
    private ArrayList<TrackInfo> foundAlbumInfo = new ArrayList(150);
    private HashSet<String> skippedSingles = new HashSet(150);

    public ImportLastFm(int uid) {
        this.uid = uid;
        this.scanStart = System.currentTimeMillis() / 1000;
    }

    protected Action action() {
        switch(this.state) {
            case LoadLastScan:
                return Command.Action.Query(c -> this.loadLastScan(c));
            case InitialConfirmation:
                if(this.lastScan == 0) // The user has never scanned before
                    return Command.Action.Prompt(confirmationMessage);
                else
                    return Command.Action.Prompt(String.format(
                        "Your last scan was %s.  Would you like to check for new scrobbles? (y/n)",
                        formatDuration(this.scanStart - this.lastScan)
                    ));
            case InitialConfirmationBadInput:
                return Command.Action.Prompt(
                    "I couldn't tell what you meant by that.  You can type 'n' to leave, or 'y' to continue"
                );
            case GetToken:
                return Command.Action.Network(c -> this.getToken(c));
            case ApproveLogin:
                return Command.Action.Prompt(
                    String.format(
                        "%nPlease open this URL in your browser, and press Enter once you've approved MeamDB.%nhttp://www.last.fm/api/auth/?api_key=%s&token=%s",
                        apiKey,
                        this.token
                    )
                );
            case GetSession:
                return Command.Action.Network(c -> this.getSession(c));
            case GetTracks:
                return Command.Action.Network(c -> this.getTracks(c));
            case StoreResults:
                return Command.Action.Query(c -> this.storeResults(c));
            case FetchMissingAlbums:
                return Command.Action.Network(c -> this.fetchMissingAlbums(c));
            case ImportFoundAlbums:
                return Command.Action.Query(c -> this.importFoundAlbums(c));
            case FinishLoadingResults:
                return Command.Action.Query(c -> this.finishLoadingResults(c));
            case StoreLastScan:
                return Command.Action.Query(c -> this.storeLastScan(c));
            case Finished:
                return Command.Action.Exit("Import complete!");
            case UserChangedMind:
                return Command.Action.Exit(null);
            case UnknownError:
                return Command.Action.Exit("An unexpected error occured: " + this.error);
        }
        throw new RuntimeException("Unreachable " + this.state);
    }

    protected void processInput(String input) {
        switch(this.state) {
            case InitialConfirmation:
            case InitialConfirmationBadInput:
                switch(input.toLowerCase()) {
                    case "y":
                    case "yes":
                        this.state = State.GetToken; return;
                    case "n":
                    case "no":
                    case "quit":
                    case "exit":
                        this.state = State.UserChangedMind; return;
                    default:
                        this.state = State.InitialConfirmationBadInput; return;
                }
            case ApproveLogin:
                this.state = State.GetSession; return;
        }
    }

    private void loadLastScan(Connection c) throws SQLException {
        PreparedStatement stmt = c.prepareStatement(
            "SELECT last_scan FROM p320_12.user_last_scan WHERE uid = ?"
        );
        stmt.setInt(1, this.uid);
        ResultSet r = stmt.executeQuery();
        if(r.next())
            this.lastScan = r.getLong(1);
        else
            this.lastScan = 0;

        this.state = State.InitialConfirmation;
    }

    private void storeLastScan(Connection c) throws SQLException {
        PreparedStatement stmt = c.prepareStatement(
            "INSERT INTO p320_12.user_last_scan VALUES (?, ?) "+
            "ON CONFLICT (uid) DO UPDATE SET last_scan = EXCLUDED.last_scan;"
        );
        stmt.setInt(1, this.uid);
        stmt.setLong(2, this.scanStart);
        stmt.execute();

        this.state = State.Finished;
    }

    private void getToken(HttpClient c) throws IOException, InterruptedException {
        // We're gonna break from the state machine and do things imperatively for a bit
        // now
        Request req = new Request("auth.gettoken", apiKey);

        Element response = req.doRequest(c);

        Node token = response.getElementsByTagName("token").item(0);
        if(token == null) {
            this.error = "Missing token";
            this.state = State.UnknownError;
            return;
        }

        this.token = token.getFirstChild().getNodeValue();
        this.state = State.ApproveLogin;
    }

    private void getSession(HttpClient c) throws IOException, InterruptedException {
        Request req = new Request("auth.getSession", apiKey);
        req.params.put("token", this.token);

        Element response = req.doRequest(c);

        if(response.getAttribute("status").equalsIgnoreCase("failed")) {
            // There was some kind of problem
            Element error = (Element) response.getElementsByTagName("error").item(0);
            if(error == null) {
                this.error = "Request failed, but no error tag was provided";
                this.state = State.UnknownError;
                return;
            }
            String errorCodeStr = error.getAttribute("code");
            int errorCode;
            try {
                errorCode = Integer.parseInt(errorCodeStr);
            } catch(NumberFormatException e) {
                this.error = "Unexpected error code " + errorCodeStr;
                this.state = State.UnknownError;
                return;
            }
            switch(errorCode) {
                case 4: // Invalid token, probably expired
                    System.out.println("Warning:  Bad token.  Retrying...");
                    this.state = State.GetToken;
                    return;
                case 14: // Unauthorized Token:  The user hasn't approved us yet
                    this.state = State.ApproveLogin;
                    return;
                default:
                    this.error = "Unexpected error from remote:  " + error.getFirstChild().getNodeValue();
                    this.state = State.UnknownError;
                    return;
            }
        }

        Node session = (Element) response.getElementsByTagName("session").item(0);
        if(session == null) {
            // The user hasn't approved us yet
            this.error = "Missing session";
            this.state = State.UnknownError;
            return;
        }

        Node username = response.getElementsByTagName("name").item(0);
        if(username == null) {
            this.error = "Missing username";
            this.state = State.UnknownError;
            return;
        }
        this.lastUsername = username.getFirstChild().getNodeValue();
        System.out.format("Logged in as %s%n", this.lastUsername);

        Node key = response.getElementsByTagName("key").item(0);
        if(key == null) {
            this.error = "Missing session key";
            this.state = State.UnknownError;
            return;
        }
        this.session = key.getFirstChild().getNodeValue();
        this.token = null;
        this.state = State.GetTracks;
        this.currentPage = 1;
    }

    private void getTracks(HttpClient c) throws IOException, InterruptedException {
        Request req = new Request("user.getRecentTracks", apiKey);
        req.params.put("token", this.token);
        req.params.put("limit", "200");
        // use an older time to catch songs that were playing during the scan
        req.params.put("from", String.format("%d", this.lastScan - 1312));
        req.params.put("user", this.lastUsername);
        req.params.put("page", String.format("%d", this.currentPage));

        Element response = req.doRequest(c);
        Element recentTracks = (Element) response.getElementsByTagName("recenttracks").item(0);
        if(recentTracks == null) {
            this.error = "No recenttracks sent by server? :(";
            this.state = State.UnknownError;
            return;
        }
        try {
            this.totalPages = Integer.parseInt(recentTracks.getAttribute("totalPages"));
        } catch(NumberFormatException e) {
            this.error = "Confusing response received from server:  totalPages wasn't an int";
            this.state = State.UnknownError;
            return;
        }
        NodeList trackNodes = recentTracks.getElementsByTagName("track");
        this.scrobbles.clear();
        for(int i = 0; i != trackNodes.getLength(); i++) {
            Element track = (Element) trackNodes.item(i);

            if(track.hasAttribute("nowplaying"))
                continue;

            Node artist = track.getElementsByTagName("artist").item(0);
            if(artist == null) {
                this.error = "Track received with no artist?";
                this.state = State.UnknownError;
                return;
            }
            String artistName = artist.getFirstChild().getNodeValue();

            Node name = track.getElementsByTagName("name").item(0);
            if(artist == null) {
                this.error = "Track received with no name?";
                this.state = State.UnknownError;
                return;
            }
            String trackName = name.getFirstChild().getNodeValue();

            Element album = (Element) track.getElementsByTagName("album").item(0);
            if(album == null) {
                this.error = "Track received with no album element?";
                this.state = State.UnknownError;
                return;
            }
            String albumMBID = album.getAttribute("mbid");

            Element date = (Element) track.getElementsByTagName("date").item(0);
            if(date == null) {
                this.error = "Scrobble received with no date element?";
                this.state = State.UnknownError;
                return;
            }
            long epoch;
            try {
                epoch = Long.parseLong(date.getAttribute("uts"));
            } catch(NumberFormatException e) {
                this.error = "Received malformed date from remote";
                this.state = State.UnknownError;
                return;
            }
            Timestamp time = new Timestamp(epoch * 1000);

            if(albumMBID != null && !albumMBID.isEmpty())
                this.scrobbles.add(new Scrobble(trackName, albumMBID, artistName, time));
            else {
                this.skippedSingles.add(trackName);
            }
        }

        if(this.scrobbles.isEmpty())
            if(this.currentPage++ >= this.totalPages)
                this.state = State.StoreLastScan;
            else
                this.state = State.GetTracks;
        else
            this.state = State.StoreResults;
    }

    private void storeResults(Connection c) throws SQLException {
        c.prepareStatement("BEGIN;").execute();
        c.prepareStatement(
            "CREATE TEMPORARY TABLE IF NOT EXISTS " +
            "new_scrobbles("                        +
                "track VARCHAR(255),"               +
                "artist VARCHAR(127),"              +
                "album CHAR(36),"                   +
                "timestamp TIMESTAMP NOT NULL"      +
            ") ON COMMIT DELETE ROWS;"
        ).execute();

        PreparedStatement loadScrobbles = c.prepareStatement(String.format(
            "INSERT INTO new_scrobbles VALUES %s;",
            "(?,?,?,?),".repeat(this.scrobbles.size())
                .substring(0, 10*this.scrobbles.size() - 1)
        ));
        int i = 0;
        for(Scrobble scrobble : this.scrobbles) {
            loadScrobbles.setString(i + 1,
                scrobble.track.substring(0, Math.min(255, scrobble.track.length())));
            loadScrobbles.setString(i + 2,
                scrobble.artist.substring(0, Math.min(127, scrobble.artist.length())));
            loadScrobbles.setString(i + 3,
                scrobble.album);
            loadScrobbles.setTimestamp(i + 4,
                scrobble.timestamp);
            i += 4;
        }
        loadScrobbles.execute();

        ResultSet missingAlbumsQ =
            c.prepareStatement(
                "SELECT DISTINCT album                 " +
                "FROM new_scrobbles                    " +
                "WHERE album NOT IN (                  " +
                "  SELECT mbid FROM p320_12.album_mbid " +
                ");                                    "
            ).executeQuery();
        while(missingAlbumsQ.next())
            this.missingAlbums.add(missingAlbumsQ.getString(1));
        if(this.missingAlbums.isEmpty())
            this.state = State.FinishLoadingResults;
        else
            this.state = State.FetchMissingAlbums;
    }

    private static Date NULL_DATE = new Date(9999, 01, 01);
    private void fetchMissingAlbums(HttpClient c) throws IOException, InterruptedException {

        for(String mbid : this.missingAlbums) {
            Request req = new Request("album.getInfo", this.apiKey);
            req.params.put("mbid", mbid);

            Element response = req.doRequest(c);
            Element artistNode = (Element) response.getElementsByTagName("artist").item(0);
            if(artistNode == null) {
                this.error = "Artist not included with album information";
                this.state = State.UnknownError;
                return;
            }
            String artist = artistNode.getFirstChild().getNodeValue();

            Element nameNode = (Element) response.getElementsByTagName("name").item(0);
            String albumName = nameNode.getFirstChild().getNodeValue();

            Element tags = (Element) response.getElementsByTagName("tags").item(0);
            Element tag = (Element) tags.getElementsByTagName("tag").item(0);
            String genre;
            if(tag != null) {
                Element tagName = (Element) tag.getElementsByTagName("name").item(0);
                genre = tagName.getFirstChild().getNodeValue();
            } else {
                genre = "untagged";
            }

            Element tracksElem = (Element) response.getElementsByTagName("tracks")
                .item(0);
            if(tracksElem == null) {
                System.out.format("[WARN]  Album %s has no track listing%n", albumName);
                continue;
            }
            NodeList trackNodes = tracksElem.getElementsByTagName("track");

            ArrayList<TrackInfo> tracks = new ArrayList(trackNodes.getLength());

            for(int i = 0; i < trackNodes.getLength(); i++) {
                Element track = (Element) trackNodes.item(i);

                int trackNum;
                try {
                    trackNum = Integer.parseInt(
                        track.getAttribute("rank")
                    );
                } catch(NumberFormatException e) {
                    trackNum = 999;
                }

                String trackName = track.getElementsByTagName("name")
                    .item(0)
                    .getFirstChild()
                    .getNodeValue();

                int length;
                try {
                    length = Integer.parseInt(
                        track.getElementsByTagName("duration")
                            .item(0)
                            .getFirstChild()
                            .getNodeValue()
                    );
                } catch(NumberFormatException e) {
                    length = 0;
                }

                this.foundAlbumInfo.add(
                    new TrackInfo(
                        artist, mbid, albumName, trackNum, length,
                        trackName, genre, NULL_DATE
                    )
                );
            }

            System.out.format("Creating album: %s - %s [%s]%n", albumName, artist, genre);
        }
        this.missingAlbums.clear();
        this.state = State.ImportFoundAlbums;
    }

    private void importFoundAlbums(Connection c) throws SQLException {
        if(this.foundAlbumInfo.isEmpty()) {
            this.state = State.FinishLoadingResults;
            return;
        }

        c.prepareStatement(
            "CREATE TEMPORARY TABLE IF NOT EXISTS "     +
            "found_album_info (" +
                "artist    TEXT,"                       +
                "mbid      CHAR(36),"                   +
                "albumName TEXT,"                       +
                "trackNum  INTEGER,"                    +
                "length    INTEGER,"                    +
                "trackName TEXT,"                       +
                "genre     TEXT,"                       +
                "release   DATE"                        +
            ") ON COMMIT DELETE ROWS;"
        ).execute();
        PreparedStatement insertAlbums = c.prepareStatement(String.format(
            "INSERT INTO found_album_info VALUES %s;",
            "(?, ?, ?, ?, ?, ?, ?, ?),".repeat(this.foundAlbumInfo.size())
                .substring(0, 25*this.foundAlbumInfo.size() - 1)
        ));
        for(int i = 0; i != this.foundAlbumInfo.size(); i++) {
            TrackInfo info = this.foundAlbumInfo.get(i);
            insertAlbums.setString(8*i + 1, info.artist);
            insertAlbums.setString(8*i + 2, info.albumMBID);
            insertAlbums.setString(8*i + 3, info.albumName);
            insertAlbums.setInt(   8*i + 4, info.tracknum);
            insertAlbums.setInt(   8*i + 5, info.length);
            insertAlbums.setString(8*i + 6, info.title);
            insertAlbums.setString(8*i + 7, info.genre);
            insertAlbums.setDate(  8*i + 8, info.release);
        }
        insertAlbums.execute();

        c.prepareStatement(
            "INSERT INTO p320_12.artist (name)  " +
            "  SELECT artist                    " +
            "  FROM found_album_info            " +
            "  ON CONFLICT DO NOTHING           "
        ).execute();
        c.prepareStatement(
            "CREATE TEMPORARY TABLE "          +
            "new_album_ids "                                 +
            "ON COMMIT DROP "                         +
            "AS "                                            +
            "SELECT DISTINCT ON (mbid) "                     +
                "nextval('p320_12.album_ids') as album_id, " +
                "mbid "                                      +
            "FROM found_album_info"
        ).execute();
        c.prepareStatement(
            "INSERT INTO p320_12.album "           +
            "SELECT DISTINCT ON (mbid) "           +
                "album_id, albumName, release "    +
            "FROM new_album_ids "                  +
            "JOIN found_album_info USING (mbid)"
        ).execute();
        c.prepareStatement(
            "INSERT INTO p320_12.album_mbid " +
            "SELECT * FROM new_album_ids;"
        ).execute();
        c.prepareStatement(
            "INSERT INTO p320_12.album_artist "              +
                "SELECT DISTINCT album_id, artist.artist_id "+
                "FROM new_album_ids AS ids "                 +
                "JOIN found_album_info "                     +
                    "AS found "                              +
                    "USING (mbid) "                          +
                "JOIN p320_12.artist AS artist "             +
                    "ON artist.name = found.artist;"
        ).execute();
        c.prepareStatement(
            "CREATE TEMPORARY TABLE "          +
            "new_song_ids "                                  +
            "ON COMMIT DROP "                         +
            "AS "                                            +
            "SELECT DISTINCT ON (mbid, trackNum) "     +
                "nextval('p320_12.song_ids') as sid, " +
                "trackNum, "                           +
                "mbid "                                +
            "FROM found_album_info"
        ).execute();
        c.prepareStatement(
            "INSERT INTO p320_12.song "                          +
                "SELECT sid, length, trackName, genre, release " +
                "FROM new_song_ids "                             +
                "JOIN found_album_info USING (mbid, trackNum)"
        ).execute();
        c.prepareStatement(
            "INSERT INTO p320_12.album_song "     +
                "SELECT album_id, sid, trackNum " +
                "FROM new_song_ids "              +
                "JOIN new_album_ids USING (mbid)"
        ).execute();
        c.prepareStatement(
            "INSERT INTO p320_12.song_artist "    +
                "SELECT sid, artist_id "          +
                "FROM new_song_ids "              +
                "JOIN new_album_ids USING (mbid) "+
                "JOIN p320_12.album_artist USING (album_id);"
        ).execute();


        this.foundAlbumInfo.clear();
        this.state = State.FinishLoadingResults;
    }

    private void finishLoadingResults(Connection c) throws SQLException {
        PreparedStatement stmt = c.prepareStatement(
            "WITH new_scrobble_timestamps AS ("                 +
                "INSERT INTO p320_12.play "                     +
                    "SELECT DISTINCT ON (timestamp) "           +
                        "?, sid, timestamp "                    +
                    "FROM new_scrobbles "                       +
                    "JOIN p320_12.album_mbid ON mbid = album "  +
                    "JOIN p320_12.album_song USING (album_id) " +
                    "JOIN p320_12.song USING (sid) "            +
                    "WHERE LOWER(title) = LOWER(track) "        +
                "ON CONFLICT DO NOTHING "                       +
                "RETURNING timestamp"                           +
            ") "                                                +
            "SELECT COUNT(timestamp), MIN(timestamp) "          +
            "FROM new_scrobble_timestamps;"
        );
        stmt.setInt(1, this.uid);
        ResultSet res = stmt.executeQuery();
        res.next();
        int numNewScrobbles = res.getInt(1);
        Timestamp oldestScrobble = res.getTimestamp(2);
        c.prepareStatement("COMMIT;").execute();
        if(numNewScrobbles != 0)
            System.out.format(
                "[%d/%d]  Imported %d new scrobbles dating back to %s%n",
                this.currentPage, this.totalPages,
                numNewScrobbles,
                oldestScrobble.toString().substring(0,  10) // yyyy-mm-dd
            );
        else
            System.out.format(
                "[%d/%d]  No new scrobbles on this page.  Next!%n",
                this.currentPage, this.totalPages
            );

        if(this.totalPages != this.currentPage) {
            this.state = State.GetTracks;
            this.currentPage++;
        } else {
            System.out.format(
                "Skipped import of %d singles, since single import currently isn't supported.%n",
                this.skippedSingles.size()
            );
            this.state = State.StoreLastScan;
        }
    }

    private static String formatDuration(long duration) {
        if(duration > 2 * 86400)
            return String.format("%d days ago", duration / 86400);
        else if(duration > 86400)
            return "yesterday";
        else if(duration > 3600 * 2)
            return String.format("%d hours ago", duration / 3600);
        else if(duration > 3600)
            return "an hour ago";
        else if(duration > 60 * 2)
            return String.format("%d minutes ago", duration / 60);
        else if(duration > 60)
            return String.format("a minute ago", duration / 60);
        else
            return String.format("%d seconds ago", duration);
    }

    private enum State {
        LoadLastScan,
        InitialConfirmation,
        InitialConfirmationBadInput,
        GetToken,
        ApproveLogin,
        GetSession,
        GetTracks,
        StoreResults,
        FetchMissingAlbums,
        ImportFoundAlbums,
        FinishLoadingResults,
        StoreLastScan,
        Finished,
        UserChangedMind,
        UnknownError
    }

    private static class Request {
        TreeMap<String, String> params = new TreeMap<>();

        Request(String method, String apiKey) {
            this.params.put("method", method);
            this.params.put("api_key", apiKey);
        }

        /** https://www.last.fm/api/desktopauth#_6-sign-your-calls */
        String getApiSig() {
            StringBuilder svscheme = new StringBuilder(100);
            this.params.entrySet()
                .stream()
                .forEachOrdered(e -> svscheme.append(e.getKey()).append(e.getValue()));
            svscheme.append(secret);
            byte[] hash;
            try {
                MessageDigest hasher = MessageDigest.getInstance("MD5");
                hash = hasher.digest(svscheme.toString().getBytes("UTF-8"));
            } catch(Exception e) {
                e.printStackTrace();
                return null; // unreachable?
            }
            return String.format(
                "%02x".repeat(16),
                hash[ 0], hash[ 1], hash[ 2], hash[ 3],
                hash[ 4], hash[ 5], hash[ 6], hash[ 7],
                hash[ 8], hash[ 9], hash[10], hash[11],
                hash[12], hash[13], hash[14], hash[15]
            );
        }

        String getUrl() {
            StringBuilder url = new StringBuilder(127)
                .append(endpoint)
                .append('?');
            this.params.put("api_sig", this.getApiSig());
            this.params.entrySet()
                .stream()
                .forEach(
                    e ->
                        url.append(e.getKey())
                            .append('=')
                            .append(e.getValue())
                            .append('&')
                );
            return url.substring(0, url.length() - 1);
        }

        HttpRequest buildRequest() {
            return HttpRequest.newBuilder(URI.create(this.getUrl()))
                .GET()
                .build();
        }

        Element doRequest(HttpClient c) throws IOException, InterruptedException {
            HttpRequest httpReq = this.buildRequest();
            HttpResponse resp = c.send(httpReq, HttpResponse.BodyHandlers.ofInputStream());
            //HttpResponse resp = c.send(httpReq, HttpResponse.BodyHandlers.ofString());
            //System.out.println(resp.body());
            try {
                return (Element) (
                    DocumentBuilderFactory
                    .newInstance()
                    .newDocumentBuilder()
                    .parse((InputStream) resp.body())
                    //.parse(new ByteArrayInputStream(((String) resp.body()).getBytes()))
                    .getDocumentElement()
                );
            } catch(Exception e) {
                e.printStackTrace();
                System.exit(99);
                return null;
            }
        }
    }

    private static class TrackInfo {
        String artist;
        String albumMBID;
        String albumName;
        int tracknum;
        int length;
        String title;
        String genre;
        Date release;

        TrackInfo(
            String artist, String albumMBID, String albumName, int tracknum, int length,
            String title, String genre, Date release
        ) {
            this.artist = artist; this.albumMBID = albumMBID; this.albumName = albumName;
            this.tracknum = tracknum; this.length = length; this.title = title;
            this.genre = genre; this.release = release;
        }
    }

    private static class Scrobble {
        String track;
        String artist;
        String album;
        Timestamp timestamp;

        Scrobble(String track, String album, String artist, Timestamp timestamp) {
            this.track = track; this.album = album; this.artist = artist;
            this.timestamp = timestamp;
        }
    }
}
