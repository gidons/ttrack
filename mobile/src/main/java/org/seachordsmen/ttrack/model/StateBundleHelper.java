package org.seachordsmen.ttrack.model;

import android.os.Bundle;

/**
 * Created by gidon.shavit@gmail.com on 1/27/2018.
 */
public class StateBundleHelper {

    private final Bundle bundle;

    public StateBundleHelper() {
        this(new Bundle());
    }

    public StateBundleHelper(Bundle bundle) {
        this.bundle = bundle;
    }

    public Bundle getBundle() {
        return bundle;
    }

    public AudioMix getAudioMix() {
        Float leftMix = bundle.getFloat(TC.BUNDLE_EXTRA_LEFT_MIX, -1f);
        Float rightMix = bundle.getFloat(TC.BUNDLE_EXTRA_RIGHT_MIX, -1f);
        return (leftMix >= 0) && (rightMix >= 0) ? new AudioMix(leftMix, rightMix) : null;
    }

    public void setAudioMix(AudioMix audioMix) {
        if (audioMix != null) {
            bundle.putFloat(TC.BUNDLE_EXTRA_LEFT_MIX, audioMix.getLeftMix());
            bundle.putFloat(TC.BUNDLE_EXTRA_RIGHT_MIX, audioMix.getRightMix());
        }
    }

    public Long getBookmarkPosition() {
        long position = bundle.getLong(TC.BUNDLE_EXTRA_BOOKMARK_POSITION, -1L);
        return position >= 0 ? position : null;
    }

    public void setBookmarkPosition(Long position) {
        if (position != null) {
            bundle.putLong(TC.BUNDLE_EXTRA_BOOKMARK_POSITION, position);
        }
    }

    public Float getPlaybackSpeed() {
        float speed = bundle.getFloat(TC.BUNDLE_EXTRA_PLAYBACK_SPEED, -1f);
        return speed >= 0f ? speed : null;
    }

    public void setPlaybackSpeed(float speed) {
        bundle.putFloat(TC.BUNDLE_EXTRA_PLAYBACK_SPEED, speed);
    }
}
