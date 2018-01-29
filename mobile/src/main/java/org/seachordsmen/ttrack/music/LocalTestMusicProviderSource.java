package org.seachordsmen.ttrack.music;

import android.support.v4.media.MediaMetadataCompat;

import com.example.android.uamp.model.MusicProviderSource;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

import java.util.Iterator;
import java.util.List;

/**
 * Created by Gidon on 1/20/2018.
 */

public class LocalTestMusicProviderSource implements MusicProviderSource {

    private static class TrackInfo {
        final String title;
        final String path;
        final String genre;
        final Long durationMs;
        TrackInfo(String title, String path, Long durationMs) {
            this.title = title;
            this.path = path;
            this.durationMs = durationMs;
            this.genre = "Training";
        }
    }

    private static final List<TrackInfo> tracks = ImmutableList.of(
            new TrackInfo("Second Star from the Right", "/storage/emulated/0/Download/The Second Star From The Right - Bass Left.MP3", 179000L)
    );

    @Override
    public Iterator<MediaMetadataCompat> iterator() {
        List<MediaMetadataCompat> metadata = Lists.transform(tracks, this::toMediaMetadata);
        return metadata.iterator();
    }

    private MediaMetadataCompat toMediaMetadata(TrackInfo info) {
        String id = Long.toString(info.path.hashCode());
        return new MediaMetadataCompat.Builder()
                .putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID, id)
                .putString(MediaMetadataCompat.METADATA_KEY_TITLE, info.title)
                .putString(MusicProviderSource.CUSTOM_METADATA_TRACK_SOURCE, info.path)
                .putString(MediaMetadataCompat.METADATA_KEY_GENRE, info.genre)
                .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, info.durationMs)
                .build();
    }
}
