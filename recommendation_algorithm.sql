-- Song Recommendation Algorithm --
-----------------------------------
-- Here's how it works:
--
-- For any song, we can compute a list of similar songs with similarity scores  in this
-- way:
-- * Gather a list of plays that occured within 12h of a play of this song by the same user
-- * For every play, gather each other play that was played by the same user within 1 day
-- * Group by user and sid, then assign each song a score equal to
--   log4(# of nearby plays by that user) + 1
-- * Group by sid and compute the final score as the sum of the previous scores
--
-- To recommend songs to a user, we:
-- * Create a list of the user's top 20 songs for the last month
-- * For each song, get the list of the 50 most similar songs
-- * Normalize the similarity scores of the 50 songs to the range 1 - 3
-- * Flatten the 20x50 list into a single list of 1000 songs
-- * Group by sid and compute the sum of similarity scores for each song
-- * Remove songs that the user has listened to more than once
-- * Remove songs that the user has listened to in the last month
-- * Sort by score

CREATE OR REPLACE FUNCTION p320_12.similar (similar_to_sid INTEGER)
	RETURNS TABLE(sid INTEGER, score NUMERIC)
	LANGUAGE SQL
	AS $$
		WITH unnormalized AS (
			WITH plays_by_users AS (
				WITH play_timestamps AS (
					SELECT
						timestamp - INTERVAL '12 hours' AS mini,
						timestamp + INTERVAL '12 hours' AS maxi,
						uid
					FROM p320_12.play
					WHERE sid = similar_to_sid
					-- TODO igore plays by the user recommendations are being made for
				)
				SELECT sid, uid, LOG(4, COUNT(timestamp)) + 1 AS score
				FROM p320_12.play
				WHERE play.sid != similar_to_sid
				AND EXISTS (
					SELECT 1 FROM play_timestamps AS ts
					WHERE ts.uid = play.uid
					AND play.timestamp BETWEEN ts.mini AND ts.maxi
				)
				GROUP BY uid, sid
			)
			SELECT sid, SUM(score) AS score
			FROM plays_by_users
			GROUP BY sid
			LIMIT 50
		),
		score_extremes AS (
			SELECT
				MIN(score) as min_score,
				MAX(score) AS max_score
			FROM unnormalized
		)
		SELECT
			sid,
			CASE
				WHEN min_score = max_score THEN 2
				ELSE (score - min_score) / (max_score - min_score) * 2 + 1
				END
				AS score
		FROM unnormalized, score_extremes
	$$;

CREATE OR REPLACE FUNCTION p320_12.recommendations (rec_for_uid INTEGER)
	RETURNS TABLE(sid INTEGER, score NUMERIC)
	LANGUAGE SQL
	AS $$
		WITH top_songs AS (
			SELECT sid
			FROM p320_12.play
			WHERE uid = rec_for_uid
			AND timestamp > current_timestamp - interval '1 month'
			GROUP BY sid
			ORDER BY COUNT(timestamp) DESC
			LIMIT 20
		)
		SELECT similar_song.sid, SUM(score) AS score
		FROM top_songs
		JOIN p320_12.similar(top_songs.sid) AS similar_song ON 1 = 1
		GROUP BY similar_song.sid
		HAVING NOT EXISTS (
			SELECT 1
			FROM p320_12.play AS oplay
			WHERE uid = rec_for_uid
			AND oplay.sid = similar_song.sid
			AND timestamp > current_timestamp - interval '1 month'
		)
		AND (
			SELECT COUNT(timestamp)
			FROM p320_12.play AS oplay
			WHERE uid = rec_for_uid
			AND oplay.sid = similar_song.sid
		) < 2
		ORDER BY score DESC
	$$;
