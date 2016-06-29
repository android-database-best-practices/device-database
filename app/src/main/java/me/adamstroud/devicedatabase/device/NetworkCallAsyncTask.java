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

import android.os.AsyncTask;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import me.adamstroud.devicedatabase.model.Device;
import me.adamstroud.devicedatabase.model.Manufacturer;

/**
 * Shows how to use the Android SDK to make a web API call.
 *
 * @author Adam Stroud &#60;<a href="mailto:adam.stroud@gmail.com">adam.stroud@gmail.com</a>&#62;
 */
public class NetworkCallAsyncTask
        extends AsyncTask<String, Void, List<Manufacturer>> {
    @Override
    protected List<Manufacturer> doInBackground(String... params) {
        HttpURLConnection connection = null;
        StringBuffer buffer = new StringBuffer();
        BufferedReader reader = null;
        List<Manufacturer> manufacturers = new ArrayList<>();

        try {
            connection =
                    (HttpURLConnection) new URL(params[0])
                            .openConnection();

            InputStream input =
                    new BufferedInputStream(connection.getInputStream());

            reader = new BufferedReader(new InputStreamReader(input));
            String line;

            while ((line = reader.readLine()) != null) {
                buffer.append(line);
            }

            if (!isCancelled()) {
                JSONObject response = new JSONObject(buffer.toString());

                JSONArray jsonManufacturers =
                        response.getJSONArray("manufacturers");

                for (int i = 0; i < jsonManufacturers.length(); i++) {
                    JSONObject jsonManufacturer =
                            jsonManufacturers.getJSONObject(i);

                    Manufacturer manufacturer = new Manufacturer();

                    manufacturer
                            .setShortName(jsonManufacturer
                                    .getString("short_name"));

                    manufacturer
                            .setLongName(jsonManufacturer
                                    .getString("long_name"));

                    JSONArray jsonDevices =
                            jsonManufacturer.getJSONArray("devices");

                    List<Device> devices = new ArrayList<>();

                    for (int j = 0; j < jsonDevices.length(); j++) {
                        JSONObject jsonDevice =
                                jsonDevices.getJSONObject(j);

                        Device device = new Device();

                        device.setDisplaySizeInches((float) jsonDevice
                                .getDouble("display_size_inches"));

                        device.setNickname(jsonDevice
                                .getString("nickname"));

                        device.setModel(jsonDevice.getString("model"));

                        devices.add(device);
                    }

                    manufacturer.setDevices(devices);
                    manufacturers.add(manufacturer);
                }
            }
        } catch (IOException | JSONException e) {
            // Log error
        } finally {
            if (connection != null) {
                connection.disconnect();
            }

            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    // do something meaningless
                }
            }
        }

        return manufacturers;
    }

    @Override
    protected void onPostExecute(List<Manufacturer> manufacturers) {
        super.onPostExecute(manufacturers);

        if (!isCancelled()) {
            // update display
        }
    }
}