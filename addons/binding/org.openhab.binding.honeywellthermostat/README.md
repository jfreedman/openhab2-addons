# Honeywell Thermostat Binding

This is the binding for Honeywell Thermostats that use the MyTotalComfort.com site to be managed.
This binding allows you to view and update thermostat settings for system mode, fan and temperature.  It also allows you to view indoor humidity

**Note:**  This has only be tested on model TH9320WF5003 and uses fahrenheit for values.  In theory, any Honeywell thermostat that uses mytotalcomfort.com should work. 
## Configuration

There are several settings to connect to the Honeywell api:
- **email** (required)  
email address for logging into Honeywell mytotalcomfort.com

- **password**  (required)  
password for logging into Honeywell mytotalcomfort.com

- **deviceId**  (required)  
The deviceId of your thermostat.  After logging into mytotalcomfort.com, your device is listed in the url:  https://mytotalconnectcomfort.com/portal/Device/Control/***<YOUR-DEVICE-ID>***
