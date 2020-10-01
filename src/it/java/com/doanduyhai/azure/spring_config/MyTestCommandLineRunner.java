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

import static java.util.stream.Collectors.joining;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class MyTestCommandLineRunner implements CommandLineRunner {
    private static final Logger logger = LoggerFactory.getLogger(MyTestCommandLineRunner.class);

    @Autowired
    private MyTestAppConf appConf;

    @Override
    public void run(String... args) throws Exception {
        logger.info("Val1 = '{}'", appConf.getVal1());
        logger.info("Val2 = '{}'", appConf.getVal2());
        logger.info("Val3 = '{}'", appConf.getVal3());

        List<Integer> myList = appConf.getMyList();
        if (myList != null) {
            logger.info("My List ='{}", myList.stream().map(Object::toString).collect(joining(",")));
        } else {
            logger.info("My List = null");
        }

        Map<String, Double> myMap = appConf.getMyMap();
        if (myMap != null) {
            logger.info("My Map = '{}'", myMap.entrySet().stream().map(entry -> entry.getKey() + ":" + entry.getValue()).collect(joining(",")));
        } else {
            logger.info("My Map = null");
        }

        Map<String, MyTestAppConf.Oauth2Config> myOauth2Config = appConf.getMyOauth2Config();

        if (myOauth2Config != null) {
            logger.info("My Oauth2Config = '{}'", myOauth2Config.entrySet().stream().map(entry -> entry.getKey() + ":" + entry.getValue()).collect(joining(",")));
        } else {
            logger.info("My Oauth2Config = null");
        }


    }
}
