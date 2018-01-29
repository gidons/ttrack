package org.seachordsmen.ttrack.playback;

import android.os.Handler;
import android.util.Log;

import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.audio.AudioCapabilities;
import com.google.android.exoplayer2.audio.AudioRendererEventListener;
import com.google.android.exoplayer2.audio.MediaCodecAudioRenderer;
import com.google.android.exoplayer2.mediacodec.MediaCodecSelector;

import org.seachordsmen.ttrack.model.AudioMix;
import org.seachordsmen.ttrack.model.TC;

/**
 * Created by Gidon on 1/21/2018.
 */

public class TTrackAudioRenderer extends MediaCodecAudioRendererDecorator {

    private static final String TAG = "ttrack." + TTrackAudioRenderer.class.getSimpleName();

    private ChannelMixingAudioProcessor mixingProcessor = new ChannelMixingAudioProcessor();

    public TTrackAudioRenderer() {
        this(MediaCodecSelector.DEFAULT);
    }

    public TTrackAudioRenderer(MediaCodecSelector mediaCodecSelector) {
        setDelegate(new MediaCodecAudioRenderer(mediaCodecSelector, null, true, null, null, null));//, mixingProcessor));
    }
    public TTrackAudioRenderer(MediaCodecSelector mediaCodecSelector, Handler eventHandler, AudioRendererEventListener eventListener, AudioCapabilities audioCapabilities) {
        setDelegate(new MediaCodecAudioRenderer(mediaCodecSelector, null, true, eventHandler, eventListener, audioCapabilities, mixingProcessor));
        mixingProcessor.setAudioMix(AudioMix.FULL_MIX);
    }

    @Override
    public void handleMessage(int messageType, Object message) throws ExoPlaybackException {
        switch(messageType) {
            case TC.MSG_SET_MIX:
                Log.d(TAG, "Audio mix changed");
                mixingProcessor.setAudioMix((AudioMix) message);
                break;
            case TC.MSG_SET_BOOKMARK:
                Log.d(TAG, "Bookmark set");
                break;
            default:
                super.handleMessage(messageType, message);
        }
    }
}
