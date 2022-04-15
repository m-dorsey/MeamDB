CREATE INDEX ON p320_12.play (sid);
CREATE INDEX ON p320_12.play USING BTREE (uid, timestamp);
CREATE INDEX ON p320_12.play USING BTREE (timestamp);
CREATE INDEX ON p320_12.user (email);
CREATE INDEX ON p320_12.user (username);

CREATE INDEX ON p320_12.album_artist (album_id);
CREATE INDEX ON p320_12.album_song (sid);
CREATE INDEX ON p320_12.album_song (album_id);
CREATE INDEX ON p320_12.song_artist (sid);
CREATE INDEX ON p320_12.song_artist (sid);
CREATE INDEX ON p320_12.song_collection (cid);

-- Additionally, and index exists on every primary key
