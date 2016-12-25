/**
 * Copyright (c) 2014-2016 by the respective copyright holders.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.honeywellmytotalcomfortthermostat;

import org.eclipse.smarthome.core.thing.ThingTypeUID;

/**
 * The {@link HoneywellThermostatBindingConstants} class defines common constants, which are
 * used across the whole binding.
 *
 * @author Jeremy Freedman - Initial contribution
 */
public class HoneywellThermostatBindingConstants {

    public static final String BINDING_ID = "honeywellmytotalconnectcomfort";

    // List of all Thing Type UIDs
    public final static ThingTypeUID HONEY_BRIDGE = new ThingTypeUID(BINDING_ID, "bridge");
    public final static ThingTypeUID HONEY_THERM_THING = new ThingTypeUID(BINDING_ID, "thermostat");

    // List of all Channel ids
    public final static String SYSTEM_MODE = "sysMode";
    public final static String CURRENT_TEMPERATURE = "currentTemp";
    public final static String HEAT_SETPOINT = "heatSP";
    public final static String COOL_SETPOINT = "coolSP";
    public final static String FAN_MODE = "fanMode";
    public final static String CURRENT_HUMIDITY = "currentHumidity";
    public final static String SCHEDULE_MODE = "scheduleMode";
    public final static String HOLD_UNTIL = "holdUntil";

    // constants for config
    public final static String EMAIL = "email";
    public final static String PASSWORD = "password";
    public final static String DEVICE_ID = "deviceId";

};