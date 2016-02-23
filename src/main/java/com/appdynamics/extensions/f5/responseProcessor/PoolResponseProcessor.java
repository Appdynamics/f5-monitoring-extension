package com.appdynamics.extensions.f5.responseProcessor;

import com.appdynamics.extensions.f5.NoDataException;
import com.appdynamics.extensions.f5.models.DiskStats;
import com.appdynamics.extensions.f5.models.HostCPUMemoryStats;
import com.appdynamics.extensions.f5.models.StatEntry;
import com.appdynamics.extensions.f5.models.Stats;
import com.appdynamics.extensions.f5.util.F5Util;
import org.json.JSONArray;
import org.json.JSONObject;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * @author Satish Muddam
 */
public class PoolResponseProcessor {

    public static Stats processPoolStatsResponse(String poolStatsResponse, Pattern poolIncludesPattern, KeyField keyField) {
        JSONObject object = new JSONObject(poolStatsResponse);

        Stats stats = new Stats();

        object = object.getJSONObject("entries");

        if (object == null) {
            throw new NoDataException("No metric data found");
        }

        Set<String> entryKeys = object.keySet();
        String keyName = null;
        for (String entry : entryKeys) {

            JSONObject entryObject = object.getJSONObject(entry);

            JSONObject statsObject = entryObject.getJSONObject("nestedStats").getJSONObject("entries");

            keyName = getKeyFieldName(statsObject, keyField);

            if (F5Util.isToMonitor(keyName, poolIncludesPattern)) {
                Set<String> statKeys = statsObject.keySet();
                for (String statKey : statKeys) {
                    StatEntry statEntry = new StatEntry();
                    statEntry.setName(statKey);
                    setValue(statsObject.getJSONObject(statKey), statEntry);
                    stats.addStat(keyName, statEntry);
                }
            }
        }

        return stats;
    }

    private static String getKeyFieldName(JSONObject statsObject, KeyField keyField) {

        Field[] fieldNames = keyField.getFieldNames();

        String keyName = "";
        for (Field field : fieldNames) {

            if (keyName != null && keyName.length() > 0) {
                keyName += keyField.getFieldSeparator();
            }

            JSONObject jsonObject = statsObject.getJSONObject(field.getFieldName());
            Object value = getValue(jsonObject);
            keyName += value;
        }

        return keyName;
    }

    public static Map<String, BigInteger> aggregateStatsResponse(String poolStatsResponse) {
        JSONObject object = new JSONObject(poolStatsResponse);

        Map<String, BigInteger> aggregatedStats = new HashMap<String, BigInteger>();

        object = object.getJSONObject("entries");

        Set<String> entryKeys = object.keySet();
        for (String entry : entryKeys) {

            JSONObject entryObject = object.getJSONObject(entry);

            JSONObject statsObject = entryObject.getJSONObject("nestedStats").getJSONObject("entries");

            Set<String> statKeys = statsObject.keySet();
            for (String statKey : statKeys) {

                BigInteger value = aggregatedStats.get(statKey);
                Object jsonValue = getValue(statsObject.getJSONObject(statKey));

                if (!(jsonValue instanceof Number)) { //Ignore Non-numeric data
                    continue;
                }

                if (value == null) {
                    value = new BigInteger(String.valueOf(jsonValue));
                } else {
                    value = value.add(new BigInteger(String.valueOf(jsonValue)));
                }
                aggregatedStats.put(statKey, value);
            }
        }
        return aggregatedStats;
    }

    public PoolResponseProcessor() {
    }

    public static List<HostCPUMemoryStats> parseHostInfoResponse(String hostInfoResponse) {
        JSONObject object = new JSONObject(hostInfoResponse);

        object = object.getJSONObject("entries");

        List<HostCPUMemoryStats> cpuStatsList = new ArrayList<HostCPUMemoryStats>();

        Set<String> entryKeys = object.keySet();
        for (String entry : entryKeys) {

            JSONObject entryObject = object.getJSONObject(entry);

            JSONObject cpuInfoObject = entryObject.getJSONObject("nestedStats").getJSONObject("entries");
            Set<String> strings = cpuInfoObject.keySet();

            HostCPUMemoryStats hostCPUMemoryStatsStats = new HostCPUMemoryStats();


            for (String key : strings) {
                if ("hostId".equalsIgnoreCase(key)) {
                    hostCPUMemoryStatsStats.setHostId(String.valueOf(getValue(cpuInfoObject.getJSONObject(key))));
                } else if (key.endsWith("cpuInfo")) {
                    parseCPUInfo(cpuInfoObject.getJSONObject(key), hostCPUMemoryStatsStats);
                } else if ("cpuCount".equalsIgnoreCase(key)) {
                    hostCPUMemoryStatsStats.setCpuCount(cpuInfoObject.getJSONObject(key).getInt("value"));
                } else if ("memoryTotal".equalsIgnoreCase(key)) {
                    hostCPUMemoryStatsStats.setMemoryTotal(cpuInfoObject.getJSONObject(key).getBigInteger("value"));
                } else if ("memoryUsed".equalsIgnoreCase(key)) {
                    hostCPUMemoryStatsStats.setMemoryUsed(cpuInfoObject.getJSONObject(key).getBigInteger("value"));
                }
            }

            cpuStatsList.add(hostCPUMemoryStatsStats);
        }

        return cpuStatsList;
    }

    private static void parseCPUInfo(JSONObject jsonObject, HostCPUMemoryStats hostCPUMemoryStatsStats) {
        JSONObject cpuInfoObject = jsonObject.getJSONObject("nestedStats").getJSONObject("entries");
        Set<String> strings = cpuInfoObject.keySet();

        BigInteger oneMinAvgIdle = BigInteger.ZERO;
        for (String cpuInfoKey : strings) {

            JSONObject statsJsonObject = cpuInfoObject.getJSONObject(cpuInfoKey).getJSONObject("nestedStats").getJSONObject("entries");
            oneMinAvgIdle = oneMinAvgIdle.add(statsJsonObject.getJSONObject("oneMinAvgIdle").getBigInteger("value"));
        }
        hostCPUMemoryStatsStats.setOneMinAvgIdle(oneMinAvgIdle);

    }

    public static List<DiskStats> parseDisksResponse(String disksResponse) {
        JSONObject object = new JSONObject(disksResponse);

        List<DiskStats> diskStatses = new ArrayList<DiskStats>();
        JSONArray diskItems = object.getJSONArray("items");

        for (int i = 0; i < diskItems.length(); i++) {
            JSONObject diskJson = diskItems.getJSONObject(i);

            DiskStats diskStats = new DiskStats();
            diskStats.setName(diskJson.getString("name"));
            diskStats.setFree(diskJson.getBigInteger("vgFree"));
            diskStats.setInUse(diskJson.getBigInteger("vgInUse"));
            diskStats.setReserved(diskJson.getBigInteger("vgReserved"));
            diskStats.setSize(diskJson.getBigInteger("size"));

            diskStatses.add(diskStats);
        }

        return diskStatses;
    }

    private static void setValue(JSONObject jsonObject, StatEntry statEntry) {

        Object value = getValue(jsonObject);

        if (value instanceof Number) {
            statEntry.setType(StatEntry.Type.NUMERIC);
        } else {
            statEntry.setType(StatEntry.Type.STRING);
        }
        statEntry.setValue(value.toString());

    }

    private static Object getValue(JSONObject jsonObject) {
        Object value = null;
        try {
            value = jsonObject.get("value");
        } catch (org.json.JSONException e) {
            value = jsonObject.get("description");
        }
        return value;
    }
}
