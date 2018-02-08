package org.seachordsmen.ttrack.utils;

import android.database.Cursor;

import java.util.Iterator;
import java.util.function.Function;

/**
 * Created by gidon.shavit@gmail.com on 1/31/2018.
 */

public class CursorEntityIterator<T> implements Iterator<T> {

    final private Cursor cursor;
    final private Function<Cursor, T> entityBuilder;

    public CursorEntityIterator(Cursor cursor, Function<Cursor, T> entityBuilder) {
        this.cursor = cursor;
        this.entityBuilder = entityBuilder;
    }

    @Override
    public boolean hasNext() {
        return !cursor.isLast() && !cursor.isAfterLast();
    }

    @Override
    public T next() {
        if (cursor.isBeforeFirst()) {
            cursor.moveToFirst();
        } else {
            cursor.moveToNext();
        }

        return entityBuilder.apply(cursor);
    }
}
