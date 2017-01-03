/**
 * Copyright (c) 2014-2016 by the respective copyright holders.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.honeywellmytotalcomfortthermostat.internal.data;

/**
 * Enum representing the different schedule modes
 */
public enum HoneywellThermostatSetPoint {
    TEMP_HOLD(1),
    PERM_HOLD(2),
    SCHEDULE(0);

    private final int val;

    /**
     * Constructor that sets int value used by web api
     * @param val ID used by web api to save/set value
     */
    HoneywellThermostatSetPoint(int val) {
        this.val = val;
    }

    /**
     * gets the value of the schedule
     * @return schedule mode
     */
    public int getValue() {
        return val;
    }

    /**
     * Gets the enum to return based on the int value, used when fetching thermostat data
     * @param val the int val to convert to the enum
     * @return set point enum matching int
     */
    public static HoneywellThermostatSetPoint getEnum(int val) {
        for (HoneywellThermostatSetPoint e : HoneywellThermostatSetPoint.values()) {
            if (e.val==val) return e;
        }
        return null;
    }
}
