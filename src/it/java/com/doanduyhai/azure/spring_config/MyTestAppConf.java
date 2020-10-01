/*
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
package com.doanduyhai.azure.spring_config;

import java.util.List;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties("app")
public class MyTestAppConf {
    private String val1;
    private String val2;
    private String val3;
    private List<Integer> myList;
    private Map<String, Double> myMap;
    private Map<String, Oauth2Config> myOauth2Config;

    public MyTestAppConf() {
    }

    public String getVal1() {
        return this.val1;
    }

    public String getVal2() {
        return this.val2;
    }

    public String getVal3() {
        return this.val3;
    }

    public List<Integer> getMyList() {
        return this.myList;
    }

    public Map<String, Double> getMyMap() {
        return this.myMap;
    }

    public void setVal1(String val1) {
        this.val1 = val1;
    }

    public void setVal2(String val2) {
        this.val2 = val2;
    }

    public void setVal3(String val3) {
        this.val3 = val3;
    }

    public void setMyList(List<Integer> myList) {
        this.myList = myList;
    }

    public void setMyMap(Map<String, Double> myMap) {
        this.myMap = myMap;
    }

    public Map<String, Oauth2Config> getMyOauth2Config() {
        return myOauth2Config;
    }

    public void setMyOauth2Config(Map<String, Oauth2Config> myOauth2Config) {
        this.myOauth2Config = myOauth2Config;
    }

    public static class Oauth2Config {
        private String oauthEndpoint;
        private String clientId;
        private String clientKey;

        public String getOauthEndpoint() {
            return oauthEndpoint;
        }

        public void setOauthEndpoint(String oauthEndpoint) {
            this.oauthEndpoint = oauthEndpoint;
        }

        public String getClientId() {
            return clientId;
        }

        public void setClientId(String clientId) {
            this.clientId = clientId;
        }

        public String getClientKey() {
            return clientKey;
        }

        public void setClientKey(String clientKey) {
            this.clientKey = clientKey;
        }

        @Override
        public String toString() {
            return String.format("[oauthEndpoint: %s, clientId: %s, clientKey: %s]", oauthEndpoint, clientId, clientKey);
        }
    }
}
