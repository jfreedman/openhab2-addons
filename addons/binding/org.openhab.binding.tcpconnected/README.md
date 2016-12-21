# TCP Connected lighting Binding

This binding integrates the TCP Connected Lighting Gateway

## SETUP
When adding a thing for your TCP Connected Lighting Gateway, you must first press the button on the gateway to allow openHAB to initially sync with it.
After the initial sync, you are not required to sync again unless you reset your gateway.


## Channels

Lights support the following channels:

| Channel Type ID | Item Type    | Description  |
|-----------------|------------------------|--------------|----------------- |------------- |
| level | Dimmer  | The level of light in percent |
| state | String | Current state of the device (on, off) |
