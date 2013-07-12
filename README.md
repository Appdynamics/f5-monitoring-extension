# AppDynamics F5 Load Balancer Monitoring Extension

- [Use Case](f5monitor-readme.md#use-case)
- [Installation](f5monitor-readme.md#installation)
     - [XML Example](f5monitor-readme.md#xml-example)
     -   [monitor.xml](f5monitor-readme.md#monitorxml)
- [Contents of f5monitor.tar.gz](f5monitor-readme.md#contents-of-f5monitortargz)
- [Contributing](f5monitor-readme.md#contributing)

##Use Case

The F5 load balancer from F5 Networks, Inc. directs traffic away from servers that are overloaded or 
down to other servers that can handle the load. 
The F5 load balancer extension collects key performance metrics from an F5 load balancer and 
presents them in the AppDynamics Metric Browser. 


##Installation

1.  Create a new directory in \<machine-agent-home\>/monitors/ for the F5 monitoring extension.
2.  Copy the contents in the 'dist' directory and paste the contents into the directory created in Step 1.
3.  Edit the monitor.xml file and configure the credentials to log into your F5 load balancer.
4.  Restart the Machine Agent.
5. Look for the F5 metrics in the AppDynamics Metric Browser at: Custom Metrics|F5|\<Metric Group\>|\<Metric Name\>.

##XML Example

####monitor.xml

| Parameter | Description |
| --- | --- |
| IP address | IP address of the F5 load balancer  |
| Username | The username to log into the F5 load balancer |
| Password | The password to log into the F5 load balancer |
| exclude-metrics | A comma-separated list of F5 metrics to be filtered out of the report uploaded to the AppDynamics metric browser. |

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
                            <classpath>f5monitor.jar;lib/iControl.jar;lib/3rdParty/*</classpath>
                            <impl-class>com.appdynamics.monitors.f5.F5Monitor</impl-class>
                    </java-task>
                    <!-- 
                            These are the mandatory parameters: F5 IP address, Username, Password
                            Optional: Provide a comma-separated list of metrics you don't want to see reported
                    -->
                    <task-arguments>
                            <argument name="IP" is-required="true" default-value="172.16.0.0"/>
                            <argument name="Username" is-required="true" default-value="Username"/>
                            <argument name="Password" is-required="true" default-value="Password"/>
                            <argument name="exclude-metrics" is-required="false" default-value="comma, separated, list, of, metrics, to, exclude"/>
                    </task-arguments>
            </monitor-run-task>
    </monitor>
    
##Contents of f5monitor.tar.gz

| Directory/File | Description |
| --- | --- |
| bin | Contains class files |
| conf | Contains the monitor.xml you will configure for your environment|
| dist | Contains the distribution package (monitor.xml, the lib directory, and f5monitor.jar) |
| lib | Contains third-party project references |
| src | Contains source code to the F5 extension |
| build.xml | Ant build script to package the project (only required if changing java code)  |



##Contributing

Always feel free to fork and contribute any changes directly via GitHub.


##Support

For any support questions, please contact ace@appdynamics.com.
