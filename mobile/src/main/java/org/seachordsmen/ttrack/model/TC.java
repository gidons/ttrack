package org.seachordsmen.ttrack.model;

import com.google.android.exoplayer2.C;

/**
 * Created by gidon.shavit@gmail.com on 1/27/2018.
 */

public interface TC {
    String BUNDLE_EXTRA_LEFT_MIX = "org.seachordsmen.ttrack.extra.LEFT_MIX";
    String BUNDLE_EXTRA_RIGHT_MIX = "org.seachordsmen.ttrack.extra.RIGHT_MIX";
    String BUNDLE_EXTRA_BOOKMARK_POSITION = "org.seachordsmen.ttrack.extra.BOOKMARK_POSITION";
    String BUNDLE_EXTRA_PLAYBACK_SPEED = "org.seachordsmen.ttrack.extra.PLAYBACK_SPEED";

    int MSG_SET_MIX = C.MSG_CUSTOM_BASE + 1;
    int MSG_SET_BOOKMARK = C.MSG_CUSTOM_BASE + 2;

    // Action to thumbs up a media item
    String CUSTOM_ACTION_THUMBS_UP = "com.example.android.uamp.THUMBS_UP";
    // Action to switch audio mix between full and original
    String CUSTOM_ACTION_SWITCH_AUDIO_MIX = "com.example.android.uamp.SWITCH_AUDIO_MIX";
    // Action to set a bookmark at the current play position
    String CUSTOM_ACTION_SET_BOOKMARK = "com.example.android.uamp.SET_BOOKMARK";
    String CUSTOM_ACTION_CHANGE_SPEED = "com.example.android.uamp.CHANGE_SPEED";
}
