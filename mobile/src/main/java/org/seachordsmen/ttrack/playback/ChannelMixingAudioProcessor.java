package org.seachordsmen.ttrack.playback;

import android.util.Log;

import com.example.android.uamp.utils.LogHelper;
import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.Format;
import com.google.android.exoplayer2.audio.AudioProcessor;

import org.seachordsmen.ttrack.model.AudioMix;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;

/**
 * Created by Gidon on 1/21/2018.
 */

class ChannelMixingAudioProcessor implements AudioProcessor {
    private static final String TAG = LogHelper.makeLogTag(ChannelMixingAudioProcessor.class);

    private int sampleRateHz;
    private int channelCount;
    @C.PcmEncoding
    private int encoding;
    private ByteBuffer buffer;
    private ByteBuffer outputBuffer;
    private boolean inputEnded;

    private AudioMix audioMix;

    public ChannelMixingAudioProcessor() {
        sampleRateHz = Format.NO_VALUE;
        channelCount = Format.NO_VALUE;
        encoding = C.ENCODING_INVALID;
        audioMix = AudioMix.UNCHANGED;
        outputBuffer = EMPTY_BUFFER;
    }

    @Override
    public boolean configure(int sampleRateHz, int channelCount, int encoding) throws UnhandledFormatException {
        if (encoding != C.ENCODING_PCM_16BIT) {
            throw new UnhandledFormatException(sampleRateHz, channelCount, encoding);
        }
        if (this.sampleRateHz == sampleRateHz && this.channelCount == channelCount && this.encoding == encoding) {
            return false;
        }
        this.buffer = EMPTY_BUFFER;
        this.sampleRateHz = sampleRateHz;
        this.channelCount = channelCount;
        this.encoding = encoding;
        Log.d(TAG, String.format("Configured for channelCount: %d, sampleRate: %d, encoding: %d.", channelCount, sampleRateHz, encoding));
        return true;
    }

    public AudioMix getAudioMix() {
        return audioMix;
    }

    public void setAudioMix(AudioMix audioMix) {
        this.audioMix = audioMix;
    }

    @Override
    public boolean isActive() {
        return encoding != C.ENCODING_INVALID;
    }

    @Override
    public int getOutputChannelCount() {
        return channelCount;
    }

    @Override
    public int getOutputEncoding() {
        return encoding;
    }

    @Override
    public void queueInput(ByteBuffer inputBuffer) {
        //Log.d(TAG, "New input buffer: " + inputBuffer.toString() + " " + asHex(inputBuffer, inputBuffer.position(), 16));
        int position = inputBuffer.position();
        int limit = inputBuffer.limit();
        int size = limit - position;
        if (buffer.capacity() < size) {
            buffer = ByteBuffer.allocateDirect(size).order(ByteOrder.nativeOrder());
        } else {
            buffer.clear();
        }
        mix(inputBuffer, buffer);
        inputBuffer.position(inputBuffer.limit());
        buffer.flip();
        outputBuffer = buffer;
        //Log.d(TAG, "Output buffer: " + outputBuffer.toString() + " " + asHex(outputBuffer, 0, 16));
    }

    private String asHex(ByteBuffer buf, int from, int numBytes) {
        int pos = buf.position();
        StringBuilder sb = new StringBuilder(numBytes * 2);
        for (int i = from; (i < from + numBytes) && (i < buf.limit()); ++i) {
            sb.append(String.format("%02x", buf.get(i)));
        }
        buf.position(pos);
        return sb.toString();
    }

    private void mix(ByteBuffer inputBuffer, ByteBuffer outputBuffer) {
        int position = inputBuffer.position();
        int limit = inputBuffer.limit();
        //Log.d(TAG, "Buffer before mix(): " + buffer.toString() + " " + asHex(buffer, 0, 16));
        for (int i = position; i < limit; i += 4) {
            short l = inputBuffer.getShort();
            short r = inputBuffer.getShort();
            short mixedL = (short) (audioMix.getLeftMix() * l + (1 - audioMix.getLeftMix()) * r);
            short mixedR = (short) (audioMix.getRightMix() * l + (1 - audioMix.getRightMix()) * r);
            outputBuffer.putShort(mixedL);
            outputBuffer.putShort(mixedR);
        }
        if (inputBuffer.position() != limit) {
            Log.e(TAG, "Unexpected position of input buffer: " + inputBuffer + "; expected " + limit);
        }
        //Log.d(TAG, "Buffer after mix(): " + buffer.toString() + " " + asHex(buffer, 0, 16));
    }

    @Override
    public void queueEndOfStream() {
        inputEnded = true;
    }

    @Override
    public ByteBuffer getOutput() {
        ByteBuffer output = outputBuffer;
        outputBuffer = EMPTY_BUFFER;
        return output;
    }

    @Override
    public boolean isEnded() {
        return inputEnded && outputBuffer == EMPTY_BUFFER;
    }

    @Override
    public void flush() {
        outputBuffer = EMPTY_BUFFER;
        inputEnded = false;
    }

    @Override
    public void reset() {
        flush();
        buffer = EMPTY_BUFFER;
        sampleRateHz = Format.NO_VALUE;
        channelCount = Format.NO_VALUE;
        encoding = C.ENCODING_INVALID;
    }
}
