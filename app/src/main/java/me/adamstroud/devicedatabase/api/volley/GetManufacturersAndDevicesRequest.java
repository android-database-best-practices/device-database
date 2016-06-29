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

package me.adamstroud.devicedatabase.api.volley;

import com.android.volley.Response.ErrorListener;
import com.android.volley.Response.Listener;

import java.util.List;

import me.adamstroud.devicedatabase.model.Manufacturer;

/**
 * Returns the manufacturer/device list.
 *
 * @author Adam Stroud &#60;<a href="mailto:adam.stroud@gmail.com">adam.stroud@gmail.com</a>&#62;
 */
public class GetManufacturersAndDevicesRequest
        extends JacksonRequest<GetManufacturersAndDevicesRequest.Response> {
    public GetManufacturersAndDevicesRequest(Object tag,
                                             Listener<Response> listener,
                                             ErrorListener errorListener) {
        super(Method.GET,
              "http://www.mocky.io/v2/570bbaf6110000b003d17e3a",
                Response.class,
                listener,
                errorListener);

        this.setTag(tag);
    }

    public static class Response {
        private List<Manufacturer> manufacturers;

        public List<Manufacturer> getManufacturers() {
            return manufacturers;
        }

        public void setManufacturers(List<Manufacturer> manufacturers) {
            this.manufacturers = manufacturers;
        }
    }
}
