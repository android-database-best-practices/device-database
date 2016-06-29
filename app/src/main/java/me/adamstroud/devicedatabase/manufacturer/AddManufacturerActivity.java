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

package me.adamstroud.devicedatabase.manufacturer;

import android.content.AsyncQueryHandler;
import android.content.ContentUris;
import android.content.ContentValues;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TextInputLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;

import me.adamstroud.devicedatabase.R;
import me.adamstroud.devicedatabase.provider.DevicesContract;

public class AddManufacturerActivity extends AppCompatActivity {
    private CoordinatorLayout root;
    private TextInputLayout shortNameView;
    private TextInputLayout longNameView;
    private AddManufacturerAsyncQueryHandler queryHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_manufacturer);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        root = (CoordinatorLayout) findViewById(R.id.root);
        shortNameView = (TextInputLayout) findViewById(R.id.short_name);
        longNameView = (TextInputLayout) findViewById(R.id.long_name);
        queryHandler = new AddManufacturerAsyncQueryHandler();

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    public void onActionDoneClick(MenuItem menuItem) {
        final ContentValues contentValues = new ContentValues();
        contentValues.put(DevicesContract.Manufacturer.SHORT_NAME, shortNameView.getEditText().getText().toString());
        contentValues.put(DevicesContract.Manufacturer.LONG_NAME, longNameView.getEditText().getText().toString());

        queryHandler.startInsert(1, null, DevicesContract.Manufacturer.CONTENT_URI, contentValues);
    }

    private class AddManufacturerAsyncQueryHandler extends AsyncQueryHandler {
        public AddManufacturerAsyncQueryHandler() {
            super(getContentResolver());
        }

        @Override
        protected void onInsertComplete(int token, Object cookie, Uri uri) {
            super.onInsertComplete(token, cookie, uri);
            Snackbar.make(root, getString(R.string.device_saved_message, ContentUris.parseId(uri)), Snackbar.LENGTH_SHORT).show();
        }
    }
}
