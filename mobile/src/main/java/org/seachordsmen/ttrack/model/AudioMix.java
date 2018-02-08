package org.seachordsmen.ttrack.model;

import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;

import lombok.Value;

/**
 * Created by Gidon on 1/21/2018.
 */
@Value
public class AudioMix {
    public static final AudioMix FULL_MIX = new AudioMix(0.5f, 0.5f);
    public static final AudioMix ALL_LEFT = new AudioMix(1f, 1f);
    public static final AudioMix ALL_RIGHT = new AudioMix(0f, 0f);
    public static final AudioMix LEFT_PREDOM = new AudioMix(0.75f, 0.75f);
    public static final AudioMix RIGHT_PREDOM = new AudioMix(0.25f, 0.25f);
    public static final AudioMix UNCHANGED = new AudioMix(1f, 0f);
    public static final AudioMix REVERSED = new AudioMix(0f, 1f);

    private final float leftMix;
    private final float rightMix;

    @Override
    public String toString() {
        return String.format("L%.2f/R%.2f", leftMix, rightMix);
    }
}
