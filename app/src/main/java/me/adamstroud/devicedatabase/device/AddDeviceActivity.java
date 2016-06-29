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

package me.adamstroud.devicedatabase.device;

import android.app.LoaderManager;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.databinding.DataBindingUtil;
import android.databinding.ObservableField;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;

import me.adamstroud.devicedatabase.R;
import me.adamstroud.devicedatabase.databinding.ActivityAddDeviceBinding;
import me.adamstroud.devicedatabase.manufacturer.ManufacturerListActivity;
import me.adamstroud.devicedatabase.provider.DevicesContract;

/**
 * Allows the user to add a device to the app.
 *
 * @author Adam Stroud &#60;<a href="mailto:adam.stroud@gmail.com">adam.stroud@gmail.com</a>&#62;
 */
public class AddDeviceActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor> {
    private static final int REQUEST_MANUFACTURER_LIST = 1;
    private static final String LOADER_ARG_MANUFACTURER_URI = "manufacturerUri";
    private static final int LOADER_ID_MANUFACTURER = 1;

    private Toolbar toolbar;
    private long manufacturerId;
    private ObservableDevice observableDevice = new ObservableDevice();
    private ActivityAddDeviceBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_add_device);

        toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.inflateMenu(R.menu.activity_add_device);
    }

    public void onActionDoneClick(MenuItem menuItem) {
        final ContentValues contentValues = new ContentValues();
        contentValues.put(DevicesContract.Device.MODEL, binding.model.getEditText().getText().toString());
        contentValues.put(DevicesContract.Device.NICKNAME, binding.nickname.getEditText().getText().toString());

        Uri uri = getContentResolver().insert(DevicesContract.Device.CONTENT_URI, contentValues);

        Snackbar.make(binding.root, getString(R.string.device_saved_message, ContentUris.parseId(uri)), Snackbar.LENGTH_SHORT).show();
    }

    public void onDeviceManufacturerClick(View view) {
        startActivityForResult(new Intent(this, ManufacturerListActivity.class), REQUEST_MANUFACTURER_LIST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_MANUFACTURER_LIST && resultCode == RESULT_OK) {
            Uri manufacturerUri = data.getParcelableExtra(ManufacturerListActivity.EXTRA_MANUFACTURER_URI);
            manufacturerId = ContentUris.parseId(manufacturerUri);
            Bundle args = new Bundle();
            args.putParcelable(LOADER_ARG_MANUFACTURER_URI, manufacturerUri);
            getLoaderManager().initLoader(LOADER_ID_MANUFACTURER, args, this);
        }
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        return new CursorLoader(this,
                args.<Uri>getParcelable(LOADER_ARG_MANUFACTURER_URI),
                new String[] {DevicesContract.Manufacturer.LONG_NAME},
                null,
                null,
                null);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        if (data != null && data.moveToFirst()) {
            binding.deviceManufacturer.setText(data.getString(data.getColumnIndexOrThrow(DevicesContract.Manufacturer.LONG_NAME)));
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        // no-op
    }

    private static class ObservableDevice {
        public final ObservableField<String> nickname = new ObservableField<>();
    }
}
