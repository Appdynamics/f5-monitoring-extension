# AppDynamics F5 Load Balancer Monitoring Extension

##Use Case

The F5 load balancer from F5 Networks, Inc. directs traffic away from servers that are overloaded or down to other servers that can handle the load. 
The F5 load balancer extension collects key performance metrics from an F5 load balancer and presents them in the AppDynamics Metric Browser. 


##Installation

1. Run 'ant package' from the f5-monitoring-extension directory
2. Download the file F5Monitor.zip located in the 'dist' directory into \<machineagent install dir\>/monitors/
3. Unzip the downloaded file
4. In \<machineagent install dir\>/monitors/F5Monitor/, open monitor.xml and configure the task arguments. Optional but recommended: Configure a custom metric path.
5. Restart the Machine Agent. 
 
In the AppDynamics Metric Browser, look for: Application Infrastructure Performance  | \<Tier\> | Custom Metrics | F5 Monitor
or look for your specified path under Application Infrastructure Performance | \<Tier you specified\> | F5 Monitor

##Directory Structure

<table><tbody>
<tr>
<th align="left"> File/Folder </th>
<th align="left"> Description </th>
</tr>
<tr>
<td class='confluenceTd'> conf </td>
<td class='confluenceTd'> Contains the monitor.xml </td>
</tr>
<tr>
<td class='confluenceTd'> lib </td>
<td class='confluenceTd'> Contains third-party project references </td>
</tr>
<tr>
<td class='confluenceTd'> src </td>
<td class='confluenceTd'> Contains source code to the F5 Monitoring Extension </td>
</tr>
<tr>
<td class='confluenceTd'> dist </td>
<td class='confluenceTd'> Only obtained when using ant. Run 'ant build' to get binaries. Run 'ant package' to get the distributable .zip file </td>
</tr>
<tr>
<td class='confluenceTd'> build.xml </td>
<td class='confluenceTd'> Ant build script to package the project (required only if changing Java code) </td>
</tr>
</tbody>
</table>


##XML Example

####monitor.xml

| Parameter | Description |
| --- | --- |
| Hostname | Host name of the F5 load balancer  |
| Username | The username to log into the F5 load balancer |
| Password | The password to log into the F5 load balancer |
| pools-exclude | A comma-separated list of BigIP pool names you want to exclude from monitoring basically a regular expression|
| metric-path | Configure your own metric path. This will limit the metric reporting to one single tier (see comments in monitor.xml) |

~~~~
<monitor>
        <name>F5Monitor</name>
        <type>managed</type>
        <description>F5 monitor</description>
        <monitor-configuration></monitor-configuration>
        <monitor-run-task>
                <execution-style>continuous</execution-style>
                <name>F5 Monitor Run Task</name>
                <display-name>F5 Monitor Task</display-name>
                <description>F5 Monitor Task</description>
                <type>java</type>
                <java-task>
                        <classpath>F5Monitor.jar;lib/iControl/iControl.jar;lib/activation/activation.jar;lib/axis/axis-ant.jar;lib/axis/axis.jar;lib/commons/commons-discovery-0.2.jar;lib/commons/commons-discovery.jar;lib/commons/commons-logging-1.0.4.jar;lib/commons/commons-logging.jar;lib/jaxrpc/jaxrpc.jar;lib/mail/mail.jar;lib/axis/saaj.jar;lib/wsdl4j/wsdl4j-1.5.1.jar;lib/wsdl4j/wsdl.jar</classpath>
                        <impl-class>com.appdynamics.monitors.f5.F5Monitor</impl-class>
                </java-task>
                <!-- 
                         These are the mandatory parameters: F5 Host name, Username, Password
                         Optional: provide a comma-separated list of BigIP pool names you want to exclude from monitoring
                         Optional: configure your own metric path. This will limit the metric reporting to one single tier.
                              Example: default-value="Server|Component:Tier123" where Tier123 is the name of the tier.
                              You can also provide the tier ID instead of the name.
                -->
                <task-arguments>
                        <argument name="Hostname" is-required="true" default-value="172.16.0.0"/>
                        <argument name="Username" is-required="true" default-value="Username"/>
                        <argument name="Password" is-required="true" default-value="Password"/>
                        <argument name="pools-exclude" is-required="false" default-value="comma,separated,list, of, pool names,to,exclude,from,monitoring"/>
                        
                        <argument name="metric-path" is-required="false" default-value=""/>
                </task-arguments>
        </monitor-run-task>
</monitor>
~~~~


##Contributing

Always feel free to fork and contribute any changes directly via [GitHub](https://github.com/Appdynamics/f5-monitoring-extension).

##Community

Find out more in the [AppSphere](http://appsphere.appdynamics.com/t5/Extensions/F5-Monitoring-Extension/idi-p/753) community.

##Support

For any questions or feature request, please contact [AppDynamics Center of Excellence](mailto:ace-request@appdynamics.com).
