-- The play table is the largest table in the database, at 143 thousand entries.  It's
-- also the most intensely queried, due to its heavy use in analytics.  Simultaniously,
-- entries into the play table are bottlenecked by external network (during imports) or
-- user speed (during manual marking) not by SQL queries.  Therefor, the reduction in
-- speed that can result from additional indices is justified.

-- For this reason, we create three indices on it, each serving a role in analytics.  The
-- first of these is a simple hash index by SID.  This is used for generating top charts,
-- as well as playing an incidental role in several other queries for which no better
-- index is suiting.
CREATE INDEX ON p320_12.play (sid);

-- We additionally create a BTREE index on uid and timestamp.  This is used to produce an
-- ordered listing of plays by user, and in some cases, just to gather a list of plays
-- performed by a user, for analytic purposes.
CREATE INDEX ON p320_12.play USING BTREE (uid, timestamp);

-- Lastly, we create an ordered BTREE index on just timestamps, which is used to hasten
-- the selection of plays within a certain time range when no UID is otherwise being used.
CREATE INDEX ON p320_12.play USING BTREE (timestamp);

-- We also create two indices on the user table.  Due to the low frequency of the
-- insertion of users, these are also fairly low-cost.  The two indices we create are
-- username and email - the two means by which users can look up other users.
CREATE INDEX ON p320_12.user (email);
CREATE INDEX ON p320_12.user (username);

-- We also need indicies on all of the relationship tables in the database.  Not all of
-- these need an index on both keys.  For example, we never need to know what collections
-- a song appears in.  However, having these indices speeds up the many lookups we perform
-- on these tables when gathering metadata or looking for songs matching certain
-- parameters.
CREATE INDEX ON p320_12.album_artist (album_id);
CREATE INDEX ON p320_12.album_artist (artist_id);
CREATE INDEX ON p320_12.album_song (sid);
CREATE INDEX ON p320_12.album_song (album_id);
CREATE INDEX ON p320_12.song_artist (sid);
CREATE INDEX ON p320_12.song_artist (sid);
CREATE INDEX ON p320_12.song_collection (cid);

-- Lastly, a simple implicit index exists on the primary key of each table.  Since the
-- primary key of the table is very frequently used to look up the entries in the table,
-- this comes in handy a lot.
