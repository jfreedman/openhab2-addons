/**
 * Copyright (c) 2014-2016 by the respective copyright holders.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.honeywellmytotalcomfortthermostat.internal.data;

/**
 * Enum representing the different fan modes
 */
public enum HoneywellThermostatFanMode {

    AUTO(0),
    ON(1),
    CIRCULATE(2),
    SCHEDULE(3);

    private final int val;

    /**
     * Constructor that sets int value used by web api
     * @param val ID used by web api to save/set value
     */
    HoneywellThermostatFanMode(int val) {
        this.val = val;
    }

    /**
     * gets the value of the fan mode
     * @return fan mode
     */
    public int getValue() {
        return val;
    }

    /**
     * Gets the enum to return based on the int value, used when fetching thermostat data
     * @param val the int val to convert to the enum
     * @return fan mode enum matching int
     */
    public static HoneywellThermostatFanMode getEnum(int val) {
        for (HoneywellThermostatFanMode e : HoneywellThermostatFanMode.values()) {
            if (e.val==val) return e;
        }
        return null;
    }
}
