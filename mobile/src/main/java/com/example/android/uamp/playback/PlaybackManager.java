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

package com.example.android.uamp.playback;

import android.content.res.Resources;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.Log;

import com.example.android.uamp.R;
import com.example.android.uamp.model.MusicProvider;
import com.example.android.uamp.utils.LogHelper;
import com.example.android.uamp.utils.MediaIDHelper;
import com.example.android.uamp.utils.WearHelper;
import com.google.android.exoplayer2.PlaybackParameters;

import org.seachordsmen.ttrack.model.AudioMix;
import org.seachordsmen.ttrack.model.StateBundleHelper;
import org.seachordsmen.ttrack.model.TC;

/**
 * Manage the interactions among the container service, the queue manager and the actual playback.
 */
public class PlaybackManager implements Playback.Callback {

    private static final String TAG = LogHelper.makeLogTag(PlaybackManager.class);
    public static final int SKIP_BACK_GRACE_PERIOD_MS = 1000; // one second

    private MusicProvider mMusicProvider;
    private QueueManager mQueueManager;
    private Resources mResources;
    private Playback mPlayback;
    private PlaybackServiceCallback mServiceCallback;
    private MediaSessionCallback mMediaSessionCallback;

    public PlaybackManager(PlaybackServiceCallback serviceCallback, Resources resources,
                           MusicProvider musicProvider, QueueManager queueManager,
                           Playback playback) {
        mMusicProvider = musicProvider;
        mServiceCallback = serviceCallback;
        mResources = resources;
        mQueueManager = queueManager;
        mMediaSessionCallback = new MediaSessionCallback();
        mPlayback = playback;
        mPlayback.setCallback(this);
    }

    public Playback getPlayback() {
        return mPlayback;
    }

    public MediaSessionCompat.Callback getMediaSessionCallback() {
        return mMediaSessionCallback;
    }

    /**
     * Handle a request to play music
     */
    public void handlePlayRequest() {
        LogHelper.d(TAG, "handlePlayRequest: mState=" + mPlayback.getState());
        MediaSessionCompat.QueueItem currentMusic = mQueueManager.getCurrentMusic();
        if (currentMusic != null) {
            mServiceCallback.onPlaybackStart();
            mPlayback.play(currentMusic);
        }
    }

    /**
     * Handle a request to pause music
     */
    public void handlePauseRequest() {
        LogHelper.d(TAG, "handlePauseRequest: mState=" + mPlayback.getState());
        if (mPlayback.isPlaying()) {
            mPlayback.pause();
            mServiceCallback.onPlaybackStop();
        }
    }

    /**
     * Handle a request to stop music
     *
     * @param withError Error message in case the stop has an unexpected cause. The error
     *                  message will be set in the PlaybackState and will be visible to
     *                  MediaController clients.
     */
    public void handleStopRequest(String withError) {
        LogHelper.d(TAG, "handleStopRequest: mState=" + mPlayback.getState() + " error=", withError);
        mPlayback.stop(true);
        mServiceCallback.onPlaybackStop();
        updatePlaybackState(withError);
    }


    public void handleAudioMixRequest(AudioMix requestedMix) {
        LogHelper.d(TAG, "handleAudioMixRequest: mState=", mPlayback.getState(), " requestedMix=", requestedMix);
        final AudioMix newMix;
        if (requestedMix == null) {
            AudioMix audioMix = mPlayback.getCurrentAudioMix();
            if (AudioMix.ALL_RIGHT.equals(audioMix)) {
                newMix = AudioMix.ALL_LEFT;
            } else {
                newMix = AudioMix.ALL_RIGHT;
            }
        } else {
            newMix = requestedMix;
        }
        LogHelper.i(TAG, "setting audioMix to ", newMix);
        mPlayback.setAudioMix(newMix);
        updatePlaybackState(null);
    }

    public void handleSetBookmarkRequest() {
        Long currentBookmarkPos = mPlayback.getCurrentBookmarkPosition();
        Long newPos = currentBookmarkPos == null ? mPlayback.getCurrentStreamPosition() : null;
        LogHelper.i(TAG, "setting bookmark to ", newPos);
        mPlayback.setBookmarkPosition(newPos);
        updatePlaybackState(null);
    }

    public void handleChangeSpeedRequest(Float requestedSpeed) {
        PlaybackParameters currentParams = mPlayback.getPlaybackParameters();
        if (currentParams == null) {
            currentParams = PlaybackParameters.DEFAULT;
        }
        final float newSpeed;
        if (requestedSpeed != null) {
            newSpeed = requestedSpeed;
        } else {
            if (currentParams.speed > 0.75f) {
                newSpeed = 0.75f;
            } else {
                newSpeed = 1.0f;
            }
        }
        LogHelper.i(TAG, "setting speed to ", newSpeed);
        mPlayback.setPlaybackParameters(new PlaybackParameters(newSpeed, currentParams.pitch));
        updatePlaybackState(null);
    }

    /**
     * Update the current media player state, optionally showing an error message.
     *
     * @param error if not null, error message to present to the user.
     */
    public void updatePlaybackState(String error) {
        LogHelper.d(TAG, "updatePlaybackState, playback state=" + mPlayback.getState());
        long position = PlaybackStateCompat.PLAYBACK_POSITION_UNKNOWN;
        if (mPlayback != null && mPlayback.isConnected()) {
            position = mPlayback.getCurrentStreamPosition();
        }

        //noinspection ResourceType
        PlaybackStateCompat.Builder stateBuilder = new PlaybackStateCompat.Builder()
                .setActions(getAvailableActions());

        addFavoriteAction(stateBuilder);
        addTTrackActions(stateBuilder);
        int state = mPlayback.getState();

        // If there is an error message, sendprivatre  it to the playback state:
        if (error != null) {
            // Error states are really only supposed to be used for errors that cause playback to
            // stop unexpectedly and persist until the user takes action to fix it.
            stateBuilder.setErrorMessage(error);
            state = PlaybackStateCompat.STATE_ERROR;
        }
        //noinspection ResourceType
        stateBuilder.setState(state, position, 1.0f, SystemClock.elapsedRealtime());

        // Set the activeQueueItemId if the current index is valid.
        MediaSessionCompat.QueueItem currentMusic = mQueueManager.getCurrentMusic();
        if (currentMusic != null) {
            stateBuilder.setActiveQueueItemId(currentMusic.getQueueId());
        }

        StateBundleHelper bundleHelper = new StateBundleHelper();
        bundleHelper.setAudioMix(mPlayback.getCurrentAudioMix());
        bundleHelper.setBookmarkPosition(mPlayback.getCurrentBookmarkPosition());
        stateBuilder.setExtras(bundleHelper.getBundle());

        mServiceCallback.onPlaybackStateUpdated(stateBuilder.build());

        if (state == PlaybackStateCompat.STATE_PLAYING ||
                state == PlaybackStateCompat.STATE_PAUSED) {
            mServiceCallback.onNotificationRequired();
        }
    }

    private void addFavoriteAction(PlaybackStateCompat.Builder stateBuilder) {
        MediaSessionCompat.QueueItem currentMusic = mQueueManager.getCurrentMusic();
        if (currentMusic == null) {
            return;
        }
        // Set appropriate "Favorite" icon on Custom action:
        String mediaId = currentMusic.getDescription().getMediaId();
        if (mediaId == null) {
            return;
        }
        String musicId = MediaIDHelper.extractMusicIDFromMediaID(mediaId);
        int favoriteIcon = mMusicProvider.isFavorite(musicId) ?
                R.drawable.ic_star_on : R.drawable.ic_star_off;
        LogHelper.d(TAG, "updatePlaybackState, setting Favorite custom action of music ",
                musicId, " current favorite=", mMusicProvider.isFavorite(musicId));
        Bundle customActionExtras = new Bundle();
        WearHelper.setShowCustomActionOnWear(customActionExtras, true);
        stateBuilder.addCustomAction(new PlaybackStateCompat.CustomAction.Builder(
                TC.CUSTOM_ACTION_THUMBS_UP, mResources.getString(R.string.favorite), favoriteIcon)
                .setExtras(customActionExtras)
                .build());
    }

    private void addTTrackActions(PlaybackStateCompat.Builder stateBuilder) {
        stateBuilder.addCustomAction(new PlaybackStateCompat.CustomAction.Builder(
                TC.CUSTOM_ACTION_SWITCH_AUDIO_MIX, mResources.getString(R.string.audioMix), android.R.drawable.btn_minus)
                .build());
        stateBuilder.addCustomAction(new PlaybackStateCompat.CustomAction.Builder(
                TC.CUSTOM_ACTION_SET_BOOKMARK, mResources.getString(R.string.setBookmark), android.R.drawable.arrow_down_float)
                .build());
        stateBuilder.addCustomAction(new PlaybackStateCompat.CustomAction.Builder(
                TC.CUSTOM_ACTION_CHANGE_SPEED, mResources.getString(R.string.changeSpeed), android.R.drawable.arrow_down_float)
                .build());
    }

    private long getAvailableActions() {
        long actions =
                PlaybackStateCompat.ACTION_PLAY_PAUSE |
                PlaybackStateCompat.ACTION_PLAY_FROM_MEDIA_ID |
                PlaybackStateCompat.ACTION_PLAY_FROM_SEARCH |
                PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS |
                PlaybackStateCompat.ACTION_SKIP_TO_NEXT;
        if (mPlayback.isPlaying()) {
            actions |= PlaybackStateCompat.ACTION_PAUSE;
        } else {
            actions |= PlaybackStateCompat.ACTION_PLAY;
        }
        return actions;
    }

    /**
     * Implementation of the Playback.Callback interface
     */
    @Override
    public void onCompletion() {
        // The media player finished playing the current song, so we go ahead
        // and start the next.
        if (mQueueManager.skipQueuePosition(1)) {
            handlePlayRequest();
            mQueueManager.updateMetadata();
        } else {
            // If skipping was not possible, we stop and release the resources:
            handleStopRequest(null);
        }
    }

    @Override
    public void onPlaybackStatusChanged(int state) {
        updatePlaybackState(null);
    }

    @Override
    public void onError(String error) {
        updatePlaybackState(error);
    }

    @Override
    public void setCurrentMediaId(String mediaId) {
        LogHelper.d(TAG, "setCurrentMediaId", mediaId);
        mQueueManager.setQueueFromMusic(mediaId);
    }


    /**
     * Switch to a different Playback instance, maintaining all playback state, if possible.
     *
     * @param playback switch to this playback
     */
    public void switchToPlayback(Playback playback, boolean resumePlaying) {
        if (playback == null) {
            throw new IllegalArgumentException("Playback cannot be null");
        }
        // Suspends current state.
        int oldState = mPlayback.getState();
        long pos = mPlayback.getCurrentStreamPosition();
        String currentMediaId = mPlayback.getCurrentMediaId();
        mPlayback.stop(false);
        playback.setCallback(this);
        playback.setCurrentMediaId(currentMediaId);
        playback.seekTo(pos < 0 ? 0 : pos);
        playback.start();
        // Swaps instance.
        mPlayback = playback;
        switch (oldState) {
            case PlaybackStateCompat.STATE_BUFFERING:
            case PlaybackStateCompat.STATE_CONNECTING:
            case PlaybackStateCompat.STATE_PAUSED:
                mPlayback.pause();
                break;
            case PlaybackStateCompat.STATE_PLAYING:
                MediaSessionCompat.QueueItem currentMusic = mQueueManager.getCurrentMusic();
                if (resumePlaying && currentMusic != null) {
                    mPlayback.play(currentMusic);
                } else if (!resumePlaying) {
                    mPlayback.pause();
                } else {
                    mPlayback.stop(true);
                }
                break;
            case PlaybackStateCompat.STATE_NONE:
                break;
            default:
                LogHelper.d(TAG, "Default called. Old state is ", oldState);
        }
    }


    private class MediaSessionCallback extends MediaSessionCompat.Callback {
        @Override
        public void onPlay() {
            LogHelper.d(TAG, "play");
            if (mQueueManager.getCurrentMusic() == null) {
                mQueueManager.setRandomQueue();
            }
            handlePlayRequest();
        }

        @Override
        public void onSkipToQueueItem(long queueId) {
            LogHelper.d(TAG, "OnSkipToQueueItem:" + queueId);
            mQueueManager.setCurrentQueueItem(queueId);
            mQueueManager.updateMetadata();
        }

        @Override
        public void onSeekTo(long position) {
            LogHelper.d(TAG, "onSeekTo:", position);
            mPlayback.seekTo((int) position);
        }

        @Override
        public void onPlayFromMediaId(String mediaId, Bundle extras) {
            LogHelper.d(TAG, "playFromMediaId mediaId:", mediaId, "  extras=", extras);
            mQueueManager.setQueueFromMusic(mediaId);
            handlePlayRequest();
        }

        @Override
        public void onPause() {
            LogHelper.d(TAG, "pause. current state=" + mPlayback.getState());
            handlePauseRequest();
        }

        @Override
        public void onStop() {
            LogHelper.d(TAG, "stop. current state=" + mPlayback.getState());
            handleStopRequest(null);
        }

        @Override
        public void onSkipToNext() {
            LogHelper.d(TAG, "skipToNext");
            if (mQueueManager.skipQueuePosition(1)) {
                handlePlayRequest();
            } else {
                handleStopRequest("Cannot skip");
            }
            mQueueManager.updateMetadata();
        }

        @Override
        public void onSkipToPrevious() {
            Long bookmarkPosition = mPlayback.getCurrentBookmarkPosition();
            long streamPosition = mPlayback.getCurrentStreamPosition();
            if (bookmarkPosition != null && streamPosition > bookmarkPosition + SKIP_BACK_GRACE_PERIOD_MS) {
                // we're at least (grace-period) ahead of the bookmark, so skip back to the bookmark
                mPlayback.seekTo(bookmarkPosition);
            } else if (streamPosition > SKIP_BACK_GRACE_PERIOD_MS) {
                // we're at least (grace-period) into the track; skip back to beginning
                mPlayback.seekTo(0);
            } else {
                if (mQueueManager.skipQueuePosition(-1)) {
                    handlePlayRequest();
                } else {
                    handleStopRequest("Cannot skip");
                }
                mQueueManager.updateMetadata();
            }
        }

        @Override
        public void onCustomAction(@NonNull String action, Bundle extras) {
            StateBundleHelper bundleHelper = new StateBundleHelper(extras);
            switch(action) {
                case TC.CUSTOM_ACTION_THUMBS_UP: onThumbsUp(); break;
                case TC.CUSTOM_ACTION_SWITCH_AUDIO_MIX: onSetAudioMix(bundleHelper); break;
                case TC.CUSTOM_ACTION_SET_BOOKMARK: onSetBookmark(bundleHelper); break;
                case TC.CUSTOM_ACTION_CHANGE_SPEED: onChangeSpeed(bundleHelper); break;
                default: LogHelper.e(TAG, "Unsupported action: ", action);
            }
        }

        private void onSetAudioMix(StateBundleHelper bundleHelper) {
            Log.i(TAG, "onSetAudioMix called");
            handleAudioMixRequest(bundleHelper.getAudioMix());
        }

        private void onSetBookmark(StateBundleHelper bundleHelper) {
            Log.i(TAG, "onSetBookmark called");
            handleSetBookmarkRequest();
        }

        private void onChangeSpeed(StateBundleHelper bundleHelper) {
            Log.i(TAG, "onChangeSpeed called");
            handleChangeSpeedRequest(bundleHelper.getPlaybackSpeed());
        }

        private void onThumbsUp() {
            LogHelper.i(TAG, "onCustomAction: favorite for current track");
            MediaSessionCompat.QueueItem currentMusic = mQueueManager.getCurrentMusic();
            if (currentMusic != null) {
                String mediaId = currentMusic.getDescription().getMediaId();
                if (mediaId != null) {
                    String musicId = MediaIDHelper.extractMusicIDFromMediaID(mediaId);
                    mMusicProvider.setFavorite(musicId, !mMusicProvider.isFavorite(musicId));
                }
            }
            // playback state needs to be updated because the "Favorite" icon on the
            // custom action will change to reflect the new favorite state.
            updatePlaybackState(null);
        }

        
        /**
         * Handle free and contextual searches.
         * <p/>
         * All voice searches on Android Auto are sent to this method through a connected
         * {@link android.support.v4.media.session.MediaControllerCompat}.
         * <p/>
         * Threads and async handling:
         * Search, as a potentially slow operation, should run in another thread.
         * <p/>
         * Since this method runs on the main thread, most apps with non-trivial metadata
         * should defer the actual search to another thread (for example, by using
         * an {@link AsyncTask} as we do here).
         **/
        @Override
        public void onPlayFromSearch(final String query, final Bundle extras) {
            LogHelper.d(TAG, "playFromSearch  query=", query, " extras=", extras);

            mPlayback.setState(PlaybackStateCompat.STATE_CONNECTING);
            mMusicProvider.retrieveMediaAsync(new MusicProvider.Callback() {
                @Override
                public void onMusicCatalogReady(boolean success) {
                    if (!success) {
                        updatePlaybackState("Could not load catalog");
                    }

                    boolean successSearch = mQueueManager.setQueueFromSearch(query, extras);
                    if (successSearch) {
                        handlePlayRequest();
                        mQueueManager.updateMetadata();
                    } else {
                        updatePlaybackState("Could not find music");
                    }
                }
            });
        }
    }


    public interface PlaybackServiceCallback {
        void onPlaybackStart();

        void onNotificationRequired();

        void onPlaybackStop();

        void onPlaybackStateUpdated(PlaybackStateCompat newState);
    }
}
