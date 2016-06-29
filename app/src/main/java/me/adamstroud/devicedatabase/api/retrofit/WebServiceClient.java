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

package me.adamstroud.devicedatabase.api.retrofit;

import android.util.Log;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import me.adamstroud.devicedatabase.BuildConfig;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava.RxJavaCallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * The singleton for the web API client.
 *
 * @author Adam Stroud &#60;<a href="mailto:adam.stroud@gmail.com">adam.stroud@gmail.com</a>&#62;
 */
public class WebServiceClient {
    private static final String TAG =
            WebServiceClient.class.getSimpleName();

    private static WebServiceClient instance = new WebServiceClient();

    private final DeviceService service;

    public static WebServiceClient getInstance() {
        return instance;
    }

    private WebServiceClient() {
        final Gson gson = new GsonBuilder()
                .setFieldNamingPolicy(FieldNamingPolicy
                        .LOWER_CASE_WITH_UNDERSCORES)
                .create();

        Retrofit.Builder retrofitBuilder = new Retrofit.Builder()
                .baseUrl("http://www.mocky.io")
                .addCallAdapterFactory(RxJavaCallAdapterFactory.create())
                .addConverterFactory(GsonConverterFactory.create(gson));

        if (BuildConfig.DEBUG) {
            final HttpLoggingInterceptor loggingInterceptor =
                    new HttpLoggingInterceptor(new HttpLoggingInterceptor
                            .Logger() {
                @Override
                public void log(String message) {
                    Log.d(TAG, message);
                }
            });

            retrofitBuilder.callFactory(new OkHttpClient
                    .Builder()
                    .addNetworkInterceptor(loggingInterceptor)
                    .build());

            loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
        }

        service = retrofitBuilder.build().create(DeviceService.class);
    }

    public DeviceService getService() {
        return service;
    }
}
