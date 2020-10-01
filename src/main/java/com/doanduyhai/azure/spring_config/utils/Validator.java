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
package com.doanduyhai.azure.spring_config.utils;

import static java.lang.String.format;

import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

/**
 * Generic validation class for arguments and properties
 */
public class Validator {

    public static final String UUID_PATTERN_STRING = "[a-fA-F0-9]{8}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{12}";
    private static final Pattern UUID_PATTERN = Pattern.compile(UUID_PATTERN_STRING);

    /**
     * Validate that input is not null
     * @param input input object
     * @param label the label for the input
     * @throws IllegalArgumentException if input is null
     */
    public static void validateNotNull(Object input, String label) {
        if (input == null) {
            throw new IllegalArgumentException(format("%s should not be null", label));
        }
    }

    /**
     * Validate that input is not blank (not null and not empty)
     * @param input input string value
     * @param label the label for the input
     * @throws IllegalArgumentException if input is null or blank
     */
    public static void validateNotBlank(String input, String label) {
        validateNotNull(input, label);
        if (StringUtils.isBlank(input)) {
            throw new IllegalArgumentException(format("%s should not be blank", label));
        }
    }

    /**
     * Validate that input is not blank (not null and not empty) and matches given pattern
     * @param input input string value
     * @param pattern regexp pattern to match
     * @param label the label for the input
     * @throws IllegalArgumentException if input is null or blank or does not match given pattern
     */
    public static void validatePattern(String input, Pattern pattern, String label) {
        validateNotBlank(input, label);
        Matcher matcher = pattern.matcher(input);
        if (!matcher.matches()) {
            throw new IllegalArgumentException(format("'%s' should match pattern '%s'", label, pattern.pattern()));
        }
    }

    /**
     * Validate true
     * @param input boolean status to verify
     * @param errorMsg error message
     * @return
     */
    public static void validateTrue(boolean input, String errorMsg) {
        if (!input) {
            throw new IllegalArgumentException(errorMsg);
        }
    }


    /**
     * Validate false
     * @param input boolean status to verify
     * @param errorMsg error message
     * @return
     */
    public static void validateFalse(boolean input, String errorMsg) {
        if (input) {
            throw new IllegalArgumentException(errorMsg);
        }
    }

    /**
     * Validate that the given string input corresponds to an enum value
     * @param name input enum name
     * @param values all available enum instances
     */
    public static <T extends Enum<T>> void validateEnumName(String name, T[] values) {
        validateTrue(values.length > 0, "");
        boolean foundMatchingInstance = Arrays.stream(values)
                .anyMatch(instance -> instance.name().equals(name));
        if (!foundMatchingInstance) {
            T value = values[0];
            throw new IllegalArgumentException(format("Cannot instantiate an enum of type '%s' whose name is '%s'",
                    value.getClass().getName(), name));
        }
    }

    /**
     * Validate whether the input string is a valid UUID
     * @param input string
     * @return whether the input string is a valid UUID
     */
    public static void validateUUID(String input, String inputLabel) {
        if (StringUtils.isEmpty(input)) {
            return;
        }

        Matcher matcher = UUID_PATTERN.matcher(input);
        if(!matcher.matches()) {
            throw new IllegalArgumentException(format("The value '%s' should be a valid UUID", inputLabel));
        }
    }
}

