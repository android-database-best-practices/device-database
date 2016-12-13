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

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.LoaderManager;
import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.RemoteException;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.android.volley.Response;
import com.android.volley.VolleyError;

import java.util.ArrayList;
import java.util.List;

import me.adamstroud.devicedatabase.R;
import me.adamstroud.devicedatabase.api.retrofit.ManufacturersAndDevicesResponse;
import me.adamstroud.devicedatabase.api.retrofit.WebServiceClient;
import me.adamstroud.devicedatabase.api.volley.GetManufacturersAndDevicesRequest;
import me.adamstroud.devicedatabase.api.volley.VolleyApiClient;
import me.adamstroud.devicedatabase.model.Device;
import me.adamstroud.devicedatabase.model.Manufacturer;
import me.adamstroud.devicedatabase.provider.DevicesContract;
import rx.Observable;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Func1;
import rx.schedulers.Schedulers;
import rx.subscriptions.CompositeSubscription;

/**
 * Displays all devices to the user.
 *
 * @author Adam Stroud &#60;<a href="mailto:adam.stroud@gmail.com">adam.stroud@gmail.com</a>&#62;
 */
public class DeviceListActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor> {
    private static final String TAG =
            DeviceListActivity.class.getSimpleName();

    private static final String VOLLEY_TAG =
            DeviceListActivity.class.getCanonicalName();

    private static final int LOADER_ID_DEVICES = 1;

    private RecyclerView recyclerView;
    private TextView empty;
    private CompositeSubscription compositeSubscription;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_list);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setTitle(getTitle());

        toolbar.inflateMenu(R.menu.activity_device_list);
        toolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                boolean handled = false;

                if (item.getItemId() == R.id.action_add_retrofit_data) {
                    handled = true;

                    compositeSubscription.add(WebServiceClient
                            .getInstance()
                            .getService()
                            .rxGetManufacturersAndDevices()
                            .flatMap(new Func1<ManufacturersAndDevicesResponse, Observable<ContentProviderResult>>() {
                                @Override
                                public Observable<ContentProviderResult> call(ManufacturersAndDevicesResponse response) {
                                    final ContentResolver contentResolver = getContentResolver();
                                    final ArrayList<ContentProviderOperation> operations = new ArrayList<>();
                                    final ContentProviderResult[] results;

                                    operations.add(ContentProviderOperation.newDelete(DevicesContract.Device.CONTENT_URI).build());
                                    operations.add(ContentProviderOperation.newDelete(DevicesContract.Manufacturer.CONTENT_URI).build());

                                    for (Manufacturer manufacturer : response.getManufacturers()) {
                                        final ContentProviderOperation manufacturerOperation = ContentProviderOperation
                                                .newInsert(DevicesContract.Manufacturer.CONTENT_URI)
                                                .withValue(DevicesContract.Manufacturer.SHORT_NAME, manufacturer.getShortName())
                                                .withValue(DevicesContract.Manufacturer.LONG_NAME, manufacturer.getLongName())
                                                .build();

                                        operations.add(manufacturerOperation);

                                        int manufacturerInsertOperationIndex = operations.size() - 1;

                                        for (Device device : manufacturer.getDevices()) {
                                            final ContentProviderOperation deviceOperation = ContentProviderOperation
                                                    .newInsert(DevicesContract.Device.CONTENT_URI)
                                                    .withValueBackReference(DevicesContract.Device.MANUFACTURER_ID, manufacturerInsertOperationIndex)
                                                    .withValue(DevicesContract.Device.MODEL, device.getModel())
                                                    .withValue(DevicesContract.Device.DISPLAY_SIZE_INCHES, device.getDisplaySizeInches())
                                                    .withValue(DevicesContract.Device.MEMORY_MB, device.getMemoryMb())
                                                    .withValue(DevicesContract.Device.NICKNAME, device.getNickname())
                                                    .build();

                                            operations.add(deviceOperation);
                                        }
                                    }

                                    try {
                                        results = contentResolver.applyBatch(DevicesContract.AUTHORITY, operations);
                                    } catch (RemoteException | OperationApplicationException e) {
                                        throw new RuntimeException(e);
                                    }

                                    return Observable.from(results);
                                }
                            })
                            .toList()
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe(new Subscriber<List<ContentProviderResult>>() {
                                @Override
                                public void onCompleted() {
                                    // no-op
                                }

                                @Override
                                public void onError(Throwable e) {
                                    Log.e(TAG, "Received web API error", e);
                                }

                                @Override
                                public void onNext(List<ContentProviderResult> contentProviderResults) {
                                    Log.d(TAG, "Got response -> " + contentProviderResults.size());
                                }
                            }));
                } else if (item.getItemId() == R.id.action_add_volley_data) {
                    handled = true;

                    loadDataUsingVolley();
                } else if (item.getItemId() == R.id.action_perform_sync) {
                    Bundle bundle = new Bundle();
                    bundle.putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true);
                    bundle.putBoolean(ContentResolver.SYNC_EXTRAS_EXPEDITED, true);
                    ContentResolver.requestSync(new Account("SyncAccount", "stubAuthenticator"),
                            "me.adamstroud.devicedatabase.provider",
                            bundle);
                }

                return handled;
            }
        });

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(DeviceListActivity.this,
                        AddDeviceActivity.class));
            }
        });

        recyclerView = (RecyclerView) findViewById(R.id.recycler_view);
        empty = (TextView) findViewById(R.id.empty);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(new DeviceCursorAdapter());

        getLoaderManager().initLoader(LOADER_ID_DEVICES, null, this);

        Account account = new Account("SyncAccount", "stubAuthenticator");

        AccountManager accountManager = (AccountManager) getSystemService(ACCOUNT_SERVICE);

        if (accountManager.addAccountExplicitly(account, null, null)) {
            // do something positive
        } else {
            // do something to log error case
        }
    }

    private void loadDataUsingVolley() {
        GetManufacturersAndDevicesRequest request =
                new GetManufacturersAndDevicesRequest(VOLLEY_TAG,
                new Response.Listener<GetManufacturersAndDevicesRequest
                        .Response>() {
                    @Override
                    public void onResponse(GetManufacturersAndDevicesRequest
                                                   .Response response) {
                        List<Manufacturer> manufacturersList =
                                response.getManufacturers();

                        updateDisplay(manufacturersList);
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.e(TAG, "Received web API error", error);
            }
        });

        VolleyApiClient
                .getInstance(DeviceListActivity.this)
                .add(request);
    }

    private void updateDisplay(List<Manufacturer> manufacturersList) {
        throw new UnsupportedOperationException("Not Yet Implemented");
    }


    @Override
    protected void onStart() {
        super.onStart();
        compositeSubscription = new CompositeSubscription();
    }

    @Override
    protected void onStop() {
        super.onStop();
        compositeSubscription.unsubscribe();
        VolleyApiClient.getInstance(this).cancelAll(VOLLEY_TAG);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        Loader<Cursor> loader = null;
        String[] projection = {
                DevicesContract.DeviceManufacturer.MODEL,
                DevicesContract.DeviceManufacturer.DEVICE_ID,
                DevicesContract.DeviceManufacturer.SHORT_NAME
        };

        switch (id) {
            case LOADER_ID_DEVICES:
                loader = new CursorLoader(this,
                        DevicesContract.DeviceManufacturer.CONTENT_URI,
                        projection,
                        null,
                        null,
                        DevicesContract.DeviceManufacturer.MODEL);
                break;
        }

        return loader;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        if (data == null || data.getCount() == 0) {
            empty.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            empty.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
            ((DeviceCursorAdapter) recyclerView.getAdapter()).swapCursor(data);
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        ((DeviceCursorAdapter) recyclerView.getAdapter()).swapCursor(null);
    }

    private class DeviceCursorAdapter
            extends RecyclerView.Adapter<DeviceViewHolder> {
        private Cursor deviceCursor;

        @Override
        public DeviceViewHolder onCreateViewHolder(ViewGroup parent,
                                                   int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.list_item_device, parent, false);

            return new DeviceViewHolder(view);
        }

        @Override
        public void onBindViewHolder(DeviceViewHolder holder,
                                     int position) {
            if (deviceCursor == null) {
                throw new IllegalStateException("Cursor is null");
            }
            if (!deviceCursor.moveToPosition(position)) {
                throw new IllegalStateException("Couldn't move to position " + position);
            }
            String model = deviceCursor
                    .getString(deviceCursor
                            .getColumnIndexOrThrow(DevicesContract
                                    .DeviceManufacturer
                                    .MODEL));

            int deviceId = deviceCursor
                    .getInt(deviceCursor
                            .getColumnIndexOrThrow(DevicesContract
                                    .DeviceManufacturer
                                    .DEVICE_ID));

            String shortName = deviceCursor
                    .getString(deviceCursor
                            .getColumnIndexOrThrow(DevicesContract
                                    .DeviceManufacturer
                                    .SHORT_NAME));

            holder.name.setText(getString(R.string.device_name,
                    shortName,
                    model,
                    deviceId));
            holder.uri = ContentUris
                    .withAppendedId(DevicesContract.Device.CONTENT_URI,
                            deviceId);
        }

        @Override
        public int getItemCount() {
            return (deviceCursor == null ? 0 : deviceCursor.getCount());
        }

    public void swapCursor(Cursor newDeviceCursor) {
        if (deviceCursor != null) {
            deviceCursor.close();
        }

            deviceCursor = newDeviceCursor;

            notifyDataSetChanged();
        }
    }

    private class DeviceViewHolder
            extends RecyclerView.ViewHolder
            implements View.OnClickListener {
        public TextView name;
        public Uri uri;

        public DeviceViewHolder(View itemView) {
            super(itemView);

            itemView.setOnClickListener(this);
            name = (TextView) itemView.findViewById(R.id.name);
        }

        @Override
        public void onClick(View view) {
            Intent detailIntent =
                    new Intent(view.getContext(),
                               DeviceDetailActivity.class);

            detailIntent.putExtra(DeviceDetailActivity.EXTRA_DEVICE_URI, uri);
            startActivity(detailIntent);
        }
    }
}
