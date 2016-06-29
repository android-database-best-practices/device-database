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

import android.annotation.TargetApi;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Build;
import android.text.TextUtils;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import me.adamstroud.devicedatabase.BuildConfig;

/**
 * An open helper that reads SQL statements from assets.
 *
 * @author Adam Stroud &#60;<a href="mailto:adam.stroud@gmail.com">adam.stroud@gmail.com</a>&#62;
 */
/* package */ class DevicesOpenHelper extends SQLiteOpenHelper {
    private static final String TAG =
            DevicesOpenHelper.class.getSimpleName();
    private static final int SCHEMA_VERSION = 3;
    private static final String DB_NAME = "devices.db";

    private final Context context;

    private static DevicesOpenHelper instance;

    public synchronized static DevicesOpenHelper getInstance(Context ctx) {
        if (instance == null) {
            instance = new DevicesOpenHelper(ctx.getApplicationContext());
        }

        return instance;
    }
    /**
     * Creates a new instance of the simple open helper.
     *
     * @param context Context to read assets. This will be helped by the
     *                instance.
     */
    private DevicesOpenHelper(Context context) {
        super(context, DB_NAME, null, SCHEMA_VERSION);

        this.context = context;

        // This will happen in onConfigure for API >= 16
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
            SQLiteDatabase db = getWritableDatabase();
            db.enableWriteAheadLogging();
            db.execSQL("PRAGMA foreign_keys = ON;");
        }
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        for (int i = 1; i <= SCHEMA_VERSION; i++) {
            applySqlFile(db, i);
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase db,
                          int oldVersion,
                          int newVersion) {
        for (int i = (oldVersion + 1); i <= newVersion; i++) {
            applySqlFile(db, i);
        }
    }

    @Override
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    public void onConfigure(SQLiteDatabase db) {
        super.onConfigure(db);

        setWriteAheadLoggingEnabled(true);
        db.setForeignKeyConstraintsEnabled(true);
    }

    private void applySqlFile(SQLiteDatabase db, int version) {
        BufferedReader reader = null;

        try {
            String filename = String.format("%s.%d.sql", DB_NAME, version);
            final InputStream inputStream =
                    context.getAssets().open(filename);
            reader =
                    new BufferedReader(new InputStreamReader(inputStream));

            final StringBuilder statement = new StringBuilder();

            for (String line; (line = reader.readLine()) != null;) {
                if (BuildConfig.DEBUG) {
                    Log.d(TAG, "Reading line -> " + line);
                }

                // Ignore empty lines
                if (!TextUtils.isEmpty(line) && !line.startsWith("--")) {
                    statement.append(line.trim());
                }

                if (line.endsWith(";")) {
                    if (BuildConfig.DEBUG) {
                        Log.d(TAG, "Running statement " + statement);
                    }

                    db.execSQL(statement.toString());
                    statement.setLength(0);
                }
            }

        } catch (IOException e) {
            Log.e(TAG, "Could not apply SQL file", e);
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    Log.w(TAG, "Could not close reader", e);
                }
            }
        }
    }

    public interface Tables {
        String DEVICE = "device";
        String MANUFACTURER = "manufacturer";
    }
}