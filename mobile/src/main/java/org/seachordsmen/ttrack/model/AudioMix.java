package org.seachordsmen.ttrack.model;

import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by Gidon on 1/21/2018.
 */
public class AudioMix {
    public static final AudioMix FULL_MIX = new AudioMix(0.5f, 0.5f);
    public static final AudioMix ALL_LEFT = new AudioMix(1f, 1f);
    public static final AudioMix ALL_RIGHT = new AudioMix(0f, 0f);
    public static final AudioMix LEFT_PREDOM = new AudioMix(0.75f, 0.75f);
    public static final AudioMix RIGHT_PREDOM = new AudioMix(0.25f, 0.25f);
    public static final AudioMix UNCHANGED = new AudioMix(1f, 0f);
    public static final AudioMix REVERSED = new AudioMix(0f, 1f);

    public final float leftMix;
    public final float rightMix;

    public AudioMix(float leftMix, float rightMix) {
        this.leftMix = leftMix;
        this.rightMix = rightMix;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AudioMix)) return false;

        AudioMix audioMix = (AudioMix) o;

        if (Float.compare(audioMix.leftMix, leftMix) != 0) return false;
        return Float.compare(audioMix.rightMix, rightMix) == 0;
    }

    @Override
    public int hashCode() {
        int result = (leftMix != +0.0f ? Float.floatToIntBits(leftMix) : 0);
        result = 31 * result + (rightMix != +0.0f ? Float.floatToIntBits(rightMix) : 0);
        return result;
    }

    @Override
    public String toString() {
        return String.format("L%.2f/R%.2f", leftMix, rightMix);
    }
}
