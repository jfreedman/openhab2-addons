/**
 * Copyright (c) 2014-2016 by the respective copyright holders.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.honeywellthermostat.internal.data;

public enum HoneywellThermostatSystemMode {
    HEAT(1),
    OFF(2),
    COOL(3);

    private final int val;

    private HoneywellThermostatSystemMode(int val) {
        this.val = val;
    }

    public int getValue() {
        return val;
    }

    public static HoneywellThermostatSystemMode getEnum(String val) {
        for (HoneywellThermostatSystemMode e : HoneywellThermostatSystemMode.values()) {
            if (String.valueOf(e.val).equals(val)) return e;
        }
        return null;
    }
}
