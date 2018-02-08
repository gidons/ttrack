/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.seachordsmen.ttrack.uamp.ui;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.RemoteException;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.text.format.DateUtils;
import android.util.Pair;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;

import org.seachordsmen.ttrack.uamp.AlbumArtCache;
import org.seachordsmen.ttrack.uamp.MusicService;
import org.seachordsmen.ttrack.uamp.R;
import org.seachordsmen.ttrack.uamp.utils.LogHelper;

import com.google.common.collect.ImmutableMap;

import org.seachordsmen.ttrack.model.AudioMix;
import org.seachordsmen.ttrack.model.StateBundleHelper;
import org.seachordsmen.ttrack.model.TC;

import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static android.view.View.INVISIBLE;
import static android.view.View.VISIBLE;

/**
 * A full screen player that shows the current playing music with a background image
 * depicting the album art. The activity also has controls to seek/pause/play the audio.
 */
public class FullScreenPlayerActivity extends ActionBarCastActivity {
    private static final String TAG = LogHelper.makeLogTag(FullScreenPlayerActivity.class);
    private static final long PROGRESS_UPDATE_INTERNAL = 1000;
    private static final long PROGRESS_UPDATE_INITIAL_INTERVAL = 100;

    private ImageView mSkipPrev;
    private ImageView mSkipNext;
    private ImageView mPlayPause;
    private ImageView mChangeMix;
    private ImageView mSetBookmark;
    private ImageView mChangeSpeed;
    private View mBookmark;
    private TextView mStart;
    private TextView mEnd;
    private SeekBar mSeekbar;
    private TextView mLine1;
    private TextView mLine2;
    private TextView mLine3;
    private ProgressBar mLoading;
    private View mControllers;
    private Drawable mPauseDrawable;
    private Drawable mPlayDrawable;
    private Drawable mAudioMixUnknownDrawable;
    private Drawable mAudioMixFullDrawable;
    private Drawable mAudioMixStereoDrawable;
    private Drawable mAudioMixMonoDrawable;
    private Drawable mAudioMixMissingDrawable;
    private Drawable mSetBookmarkDrawable;
    private Drawable mClearBookmarkDrawable;
    private ImageView mBackgroundImage;

    private String mCurrentArtUrl;
    private final Handler mHandler = new Handler();
    private MediaBrowserCompat mMediaBrowser;

    private static final Map<AudioMix, Integer> AUDIO_MIX_DRAWABLE_IDS = ImmutableMap.of(
            AudioMix.UNCHANGED, R.drawable.ic_stereo_left,
            AudioMix.ALL_LEFT, R.drawable.ic_mono,
            AudioMix.ALL_RIGHT, R.drawable.ic_missing,
            AudioMix.FULL_MIX, R.drawable.ic_full_mix
    );
    private Map<AudioMix, Drawable> mAudioMixDrawables;

    private final Runnable mUpdateProgressTask = new Runnable() {
        @Override
        public void run() {
            updateProgress();
        }
    };

    private final ScheduledExecutorService mExecutorService =
        Executors.newSingleThreadScheduledExecutor();

    private ScheduledFuture<?> mScheduleFuture;
    private PlaybackStateCompat mLastPlaybackState;

    private final MediaControllerCompat.Callback mCallback = new MediaControllerCompat.Callback() {
        @Override
        public void onPlaybackStateChanged(@NonNull PlaybackStateCompat state) {
            LogHelper.d(TAG, "onPlaybackstate changed", state);
            updatePlaybackState(state);
        }

        @Override
        public void onMetadataChanged(MediaMetadataCompat metadata) {
            if (metadata != null) {
                updateMediaDescription(metadata.getDescription());
                updateDuration(metadata);
            }
        }
    };

    private final MediaBrowserCompat.ConnectionCallback mConnectionCallback =
            new MediaBrowserCompat.ConnectionCallback() {
        @Override
        public void onConnected() {
            LogHelper.d(TAG, "onConnected");
            try {
                connectToSession(mMediaBrowser.getSessionToken());
            } catch (RemoteException e) {
                LogHelper.e(TAG, e, "could not connect media controller");
            }
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_full_player);
        initializeToolbar();
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("");
        }

        mBackgroundImage = (ImageView) findViewById(R.id.background_image);
        mPauseDrawable = ContextCompat.getDrawable(this, R.drawable.uamp_ic_pause_white_48dp);
        mPlayDrawable = ContextCompat.getDrawable(this, R.drawable.uamp_ic_play_arrow_white_48dp);
        mAudioMixUnknownDrawable = ContextCompat.getDrawable(this, R.drawable.ic_unknown_mix);
        mAudioMixDrawables = AUDIO_MIX_DRAWABLE_IDS.entrySet().stream()
                .map(p -> Pair.create(p.getKey(), ContextCompat.getDrawable(this, p.getValue())))
                .collect(Collectors.toMap(p -> p.first, p -> p.second));
/*
        mAudioMixFullDrawable = ContextCompat.getDrawable(this, R.drawable.ic_full_mix);
        mAudioMixMissingDrawable = ContextCompat.getDrawable(this, R.drawable.ic_missing);
        mAudioMixStereoDrawable = ContextCompat.getDrawable(this, R.drawable.ic_stereo_left);
        mAudioMixMonoDrawable = ContextCompat.getDrawable(this, R.drawable.ic_mono);
        */
        mSetBookmarkDrawable = ContextCompat.getDrawable(this, R.drawable.ic_set_bookmark);
        mClearBookmarkDrawable = ContextCompat.getDrawable(this, R.drawable.ic_clear_bookmark);

        mPlayPause = (ImageView) findViewById(R.id.play_pause);
        mSkipNext = (ImageView) findViewById(R.id.next);
        mSkipPrev = (ImageView) findViewById(R.id.prev);
        mChangeMix = (ImageView) findViewById(R.id.changeAudioMix);
        mSetBookmark = (ImageView) findViewById(R.id.setBookmark);
        mChangeSpeed = (ImageView) findViewById(R.id.changeSpeed);
        mBookmark = findViewById(R.id.bookmark);
        mStart = (TextView) findViewById(R.id.startText);
        mEnd = (TextView) findViewById(R.id.endText);
        mSeekbar = (SeekBar) findViewById(R.id.seekBar1);
        mLine1 = (TextView) findViewById(R.id.line1);
        mLine2 = (TextView) findViewById(R.id.line2);
        mLine3 = (TextView) findViewById(R.id.line3);
        mLoading = (ProgressBar) findViewById(R.id.progressBar1);
        mControllers = findViewById(R.id.controllers);

        mSkipNext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MediaControllerCompat.TransportControls controls = MediaControllerCompat.getMediaController(FullScreenPlayerActivity.this).getTransportControls();
                controls.skipToNext();
            }
        });

        mSkipPrev.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MediaControllerCompat.TransportControls controls = MediaControllerCompat.getMediaController(FullScreenPlayerActivity.this).getTransportControls();
                controls.skipToPrevious();
            }
        });

        mPlayPause.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PlaybackStateCompat state = MediaControllerCompat.getMediaController(FullScreenPlayerActivity.this).getPlaybackState();
                if (state != null) {
                    MediaControllerCompat.TransportControls controls = MediaControllerCompat.getMediaController(FullScreenPlayerActivity.this).getTransportControls();
                    switch (state.getState()) {
                        case PlaybackStateCompat.STATE_PLAYING: // fall through
                        case PlaybackStateCompat.STATE_BUFFERING:
                            controls.pause();
                            stopSeekbarUpdate();
                            break;
                        case PlaybackStateCompat.STATE_PAUSED:
                        case PlaybackStateCompat.STATE_STOPPED:
                            controls.play();
                            scheduleSeekbarUpdate();
                            break;
                        default:
                            LogHelper.d(TAG, "onClick with state ", state.getState());
                    }
                }
            }
        });

        mChangeMix.setOnClickListener(v -> sendCustomAction(TC.CUSTOM_ACTION_SWITCH_AUDIO_MIX));
        mSetBookmark.setOnClickListener(v -> sendCustomAction(TC.CUSTOM_ACTION_SET_BOOKMARK));
        mChangeSpeed.setOnClickListener(v -> sendCustomAction(TC.CUSTOM_ACTION_CHANGE_SPEED));

        mSeekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                mStart.setText(DateUtils.formatElapsedTime(progress / 1000));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                stopSeekbarUpdate();
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                MediaControllerCompat.getMediaController(FullScreenPlayerActivity.this).getTransportControls().seekTo(seekBar.getProgress());
                scheduleSeekbarUpdate();
            }
        });

        // Only update from the intent if we are not recreating from a config change:
        if (savedInstanceState == null) {
            updateFromParams(getIntent());
        }

        mMediaBrowser = new MediaBrowserCompat(this,
            new ComponentName(this, MusicService.class), mConnectionCallback, null);
    }

    private void sendCustomAction(String action) {
        PlaybackStateCompat state = MediaControllerCompat.getMediaController(FullScreenPlayerActivity.this).getPlaybackState();
        if (state != null) {
            MediaControllerCompat.TransportControls controls = MediaControllerCompat.getMediaController(FullScreenPlayerActivity.this).getTransportControls();
            controls.sendCustomAction(action, null);
        }
    }

    private void connectToSession(MediaSessionCompat.Token token) throws RemoteException {
        MediaControllerCompat mediaController = new MediaControllerCompat(
                FullScreenPlayerActivity.this, token);
        if (mediaController.getMetadata() == null) {
            finish();
            return;
        }
        MediaControllerCompat.setMediaController(FullScreenPlayerActivity.this, mediaController);
        mediaController.registerCallback(mCallback);
        PlaybackStateCompat state = mediaController.getPlaybackState();
        updatePlaybackState(state);
        MediaMetadataCompat metadata = mediaController.getMetadata();
        if (metadata != null) {
            updateMediaDescription(metadata.getDescription());
            updateDuration(metadata);
        }
        updateProgress();
        if (state != null && (state.getState() == PlaybackStateCompat.STATE_PLAYING ||
                state.getState() == PlaybackStateCompat.STATE_BUFFERING)) {
            scheduleSeekbarUpdate();
        }
    }

    private void updateFromParams(Intent intent) {
        if (intent != null) {
            MediaDescriptionCompat description = intent.getParcelableExtra(
                MusicPlayerActivity.EXTRA_CURRENT_MEDIA_DESCRIPTION);
            if (description != null) {
                updateMediaDescription(description);
            }
        }
    }

    private void scheduleSeekbarUpdate() {
        stopSeekbarUpdate();
        if (!mExecutorService.isShutdown()) {
            mScheduleFuture = mExecutorService.scheduleAtFixedRate(
                    new Runnable() {
                        @Override
                        public void run() {
                            mHandler.post(mUpdateProgressTask);
                        }
                    }, PROGRESS_UPDATE_INITIAL_INTERVAL,
                    PROGRESS_UPDATE_INTERNAL, TimeUnit.MILLISECONDS);
        }
    }

    private void stopSeekbarUpdate() {
        if (mScheduleFuture != null) {
            mScheduleFuture.cancel(false);
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        if (mMediaBrowser != null) {
            mMediaBrowser.connect();
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        if (mMediaBrowser != null) {
            mMediaBrowser.disconnect();
        }
        MediaControllerCompat controllerCompat = MediaControllerCompat.getMediaController(FullScreenPlayerActivity.this);
        if (controllerCompat != null) {
            controllerCompat.unregisterCallback(mCallback);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopSeekbarUpdate();
        mExecutorService.shutdown();
    }

    public static void startFullScreenActivity(Activity sourceActivity) {
        Intent intent = new Intent(sourceActivity, FullScreenPlayerActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        MediaControllerCompat controller = MediaControllerCompat.getMediaController(sourceActivity);
        MediaMetadataCompat metadata = controller.getMetadata();
        if (metadata != null) {
            intent.putExtra(MusicPlayerActivity.EXTRA_CURRENT_MEDIA_DESCRIPTION,
                    metadata.getDescription());
        }
        sourceActivity.startActivity(intent);
    }

    private void fetchImageAsync(@NonNull MediaDescriptionCompat description) {
        if (description.getIconUri() == null) {
            return;
        }
        String artUrl = description.getIconUri().toString();
        mCurrentArtUrl = artUrl;
        AlbumArtCache cache = AlbumArtCache.getInstance();
        Bitmap art = cache.getBigImage(artUrl);
        if (art == null) {
            art = description.getIconBitmap();
        }
        if (art != null) {
            // if we have the art cached or from the MediaDescription, use it:
            mBackgroundImage.setImageBitmap(art);
        } else {
            // otherwise, fetch a high res version and update:
            cache.fetch(artUrl, new AlbumArtCache.FetchListener() {
                @Override
                public void onFetched(String artUrl, Bitmap bitmap, Bitmap icon) {
                    // sanity check, in case a new fetch request has been done while
                    // the previous hasn't yet returned:
                    if (artUrl.equals(mCurrentArtUrl)) {
                        mBackgroundImage.setImageBitmap(bitmap);
                    }
                }
            });
        }
    }

    private void updateMediaDescription(MediaDescriptionCompat description) {
        if (description == null) {
            return;
        }
        LogHelper.d(TAG, "updateMediaDescription called ");
        mLine1.setText(description.getTitle());
        mLine2.setText(description.getSubtitle());
        fetchImageAsync(description);
    }

    private void updateDuration(MediaMetadataCompat metadata) {
        if (metadata == null) {
            return;
        }
        LogHelper.d(TAG, "updateDuration called ");
        int duration = (int) metadata.getLong(MediaMetadataCompat.METADATA_KEY_DURATION);
        mSeekbar.setMax(duration);
        mEnd.setText(DateUtils.formatElapsedTime(duration/1000));
    }

    private void updatePlaybackState(PlaybackStateCompat state) {
        if (state == null) {
            return;
        }
        mLastPlaybackState = state;
        MediaControllerCompat controllerCompat = MediaControllerCompat.getMediaController(FullScreenPlayerActivity.this);
        if (controllerCompat != null && controllerCompat.getExtras() != null) {
            String castName = controllerCompat.getExtras().getString(MusicService.EXTRA_CONNECTED_CAST);
            String line3Text = castName == null ? "" : getResources()
                        .getString(R.string.casting_to_device, castName);
            mLine3.setText(line3Text);
        }

        StateBundleHelper bundleHelper = new StateBundleHelper(state.getExtras());

        AudioMix audioMix = bundleHelper.getAudioMix();
        LogHelper.i(TAG, "Current audioMix is ", audioMix);
        Drawable drawable = mAudioMixDrawables.getOrDefault(audioMix, mAudioMixUnknownDrawable);
        LogHelper.i(TAG, "Setting mChangeMix background to ", drawable.toString());
        mChangeMix.setImageDrawable(drawable);

        Long bookmarkPosition = bundleHelper.getBookmarkPosition();
        if (mSeekbar.getMax() <= 0) {
            mSetBookmark.setImageAlpha(128);
            mBookmark.setVisibility(View.GONE);
        } else {
            if (bookmarkPosition == null) {
                mSetBookmark.setImageDrawable(mSetBookmarkDrawable);
                mBookmark.setVisibility(View.GONE);
            } else {
                mSetBookmark.setImageDrawable(mClearBookmarkDrawable);
                int width = mSeekbar.getWidth() - mSeekbar.getPaddingRight() - mSeekbar.getPaddingLeft();
                int pos = mSeekbar.getPaddingLeft() + (width * bookmarkPosition.intValue() / mSeekbar.getMax());
                mBookmark.setTranslationX(pos);
                mBookmark.setVisibility(VISIBLE);
            }
        }
/*
        Float speed = bundleHelper.getPlaybackSpeed();
        if (speed == null) { speed = 1f; }
        mChangeSpeed.setImageAlpha((int)(speed * 255));
*/
        switch (state.getState()) {
            case PlaybackStateCompat.STATE_PLAYING:
                mLoading.setVisibility(INVISIBLE);
                mPlayPause.setVisibility(VISIBLE);
                mPlayPause.setImageDrawable(mPauseDrawable);
                mControllers.setVisibility(VISIBLE);
                scheduleSeekbarUpdate();
                break;
            case PlaybackStateCompat.STATE_PAUSED:
                mControllers.setVisibility(VISIBLE);
                mLoading.setVisibility(INVISIBLE);
                mPlayPause.setVisibility(VISIBLE);
                mPlayPause.setImageDrawable(mPlayDrawable);
                stopSeekbarUpdate();
                break;
            case PlaybackStateCompat.STATE_NONE:
            case PlaybackStateCompat.STATE_STOPPED:
                mLoading.setVisibility(INVISIBLE);
                mPlayPause.setVisibility(VISIBLE);
                mPlayPause.setImageDrawable(mPlayDrawable);
                stopSeekbarUpdate();
                break;
            case PlaybackStateCompat.STATE_BUFFERING:
                mPlayPause.setVisibility(INVISIBLE);
                mLoading.setVisibility(VISIBLE);
                mLine3.setText(R.string.loading);
                stopSeekbarUpdate();
                break;
            default:
                LogHelper.d(TAG, "Unhandled state ", state.getState());
        }

        mSkipNext.setVisibility((state.getActions() & PlaybackStateCompat.ACTION_SKIP_TO_NEXT) == 0
            ? INVISIBLE : VISIBLE );
        mSkipPrev.setVisibility((state.getActions() & PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS) == 0
            ? INVISIBLE : VISIBLE );
    }

    private void updateProgress() {
        if (mLastPlaybackState == null) {
            return;
        }
        long currentPosition = mLastPlaybackState.getPosition();
        if (mLastPlaybackState.getState() == PlaybackStateCompat.STATE_PLAYING) {
            // Calculate the elapsed time between the last position update and now and unless
            // paused, we can assume (delta * speed) + current position is approximately the
            // latest position. This ensure that we do not repeatedly call the getPlaybackState()
            // on MediaControllerCompat.
            long timeDelta = SystemClock.elapsedRealtime() -
                    mLastPlaybackState.getLastPositionUpdateTime();
            currentPosition += (int) timeDelta * mLastPlaybackState.getPlaybackSpeed();
        }
        mSeekbar.setProgress((int) currentPosition);
    }
}
