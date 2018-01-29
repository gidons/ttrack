package org.seachordsmen.ttrack.playback;

import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.Format;
import com.google.android.exoplayer2.PlaybackParameters;
import com.google.android.exoplayer2.Renderer;
import com.google.android.exoplayer2.RendererCapabilities;
import com.google.android.exoplayer2.RendererConfiguration;
import com.google.android.exoplayer2.audio.AudioProcessor;
import com.google.android.exoplayer2.audio.MediaCodecAudioRenderer;
import com.google.android.exoplayer2.mediacodec.MediaCodecRenderer;
import com.google.android.exoplayer2.mediacodec.MediaCodecSelector;
import com.google.android.exoplayer2.source.SampleStream;
import com.google.android.exoplayer2.util.MediaClock;

import java.io.IOException;

/**
 * Created by Gidon on 1/21/2018.
 */

public class MediaCodecAudioRendererDecorator implements Renderer, MediaClock {

    private MediaCodecAudioRenderer delegate;

    public MediaCodecAudioRendererDecorator() {}

    public MediaCodecAudioRendererDecorator(MediaCodecAudioRenderer delegate) {
        this.delegate = delegate;
    }

    protected void setDelegate(MediaCodecAudioRenderer delegate) {
        if (this.delegate != null) {
            throw new IllegalStateException("Trying to set delegate but it has already been set.");
        }
        this.delegate = delegate;
    }

    @Override
    public void disable() {
        delegate.disable();
    }

    public long getPositionUs() {
        return delegate.getPositionUs();
    }

    public PlaybackParameters setPlaybackParameters(PlaybackParameters playbackParameters) {
        return delegate.setPlaybackParameters(playbackParameters);
    }

    public PlaybackParameters getPlaybackParameters() {
        return delegate.getPlaybackParameters();
    }

    public int supportsMixedMimeTypeAdaptation() {
        return delegate.supportsMixedMimeTypeAdaptation();
    }

    public int supportsFormat(Format format) throws ExoPlaybackException {
        return delegate.supportsFormat(format);
    }

    @Override
    public void render(long positionUs, long elapsedRealtimeUs) throws ExoPlaybackException {
        delegate.render(positionUs, elapsedRealtimeUs);
    }

    @Override
    public boolean isReady() {
        return delegate.isReady();
    }

    @Override
    public boolean isEnded() {
        return delegate.isEnded();
    }

    @Override
    public int getTrackType() {
        return delegate.getTrackType();
    }

    @Override
    public RendererCapabilities getCapabilities() {
        return delegate.getCapabilities();
    }

    @Override
    public void setIndex(int index) {
        delegate.setIndex(index);
    }

    @Override
    public MediaClock getMediaClock() {
        return this;
    }

    @Override
    public int getState() {
        return delegate.getState();
    }

    @Override
    public void enable(RendererConfiguration configuration, Format[] formats, SampleStream stream, long positionUs, boolean joining, long offsetUs) throws ExoPlaybackException {
        delegate.enable(configuration, formats, stream, positionUs, joining, offsetUs);
    }

    @Override
    public void start() throws ExoPlaybackException {
        delegate.start();
    }

    @Override
    public void replaceStream(Format[] formats, SampleStream stream, long offsetUs) throws ExoPlaybackException {
        delegate.replaceStream(formats, stream, offsetUs);
    }

    @Override
    public SampleStream getStream() {
        return delegate.getStream();
    }

    @Override
    public boolean hasReadStreamToEnd() {
        return delegate.hasReadStreamToEnd();
    }

    @Override
    public void setCurrentStreamFinal() {
        delegate.setCurrentStreamFinal();
    }

    @Override
    public boolean isCurrentStreamFinal() {
        return delegate.isCurrentStreamFinal();
    }

    @Override
    public void maybeThrowStreamError() throws IOException {
        delegate.maybeThrowStreamError();
    }

    @Override
    public void resetPosition(long positionUs) throws ExoPlaybackException {
        delegate.resetPosition(positionUs);
    }

    @Override
    public void stop() throws ExoPlaybackException {
        delegate.stop();
    }

    @Override
    public void handleMessage(int messageType, Object message) throws ExoPlaybackException {
        delegate.handleMessage(messageType, message);
    }
}
