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

import android.accounts.Account;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.ContentProviderOperation;
import android.content.Context;
import android.content.OperationApplicationException;
import android.content.SyncResult;
import android.os.Bundle;
import android.os.RemoteException;
import android.util.Log;

import java.io.IOException;
import java.util.ArrayList;

import me.adamstroud.devicedatabase.api.retrofit.ManufacturersAndDevicesResponse;
import me.adamstroud.devicedatabase.api.retrofit.WebServiceClient;
import me.adamstroud.devicedatabase.model.Device;
import me.adamstroud.devicedatabase.model.Manufacturer;
import me.adamstroud.devicedatabase.provider.DevicesContract;
import retrofit2.Call;
import retrofit2.Response;

/**
 * Handles the sync operations when using the Android sync framework.
 *
 * @author Adam Stroud &#60;<a href="mailto:adam.stroud@gmail.com">adam.stroud@gmail.com</a>&#62;
 */
public class SyncAdapter extends AbstractThreadedSyncAdapter {
    private static final String TAG = SyncAdapter.class.getSimpleName();

    public SyncAdapter(Context context, boolean autoInitialize) {
        super(context, autoInitialize);
    }

    public SyncAdapter(Context context,
                       boolean autoInitialize,
                       boolean allowParallelSyncs) {
        super(context, autoInitialize, allowParallelSyncs);
    }

    @Override
    public void onPerformSync(Account account,
                              Bundle extras,
                              String authority,
                              ContentProviderClient provider,
                              SyncResult syncResult) {
        Call<ManufacturersAndDevicesResponse> call = WebServiceClient
                .getInstance()
                .getService()
                .getManufacturersAndDevices();

        try {
            // Perform synchronous web service call
            Response<ManufacturersAndDevicesResponse> wrappedResponse =
                    call.execute();

            ArrayList<ContentProviderOperation> operations =
                    generateDatabaseOperations(wrappedResponse.body());

            provider.applyBatch(operations);

        } catch (IOException
                | OperationApplicationException
                | RemoteException e) {
            Log.e(TAG, "Could not perform sync", e);
        }
    }

    private ArrayList<ContentProviderOperation> generateDatabaseOperations(ManufacturersAndDevicesResponse response) {
        final ArrayList<ContentProviderOperation> operations =
                new ArrayList<>();

        operations.add(ContentProviderOperation
                .newDelete(DevicesContract.Device.CONTENT_URI).build());

        operations.add(ContentProviderOperation
                .newDelete(DevicesContract.Manufacturer.CONTENT_URI)
                .build());

        for (Manufacturer manufacturer : response.getManufacturers()) {
            final ContentProviderOperation manufacturerOperation =
                    ContentProviderOperation
                            .newInsert(DevicesContract.Manufacturer
                                    .CONTENT_URI)

                            .withValue(DevicesContract.Manufacturer
                                    .SHORT_NAME,
                                       manufacturer.getShortName())

                            .withValue(DevicesContract.Manufacturer
                                    .LONG_NAME,
                                       manufacturer.getLongName())
                            .build();

            operations.add(manufacturerOperation);

            int manufacturerInsertOperationIndex =
                    operations.size() - 1;

            for (Device device : manufacturer.getDevices()) {
                final ContentProviderOperation deviceOperation =
                        ContentProviderOperation
                                .newInsert(DevicesContract.Device
                                        .CONTENT_URI)
                                .withValueBackReference(DevicesContract
                                        .Device.MANUFACTURER_ID,
                                        manufacturerInsertOperationIndex)
                                .withValue(DevicesContract.Device.MODEL,
                                           device.getModel())
                                .withValue(DevicesContract
                                        .Device
                                        .DISPLAY_SIZE_INCHES,
                                        device.getDisplaySizeInches())
                                .withValue(DevicesContract
                                        .Device
                                        .MEMORY_MB,
                                        device.getMemoryMb())
                                .withValue(DevicesContract
                                        .Device
                                        .NICKNAME, device.getNickname())
                                .build();

                operations.add(deviceOperation);
            }
        }

        return operations;
    }
}