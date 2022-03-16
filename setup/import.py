import csv
import os
from psycopg import connect, sql;
from datetime import datetime, date;
from typing import Any, Dict, Generator, List, Set, Tuple;
from dataclasses import dataclass;

@dataclass
class Artist:
	name: str

	def __hash__(self) -> int:
		return hash(self.name)

@dataclass
class Album:
	name: str
	date: date
	artist: Artist

	def __hash__(self) -> int:
		return hash(self.name) ^ hash(self.date) ^ hash(self.artist)

@dataclass
class Track:
	title: str
	artist: Artist
	album: Album
	duration: int
	track: int
	genre: str
	date: date

	def __hash__(self) -> int:
		return (hash(self.album) + self.track) % 2147483647

	def __eq__(self, other: 'Track') -> bool:
		return self.album == other.album and self.track == other.track

file = open("dataset.csv", 'r');
records = csv.reader(file)

tracks: Set[Track] = set(
	Track(
		title = record[0],
		artist = Artist(record[1]),
		album = Album(
			name = record[2],
			date = datetime.strptime(record[7], "%m/%d/%Y"),
			artist = Artist(record[1]),
		),
		duration = 60 * int(record[3][:-3]) + int(record[3][-2:]),
		track = int(record[4]),
		genre = record[5],
		date = datetime.strptime(record[7], "%m/%d/%Y"),
	)
	for record in records
)

artists: Set[Artist] = set(track.artist for track in tracks)
albums: Set[Album] = set(track.album for track in tracks)

artist_ids: Dict[Artist, int] = dict(
	(a, aid) for (aid, a) in enumerate(artists))
album_ids: Dict[Album, int] = dict(
	(a, aid) for (aid, a) in enumerate(albums))
track_ids: Dict[Track, int] = dict(
	(t, tid) for (tid, t) in enumerate(tracks))




# Alright database time

if "DATABASE_URI" not in os.environ:
	print("Please set the DATABASE_URI environment variable and rerun this script")
	print("An example value might be: postgresql://tts1848:password@localhost/p320_12")
	exit(1)
database_uri = os.environ["DATABASE_URI"]

if "DATABASE_RELATION" not in os.environ:
	print("Please set the DATABASE_RELATION environment variable and rerun this script")
	print("An example value might be: p320_12")
	exit(3)
relation = os.environ["DATABASE_RELATION"]

relation_prefix = sql.Identifier(relation) + sql.SQL(".")

def insert_many_statement(table: str, fields: List[str], n_records: int) -> sql.Composed:
	fields_sql = sql.SQL(',').join(
		map(sql.Identifier, fields)
	)
	single_value =  (
		sql.SQL('(') +
		sql.SQL(',').join(
			[sql.Placeholder()] * len(fields)
		) +
		sql.SQL(')')
	)
	all_values = sql.SQL(',').join([single_value] * n_records)
	return sql.SQL('INSERT INTO {} ({}) VALUES {}').format(
		relation_prefix + sql.Identifier(table),
		fields_sql,
		all_values,
	)

def generate_statement(
	table: str,
	fields: List[str],
	records: List[Tuple[Any, ...]]
) -> Tuple[sql.Composed, List[Tuple[Any, ...]]]:
	print(table, fields, len(records))
	return (
		insert_many_statement(table, fields, len(records)),
		records
	)

def generate_statements(
	table: str,
	fields: List[str],
	records: List[Tuple[Any, ...]]
) -> List[Tuple[sql.Composed, List[Tuple[Any, ...]]]]:
	page_size = 65535 // len(fields)
	return [
		generate_statement(table, fields, records[i:i+page_size])
		for i in range(0, len(records), page_size)
	]

artist_values = [(aid, artist.name) for artist, aid in artist_ids.items()]
album_values = [(aid, album.name, album.date) for album, aid in album_ids.items()]
track_values = [(tid, track.duration, track.title, track.genre, track.date) for track, tid in track_ids.items()]
song_artist_values = [(tid, artist_ids[track.artist]) for track, tid in track_ids.items()]
album_artist_values = [(aid, artist_ids[album.artist]) for album, aid in album_ids.items()]
album_song_values = [(album_ids[track.album], tid, track.track) for track, tid in track_ids.items()]

pairs: List[Tuple[sql.Composed, List[Tuple[Any, ...]]]] = \
	generate_statements("artist", ["artist_id", "name"], artist_values) +\
	generate_statements("album", ["album_id", "name", "release_date"], album_values) +\
	generate_statements("song", ["sid", "length", "title", "genre", "release_date"], track_values) +\
	generate_statements("song_artist", ["sid", "artist_id"], song_artist_values) +\
	generate_statements("album_artist", ["album_id", "artist_id"], album_artist_values) +\
	generate_statements("album_song", ["album_id", "sid", "track_number"], album_song_values)


database_uri = os.environ["DATABASE_URI"]
if len(database_uri) == 0:
	print("Please set the DATABASE_URI environment variable and rerun this script")
	print("An example value might be: postgresql://tts1848:password@localhost/p320_12")
	exit(1)

connection = connect(database_uri)
with connection.cursor() as c:
	for (stmt, values) in pairs:
		c.execute(stmt, [f for val in values for f in val])
	connection.commit()
