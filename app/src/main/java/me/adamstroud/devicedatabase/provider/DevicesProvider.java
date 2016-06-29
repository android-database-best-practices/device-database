/*
 * Copyright 2016 Adam Stroud
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package me.adamstroud.devicedatabase.provider;

import android.content.ContentProvider;
import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.OperationApplicationException;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.provider.BaseColumns;
import android.support.annotation.NonNull;
import android.util.SparseArray;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Provides access to Device data.
 *
 * @see DevicesContract
 *
 * @author Adam Stroud &#60;<a href="mailto:adam.stroud@gmail.com">adam.stroud@gmail.com</a>&#62;
 */
public class DevicesProvider extends ContentProvider {
    private static final String TAG =
            DevicesProvider.class.getSimpleName();

    private static final int CODE_ALL_DEVICES = 100;
    private static final int CODE_DEVICE_ID = 101;
    private static final int CODE_ALL_MANUFACTURERS = 102;
    private static final int CODE_MANUFACTURER_ID = 103;
    private static final int CODE_DEVICE_MANUFACTURER = 104;

    private static final SparseArray<String> URI_CODE_TABLE_MAP =
            new SparseArray<>();

    private static final UriMatcher URI_MATCHER =
            new UriMatcher(UriMatcher.NO_MATCH);

    static {
        URI_CODE_TABLE_MAP.put(CODE_ALL_DEVICES,
                DevicesOpenHelper.Tables.DEVICE);

        URI_CODE_TABLE_MAP.put(CODE_DEVICE_ID,
                DevicesOpenHelper.Tables.DEVICE);

        URI_CODE_TABLE_MAP.put(CODE_ALL_MANUFACTURERS,
                DevicesOpenHelper.Tables.MANUFACTURER);

        URI_CODE_TABLE_MAP.put(CODE_MANUFACTURER_ID,
                DevicesOpenHelper.Tables.MANUFACTURER);

        URI_MATCHER.addURI(DevicesContract.AUTHORITY,
                DevicesContract.Device.PATH,
                CODE_ALL_DEVICES);

        URI_MATCHER.addURI(DevicesContract.AUTHORITY,
                DevicesContract.Device.PATH + "/#",
                CODE_DEVICE_ID);

        URI_MATCHER.addURI(DevicesContract.AUTHORITY,
                DevicesContract.Manufacturer.PATH,
                CODE_ALL_MANUFACTURERS);

        URI_MATCHER.addURI(DevicesContract.AUTHORITY,
                DevicesContract.Manufacturer.PATH + "/#",
                CODE_MANUFACTURER_ID);

        URI_MATCHER.addURI(DevicesContract.AUTHORITY,
                DevicesContract.DeviceManufacturer.PATH,
                CODE_DEVICE_MANUFACTURER);
    }

    private DevicesOpenHelper helper;

    public DevicesProvider() {
        // no-op
    }

    @Override
    public boolean onCreate() {
        helper = DevicesOpenHelper.getInstance(getContext());
        return true;
    }

    @Override
    public Uri insert(@NonNull Uri uri, ContentValues values) {
        long id;
        final int code = URI_MATCHER.match(uri);
        switch (code) {
            case CODE_ALL_DEVICES:
            case CODE_ALL_MANUFACTURERS:
                id = helper
                        .getWritableDatabase()
                        .insertOrThrow(URI_CODE_TABLE_MAP.get(code),
                                null,
                                values);
                break;
            default:
                throw new IllegalArgumentException("Invalid Uri: " + uri);
        }

        notifyUris(uri);
        return ContentUris.withAppendedId(uri, id);
    }

    @Override
    public int delete(@NonNull Uri uri,
                      String selection,
                      String[] selectionArgs) {
        int rowCount;

        final int code = URI_MATCHER.match(uri);
        switch (code) {
            case CODE_ALL_DEVICES:
            case CODE_ALL_MANUFACTURERS:
                rowCount = helper
                        .getWritableDatabase()
                        .delete(URI_CODE_TABLE_MAP.get(code),
                                selection,
                                selectionArgs);
                break;
            case CODE_DEVICE_ID:
            case CODE_MANUFACTURER_ID:
                if (selection == null && selectionArgs == null) {
                    selection = BaseColumns._ID + " = ?";

                    selectionArgs = new String[] {
                        uri.getLastPathSegment()
                    };

                    rowCount = helper
                            .getWritableDatabase()
                            .delete(URI_CODE_TABLE_MAP.get(code),
                                    selection,
                                    selectionArgs);
                } else {
                    throw new IllegalArgumentException("Selection must be " +
                            "null when specifying ID as part of uri.");
                }
                break;
            default:
                throw new IllegalArgumentException("Invalid Uri: " + uri);
        }

        notifyUris(uri);

        return rowCount;
    }

    @Override
    public Cursor query(@NonNull Uri uri,
                        String[] projection,
                        String selection,
                        String[] selectionArgs,
                        String sortOrder) throws IllegalArgumentException {
        Cursor cursor;
        if (projection == null) {
            throw new IllegalArgumentException("Projection can't be null");
        }

        sortOrder = (sortOrder == null ? BaseColumns._ID : sortOrder);

        SQLiteDatabase database = helper.getReadableDatabase();

        final int code = URI_MATCHER.match(uri);
        switch (code) {
            case CODE_ALL_DEVICES:
            case CODE_ALL_MANUFACTURERS:
                cursor = database.query(URI_CODE_TABLE_MAP.get(code),
                        projection,
                        selection,
                        selectionArgs,
                        null,
                        null,
                        sortOrder);
                break;
            case CODE_DEVICE_ID:
            case CODE_MANUFACTURER_ID:
                if (selection == null) {
                    selection = BaseColumns._ID
                            + " = "
                            + uri.getLastPathSegment();
                } else {
                    throw new IllegalArgumentException("Selection must " +
                            "be null when specifying ID as part of uri.");
                }
                cursor = database.query(URI_CODE_TABLE_MAP.get(code),
                        projection,
                        selection,
                        selectionArgs,
                        null,
                        null,
                        sortOrder);
                break;
            case CODE_DEVICE_MANUFACTURER:
                SQLiteQueryBuilder builder = new SQLiteQueryBuilder();

                builder.setTables(String
                        .format("%s INNER JOIN %s ON (%s.%s=%s.%s)",
                        DevicesOpenHelper.Tables.DEVICE,
                        DevicesOpenHelper.Tables.MANUFACTURER,
                        DevicesOpenHelper.Tables.DEVICE,
                        DevicesContract.Device.MANUFACTURER_ID,
                        DevicesOpenHelper.Tables.MANUFACTURER,
                        DevicesContract.Manufacturer._ID));

                final Map<String, String> projectionMap = new HashMap<>();
                projectionMap.put(DevicesContract.DeviceManufacturer.MODEL,
                        DevicesContract.DeviceManufacturer.MODEL);

                projectionMap
                        .put(DevicesContract.DeviceManufacturer.SHORT_NAME,
                        DevicesContract.DeviceManufacturer.SHORT_NAME);

                projectionMap
                        .put(DevicesContract.DeviceManufacturer.DEVICE_ID,
                        String.format("%s.%s AS %s",
                                DevicesOpenHelper.Tables.DEVICE,
                                DevicesContract.Device._ID,
                                DevicesContract.DeviceManufacturer.DEVICE_ID));

                projectionMap.put(DevicesContract
                        .DeviceManufacturer.MANUFACTURER_ID,
                        String.format("%s.%s AS %s",
                                DevicesOpenHelper.Tables.MANUFACTURER,
                                DevicesContract.Manufacturer._ID,
                                DevicesContract
                                        .DeviceManufacturer.MANUFACTURER_ID));

                builder.setProjectionMap(projectionMap);

                cursor = builder.query(database,
                        projection,
                        selection,
                        selectionArgs,
                        null,
                        null,
                        sortOrder);

                break;
            default:
                throw new IllegalArgumentException("Invalid Uri: " + uri);
        }

        cursor.setNotificationUri(getContext().getContentResolver(), uri);
        return cursor;
    }

    @Override
    public int update(@NonNull Uri uri,
                      ContentValues values,
                      String selection,
                      String[] selectionArgs) {
        int rowCount;

        final int code = URI_MATCHER.match(uri);
        switch (code) {
            case CODE_ALL_DEVICES:
            case CODE_ALL_MANUFACTURERS:
                rowCount = helper
                        .getWritableDatabase()
                        .update(URI_CODE_TABLE_MAP.get(code),
                                values,
                                selection,
                                selectionArgs);
                break;
            case CODE_DEVICE_ID:
            case CODE_MANUFACTURER_ID:
                if (selection == null
                        && selectionArgs == null) {
                    selection = BaseColumns._ID + " = ?";

                    selectionArgs = new String[] {
                            uri.getLastPathSegment()
                    };
                } else {
                    throw new IllegalArgumentException("Selection must be " +
                            "null when specifying ID as part of uri.");
                }
                rowCount = helper
                        .getWritableDatabase()
                        .update(URI_CODE_TABLE_MAP.get(code),
                                values,
                                selection,
                                selectionArgs);
                break;
            default:
                throw new IllegalArgumentException("Invalid Uri: " + uri);
        }

        notifyUris(uri);
        return rowCount;
    }

    @Override
    public int bulkInsert(@NonNull Uri uri, @NonNull ContentValues[] values) {
        final SQLiteDatabase db = helper.getWritableDatabase();

        db.beginTransaction();

        try {
            final int count = super.bulkInsert(uri, values);
            db.setTransactionSuccessful();

            return count;
        } finally {
            db.endTransaction();
        }
    }

    @Override
    public @NonNull ContentProviderResult[] applyBatch(@NonNull ArrayList<ContentProviderOperation> operations) throws OperationApplicationException {
        final SQLiteDatabase db = helper.getWritableDatabase();

        db.beginTransaction();

        try {
            final ContentProviderResult[] results =
                    super.applyBatch(operations);
            db.setTransactionSuccessful();

            return results;
        } finally {
            db.endTransaction();
        }
    }

    private void notifyUris(Uri affectedUri) {
        final ContentResolver contentResolver =
                getContext().getContentResolver();

        if (contentResolver != null) {
            contentResolver.notifyChange(affectedUri, null);
            contentResolver
                    .notifyChange(DevicesContract
                            .DeviceManufacturer.CONTENT_URI, null);
        }
    }

    @Override
    public String getType(@NonNull Uri uri) {

        final int code = URI_MATCHER.match(uri);
        switch (code) {
            case CODE_ALL_DEVICES:
                return String.format("%s/vnd.%s.%s",
                        ContentResolver.CURSOR_DIR_BASE_TYPE,
                        DevicesContract.AUTHORITY,
                        DevicesContract.Device.PATH);
            case CODE_ALL_MANUFACTURERS:
                return String.format("%s/vnd.%s.%s",
                        ContentResolver.CURSOR_DIR_BASE_TYPE,
                        DevicesContract.AUTHORITY,
                        DevicesContract.Manufacturer.PATH);
            case CODE_DEVICE_ID:
                return String.format("%s/vnd.%s.%s",
                        ContentResolver.CURSOR_ITEM_BASE_TYPE,
                        DevicesContract.AUTHORITY,
                        DevicesContract.Device.PATH);
            case CODE_MANUFACTURER_ID:
                return String.format("%s/vnd.%s.%s",
                        ContentResolver.CURSOR_ITEM_BASE_TYPE,
                        DevicesContract.AUTHORITY,
                        DevicesContract.Manufacturer.PATH);
            default:
                return null;
        }
    }
}
