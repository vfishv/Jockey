package com.marverenic.music.model;

import android.content.Context;
import android.content.res.Resources;
import android.database.Cursor;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;
import android.provider.MediaStore;
import androidx.annotation.NonNull;

import com.marverenic.music.R;
import com.marverenic.music.data.store.MediaStoreUtil;
import com.marverenic.music.data.store.PlayCountStore;
import com.marverenic.music.utils.UriUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static android.media.MediaMetadataRetriever.METADATA_KEY_ALBUM;
import static android.media.MediaMetadataRetriever.METADATA_KEY_ARTIST;
import static android.media.MediaMetadataRetriever.METADATA_KEY_CD_TRACK_NUMBER;
import static android.media.MediaMetadataRetriever.METADATA_KEY_DATE;
import static android.media.MediaMetadataRetriever.METADATA_KEY_DURATION;
import static android.media.MediaMetadataRetriever.METADATA_KEY_TITLE;
import static com.marverenic.music.model.ModelUtil.compareLong;
import static com.marverenic.music.model.ModelUtil.hashLong;
import static com.marverenic.music.model.ModelUtil.parseUnknown;
import static com.marverenic.music.model.ModelUtil.sortableTitle;
import static com.marverenic.music.model.ModelUtil.stringToInt;
import static com.marverenic.music.model.ModelUtil.stringToLong;

public class Song implements Parcelable, Comparable<Song> {

    public static final Parcelable.Creator<Song> CREATOR = new Parcelable.Creator<Song>() {
        public Song createFromParcel(Parcel in) {
            return new Song(in);
        }

        public Song[] newArray(int size) {
            return new Song[size];
        }
    };

    protected String songName;
    protected long songId;
    protected String artistName;
    protected String albumName;
    protected long songDuration;
    protected Uri location;
    protected int year;
    protected long dateAdded; // seconds since Jan 1, 1970
    protected long albumId;
    protected long artistId;
    protected int trackNumber;

    private boolean isInLibrary;
    private String sortableName;
    private String sortableArtistName;
    private String sortableAlbumName;

    private Song() {

    }

    private Song(Parcel in) {
        songName = in.readString();
        songId = in.readLong();
        albumName = in.readString();
        artistName = in.readString();
        songDuration = in.readLong();
        location = in.readParcelable(Uri.class.getClassLoader());
        year = in.readInt();
        dateAdded = in.readLong();
        albumId = in.readLong();
        artistId = in.readLong();

        isInLibrary = (in.readByte() != 0);
        sortableName = in.readString();
        sortableArtistName = in.readString();
        sortableAlbumName = in.readString();
    }

    public Song(Song s) {
        this.songName = s.songName;
        this.songId = s.songId;
        this.artistName = s.artistName;
        this.albumName = s.albumName;
        this.songDuration = s.songDuration;
        this.location = s.location;
        this.year = s.year;
        this.dateAdded = s.dateAdded;
        this.albumId = s.albumId;
        this.artistId = s.artistId;
        this.trackNumber = s.trackNumber;

        this.isInLibrary = s.isInLibrary;
        this.sortableName = s.sortableName;
        this.sortableArtistName = s.sortableArtistName;
        this.sortableAlbumName = s.sortableAlbumName;
    }

    /**
     * Builds a {@link List} of Songs from a Cursor
     * @param cur A {@link Cursor} to use when reading the {@link MediaStore}. This Cursor may have
     *            any filters and sorting, but MUST have AT LEAST the columns in
     *            {@link MediaStoreUtil#SONG_PROJECTION}. The caller is responsible for closing
     *            this Cursor.
     * @param res A {@link Resources} Object from {@link Context#getResources()} used to get the
     *            default values if an unknown value is encountered
     * @return A List of songs populated by entries in the Cursor
     */
    public static List<Song> buildSongList(Cursor cur, Resources res) {
        List<Song> songs = new ArrayList<>(cur.getCount());

        int titleIndex = cur.getColumnIndex(MediaStore.Audio.Media.TITLE);
        int idIndex = cur.getColumnIndex(MediaStore.Audio.Media._ID);
        int artistIndex = cur.getColumnIndex(MediaStore.Audio.Media.ARTIST);
        int albumIndex = cur.getColumnIndex(MediaStore.Audio.Media.ALBUM);
        int durationIndex = cur.getColumnIndex(MediaStore.Audio.Media.DURATION);
        int dataIndex = cur.getColumnIndex(MediaStore.Audio.Media.DATA);
        int yearIndex = cur.getColumnIndex(MediaStore.Audio.Media.YEAR);
        int dateIndex = cur.getColumnIndex(MediaStore.Audio.Media.DATE_ADDED);
        int albumIdIndex = cur.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID);
        int artistIdIndex = cur.getColumnIndex(MediaStore.Audio.Media.ARTIST_ID);
        int trackIndex = cur.getColumnIndex(MediaStore.Audio.Media.TRACK);

        if (idIndex == -1) {
            idIndex = cur.getColumnIndex(MediaStore.Audio.Playlists.Members.AUDIO_ID);
        }

        final String unknownSong = res.getString(R.string.unknown);
        final String unknownArtist = res.getString(R.string.unknown_artist);
        final String unknownAlbum = res.getString(R.string.unknown_album);

        for (int i = 0; i < cur.getCount(); i++) {
            cur.moveToPosition(i);
            Song next = new Song();
            next.songName = parseUnknown(cur.getString(titleIndex), unknownSong);
            next.songId = cur.getLong(idIndex);
            next.artistName = parseUnknown(cur.getString(artistIndex), unknownArtist);
            next.albumName = parseUnknown(cur.getString(albumIndex), unknownAlbum);
            next.songDuration = cur.getLong(durationIndex);
            next.location = Uri.fromFile(new File(cur.getString(dataIndex)));
            next.year = cur.getInt(yearIndex);
            next.dateAdded = cur.getLong(dateIndex);
            next.albumId = cur.getLong(albumIdIndex);
            next.artistId = cur.getLong(artistIdIndex);
            next.trackNumber = cur.getInt(trackIndex);

            next.isInLibrary = true;
            next.sortableName = sortableTitle(next.songName, res);
            next.sortableArtistName = sortableTitle(next.artistName, res);
            next.sortableAlbumName = sortableTitle(next.albumName, res);

            songs.add(next);
        }

        return songs;
    }

    /**
     * Builds a Song that corresponds to a specific URI
     * @param context A Context used to load information about the URI
     * @param uri The URI to build a song from
     * @return A Song with data corresponding to that of the given URI
     */
    public static Song fromUri(Context context, Uri uri) {
        Song song = new Song();
        song.location = uri;
        song.songName = UriUtils.getDisplayName(context, uri);
        song.artistName = context.getResources().getString(R.string.unknown_artist);
        song.albumName = context.getResources().getString(R.string.unknown_album);
        song.songDuration = 0;
        song.year = 0;
        song.trackNumber = 0;
        song.dateAdded = 0;
        song.songId = -1 * Math.abs(uri.hashCode());
        song.albumId = -1;
        song.artistId = -1;

        song.isInLibrary = false;
        song.sortableName = sortableTitle(song.songName, context.getResources());
        song.sortableArtistName = sortableTitle(song.artistName, context.getResources());
        song.sortableAlbumName = sortableTitle(song.albumName, context.getResources());

        if (uri.getScheme().equals("content") || uri.getScheme().equals("file")) {
            song.loadInfoFromMetadata(context);
        }

        return song;
    }

    private void loadInfoFromMetadata(Context context) {
        MediaMetadataRetriever mmr = new MediaMetadataRetriever();
        mmr.setDataSource(context, location);

        songName = parseUnknown(mmr.extractMetadata(METADATA_KEY_TITLE), songName);
        artistName = parseUnknown(mmr.extractMetadata(METADATA_KEY_ARTIST), artistName);
        albumName = parseUnknown(mmr.extractMetadata(METADATA_KEY_ALBUM), albumName);
        songDuration = stringToLong(mmr.extractMetadata(METADATA_KEY_DURATION), songDuration);
        year = stringToInt(mmr.extractMetadata(METADATA_KEY_DATE), year);
        trackNumber = stringToInt(mmr.extractMetadata(METADATA_KEY_CD_TRACK_NUMBER), trackNumber);

        try {
            mmr.release();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public String getSongName() {
        return songName;
    }

    public long getSongId() {
        return songId;
    }

    public String getArtistName() {
        return artistName;
    }

    public String getAlbumName() {
        return albumName;
    }

    public long getSongDuration() {
        return songDuration;
    }

    public Uri getLocation() {
        return location;
    }

    public int getYear() {
        return year;
    }

    public long getDateAdded() {
        return dateAdded;
    }

    public long getAlbumId() {
        return albumId;
    }

    public long getArtistId() {
        return artistId;
    }

    public int getTrackNumber() {
        return trackNumber;
    }

    public boolean isInLibrary() {
        return isInLibrary;
    }

    @Override
    public int hashCode() {
        return hashLong(songId);
    }

    @Override
    public boolean equals(final Object obj) {
        return this == obj
                || (obj != null && obj instanceof Song && songId == ((Song) obj).songId);
    }

    public String toString() {
        return songName;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(songName);
        dest.writeLong(songId);
        dest.writeString(albumName);
        dest.writeString(artistName);
        dest.writeLong(songDuration);
        dest.writeParcelable(location, 0);
        dest.writeInt(year);
        dest.writeLong(dateAdded);
        dest.writeLong(albumId);
        dest.writeLong(artistId);

        dest.writeByte((byte) (isInLibrary ? 1 : 0));
        dest.writeString(sortableName);
        dest.writeString(sortableArtistName);
        dest.writeString(sortableAlbumName);
    }

    @Override
    public int compareTo(@NonNull Song another) {
        return this.sortableName.compareTo(another.sortableName);
    }

    public static final Comparator<Song> ARTIST_COMPARATOR = (s1, s2) ->
            s1.sortableArtistName.compareTo(s2.sortableArtistName);

    public static final Comparator<Song> ALBUM_COMPARATOR = (s1, s2) ->
            s1.sortableAlbumName.compareTo(s2.sortableAlbumName);

    public static Comparator<Song> playCountComparator(PlayCountStore countStore) {
        return (s1, s2) -> countStore.getPlayCount(s2) - countStore.getPlayCount(s1);
    }

    public static Comparator<Song> skipCountComparator(PlayCountStore countStore) {
        return (s1, s2) -> countStore.getSkipCount(s2) - countStore.getSkipCount(s1);
    }

    public static final Comparator<Song> DATE_ADDED_COMPARATOR = (s1, s2) ->
            compareLong(s2.getDateAdded(), s1.getDateAdded());

    public static Comparator<Song> playDateComparator(PlayCountStore countStore) {
        return (s1, s2) -> compareLong(countStore.getPlayDate(s2), countStore.getPlayDate(s1));
    }

    public static final Comparator<Song> YEAR_COMPARATOR = (s1, s2) ->
            s2.getYear() - s1.getYear();

    public static final Comparator<Song> TRACK_COMPARATOR = (s1, s2) -> {
        int diff = s1.getTrackNumber() - s2.getTrackNumber();
        if (diff == 0) {
            // Sort by name when there's a conflict
            diff = s1.compareTo(s2);
        }
        return diff;
    };

}
