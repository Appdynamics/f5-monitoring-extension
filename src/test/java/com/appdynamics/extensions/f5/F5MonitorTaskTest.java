package com.appdynamics.extensions.f5;

import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.whenNew;

import com.appdynamics.extensions.f5.collectors.CPUMetricsCollector;
import com.appdynamics.extensions.f5.collectors.ClientSSLMetricsCollector;
import com.appdynamics.extensions.f5.collectors.DiskMetricsCollector;
import com.appdynamics.extensions.f5.collectors.HttpCompressionMetricsCollector;
import com.appdynamics.extensions.f5.collectors.HttpMetricsCollector;
import com.appdynamics.extensions.f5.collectors.IRuleMetricsCollector;
import com.appdynamics.extensions.f5.collectors.MemoryMetricsCollector;
import com.appdynamics.extensions.f5.collectors.NetworkInterfaceMetricsCollector;
import com.appdynamics.extensions.f5.collectors.PoolMetricsCollector;
import com.appdynamics.extensions.f5.collectors.ServerSSLMetricsCollector;
import com.appdynamics.extensions.f5.collectors.SnatPoolMetricsCollector;
import com.appdynamics.extensions.f5.collectors.SystemMetricsCollector;
import com.appdynamics.extensions.f5.collectors.TCPMetricsCollector;
import com.appdynamics.extensions.f5.collectors.VirtualServerMetricsCollector;
import com.appdynamics.extensions.f5.config.F5;
import com.appdynamics.extensions.f5.config.MetricsFilter;
import com.singularity.ee.agent.systemagent.api.MetricWriter;
import iControl.Interfaces;
import iControl.SystemSystemInfoBindingStub;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.math.BigInteger;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;

@RunWith(PowerMockRunner.class)
@PrepareForTest({F5MonitorTask.class})
@PowerMockIgnore({"org.apache.*, javax.xml.*"})
public class F5MonitorTaskTest {

    private F5MonitorTask classUnderTest;

    private F5 f5;

    @Mock
    private MetricsFilter mockMetricsFilter;

    @Mock
    private Interfaces mockIcontrolInterfaces;

    @Mock
    private PoolMetricsCollector mockPoolMetricsCollector;

    @Mock
    private SnatPoolMetricsCollector mockSnatPoolMetricsCollector;

    @Mock
    private VirtualServerMetricsCollector mockVirtualServerMetricsCollector;

    @Mock
    private IRuleMetricsCollector mockIRuleMetricsCollector;

    @Mock
    private ClientSSLMetricsCollector mockClientSSLMetricsCollector;

    @Mock
    private ServerSSLMetricsCollector mockServerSSLMetricsCollector;

    @Mock
    private NetworkInterfaceMetricsCollector mockNetworkInterfaceMetricsCollector;

    @Mock
    private TCPMetricsCollector mockTCPMetricsCollector;

    @Mock
    private HttpMetricsCollector mockHttpMetricsCollector;

    @Mock
    private HttpCompressionMetricsCollector mockHttpCompressionMetricsCollector;

    @Mock
    private SystemMetricsCollector mockSystemMetricsCollector;

    @Mock
    private DiskMetricsCollector mockDiskMetricsCollector;

    @Mock
    private CPUMetricsCollector mockCPUMetricsCollector;

    @Mock
    private MemoryMetricsCollector mockMemoryMetricsCollector;

    @Mock
    private F5Monitor monitor;

    @Mock
    private MetricWriter metricWriter;

    private String metricPrefix = F5Constants.DEFAULT_METRIC_PATH;

    private List<Callable<Void>> metricCollectors;

    @Before
    public void setUp() throws Exception {
        f5 = getTestF5();
        whenNew(Interfaces.class).withNoArguments().thenReturn(mockIcontrolInterfaces);
        when(monitor.getMetricWriter(anyString(), anyString(), anyString(), anyString())).thenReturn(metricWriter);

        metricCollectors = setUpMockCollectors();

        classUnderTest = new F5MonitorTask(monitor, metricPrefix, mockIcontrolInterfaces, f5, mockMetricsFilter, 2, 30);
    }

    @Test
    public void testCredentialsAreInvalid() throws Exception {
        SystemSystemInfoBindingStub mockSystemStub = mock(SystemSystemInfoBindingStub.class);
        when(mockIcontrolInterfaces.getSystemSystemInfo()).thenReturn(mockSystemStub);
        when(mockSystemStub.get_version()).thenThrow(new RemoteException("(401)"));

        classUnderTest.call();
        //assertTrue(result.getMetrics().isEmpty());
        verify(metricWriter, never());
    }

    @Test
    public void testMetricsCollectedSuccessfully() throws Exception {
        SystemSystemInfoBindingStub mockSystemStub = mock(SystemSystemInfoBindingStub.class);
        when(mockIcontrolInterfaces.getSystemSystemInfo()).thenReturn(mockSystemStub);

        classUnderTest.call();

        for (Callable<Void> metricCollector : metricCollectors) {
            verify(metricCollector, times(1)).call();
        }
    }

    private List<Callable<Void>> setUpMockCollectors() throws Exception {

        List<Callable<Void>> metricCollectors = new ArrayList<Callable<Void>>();

        F5Metrics metrics = new F5Metrics();
        metrics.add("TEST|POOL|POOL1|METRIC1", BigInteger.ONE);
        metrics.add("TEST|POOL|POOL1|METRIC2", BigInteger.valueOf(2));
        whenNew(PoolMetricsCollector.class).withArguments(mockIcontrolInterfaces,
                f5, mockMetricsFilter, monitor, metricPrefix).thenReturn(mockPoolMetricsCollector);
        //when(mockPoolMetricsCollector.call()).thenReturn(metrics);
        metricCollectors.add(mockPoolMetricsCollector);

        metrics = new F5Metrics();
        metrics.add("TEST|SNAT_POOL|SNAT_POOL1|METRIC1", BigInteger.ONE);
        metrics.add("TEST|SNAT_POOL|SNAT_POOL1|METRIC2", BigInteger.valueOf(2));
        whenNew(SnatPoolMetricsCollector.class).withArguments(mockIcontrolInterfaces,
                f5, mockMetricsFilter, monitor, metricPrefix).thenReturn(mockSnatPoolMetricsCollector);
        //when(mockSnatPoolMetricsCollector.call()).thenReturn(metrics);
        metricCollectors.add(mockSnatPoolMetricsCollector);

        metrics = new F5Metrics();
        metrics.add("TEST|VIRTUAL_SERVERS|VIRTUAL_SERVER1|METRIC1", BigInteger.ONE);
        metrics.add("TEST|VIRTUAL_SERVERS|VIRTUAL_SERVER1|METRIC2", BigInteger.valueOf(2));
        whenNew(VirtualServerMetricsCollector.class).withArguments(mockIcontrolInterfaces,
                f5, mockMetricsFilter, monitor, metricPrefix).thenReturn(mockVirtualServerMetricsCollector);
        //when(mockVirtualServerMetricsCollector.call()).thenReturn(metrics);
        metricCollectors.add(mockVirtualServerMetricsCollector);

        metrics = new F5Metrics();
        metrics.add("TEST|IRULES|IRULE1|METRIC1", BigInteger.ONE);
        metrics.add("TEST|IRULES|IRULE1|METRIC2", BigInteger.valueOf(2));
        whenNew(IRuleMetricsCollector.class).withArguments(mockIcontrolInterfaces,
                f5, mockMetricsFilter, monitor, metricPrefix).thenReturn(mockIRuleMetricsCollector);
        //when(mockIRuleMetricsCollector.call()).thenReturn(metrics);
        metricCollectors.add(mockIRuleMetricsCollector);

        metrics = new F5Metrics();
        metrics.add("TEST|SSL|CLIENT1|METRIC1", BigInteger.ONE);
        metrics.add("TEST|SSL|CLIENT1|METRIC2", BigInteger.valueOf(2));
        whenNew(ClientSSLMetricsCollector.class).withArguments(mockIcontrolInterfaces,
                f5, mockMetricsFilter, monitor, metricPrefix).thenReturn(mockClientSSLMetricsCollector);
        //when(mockClientSSLMetricsCollector.call()).thenReturn(metrics);
        metricCollectors.add(mockClientSSLMetricsCollector);

        metrics = new F5Metrics();
        metrics.add("TEST|SSL|SERVER1|METRIC1", BigInteger.ONE);
        metrics.add("TEST|SSL|SERVER1|METRIC2", BigInteger.valueOf(2));
        whenNew(ServerSSLMetricsCollector.class).withArguments(mockIcontrolInterfaces,
                f5, mockMetricsFilter, monitor, metricPrefix).thenReturn(mockServerSSLMetricsCollector);
        //when(mockServerSSLMetricsCollector.call()).thenReturn(metrics);
        metricCollectors.add(mockServerSSLMetricsCollector);

        metrics = new F5Metrics();
        metrics.add("TEST|NETWORK|1.1|METRIC1", BigInteger.ONE);
        metrics.add("TEST|NETWORK|1.1|METRIC2", BigInteger.valueOf(2));
        whenNew(NetworkInterfaceMetricsCollector.class).withArguments(mockIcontrolInterfaces,
                f5, mockMetricsFilter, monitor, metricPrefix).thenReturn(mockNetworkInterfaceMetricsCollector);
        //when(mockNetworkInterfaceMetricsCollector.call()).thenReturn(metrics);
        metricCollectors.add(mockNetworkInterfaceMetricsCollector);

        metrics = new F5Metrics();
        metrics.add("TEST|TCP|METRIC1", BigInteger.ONE);
        metrics.add("TEST|TCP|METRIC2", BigInteger.valueOf(2));
        whenNew(TCPMetricsCollector.class).withArguments(mockIcontrolInterfaces,
                f5, mockMetricsFilter, monitor, metricPrefix).thenReturn(mockTCPMetricsCollector);
        //when(mockTCPMetricsCollector.call()).thenReturn(metrics);
        metricCollectors.add(mockTCPMetricsCollector);

        metrics = new F5Metrics();
        metrics.add("TEST|HTTP|METRIC1", BigInteger.ONE);
        metrics.add("TEST|HTTP|METRIC2", BigInteger.valueOf(2));
        whenNew(HttpMetricsCollector.class).withArguments(mockIcontrolInterfaces,
                f5, mockMetricsFilter, monitor, metricPrefix).thenReturn(mockHttpMetricsCollector);
        //when(mockHttpMetricsCollector.call()).thenReturn(metrics);
        metricCollectors.add(mockHttpMetricsCollector);

        metrics = new F5Metrics();
        metrics.add("TEST|HTTP|COMPRESSION|METRIC1", BigInteger.ONE);
        metrics.add("TEST|HTTP|COMPRESSION|METRIC2", BigInteger.valueOf(2));
        whenNew(HttpCompressionMetricsCollector.class).withArguments(mockIcontrolInterfaces,
                f5, mockMetricsFilter, monitor, metricPrefix).thenReturn(mockHttpCompressionMetricsCollector);
        //when(mockHttpCompressionMetricsCollector.call()).thenReturn(metrics);
        metricCollectors.add(mockHttpCompressionMetricsCollector);

        metrics = new F5Metrics();
        metrics.add("TEST|SYSTEM|METRIC1", BigInteger.ONE);
        metrics.add("TEST|SYSTEM|METRIC2", BigInteger.valueOf(2));
        whenNew(SystemMetricsCollector.class).withArguments(mockIcontrolInterfaces,
                f5, monitor, metricPrefix).thenReturn(mockSystemMetricsCollector);
        //when(mockSystemMetricsCollector.call()).thenReturn(metrics);
        metricCollectors.add(mockSystemMetricsCollector);

        metrics = new F5Metrics();
        metrics.add("TEST|DISKS|DISK1|METRIC1", BigInteger.ONE);
        metrics.add("TEST|DISKS|DISK1|METRIC2", BigInteger.valueOf(2));
        whenNew(DiskMetricsCollector.class).withArguments(mockIcontrolInterfaces,
                f5, monitor, metricPrefix).thenReturn(mockDiskMetricsCollector);
        //when(mockDiskMetricsCollector.call()).thenReturn(metrics);
        metricCollectors.add(mockDiskMetricsCollector);

        metrics = new F5Metrics();
        metrics.add("TEST|CPU|CPU1|METRIC1", BigInteger.ONE);
        metrics.add("TEST|CPU|CPU1|METRIC2", BigInteger.valueOf(2));
        whenNew(CPUMetricsCollector.class).withArguments(mockIcontrolInterfaces,
                f5, monitor, metricPrefix).thenReturn(mockCPUMetricsCollector);
        //when(mockCPUMetricsCollector.call()).thenReturn(metrics);
        metricCollectors.add(mockCPUMetricsCollector);

        metrics = new F5Metrics();
        metrics.add("TEST|MEMORY|MEMORY1|METRIC1", BigInteger.ONE);
        metrics.add("TEST|MEMORY|MEMORY1|METRIC2", BigInteger.valueOf(2));
        whenNew(MemoryMetricsCollector.class).withArguments(mockIcontrolInterfaces,
                f5, monitor, metricPrefix).thenReturn(mockMemoryMetricsCollector);
        //when(mockMemoryMetricsCollector.call()).thenReturn(metrics);
        metricCollectors.add(mockMemoryMetricsCollector);
        return metricCollectors;
    }

    private F5 getTestF5() {
        F5 f5 = new F5();
        f5.setName("TEST");
        f5.setHostname("1.1.1.2");
        f5.setUsername("testUsername");
        f5.setPassword("testPassword");
        f5.setClientSSLProfileIncludes(getTestRegexInclusions());
        f5.setiRuleIncludes(getTestRegexInclusions());
        f5.setNetworkInterfaceIncludes(getTestRegexInclusions());
        f5.setPoolIncludes(getTestRegexInclusions());
        f5.setPoolMemberIncludes(getTestRegexInclusions());
        f5.setServerSSLProfileIncludes(getTestRegexInclusions());
        f5.setSnatPoolIncludes(getTestRegexInclusions());
        f5.setSnatPoolMemberIncludes(getTestRegexInclusions());
        f5.setVirtualServerIncludes(getTestRegexInclusions());
        return f5;
    }

    private Set<String> getTestRegexInclusions() {
        Set<String> regexSet = new HashSet<String>();
        regexSet.add(".*");
        return regexSet;
    }

}
