package org.seachordsmen.ttrack.playback;

import android.content.Context;
import android.os.Handler;

import com.google.android.exoplayer2.DefaultRenderersFactory;
import com.google.android.exoplayer2.Renderer;
import com.google.android.exoplayer2.audio.AudioCapabilities;
import com.google.android.exoplayer2.audio.AudioProcessor;
import com.google.android.exoplayer2.audio.AudioRendererEventListener;
import com.google.android.exoplayer2.audio.MediaCodecAudioRenderer;
import com.google.android.exoplayer2.drm.DrmSessionManager;
import com.google.android.exoplayer2.drm.FrameworkMediaCrypto;
import com.google.android.exoplayer2.mediacodec.MediaCodecSelector;

import java.util.ArrayList;

/**
 * Created by Gidon on 1/21/2018.
 */

public class TTrackRendererFactory extends DefaultRenderersFactory {

    public TTrackRendererFactory(Context context) {
        super(context);
    }

    @Override
    protected void buildAudioRenderers(Context context, DrmSessionManager<FrameworkMediaCrypto> drmSessionManager, AudioProcessor[] audioProcessors,
                                       Handler eventHandler, AudioRendererEventListener eventListener, int extensionRendererMode,
                                       ArrayList<Renderer> out) {
        //super.buildAudioRenderers(context, drmSessionManager, audioProcessors, eventHandler, eventListener, extensionRendererMode, out);
        out.add(new TTrackAudioRenderer(MediaCodecSelector.DEFAULT, eventHandler, eventListener, AudioCapabilities.getCapabilities(context)));
    }

    @Override
    protected AudioProcessor[] buildAudioProcessors() {
        // TODO get parent processors and append to them
        AudioProcessor mixingProcessor = new ChannelMixingAudioProcessor();
        return new AudioProcessor[] { mixingProcessor };
    }
}
