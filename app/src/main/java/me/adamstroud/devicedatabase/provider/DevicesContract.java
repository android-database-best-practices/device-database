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

import android.content.ContentResolver;
import android.net.Uri;
import android.provider.BaseColumns;

import me.adamstroud.devicedatabase.BuildConfig;

/**
 * Defines the API for {@link DevicesProvider}.
 *
 * @author Adam Stroud &#60;<a href="mailto:adam.stroud@gmail.com">adam.stroud@gmail.com</a>&#62;
 */
public final class DevicesContract {
    public static final String AUTHORITY =
            String.format("%s.provider", BuildConfig.APPLICATION_ID);

    public static final Uri AUTHORITY_URI = new Uri.Builder()
            .scheme(ContentResolver.SCHEME_CONTENT)
            .authority(AUTHORITY)
            .build();

    public interface Device extends BaseColumns {
        /* default */ static final String PATH = "device";
        public static final String MODEL = "model";
        public static final String NICKNAME = "nickname";
        public static final String MEMORY_MB = "memory_mb";

        public static final String DISPLAY_SIZE_INCHES =
                "display_size_inches";

        public static final String MANUFACTURER_ID = "manufacturer_id";

        public static final Uri CONTENT_URI =
                Uri.withAppendedPath(AUTHORITY_URI, PATH);
    }

    public interface Manufacturer extends BaseColumns {
        /* default */ static final String PATH = "manufacturer";
        public static final String SHORT_NAME = "short_name";
        public static final String LONG_NAME = "long_name";

        public static final Uri CONTENT_URI =
                Uri.withAppendedPath(AUTHORITY_URI, PATH);
    }

    public interface DeviceManufacturer extends Device, Manufacturer {
        /* default */ static final String PATH = "device-manufacturer";
        public static final String DEVICE_ID = "device_id";
        public static final String MANUFACTURER_ID = "manufacturer_id";

        public static final Uri CONTENT_URI =
                Uri.withAppendedPath(AUTHORITY_URI, PATH);
    }
}
