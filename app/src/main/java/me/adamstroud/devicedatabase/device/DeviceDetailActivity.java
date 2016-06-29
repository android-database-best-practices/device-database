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
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.databinding.DataBindingUtil;
import android.databinding.ObservableField;
import android.databinding.ObservableFloat;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.support.v4.app.NavUtils;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.ShareActionProvider;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import me.adamstroud.devicedatabase.R;
import me.adamstroud.devicedatabase.databinding.ActivityDeviceDetailBinding;
import me.adamstroud.devicedatabase.provider.DevicesContract;

/**
 * Shows the device properties to the user.
 *
 * @author Adam Stroud &#60;<a href="mailto:adam.stroud@gmail.com">adam.stroud@gmail.com</a>&#62;
 */
public class DeviceDetailActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor> {
    public static final String EXTRA_DEVICE_URI = "deviceUri";

    private static final int ID_DEVICE = 1;

    private Uri deviceUri;
    private CoordinatorLayout coordinatorLayout;
    private Intent shareIntent;
    private ActivityDeviceDetailBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_device_detail);
        binding.setDevice(new ObservableDevice());

        coordinatorLayout = (CoordinatorLayout) findViewById(R.id.coordinator_layout);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        deviceUri = getIntent().getExtras().getParcelable(EXTRA_DEVICE_URI);

        getLoaderManager().initLoader(ID_DEVICE, null, this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.activity_device_detail, menu);

        MenuItem menuItem = menu.findItem(R.id.action_share);
        shareIntent = new Intent(Intent.ACTION_SEND)
                .setType("text/plain");
        ShareActionProvider provider =
                (ShareActionProvider) MenuItemCompat.getActionProvider(menuItem);
        provider.setShareIntent(shareIntent);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            // This ID represents the Home or Up button. In the case of this
            // activity, the Up button is shown. Use NavUtils to allow users
            // to navigate up one level in the application structure. For
            // more details, see the Navigation pattern on Android Design:
            //
            // http://developer.android.com/design/patterns/navigation.html#up-vs-back
            //
            NavUtils.navigateUpTo(this, new Intent(this, DeviceListActivity.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        return new CursorLoader(this,
                deviceUri,
                new String[] {DevicesContract.Device.MODEL,
                        DevicesContract.Device.NICKNAME,
                        DevicesContract.Device.DISPLAY_SIZE_INCHES,
                        DevicesContract.Device.MEMORY_MB,
                        DevicesContract.Device._ID},
                null,
                null,
                null);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        if (data != null && data.moveToFirst()) {
            ObservableDevice observableDevice = binding.getDevice();

            observableDevice
                    .model
                    .set(data.getString(data
                            .getColumnIndexOrThrow(DevicesContract
                            .Device
                            .MODEL)));

            observableDevice
                    .nickname
                    .set(data.getString(data
                            .getColumnIndexOrThrow(DevicesContract
                            .Device
                            .NICKNAME)));

            observableDevice
                    .memoryInMb
                    .set(data.getFloat(data
                            .getColumnIndexOrThrow(DevicesContract
                            .Device
                            .MEMORY_MB)));

            observableDevice
                    .displaySizeInInches
                    .set(data.getFloat(data
                            .getColumnIndexOrThrow(DevicesContract
                            .Device
                            .DISPLAY_SIZE_INCHES)));

            binding
                    .id
                    .setText(getString(R.string.id,
                             data.getLong(data
                                     .getColumnIndex(DevicesContract
                    .Device
                    ._ID))));
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        // no-op
    }

    public void onActionShareClick(MenuItem menuItem) {
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType(getContentResolver().getType(DevicesContract.Device.CONTENT_URI));

        if (intent.resolveActivity(this.getPackageManager()) == null) {
            Snackbar.make(coordinatorLayout, "Could not launch activity", Snackbar.LENGTH_LONG).show();
        } else {
            startActivity(Intent.createChooser(intent, "Title"));
        }
    }

    public static class ObservableDevice {
        public final ObservableField<String> nickname =
                new ObservableField<>();

        public final ObservableField<String> model =
                new ObservableField<>();

        public final ObservableFloat memoryInMb =
                new ObservableFloat();

        public final ObservableFloat displaySizeInInches =
                new ObservableFloat();
    }
}
