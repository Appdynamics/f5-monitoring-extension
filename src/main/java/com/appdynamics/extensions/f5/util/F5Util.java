package com.appdynamics.extensions.f5.util;

import com.appdynamics.extensions.PathResolver;
import com.singularity.ee.agent.systemagent.api.AManagedMonitor;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;

import java.io.File;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Florencio Sarmiento
 */
public class F5Util {

    public enum NetworkInterfaceStatus {
        UNKNOWN("unknown"),
        MEDIA_STATUS_UP("up"),
        MEDIA_STATUS_DOWN("down"),
        MEDIA_STATUS_DISABLED("disabled"),
        MEDIA_STATUS_UNINITIALIZED("uninit"),
        MEDIA_STATUS_LOOPBACK("loopback"),
        MEDIA_STATUS_UNPOPULATED("miss");

        private String value;

        NetworkInterfaceStatus(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }

        public static int getStatus(String status) {

            if (NetworkInterfaceStatus.MEDIA_STATUS_UP.getValue().equalsIgnoreCase(status)) {
                return MEDIA_STATUS_UP.ordinal();
            }

            if (NetworkInterfaceStatus.MEDIA_STATUS_DOWN.getValue().equalsIgnoreCase(status)) {
                return MEDIA_STATUS_DOWN.ordinal();
            }

            if (NetworkInterfaceStatus.MEDIA_STATUS_DISABLED.getValue().equalsIgnoreCase(status)) {
                return MEDIA_STATUS_DISABLED.ordinal();
            }

            if (NetworkInterfaceStatus.MEDIA_STATUS_UNINITIALIZED.getValue().equalsIgnoreCase(status)) {
                return MEDIA_STATUS_UNINITIALIZED.ordinal();
            }

            if (NetworkInterfaceStatus.MEDIA_STATUS_LOOPBACK.getValue().equalsIgnoreCase(status)) {
                return MEDIA_STATUS_LOOPBACK.ordinal();
            }

            if (NetworkInterfaceStatus.MEDIA_STATUS_UNPOPULATED.getValue().equalsIgnoreCase(status)) {
                return MEDIA_STATUS_UNPOPULATED.ordinal();
            }

            return UNKNOWN.ordinal();
        }
    }

    public enum PoolStatus {
        UNKNOWN(0),
        AVAILABLE_AND_ENABLED(1),
        OFFLINE_AND_ENABLED(2),
        AVAILABLE_BUT_DISABLED(3),
        OFFLINE_AND_DISABLED(4);

        private int value;

        PoolStatus(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }
    }

    public static Pattern createPattern(Set<String> rawPatterns) {
        Pattern pattern = null;

        if (rawPatterns != null && !rawPatterns.isEmpty()) {
            StringBuilder rawPatternsStringBuilder = new StringBuilder();
            int index = 0;

            for (String rawPattern : rawPatterns) {
                if (index > 0) {
                    rawPatternsStringBuilder.append("|");
                }

                rawPatternsStringBuilder.append(rawPattern);
                index++;
            }

            pattern = Pattern.compile(rawPatternsStringBuilder.toString());
        }

        return pattern;
    }

    public static String[] filterIncludes(String[] stringArrayToFilter, Pattern includePatterns) {
        if (ArrayUtils.isNotEmpty(stringArrayToFilter) && includePatterns != null) {
            List<String> filteredString = new ArrayList<String>();

            for (String stringToCheck : stringArrayToFilter) {
                Matcher matcher = includePatterns.matcher(stringToCheck);

                if (matcher.matches()) {
                    filteredString.add(stringToCheck);
                }
            }

            return filteredString.toArray(new String[filteredString.size()]);
        }

        return stringArrayToFilter;
    }

    public static boolean isMetricToMonitor(String metricName, Pattern excludePatterns) {
        return !isToMonitor(metricName, excludePatterns);
    }

    public static final boolean isToMonitor(String name, Pattern pattern) {
        if (name != null && pattern != null) {
            Matcher matcher = pattern.matcher(name);

            if (matcher.matches()) {
                return true;
            }
        }

        return false;
    }

    public static String changePathSeparator(String strToChange, String fromSeperator,
                                             String toSeperator, boolean insertSeparatorAtStartIfNotThere) {
        if (StringUtils.isNotBlank(strToChange)) {
            String updatedString = strToChange.replaceAll(fromSeperator, toSeperator);

            if (insertSeparatorAtStartIfNotThere) {
                updatedString = insertSeparatorAtStartIfNotThere(updatedString, toSeperator);
            }

            return updatedString;
        }

        return strToChange;
    }

    public static String insertSeparatorAtStartIfNotThere(String strToChange, String separator) {
        if (strToChange != null && !strToChange.startsWith(separator)) {
            return String.format("%s%s", separator, strToChange);
        }

        return strToChange;
    }

    public static PoolStatus convertToStatus(String availability, String enabled) {
        PoolStatus status;

        if ("available".equalsIgnoreCase(availability) &&
                "enabled".equalsIgnoreCase(enabled)) {
            status = PoolStatus.AVAILABLE_AND_ENABLED;

        } else if (!"available".equalsIgnoreCase(availability) &&
                "enabled".equalsIgnoreCase(enabled)) {
            status = PoolStatus.OFFLINE_AND_ENABLED;

        } else if ("available".equalsIgnoreCase(availability) &&

                !"enabled".equalsIgnoreCase(enabled)) {
            status = PoolStatus.AVAILABLE_BUT_DISABLED;

        } else if (!"available".equalsIgnoreCase(availability) &&
                !"enabled".equalsIgnoreCase(enabled)) {
            status = PoolStatus.OFFLINE_AND_DISABLED;

        } else {
            status = PoolStatus.UNKNOWN;
        }

        return status;
    }

    public static String resolvePath(String filename) {
        if (StringUtils.isBlank(filename)) {
            return "";
        }

        //for absolute paths
        if (new File(filename).exists()) {
            return filename;
        }

        //for relative paths
        File jarPath = PathResolver.resolveDirectory(AManagedMonitor.class);
        String configFileName = String.format("%s%s%s", jarPath, File.separator, filename);
        return configFileName;
    }

    public static String extractMemberName(String rawName, String delimiter) {
        if (rawName != null && delimiter != null && rawName.contains(delimiter)) {
            String[] splitWords = rawName.split(delimiter);
            return splitWords[splitWords.length - 1];
        }

        return rawName;
    }

    public static BigInteger convertValueToZeroIfNullOrNegative(BigInteger value) {
        if (value == null || value.compareTo(BigInteger.ZERO) < 0) {
            return BigInteger.ZERO;
        }

        return value;
    }

}
