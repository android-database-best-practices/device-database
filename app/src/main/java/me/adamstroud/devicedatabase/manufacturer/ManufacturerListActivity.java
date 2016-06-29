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

import android.app.LoaderManager;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import me.adamstroud.devicedatabase.R;
import me.adamstroud.devicedatabase.provider.DevicesContract;

/**
 * A device manufacturer.
 *
 * @author Adam Stroud &#60;<a href="mailto:adam.stroud@gmail.com">adam.stroud@gmail.com</a>&#62;
 */
public class ManufacturerListActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor> {
    private static final int LOADER_ID_MANUFACTURERS = 1;

    public static final String EXTRA_MANUFACTURER_URI = "manufacturerUri";

    private ManufacturerCursorAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manufacturer_list);

        final RecyclerView recyclerView = (RecyclerView) findViewById(R.id.recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ManufacturerCursorAdapter();
        recyclerView.setAdapter(adapter);

        getLoaderManager().initLoader(LOADER_ID_MANUFACTURERS, null, this);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        Loader<Cursor> loader = null;
        switch (id) {
            case LOADER_ID_MANUFACTURERS:
                loader = new CursorLoader(this,
                        DevicesContract.Manufacturer.CONTENT_URI,
                        new String[] {DevicesContract.Manufacturer._ID,
                                DevicesContract.Manufacturer.LONG_NAME,
                                DevicesContract.Manufacturer.SHORT_NAME},
                        null,
                        null,
                        DevicesContract.Manufacturer.LONG_NAME);
                break;
        }

        return loader;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        if (data == null || data.getCount() == 0) {

        } else {
            adapter.swapCursor(data);
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        adapter.swapCursor(null);
    }

    private class ManufacturerCursorAdapter extends RecyclerView.Adapter<ManufacturerViewHolder> {
        private Cursor cursor;

        @Override
        public ManufacturerViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.list_item_manufacturer, parent, false);

            return new ManufacturerViewHolder(view);
        }

        @Override
        public void onBindViewHolder(ManufacturerViewHolder holder, int position) {
            final ContentValues contentValues = new ContentValues();

            if (cursor != null && cursor.moveToPosition(position)) {
                holder.longNameView.setText(cursor.getString(cursor.getColumnIndexOrThrow(DevicesContract.Manufacturer.LONG_NAME)));
                holder.shortNameView.setText(cursor.getString(cursor.getColumnIndexOrThrow(DevicesContract.Manufacturer.SHORT_NAME)));
                holder.uri = ContentUris.withAppendedId(DevicesContract.Manufacturer.CONTENT_URI,
                        cursor.getLong(cursor.getColumnIndexOrThrow(DevicesContract.Manufacturer._ID)));
            }
        }

        @Override
        public int getItemCount() {
            return (cursor == null ? 0 : cursor.getCount());
        }

        public void swapCursor(Cursor newDeviceCursor) {
            if (cursor != null) {
                cursor.close();
            }

            cursor = newDeviceCursor;

            notifyDataSetChanged();
        }
    }

    private class ManufacturerViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        public TextView longNameView;
        public TextView shortNameView;
        public Uri uri;

        public ManufacturerViewHolder(View itemView) {
            super(itemView);

            itemView.setOnClickListener(this);
            longNameView = (TextView) itemView.findViewById(R.id.short_name);
            shortNameView = (TextView) itemView.findViewById(R.id.long_name);
        }

        @Override
        public void onClick(View view) {
            setResult(RESULT_OK, new Intent().putExtra(EXTRA_MANUFACTURER_URI, uri));
            finish();
        }
    }
}
