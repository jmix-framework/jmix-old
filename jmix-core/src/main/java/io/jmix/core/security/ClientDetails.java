/*
 * Copyright 2019 Haulmont.
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

package io.jmix.core.security;

import java.util.TimeZone;

public class ClientDetails {

    public static final ClientDetails UNKNOWN = builder().build();

    private TimeZone timeZone;
    private String address;
    private String info;

    private ClientDetails() {
    }

    public static Builder builder() {
        return new Builder();
    }

    public TimeZone getTimeZone() {
        return timeZone;
    }

    public String getAddress() {
        return address;
    }

    public String getInfo() {
        return info;
    }

    public static class Builder {
        private ClientDetails obj;

        public Builder() {
            obj = new ClientDetails();
            obj.timeZone = TimeZone.getDefault();
        }

        public Builder timeZone(TimeZone timeZone) {
            obj.timeZone = timeZone;
            return this;
        }

        public Builder address(String address) {
            obj.address = address;
            return this;
        }

        public Builder info(String info) {
            obj.info = info;
            return this;
        }

        public ClientDetails build() {
            return obj;
        }
    }
}
