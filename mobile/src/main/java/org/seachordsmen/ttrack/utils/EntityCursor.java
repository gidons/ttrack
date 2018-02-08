package org.seachordsmen.ttrack.utils;

import android.database.Cursor;

import java.io.Closeable;
import java.io.IOException;
import java.util.Iterator;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Function;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import lombok.Value;

/**
 * Created by gidon.shavit@gmail.com on 2/2/2018.
 */

@Value
public class EntityCursor<T> implements Iterable<T>, Closeable {
    Cursor cursor;
    Function<Cursor, T> creator;

    @Override
    public Iterator<T> iterator() {
        return new Iterator<T>() {
            @Override
            public boolean hasNext() {
                return !cursor.isLast() && !cursor.isAfterLast();
            }

            @Override
            public T next() {
                cursor.moveToNext();
                return creator.apply(cursor);
            }
        };
    }

    public Stream<T> stream() {
        return StreamSupport.stream(this.spliterator(), false);
    }

    @Override
    public void close() {
        if (!cursor.isClosed()) {
            cursor.close();
        }
    }
}
