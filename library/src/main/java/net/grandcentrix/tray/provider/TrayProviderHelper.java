/*
 * Copyright (C) 2015 grandcentrix GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.grandcentrix.tray.provider;

import net.grandcentrix.tray.accessor.TrayPreference;
import net.grandcentrix.tray.util.ProviderHelper;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by pascalwelsch on 11/20/14.
 */
public class TrayProviderHelper {

    final Context mContext;

    private final Uri mContentUri;

    public TrayProviderHelper(@NonNull final Context context) {
        mContext = context;
        mContentUri = TrayContract.generateContentUri(context);
    }

    /**
     * clears <b>all</b> Preferences saved. Module independent. Erases everything
     */
    public void clear() {
        mContext.getContentResolver().delete(mContentUri, null, null);
    }

    /**
     * clears the stated modules
     */
    public void clear(TrayPreference... modules) {
        if (modules == null) {
            return;
        }

        for (TrayPreference module : modules) {
            if (module == null) {
                continue;
            }
            module.clear();
        }

    }

    /**
     * clears <b>all</b> Preferences saved but the modules stated.
     *
     * @param modules modules excluded when deleting preferences
     */
    public void clearBut(TrayPreference... modules) {
        if (modules == null) {
            clear();
            return;
        }

        String selection = null;
        String[] selectionArgs = new String[]{};

        for (final TrayPreference module : modules) {
            if (module == null) {
                continue;
            }
            String moduleName = module.getModularizedStorage().getModule();
            selection = ProviderHelper
                    .extendSelection(selection, TrayContract.Preferences.Columns.MODULE + " != ?");
            selectionArgs = ProviderHelper
                    .extendSelectionArgs(selectionArgs, new String[]{moduleName});
        }

        mContext.getContentResolver().delete(mContentUri, selection, selectionArgs);
    }

    /**
     * Builds a list of all Preferences saved.
     *
     * @return all Preferences as list.
     */
    public List<TrayItem> getAll() {
        return queryProvider(mContentUri);
    }

    public Uri getContentUri() {
        return mContentUri;
    }

    public Uri getUri() {
        return getUri(null, null);
    }

    public Uri getUri(final String module) {
        return getUri(module, null);
    }

    public Uri getUri(@Nullable final String module, @Nullable final String key) {
        if (module == null && key != null) {
            throw new IllegalArgumentException(
                    "key without module is not valid. Look into the TryProvider for valid Uris");
        }
        final Uri.Builder builder = mContentUri
                .buildUpon();
        if (module != null) {
            builder.appendPath(module);
        }
        if (key != null) {
            builder.appendPath(key);
        }
        return builder.build();
    }

    /**
     * saves the value into the database.
     */
    public void persist(@NonNull final String module, @NonNull final String key,
            @NonNull final String value) {
        //noinspection ConstantConditions
        if (value == null) {
            return;
        }

        final Uri uri = mContentUri
                .buildUpon()
                .appendPath(module)
                .appendPath(key)
                .build();
        ContentValues values = new ContentValues();
        values.put(TrayContract.Preferences.Columns.VALUE, value);
        mContext.getContentResolver().insert(uri, values);
    }

    public List<TrayItem> queryProvider(@NonNull final Uri uri)
            throws IllegalStateException {
        final Cursor cursor = mContext.getContentResolver().query(uri, null, null, null, null);

        // Return Preference if found
        if (cursor == null) {
            throw new IllegalStateException(
                    "could not access stored data with uri " + uri
                            + ". Is the provider registered in the manifest of your application?");
        }
        final ArrayList<TrayItem> list = new ArrayList<>();
        for (boolean hasItem = cursor.moveToFirst(); hasItem;
                hasItem = cursor.moveToNext()) {
            list.add(new TrayItem(cursor));
        }
        cursor.close();
        return list;
    }
}