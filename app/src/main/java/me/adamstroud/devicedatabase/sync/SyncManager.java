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

package me.adamstroud.devicedatabase.sync;

import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
import android.content.ContentResolver;
import android.content.Context;
import android.content.OperationApplicationException;
import android.os.RemoteException;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

import me.adamstroud.devicedatabase.api.retrofit.ManufacturersAndDevicesResponse;
import me.adamstroud.devicedatabase.api.retrofit.WebServiceClient;
import me.adamstroud.devicedatabase.model.Device;
import me.adamstroud.devicedatabase.model.Manufacturer;
import me.adamstroud.devicedatabase.provider.DevicesContract;
import rx.Observable;
import rx.Subscriber;
import rx.functions.Func1;
import rx.schedulers.Schedulers;

/**
 * Provides manual synchronization support.
 *
 * @author Adam Stroud &#60;<a href="mailto:adam.stroud@gmail.com">adam.stroud@gmail.com</a>&#62;
 */
public class SyncManager extends Subscriber<List<ContentProviderResult>>
    implements Func1<ManufacturersAndDevicesResponse, Observable<ContentProviderResult>> {
    private static final String TAG = SyncAdapter.class.getSimpleName();

    private static SyncManager instance;

    private final Context context;

    public static synchronized SyncManager getInstance(Context context) {
        if (instance == null) {
            instance = new SyncManager(context);
        }

        return instance;
    }

    private SyncManager(Context context) {
        this.context = context.getApplicationContext();
    }

    public void syncManufacturersAndDevices() {
        WebServiceClient
                .getInstance()
                .getService()
                .rxGetManufacturersAndDevices()
                .flatMap(this)
                .toList()
                .subscribeOn(Schedulers.io())
                .subscribe(this);
    }

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

    @Override
    public Observable<ContentProviderResult> call(ManufacturersAndDevicesResponse response) {
        final ContentResolver contentResolver =
                context.getContentResolver();

        final ArrayList<ContentProviderOperation> operations =
                new ArrayList<>();

        final ContentProviderResult[] results;

        operations.add(ContentProviderOperation
                .newDelete(DevicesContract.Device.CONTENT_URI)
                .build());

        operations.add(ContentProviderOperation
                .newDelete(DevicesContract.Manufacturer.CONTENT_URI)
                .build());

        for (Manufacturer manufacturer : response.getManufacturers()) {
            final ContentProviderOperation manufacturerOperation =
                    ContentProviderOperation
                    .newInsert(DevicesContract.Manufacturer.CONTENT_URI)
                    .withValue(DevicesContract.Manufacturer.SHORT_NAME,
                            manufacturer.getShortName())
                    .withValue(DevicesContract.Manufacturer.LONG_NAME,
                            manufacturer.getLongName())
                    .build();

            operations.add(manufacturerOperation);

            int manufacturerInsertOperationIndex = operations.size() - 1;

            for (Device device : manufacturer.getDevices()) {
                final ContentProviderOperation deviceOperation =
                        ContentProviderOperation
                        .newInsert(DevicesContract.Device.CONTENT_URI)
                        .withValueBackReference(DevicesContract.Device.MANUFACTURER_ID,
                                manufacturerInsertOperationIndex)
                        .withValue(DevicesContract.Device.MODEL,
                                device.getModel())
                        .withValue(DevicesContract.Device.DISPLAY_SIZE_INCHES,
                                device.getDisplaySizeInches())
                        .withValue(DevicesContract.Device.MEMORY_MB,
                                device.getMemoryMb())
                        .withValue(DevicesContract.Device.NICKNAME,
                                device.getNickname())
                        .build();

                operations.add(deviceOperation);
            }
        }

        try {
            results =
                    contentResolver.applyBatch(DevicesContract.AUTHORITY,
                            operations);
        } catch (RemoteException | OperationApplicationException e) {
            throw new RuntimeException(e);
        }

        return Observable.from(results);
    }
}
