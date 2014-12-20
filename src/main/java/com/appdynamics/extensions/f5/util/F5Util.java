package com.appdynamics.extensions.f5.util;

import iControl.CommonULong64;
import iControl.LocalLBAvailabilityStatus;
import iControl.LocalLBEnabledStatus;

import java.io.File;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;

import com.appdynamics.extensions.PathResolver;
import com.singularity.ee.agent.systemagent.api.AManagedMonitor;

/**
 * @author Florencio Sarmiento
 *
 */
public class F5Util {
	
	public static enum NetworkInterfaceStatus {
		UNKNOWN (0),
		MEDIA_STATUS_UP (1),
		MEDIA_STATUS_DOWN (2),
		MEDIA_STATUS_DISABLED (3),
		MEDIA_STATUS_UNINITIALIZED (4),
		MEDIA_STATUS_LOOPBACK (5),
		MEDIA_STATUS_UNPOPULATED (6);
		
		private int value;

		private NetworkInterfaceStatus(int value) {
			this.value = value;
		}
		
		public int getValue() {
			return value;
		}
		
		public static int getValue(String status) {
			NetworkInterfaceStatus netStatus = valueOf(status);
			if (netStatus != null) {
				return netStatus.value;
			} 
			
			return UNKNOWN.value;
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
			
			if (insertSeparatorAtStartIfNotThere && !updatedString.startsWith(toSeperator)) {
				updatedString = String.format("%s%s", toSeperator, strToChange);
			}
			
			return updatedString;
		}
		
		return strToChange;
	}
	
	public static BigInteger convertValue(CommonULong64 value) {
		if (value != null) {
			long high = value.getHigh(); 
			long low = value.getLow(); 
			Double retVal; 
			
			Double rollOver = new Double((double)0x7fffffff); 
			rollOver = new Double(rollOver.doubleValue() + 1.0); 
	
			if(high >= 0) {
				retVal = new Double((high << 32 & 0xffff0000)); 
			} else {
				retVal = new Double(((high & 0x7fffffff) << 32) + (0x80000000 << 32)); 
			}

			if(low >= 0) {
				retVal = new Double(retVal.doubleValue() + (double)low); 
			} else {
				retVal = new Double(retVal.doubleValue() + (double)((low & 0x7fffffff)) + rollOver.doubleValue()); 
			}
			
			return BigInteger.valueOf(retVal.longValue());
		}
		
		return BigInteger.ZERO;
	}
	
	public static int convertToStatus(LocalLBAvailabilityStatus availability, LocalLBEnabledStatus enabled) {
		int status;

		if(availability.getValue().contains("GREEN") && 
				enabled.getValue().contains("STATUS_ENABLED")) {
			status = 1; // Available (Enabled)
			
		} else if(availability.getValue().contains("RED") && 
				enabled.getValue().contains("STATUS_ENABLED")) {
			status = 2; // Offline (Enabled)
			
		} else if(availability.getValue().contains("GREEN") && 
				enabled.getValue().contains("STATUS_DISABLED")) {
			status = 3; // Available (Disabled)
			
		} else if(availability.getValue().contains("RED") && 
				enabled.getValue().contains("STATUS_DISABLED")) {
			status = 4; // Offline (Disabled)
			
		} else {
			status = 0; // UNKNOWN
		}
		
		return status;
	}
	
    public static String resolvePath(String filename) {
        if(StringUtils.isBlank(filename)){
            return "";
        }
        
        //for absolute paths
        if(new File(filename).exists()){
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
