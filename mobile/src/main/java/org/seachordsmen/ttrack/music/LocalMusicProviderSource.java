package org.seachordsmen.ttrack.music;

import android.content.Context;
import android.database.Cursor;
import android.provider.MediaStore.Audio.Genres;
import android.provider.MediaStore.Audio.Media;
import android.support.v4.media.MediaMetadataCompat;
import android.util.Log;

import com.example.android.uamp.model.MusicProviderSource;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import org.seachordsmen.ttrack.model.TC;
import org.seachordsmen.ttrack.utils.EntityCursor;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import lombok.Value;

import static android.support.v4.media.MediaMetadataCompat.METADATA_KEY_ARTIST;
import static android.support.v4.media.MediaMetadataCompat.METADATA_KEY_DURATION;
import static android.support.v4.media.MediaMetadataCompat.METADATA_KEY_GENRE;
import static android.support.v4.media.MediaMetadataCompat.METADATA_KEY_MEDIA_ID;
import static android.support.v4.media.MediaMetadataCompat.METADATA_KEY_TITLE;

/**
 * Created by Gidon on 1/20/2018.
 */

public class LocalMusicProviderSource implements MusicProviderSource {

    private static final String TAG = "ttrack." + LocalMusicProviderSource.class.getSimpleName();

    private final Context context;

    public LocalMusicProviderSource(Context context) {
        this.context = context;
    }

    @Value
    private static class GenreInfo {
        public final Integer id;
        public final String name;
    }

    @Override
    public Iterator<MediaMetadataCompat> iterator() {
        Map<Integer, String> audioIdToGenre = getAudioIdToGenre();

        Cursor cursor = context.getContentResolver().query(
                Media.EXTERNAL_CONTENT_URI,
                    new String[] {
                            Media._ID,
                            Media.TITLE,
                            Media.DATA,
                            Media.ARTIST,
                            Media.DURATION,
                            Media.DATE_ADDED
                    },
                    null,null,null
        );

        try (EntityCursor<MediaMetadataCompat> ec = new EntityCursor<>(cursor, c -> {
            int id = c.getInt(0);
            Log.i(TAG, "Raeding song ID " + id + ": " + c.getString(1));
            return new MediaMetadataCompat.Builder()
                    .putString(METADATA_KEY_MEDIA_ID, Integer.toString(id))
                    .putString(METADATA_KEY_TITLE, c.getString(1))
                    .putString(CUSTOM_METADATA_TRACK_SOURCE, "file:///" + c.getString(2))
                    .putString(METADATA_KEY_ARTIST, c.getString(3))
                    .putLong(METADATA_KEY_DURATION, c.getLong(4))
                    .putString(TC.METADATA_KEY_PLAYLIST_ID, "All Music")
                    .putString(METADATA_KEY_GENRE, audioIdToGenre.getOrDefault(id, "Unknown"))
                    .build();
        })){
            return ec.stream().collect(Collectors.toList()).iterator();
        }
    }

    private Map<Integer, String> getAudioIdToGenre() {
        Log.i(TAG,"Fetching genres from external URI");
        ImmutableMap.Builder<Integer, String> genreMapBuilder = ImmutableMap.builder();
        try (Cursor cursor = context.getContentResolver().query(
                Genres.EXTERNAL_CONTENT_URI,
                new String[]{
                        Genres._ID,
                        Genres.NAME
                },
                null,
                null,
                null)) {
            while (cursor.moveToNext()) {
                Log.i(TAG, String.format("Found genre: %d %s", cursor.getInt(0), cursor.getString(1)));
                GenreInfo genreInfo = new GenreInfo(cursor.getInt(0), cursor.getString(1));
                try (EntityCursor<Integer> audioIdsForGenre = getAudioIdsForGenre(genreInfo)) {
                    audioIdsForGenre.forEach(id -> genreMapBuilder.put(id, genreInfo.getName()));
                }
            }
        }
        return genreMapBuilder.build();
    }

    private EntityCursor<Integer> getAudioIdsForGenre(GenreInfo genreInfo) {
        Log.i(TAG, "Loading audio IDs for genre " + genreInfo.name + " (" + genreInfo.id + ")");
        Cursor cursor = context.getContentResolver().query(
                Genres.Members.getContentUri("external", genreInfo.id),
                new String[] { Media._ID },
                null,
                null,
                null);
        return new EntityCursor<>(cursor, c -> c.getInt(0));
    }

    private MediaMetadataCompat toPlaylistMetadata(Cursor cursor) {
        return new MediaMetadataCompat.Builder().build();
    }
}
