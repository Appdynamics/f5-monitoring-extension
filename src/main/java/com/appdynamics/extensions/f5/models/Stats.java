package com.appdynamics.extensions.f5.models;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Satish Muddam
 */
public class Stats extends Model {
    private Map<String, List<StatEntry>> poolStats;

    public Map<String, List<StatEntry>> getPoolStats() {
        return poolStats;
    }

    public void addStat(String poolName, StatEntry statEntry) {
        if (poolStats == null) {
            poolStats = new HashMap<String, List<StatEntry>>();
        }
        List<StatEntry> statEntries = poolStats.get(poolName);
        if (statEntries == null) {
            statEntries = new ArrayList<StatEntry>();
            poolStats.put(poolName, statEntries);
        }
        statEntries.add(statEntry);
    }
}
