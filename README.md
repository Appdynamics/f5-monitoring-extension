# AppDynamics Extension to Monitor F5 BIG-IP

##Use Case
This is an addon to the Appdynamics Machine Agent. This extension collects the metrics by invoking the iControl® REST Interface. These metrics will be reported to AppDynamics Controller.

##Installation

The instructions are available at [AppDynamics Community](https://www.appdynamics.com/community/exchange/extension/f5-monitoring-extension)

##BIG-IP user account properties
### Create User
To use this extension, the user account should have the property `Partition Access = All` selected.
![](https://github.com/Appdynamics/f5-monitoring-extension/raw/master/F5-access-control.png)

###Grant REST Interface Access
1. Get the "selfLink" of the guest user(created in step #1) from the response JSON of the following API
```
curl -i -k -u <adminuser>:<pwd> https://f5-ip/mgmt/shared/authz/users
```

2. Replace the "link" in the request json with the value of "selfLink" from the response of step #2 
```
curl -i -k -u <adminuser>:<pwd> --request PATCH --data '{"userReferences":[{"link":"https://localhost/mgmt/shared/authz/users/guestuser"}]}' https://f5-ip/mgmt/shared/authz/roles/iControl_REST_API_User
```

##Contributing

Always feel free to fork and contribute any changes directly via [GitHub](https://github.com/Appdynamics/f5-monitoring-extension).

##Community

Find out more in the [AppSphere](https://www.appdynamics.com/community/exchange/extension/f5-monitoring-extension/) community.

##Support

For any questions or feature request, please contact [AppDynamics Center of Excellence](mailto:help@appdynamics.com).

