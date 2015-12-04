package com.appdynamics.extensions.f5.collectors;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.appdynamics.extensions.f5.F5Constants;
import com.appdynamics.extensions.f5.F5Monitor;
import com.appdynamics.extensions.f5.config.F5;
import com.appdynamics.extensions.f5.config.MetricsFilter;
import com.singularity.ee.agent.systemagent.api.MetricWriter;
import iControl.CommonStatistic;
import iControl.CommonStatisticType;
import iControl.CommonULong64;
import iControl.CommonVirtualServerDefinition;
import iControl.Interfaces;
import iControl.LocalLBVirtualServerBindingStub;
import iControl.LocalLBVirtualServerVirtualServerStatisticEntry;
import iControl.LocalLBVirtualServerVirtualServerStatistics;
import iControl.ManagementPartitionAuthZPartition;
import iControl.ManagementPartitionBindingStub;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

@RunWith(MockitoJUnitRunner.class)
public class VirtualServerMetricsCollectorTest {

    private VirtualServerMetricsCollector classUnderTest;

    @Mock
    private Interfaces mockIcontrolInterfaces;

    @Mock
    private LocalLBVirtualServerBindingStub mockVirtualServerSub;

    @Mock
    private F5 mockF5;

    @Mock
    private MetricsFilter mockMetricsFilter;

    @Mock
    private ManagementPartitionBindingStub mockManagementPartitionBindingStub;

    @Mock
    private ManagementPartitionAuthZPartition mockManagementPartitionAuthZPartition;

    @Mock
    private F5Monitor monitor;

    @Mock
    private MetricWriter metricWriter;

    private String metricPrefix = F5Constants.DEFAULT_METRIC_PATH;

    @Before
    public void setUp() throws Exception {
        when(mockF5.getDisplayName()).thenReturn("TestF5");
        when(mockIcontrolInterfaces.getLocalLBVirtualServer()).thenReturn(mockVirtualServerSub);
        when(mockIcontrolInterfaces.getManagementPartition()).thenReturn(mockManagementPartitionBindingStub);
        when(mockManagementPartitionBindingStub.get_partition_list()).thenReturn(new ManagementPartitionAuthZPartition[]{mockManagementPartitionAuthZPartition});
        when(mockManagementPartitionAuthZPartition.getPartition_name()).thenReturn("TestPartion");
        when(monitor.getMetricWriter(anyString(), anyString(), anyString(), anyString())).thenReturn(metricWriter);

    }

    @Test
    public void testNoVirtualServersIncluded() throws Exception {
        classUnderTest = new VirtualServerMetricsCollector(mockIcontrolInterfaces,
                mockF5, mockMetricsFilter, monitor, metricPrefix);

        classUnderTest.call();
        //assertEquals(0, result.getMetrics().size());
        verify(metricWriter, never()).printMetric(anyString());
    }

    @Test
    public void testIncludeAllVirtualServers() throws Exception {
        Set<String> testIncludes = new HashSet<String>();
        testIncludes.add(".*");
        when(mockF5.getVirtualServerIncludes()).thenReturn(testIncludes);

        String[] testProfiles = getTestVirtualServers();
        when(mockVirtualServerSub.get_list()).thenReturn(testProfiles);

        LocalLBVirtualServerVirtualServerStatistics testStats = getTestStatistics();
        when(mockVirtualServerSub.get_statistics(any(String[].class))).thenReturn(testStats);

        classUnderTest = new VirtualServerMetricsCollector(mockIcontrolInterfaces,
                mockF5, mockMetricsFilter, monitor, metricPrefix);
        classUnderTest.call();

        verify(metricWriter, times(9)).printMetric(anyString());

		/*assertEquals(9, result.getMetrics().size());

		assertTrue(result.getMetrics().containsKey("TestF5|Virtual Servers|Common|devcontr7_VIP|STATISTIC_ACL_NO_MATCH"));
		assertTrue(result.getMetrics().containsKey("TestF5|Virtual Servers|Common|devcontr7_VIP|STATISTIC_CLIENT_SIDE_BYTES_IN"));
		assertTrue(result.getMetrics().containsKey("TestF5|Virtual Servers|Common|devcontr7_VIP|STATISTIC_CLIENT_SIDE_BYTES_OUT"));
		
		assertTrue(result.getMetrics().containsKey("TestF5|Virtual Servers|Common|devcontr7_VIP_SSL|STATISTIC_ACL_NO_MATCH"));
		assertTrue(result.getMetrics().containsKey("TestF5|Virtual Servers|Common|devcontr7_VIP_SSL|STATISTIC_CLIENT_SIDE_BYTES_IN"));
		assertTrue(result.getMetrics().containsKey("TestF5|Virtual Servers|Common|devcontr7_VIP_SSL|STATISTIC_CLIENT_SIDE_BYTES_OUT"));
		
		assertTrue(result.getMetrics().containsKey("TestF5|Virtual Servers|Common|Outbound_Forwarding|STATISTIC_ACL_NO_MATCH"));
		assertTrue(result.getMetrics().containsKey("TestF5|Virtual Servers|Common|Outbound_Forwarding|STATISTIC_CLIENT_SIDE_BYTES_IN"));
		assertTrue(result.getMetrics().containsKey("TestF5|Virtual Servers|Common|Outbound_Forwarding|STATISTIC_CLIENT_SIDE_BYTES_OUT"));*/
    }

    @Test
    public void testExcludeMetrics() throws Exception {
        Set<String> testIncludes = new HashSet<String>();
        testIncludes.add(".*");
        when(mockF5.getVirtualServerIncludes()).thenReturn(testIncludes);

        Set<String> testMetricExcludes = new HashSet<String>();
        testMetricExcludes.add(".*CLIENT.*");
        when(mockMetricsFilter.getVirtualServerMetricExcludes()).thenReturn(testMetricExcludes);

        String[] testProfiles = getTestVirtualServers();
        when(mockVirtualServerSub.get_list()).thenReturn(testProfiles);

        LocalLBVirtualServerVirtualServerStatistics testStats = getTestStatistics();
        when(mockVirtualServerSub.get_statistics(any(String[].class))).thenReturn(testStats);

        classUnderTest = new VirtualServerMetricsCollector(mockIcontrolInterfaces,
                mockF5, mockMetricsFilter, monitor, metricPrefix);
        classUnderTest.call();

        verify(metricWriter, times(3)).printMetric(anyString());

		/*assertEquals(3, result.getMetrics().size());
		
		assertTrue(result.getMetrics().containsKey("TestF5|Virtual Servers|Common|devcontr7_VIP|STATISTIC_ACL_NO_MATCH"));
		assertTrue(result.getMetrics().containsKey("TestF5|Virtual Servers|Common|devcontr7_VIP_SSL|STATISTIC_ACL_NO_MATCH"));
		assertTrue(result.getMetrics().containsKey("TestF5|Virtual Servers|Common|Outbound_Forwarding|STATISTIC_ACL_NO_MATCH"));

		// excluded
		assertFalse(result.getMetrics().containsKey("TestF5|Virtual Servers|Common|devcontr7_VIP|STATISTIC_CLIENT_SIDE_BYTES_IN"));
		assertFalse(result.getMetrics().containsKey("TestF5|Virtual Servers|Common|devcontr7_VIP|STATISTIC_CLIENT_SIDE_BYTES_OUT"));
		assertFalse(result.getMetrics().containsKey("TestF5|Virtual Servers|Common|devcontr7_VIP_SSL|STATISTIC_CLIENT_SIDE_BYTES_IN"));
		assertFalse(result.getMetrics().containsKey("TestF5|Virtual Servers|Common|devcontr7_VIP_SSL|STATISTIC_CLIENT_SIDE_BYTES_OUT"));
		assertFalse(result.getMetrics().containsKey("TestF5|Virtual Servers|Common|Outbound_Forwarding|STATISTIC_CLIENT_SIDE_BYTES_IN"));
		assertFalse(result.getMetrics().containsKey("TestF5|Virtual Servers|Common|Outbound_Forwarding|STATISTIC_CLIENT_SIDE_BYTES_OUT"));*/
    }

    private LocalLBVirtualServerVirtualServerStatistics getTestStatistics() {
        String[] testVirtualServers = getTestVirtualServers();
        LocalLBVirtualServerVirtualServerStatistics stats =
                new LocalLBVirtualServerVirtualServerStatistics();

        LocalLBVirtualServerVirtualServerStatisticEntry[] entries =
                new LocalLBVirtualServerVirtualServerStatisticEntry[testVirtualServers.length];
        stats.setStatistics(entries);

        CommonStatisticType[] statisticTypes = getTestStatisticTypes();

        for (int vsIndex = 0; vsIndex < testVirtualServers.length; vsIndex++) {
            LocalLBVirtualServerVirtualServerStatisticEntry entry =
                    new LocalLBVirtualServerVirtualServerStatisticEntry();

            CommonVirtualServerDefinition virtualServer = new CommonVirtualServerDefinition();
            virtualServer.setName(testVirtualServers[vsIndex]);
            entry.setVirtual_server(virtualServer);
            entries[vsIndex] = entry;

            CommonStatistic[] commonStats = new CommonStatistic[statisticTypes.length];

            for (int index = 0; index < statisticTypes.length; index++) {
                CommonStatistic commonStat = new CommonStatistic();
                commonStat.setType(statisticTypes[index]);
                commonStat.setValue(getTestValue());
                commonStats[index] = commonStat;
            }

            entry.setStatistics(commonStats);
        }

        return stats;
    }

    private String[] getTestVirtualServers() {
        String[] testInterfaces = {"/Common/devcontr7_VIP",
                "/Common/devcontr7_VIP_SSL", "/Common/Outbound_Forwarding"};
        return testInterfaces;
    }

    private CommonStatisticType[] getTestStatisticTypes() {
        CommonStatisticType[] testMetricTypes = {
                CommonStatisticType.STATISTIC_ACL_NO_MATCH,
                CommonStatisticType.STATISTIC_CLIENT_SIDE_BYTES_IN,
                CommonStatisticType.STATISTIC_CLIENT_SIDE_BYTES_OUT};

        return testMetricTypes;
    }

    private CommonULong64 getTestValue() {
        CommonULong64 testValue = new CommonULong64();
        testValue.setHigh(new Random(100).nextLong());
        testValue.setHigh(new Random(10).nextLong());
        return testValue;
    }
}
