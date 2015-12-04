package com.appdynamics.extensions.f5.collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

import com.appdynamics.extensions.f5.config.F5;
import com.appdynamics.extensions.f5.config.MetricsFilter;
import iControl.CommonIPPortDefinition;
import iControl.CommonStatistic;
import iControl.CommonStatisticType;
import iControl.CommonULong64;
import iControl.Interfaces;
import iControl.LocalLBAvailabilityStatus;
import iControl.LocalLBEnabledStatus;
import iControl.LocalLBNodeAddressV2BindingStub;
import iControl.LocalLBObjectStatus;
import iControl.LocalLBPoolBindingStub;
import iControl.LocalLBPoolMemberBindingStub;
import iControl.LocalLBPoolMemberMemberObjectStatus;
import iControl.LocalLBPoolMemberMemberStatisticEntry;
import iControl.LocalLBPoolMemberMemberStatistics;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.math.BigInteger;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;

@RunWith(MockitoJUnitRunner.class)
public class PreVersion11PoolMemberMetricsCollectorTest {

    private PreVersion11PoolMemberMetricsCollector classUnderTest;

    @Mock
    private Interfaces mockIcontrolInterfaces;

    @Mock
    private LocalLBPoolBindingStub mockLocalLBPoolStub;

    @Mock
    private LocalLBNodeAddressV2BindingStub mockNodeAddressV2Stub;

    @Mock
    private LocalLBPoolMemberBindingStub mockLocalLBPoolMemberStub;

    @Mock
    private F5 mockF5;

    @Mock
    private MetricsFilter mockMetricsFilter;

    @Before
    public void setUp() throws Exception {
        when(mockF5.getDisplayName()).thenReturn("TestF5");
        when(mockIcontrolInterfaces.getLocalLBPoolMember()).thenReturn(mockLocalLBPoolMemberStub);

        Set<String> excludes = new HashSet<String>();
        excludes.add(".*Total.*");
        when(mockMetricsFilter.getPoolMetricExcludes()).thenReturn(excludes);

        LocalLBPoolMemberMemberStatistics[] testStats = getTestStatistics();
        when(mockLocalLBPoolMemberStub.get_all_statistics(any(String[].class))).thenReturn(testStats);

        LocalLBPoolMemberMemberObjectStatus[][] testStatuses = getTestStatuses();
        when(mockLocalLBPoolMemberStub.get_object_status(any(String[].class))).thenReturn(testStatuses);
    }

    @Test
    public void testNoPoolMembersIncluded() throws Exception {
        classUnderTest = new PreVersion11PoolMemberMetricsCollector(mockF5.getPoolMemberIncludes(),
                mockMetricsFilter.getPoolMetricExcludes(), mockIcontrolInterfaces);

        String poolMetricPrefix = "TestF5|Pools";
        String[] pools = getTestPools();

        Map<String, BigInteger> result = classUnderTest.collectMemberMetrics(poolMetricPrefix, pools);
        assertEquals(0, result.size());
    }

    @Test
    public void testAllPoolMembersIncluded() throws Exception {
        Set<String> testIncludes = new HashSet<String>();
        testIncludes.add(".*");
        when(mockF5.getPoolMemberIncludes()).thenReturn(testIncludes);

        classUnderTest = new PreVersion11PoolMemberMetricsCollector(mockF5.getPoolMemberIncludes(),
                mockMetricsFilter.getPoolMetricExcludes(), mockIcontrolInterfaces);

        String poolMetricPrefix = "TestF5|Pools";
        String[] pools = getTestPools();

        Map<String, BigInteger> result = classUnderTest.collectMemberMetrics(poolMetricPrefix, pools);
        assertEquals(8, result.size());

        assertTrue(result.containsKey("TestF5|Pools|Common|devcontr7|10.10.10.1|8080|STATISTIC_PVA_CLIENT_SIDE_BYTES_IN"));
        assertTrue(result.containsKey("TestF5|Pools|Common|devcontr7|10.10.10.1|8080|STATISTIC_CONNQUEUE_AGE_MOVING_AVG"));
        assertTrue(result.containsKey("TestF5|Pools|Common|devcontr7|10.10.10.1|8080|STATISTIC_SERVER_SIDE_BYTES_IN"));
        assertTrue(result.containsKey("TestF5|Pools|Common|devcontr7|10.10.10.1|8080|STATUS"));

        assertTrue(result.containsKey("TestF5|Pools|Common|devcontr7|10.10.10.2|8090|STATISTIC_PVA_CLIENT_SIDE_BYTES_IN"));
        assertTrue(result.containsKey("TestF5|Pools|Common|devcontr7|10.10.10.2|8090|STATISTIC_CONNQUEUE_AGE_MOVING_AVG"));
        assertTrue(result.containsKey("TestF5|Pools|Common|devcontr7|10.10.10.2|8090|STATISTIC_SERVER_SIDE_BYTES_IN"));
        assertTrue(result.containsKey("TestF5|Pools|Common|devcontr7|10.10.10.2|8090|STATUS"));
    }

    @Test
    public void testExcludeMetrics() throws Exception {
        Set<String> testIncludes = new HashSet<String>();
        testIncludes.add(".*");
        when(mockF5.getPoolMemberIncludes()).thenReturn(testIncludes);

        Set<String> testMetricExcludes = new HashSet<String>();
        testMetricExcludes.add(".*STATISTIC.*");
        testMetricExcludes.add(".*Total.*");
        when(mockMetricsFilter.getPoolMetricExcludes()).thenReturn(testMetricExcludes);

        classUnderTest = new PreVersion11PoolMemberMetricsCollector(mockF5.getPoolMemberIncludes(),
                mockMetricsFilter.getPoolMetricExcludes(), mockIcontrolInterfaces);

        String poolMetricPrefix = "TestF5|Pools";
        String[] pools = getTestPools();

        Map<String, BigInteger> result = classUnderTest.collectMemberMetrics(poolMetricPrefix, pools);
        assertEquals(2, result.size());

        assertTrue(result.containsKey("TestF5|Pools|Common|devcontr7|10.10.10.1|8080|STATUS"));
        assertTrue(result.containsKey("TestF5|Pools|Common|devcontr7|10.10.10.2|8090|STATUS"));

        assertFalse(result.containsKey("TestF5|Pools|Common|devcontr7|10.10.10.1|8080|STATISTIC_PVA_CLIENT_SIDE_BYTES_IN"));
        assertFalse(result.containsKey("TestF5|Pools|Common|devcontr7|10.10.10.1|8080|STATISTIC_CONNQUEUE_AGE_MOVING_AVG"));
        assertFalse(result.containsKey("TestF5|Pools|Common|devcontr7|10.10.10.1|8080|STATISTIC_SERVER_SIDE_BYTES_IN"));
        assertFalse(result.containsKey("TestF5|Pools|Common|devcontr7|10.10.10.2|8090|STATISTIC_PVA_CLIENT_SIDE_BYTES_IN"));
        assertFalse(result.containsKey("TestF5|Pools|Common|devcontr7|10.10.10.2|8090|STATISTIC_CONNQUEUE_AGE_MOVING_AVG"));
        assertFalse(result.containsKey("TestF5|Pools|Common|devcontr7|10.10.10.2|8090|STATISTIC_SERVER_SIDE_BYTES_IN"));
    }

    private LocalLBPoolMemberMemberStatistics[] getTestStatistics() {
        String[] testPools = getTestPools();

        LocalLBPoolMemberMemberStatistics[] statsArray =
                new LocalLBPoolMemberMemberStatistics[testPools.length];

        CommonStatisticType[] statisticTypes = getTestStatisticTypes();

        for (int poolIndex = 0; poolIndex < testPools.length; poolIndex++) {
            LocalLBPoolMemberMemberStatistics stats = new LocalLBPoolMemberMemberStatistics();
            statsArray[poolIndex] = stats;

            CommonIPPortDefinition[] testMembers = getTestMembers();
            LocalLBPoolMemberMemberStatisticEntry[] memberEntries =
                    new LocalLBPoolMemberMemberStatisticEntry[testMembers.length];
            stats.setStatistics(memberEntries);

            for (int memberIndex = 0; memberIndex < testMembers.length; memberIndex++) {
                LocalLBPoolMemberMemberStatisticEntry memberEntry = new LocalLBPoolMemberMemberStatisticEntry();
                memberEntry.setMember(testMembers[memberIndex]);

                CommonStatistic[] commonStats = new CommonStatistic[statisticTypes.length];

                for (int index = 0; index < statisticTypes.length; index++) {
                    CommonStatistic commonStat = new CommonStatistic();
                    commonStat.setType(statisticTypes[index]);
                    commonStat.setValue(getTestValue());
                    commonStats[index] = commonStat;
                }

                memberEntry.setStatistics(commonStats);
                memberEntries[memberIndex] = memberEntry;
            }

        }

        return statsArray;
    }

    private LocalLBPoolMemberMemberObjectStatus[][] getTestStatuses() {
        String[] testPools = getTestPools();

        LocalLBPoolMemberMemberObjectStatus[][] testStatuses =
                new LocalLBPoolMemberMemberObjectStatus[testPools.length][];

        for (int poolIndex = 0; poolIndex < testPools.length; poolIndex++) {
            CommonIPPortDefinition[] testMembers = getTestMembers();

            LocalLBPoolMemberMemberObjectStatus[] memberStatuses =
                    new LocalLBPoolMemberMemberObjectStatus[testMembers.length];
            testStatuses[poolIndex] = memberStatuses;

            for (int memberIndex = 0; memberIndex < testMembers.length; memberIndex++) {
                LocalLBPoolMemberMemberObjectStatus memberStatus = new LocalLBPoolMemberMemberObjectStatus();
                memberStatus.setMember(testMembers[memberIndex]);

                LocalLBObjectStatus status = new LocalLBObjectStatus();
                status.setAvailability_status(LocalLBAvailabilityStatus.AVAILABILITY_STATUS_GREEN);
                status.setEnabled_status(LocalLBEnabledStatus.ENABLED_STATUS_ENABLED);
                memberStatus.setObject_status(status);

                memberStatuses[memberIndex] = memberStatus;
            }
        }

        return testStatuses;
    }

    private String[] getTestPools() {
        return new String[]{"/Common/devcontr7"};
    }

    private CommonIPPortDefinition[] getTestMembers() {
        CommonIPPortDefinition[] members = new CommonIPPortDefinition[2];

        CommonIPPortDefinition member1 = new CommonIPPortDefinition();
        member1.setAddress("10.10.10.1");
        member1.setPort(8080);
        members[0] = member1;

        CommonIPPortDefinition member2 = new CommonIPPortDefinition();
        member2.setAddress("10.10.10.2");
        member2.setPort(8090);
        members[1] = member2;

        return members;
    }

    private CommonStatisticType[] getTestStatisticTypes() {
        CommonStatisticType[] testMetricTypes = {
                CommonStatisticType.STATISTIC_PVA_CLIENT_SIDE_BYTES_IN,
                CommonStatisticType.STATISTIC_CONNQUEUE_AGE_MOVING_AVG,
                CommonStatisticType.STATISTIC_SERVER_SIDE_BYTES_IN};

        return testMetricTypes;
    }

    private CommonULong64 getTestValue() {
        CommonULong64 testValue = new CommonULong64();
        testValue.setHigh(new Random(100).nextLong());
        testValue.setHigh(new Random(10).nextLong());
        return testValue;
    }
}
