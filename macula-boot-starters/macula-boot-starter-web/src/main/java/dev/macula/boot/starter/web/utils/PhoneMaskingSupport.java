/*
 * Copyright (c) 2026 Macula
 *   macula.dev, China
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package dev.macula.boot.starter.web.utils;

import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.PhoneNumberUtil.PhoneNumberType;
import com.google.i18n.phonenumbers.Phonenumber.PhoneNumber;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 电话号码的离线解析和原位置掩码，仅供 SensitiveUtil 使用。
 *
 * @author Rain
 */
final class PhoneMaskingSupport {
    // 在任何正则或原位置扫描前限制输入，避免异常长文本触发非线性扫描。
    private static final int MAX_RAW_LENGTH = 256;
    private static final PhoneNumberUtil PHONE_UTIL = PhoneNumberUtil.getInstance();
    private static final Pattern EXTENSION = Pattern.compile(
        "[ \\t]*(?:ext\\.?|x|#|转|分机)[ \\t]*[0-9]+[ \\t]*$", Pattern.CASE_INSENSITIVE);
    private static final Pattern DOMESTIC_EXTENSION = Pattern.compile(
        "^[ \\t]*((?:(?:\\+86|0086)[ \\t-]*)?0?(?:10|2[0-9]|[3-9][0-9]{2})"
            + "-[0-9]{7,8})(-[0-9]+)[ \\t]*$");

    private PhoneMaskingSupport() {
    }

    static boolean isMobile(String raw) {
        ParsedPhone parsed = parse(raw);
        return parsed != null && parsed.type == PhoneNumberType.MOBILE;
    }

    static String mask(String raw, boolean mobile) {
        ParsedPhone parsed = parse(raw);
        if (parsed == null) {
            return String.valueOf(SensitiveUtil.ASTERISK_CHAR);
        }
        int start = parsed.countryDigits + parsed.trunkDigits;
        if (!mobile) {
            start += parsed.areaDigits;
        }
        int length = parsed.positions.size() - start;
        if (length <= 0) {
            return String.valueOf(SensitiveUtil.ASTERISK_CHAR);
        }
        int from;
        int to;
        if (length <= 3) {
            from = 0;
            to = length;
        } else if (length <= 7) {
            from = (length - 2) / 2;
            to = from + 2;
        } else {
            from = mobile ? 3 : 0;
            to = length - 4;
        }
        char[] masked = raw.toCharArray();
        for (int i = from; i < to; i++) {
            masked[parsed.positions.get(start + i)] = SensitiveUtil.ASTERISK_CHAR;
        }
        return new String(masked);
    }

    private static ParsedPhone parse(String raw) {
        if (raw == null || raw.isEmpty() || raw.length() > MAX_RAW_LENGTH) {
            return null;
        }
        int mainEnd = raw.length();
        Matcher extension = EXTENSION.matcher(raw);
        if (extension.find()) {
            mainEnd = extension.start();
        } else {
            Matcher domestic = DOMESTIC_EXTENSION.matcher(raw);
            if (domestic.matches()) {
                mainEnd = domestic.start(2);
            }
        }
        String main = raw.substring(0, mainEnd);
        if (!validMain(main)) {
            return null;
        }
        ParsedPhone result = new ParsedPhone();
        StringBuilder digits = new StringBuilder();
        for (int i = 0; i < main.length(); i++) {
            char ch = main.charAt(i);
            if (ch >= '0' && ch <= '9') {
                result.positions.add(i);
                digits.append(ch);
            }
        }
        if (digits.length() == 0) {
            return null;
        }
        String allDigits = digits.toString();
        boolean plus = main.trim().startsWith("+");
        boolean idd = !plus && allDigits.startsWith("00");
        int countryCode = 86;
        if (plus || idd) {
            int prefix = idd ? 2 : 0;
            countryCode = 0;
            for (int length = 1; length <= 3 && prefix + length <= allDigits.length(); length++) {
                int candidate = Integer.parseInt(allDigits.substring(prefix, prefix + length));
                if (allDigits.charAt(prefix) != '0' && PHONE_UTIL.getSupportedCallingCodes().contains(candidate)) {
                    countryCode = candidate;
                    result.countryDigits = prefix + length;
                    break;
                }
            }
            if (countryCode == 0) {
                return null;
            }
        }
        String national = allDigits.substring(result.countryDigits);
        if (national.isEmpty() || national.length() > 17) {
            return null;
        }
        PhoneNumber number = null;
        try {
            number = PHONE_UTIL.parse(plus || idd ? "+" + countryCode + national : national, "CN");
        } catch (NumberParseException ex) {
            // 极短号码也要脱敏；解析失败后仍使用原数字和明确前缀。
        }
        if (number != null) {
            if (number.getCountryCode() != countryCode) {
                return null;
            }
            String significant = PHONE_UTIL.getNationalSignificantNumber(number);
            result.type = PHONE_UTIL.getNumberType(number);
            if (result.type == PhoneNumberType.UNKNOWN) {
                // 短号及未知类型仍按原数字计数，不能被解析器的运营商前缀规则截短。
                result.trunkDigits = 0;
            } else if (national.equals(significant)) {
                result.trunkDigits = 0;
            } else if (national.equals("0" + significant)) {
                result.trunkDigits = 1;
            } else {
                // 无法可靠映射规范化号码与原字符时，不保留猜测出的数字。
                return null;
            }
            result.areaDigits = PHONE_UTIL.getLengthOfGeographicalAreaCode(number);
        }
        if (countryCode == 86) {
            preserveExplicitChineseArea(main, national, result);
        }
        if (result.countryDigits + result.trunkDigits + result.areaDigits >= result.positions.size()) {
            return null;
        }
        return result;
    }

    private static void preserveExplicitChineseArea(String main, String national, ParsedPhone result) {
        int trunk = national.startsWith("0") ? 1 : 0;
        // 未写国际码的国内座机必须带拨号前导零，避免把本地主号误当区号。
        if (trunk == 0 && result.countryDigits == 0) {
            return;
        }
        for (int length = 2; length <= 3; length++) {
            int end = trunk + length;
            if (end >= national.length()) {
                continue;
            }
            int lastAreaPosition = result.positions.get(result.countryDigits + end - 1);
            int firstSubscriberPosition = result.positions.get(result.countryDigits + end);
            String separator = main.substring(lastAreaPosition + 1, firstSubscriberPosition);
            if (separator.isEmpty()) {
                continue;
            }
            String area = national.substring(trunk, end);
            if (validChineseArea(area)) {
                result.trunkDigits = trunk;
                result.areaDigits = length;
                return;
            }
        }
    }

    private static boolean validChineseArea(String area) {
        for (String subscriber : new String[] {"2345678", "23456789"}) {
            try {
                PhoneNumber probe = PHONE_UTIL.parse("+86" + area + subscriber, "CN");
                PhoneNumberType type = PHONE_UTIL.getNumberType(probe);
                if (PHONE_UTIL.isValidNumberForRegion(probe, "CN")
                    && (type == PhoneNumberType.FIXED_LINE || type == PhoneNumberType.FIXED_LINE_OR_MOBILE)
                    && PHONE_UTIL.getLengthOfGeographicalAreaCode(probe) == area.length()
                    && PHONE_UTIL.getNationalSignificantNumber(probe).startsWith(area)) {
                    return true;
                }
            } catch (NumberParseException ex) {
                // 不保留不能由元数据确认的候选区号。
            }
        }
        return false;
    }

    private static boolean validMain(String main) {
        String trimmed = main.trim();
        if (trimmed.startsWith("-") || trimmed.endsWith("-") || trimmed.contains("()")
            || trimmed.matches(".*-[ \\t]*-.*")) {
            return false;
        }
        int depth = 0;
        boolean foundDigit = false;
        boolean foundPlus = false;
        for (int i = 0; i < main.length(); i++) {
            char ch = main.charAt(i);
            if (ch >= '0' && ch <= '9') {
                foundDigit = true;
            } else if (ch == '+') {
                if (foundPlus || foundDigit || depth != 0 || !main.substring(0, i).trim().isEmpty()) {
                    return false;
                }
                foundPlus = true;
            } else if (ch == '(') {
                if (++depth > 1) {
                    return false;
                }
            } else if (ch == ')') {
                if (--depth < 0) {
                    return false;
                }
            } else if (ch != ' ' && ch != '\t' && ch != '-') {
                return false;
            }
        }
        return foundDigit && depth == 0;
    }

    private static final class ParsedPhone {
        private final List<Integer> positions = new ArrayList<>();
        private int countryDigits;
        private int trunkDigits;
        private int areaDigits;
        private PhoneNumberType type = PhoneNumberType.UNKNOWN;
    }
}
