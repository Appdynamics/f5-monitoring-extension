# F5 Monitoring Extension

## Use Case

The F5 load balancer from F5 Networks, Inc. directs traffic away from servers that are overloaded or down to other servers that can handle the load. The F5 load balancer extension collects key performance metrics from an F5 load balancer and presents them in the AppDynamics Metric Browser.

## Prerequisites

1.  Before the extension is installed, the prerequisites mentioned [here](https://community.appdynamics.com/t5/Knowledge-Base/Extensions-Prerequisites-Guide/ta-p/35213) need to be met. Please do not proceed with the extension installation if the specified prerequisites are not met.
2.  Download and install [Apache Maven](https://maven.apache.org/) which is configured with `Java 8` to build the extension artifact from source. You can check the java version used in maven using command `mvn -v` or `mvn --version`. If your maven is using some other java version then please download java 8 for your platform and set JAVA_HOME parameter before starting maven.
3.  The extension collects the data from the REST API. Please make sure that the API is available and accessible. To access F5 REST API, user account must have **admin** level access. Try this URL from the Browser `https://f5-host:f5-port/mgmt/tm/ltm/pool/stats` or via curl.
4.  The extension needs to be able to connect to the F5 in order to collect and send metrics. To do this, you will have to either establish a remote connection in between the extension and the product, or have an agent on the same machine running the product in order for the extension to collect and send the metrics.

## Installation
1. Run 'mvn clean install' from "F5MonitorRepo"
2. Unzip the `F5Monitor-<version>.zip` from `target` directory into the "<MachineAgent_Dir>/monitors" directory.
3. Edit the file config.yml located at <MachineAgent_Dir>/monitors/F5Monitor The metricPrefix of the extension has to be configured as specified [here](https://community.appdynamics.com/t5/Knowledge-Base/How-do-I-troubleshoot-missing-custom-metrics-or-extensions/ta-p/28695#Configuring%20an%20Extension). Please make sure that the right metricPrefix is chosen based on your machine agent deployment, otherwise this could lead to metrics not being visible in the controller.
4. All metrics to be reported are configured in metrics.xml. Users can remove entries from metrics.xml to stop the metric from reporting, or add new entries as well.
5. Restart the Machine Agent.

Please place the extension in the **"monitors"** directory of your **Machine Agent** installation directory. Do not place the extension in the **"extensions"** directory of your **Machine Agent** installation directory.
In the AppDynamics Metric Browser, look for **Application Infrastructure Performance|\<Tier\>|Custom Metrics|F5 Monitor** and you should be able to see all the metrics.

## Configuration
### Config.yml

Configure the extension by editing the config.yml file in `<MACHINE_AGENT_HOME>/monitors/F5Monitor/`.

  1. Configure the "COMPONENT_ID" under which the metrics need to be reported. This can be done by changing the value of `<COMPONENT_ID>` in   **metricPrefix: Server|Component:<COMPONENT_ID>|Custom Metrics|F5 Monitor|**.
       For example,
       ```
       metricPrefix: "Server|Component:100|Custom Metrics|F5 Monitor|"
       ```

  2.  **Server Details**
        ```
                        servers:
                          - uri: "https://server1:8443"
                            name: "Server1"
                            username: "user"
                            password: "password"
                            encryptedPassword: "" # put the password as empty when using encryptedPassword
                
                          - uri: "https://server2:443"
                            name: "server2"
                            username: "user"
                            password: ""
                            encryptedPassword: "y444543gt3="
        ```

If `encryptedPassword` is used, make sure to update the `encryptionKey` in `config.yml`. Please read the documentation [here](https://community.appdynamics.com/t5/Knowledge-Base/How-do-I-use-Password-Encryption-with-Extensions/ta-p/29397) to generate encrypted password.

3.  **Filters**
    By default, everything is included as shown in the filters with the `.*` regex. This will report a lot of data. Set the correct filters to make sure that you only collect the data you will need
    ```
            filter:
              pools:
                includes: [".*"]
              poolMembers:
                includes: [".*"]
              rules:
                includes: [".*"]
              ...
              ...
    ```

Please refer to the `config.yml` for the complete configuration.

#### Token Based Authentication (BIG IP 12+)

[Token-based authentication](https://devcentral.f5.com/wiki/iControl.Authentication_with_the_F5_REST_API.ashx) can be used in BIG IP v12+. Please refer to [F5 documentation](https://devcentral.f5.com/wiki/iControl.Authentication_with_the_F5_REST_API.ashx) for more details. To enabled token based auth, please modify the `config.yml` as shown below.

```
    servers:
      - uri: "https://server1:8443"
        name: "Server1"
        username: "user"
        password: "password"
        encryptedPassword: ""
        authType: "TOKEN"
        loginReference: ""
```

1.  If you are using an external authentication provider, get the `loginReference` from your system administrator. It looks something like `https://localhost/mgmt/cm/system/authn/providers/ldap/-id-/login`
2.  If you are using local authentication (that is, there is no external authentication provider), then leave the `loginReference` blank

#### Using a Non Admin account (BI IP 11.6+)

This is applicable only for BIG IP versions 11.6+. To use a non-admin, you have to explicitly grant access to the rest interface. Please follow the steps.

1.  Create a user with guest role from the F5 Admin UI.
2.  Run the following command and get the `selfLink` of the guest user(created in step #1) from the response JSON
    ```
    curl -i -k -u <adminuser>:<pwd> https://f5-ip/mgmt/shared/authz/users
    ```

4.  Replace the `link` in the `--data` argument json with the value of `selfLink` from the response of step #2
    ```
    curl -i -k -u <adminuser>:<pwd> --request PATCH \
        --data '{"userReferences":[{"link":"https://localhost/mgmt/shared/authz/users/guestuser"}]}' \
        https://f5-ip/mgmt/shared/authz/roles/iControl_REST_API_User
    ```

This should give access to the user created in step #1 to invoke the rest api to invoke GET. If the guest role doesn't work, try "manager" role in step#1

### Metrics.xml

You can add/remove metrics of your choice by modifying the provided metrics.xml file. This file consists of all the metrics that will be monitored and sent to the controller. Please look how the metrics have been defined and follow the same convention, when adding new metrics. You do have the ability to chosoe your Rollup types as well as set an alias that you would like to be displayed on the metric browser.

   1. Metric Configuration
    Add the `metric` to be monitored with the metric tag as shown below.
        ```
        <metric attr="nestedStats|entries|activeMemberCnt" alias="Active Members" aggregationType = "AVERAGE" timeRollUpType = "AVERAGE" clusterRollUpType = "COLLECTIVE"/>
         ```
For configuring the metrics, the following properties can be used:

 |     Property      |   Default value |         Possible values         |                                               Description                                                      |
 | ----------------- | --------------- | ------------------------------- | -------------------------------------------------------------------------------------------------------------- |
 | alias             | metric name     | Any string                      | The substitute name to be used in the metric browser instead of metric name.                                   |
 | aggregationType   | "AVERAGE"       | "AVERAGE", "SUM", "OBSERVATION" | [Aggregation qualifier](https://docs.appdynamics.com/display/latest/Build+a+Monitoring+Extension+Using+Java)    |
 | timeRollUpType    | "AVERAGE"       | "AVERAGE", "SUM", "CURRENT"     | [Time roll-up qualifier](https://docs.appdynamics.com/display/latest/Build+a+Monitoring+Extension+Using+Java)   |
 | clusterRollUpType | "INDIVIDUAL"    | "INDIVIDUAL", "COLLECTIVE"      | [Cluster roll-up qualifier](https://docs.appdynamics.com/display/latest/Build+a+Monitoring+Extension+Using+Java)|
 | multiplier        | 1               | Any number                      | Value with which the metric needs to be multiplied.                                                            |
 | convert           | null            | Any key value map               | Set of key value pairs that indicates the value to which the metrics need to be transformed. eg: UP:1, OPEN:1  |
 | delta             | false           | true, false                     | If enabled, gives the delta values of metrics instead of actual values.                                        |


 **All these metric properties are optional, and the default value shown in the table is applied to the metric (if a property has not been specified) by default.**

#### Metrics

The metrics will be reported under the tree `Application Infrastructure Performance|$TIER|Custom Metrics|F5 Monitor|`

## Credentials Encryption

Please visit [this page](https://community.appdynamics.com/t5/Knowledge-Base/How-to-use-Password-Encryption-with-Extensions/ta-p/29397) to get detailed instructions on password encryption. The steps in this document will guide you through the whole process.

## Extensions Workbench
Workbench is an inbuilt feature provided with each extension in order to assist you to fine tune the extension setup before you actually deploy it on the controller. Please review the following document on [How to use the Extensions WorkBench](https://community.appdynamics.com/t5/Knowledge-Base/How-to-use-the-Extensions-WorkBench/ta-p/30130).


## Troubleshooting

1.  Please follow the steps listed in this [troubleshooting-document](https://community.appdynamics.com/t5/Knowledge-Base/How-do-I-troubleshoot-missing-custom-metrics-or-extensions/ta-p/28695) in order to troubleshoot your issue. These are a set of common issues that customers might have faced during the installation of the extension.
2.  **config.yml:** Validate the file [here](https://jsonformatter.org/yaml-validator)
3.  **F5 REST API:** Please make sure that the F5 REST API is available and accessible. Try this URL from the Browser or via curl `https://f5-host:f5-port/mgmt/tm/ltm/pool/stats`
4.  **Metric Limit:** Please start the machine agent with the argument `-Dappdynamics.agent.maxMetrics=5000` if there is a metric limit reached error in the logs. If you dont see the expected metrics, this could be the cause.
5.  **Check Logs:** There could be some obvious errors in the machine agent logs. Please take a look.

## Contributing
Always feel free to fork and contribute any changes directly here on [GitHub](https://github.com/Appdynamics/f5-monitoring-extension).

## Version
|          Name            |  Version   |
|--------------------------|------------|
|Extension Version         |2.5.2       |
|Product Tested On         |15.0.0       |
|Last Update               |04/02/2021  |
|Changes list              |[ChangeLog](https://github.com/Appdynamics/f5-monitoring-extension/blob/master/CHANGELOG.md)|

**Note**: While extensions are maintained and supported by customers under the open-source licensing model, they interact with agents and Controllers that are subject to [AppDynamics’ maintenance and support policy](https://docs.appdynamics.com/latest/en/product-and-release-announcements/maintenance-support-for-software-versions). Some extensions have been tested with AppDynamics 4.5.13+ artifacts, but you are strongly recommended against using versions that are no longer supported.
