/**
 * Copyright (c) 2014-2016 by the respective copyright holders.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.honeywellthermostat.internal.data;

public class HoneywellThermostatData {
    private int currentTemperature = 70;
    private int currentHumidity = 35;
    private HoneywellThermostatSystemMode currentSystemMode = HoneywellThermostatSystemMode.OFF;
    private int heatSetPoint = 70;
    private int coolSetPoint = 70;
    private String displayUnits = "";
    private HoneywellThermostatFanMode currentFanMode = HoneywellThermostatFanMode.SCHEDULE;

    public int getCurrentTemperature() {
        return currentTemperature;
    }

    public void setCurrentTemperature(int currentTemperature) {
        this.currentTemperature = currentTemperature;
    }

    public int getCurrentHumidity() {
        return this.currentHumidity;
    }

    public void setCurrentHumidity(int currentHumidity) {
        this.currentHumidity = currentHumidity;
    }

    public HoneywellThermostatSystemMode getCurrentSystemMode() {
        return currentSystemMode;
    }

    public void setCurrentSystemMode(HoneywellThermostatSystemMode currentSystemMode) {
        this.currentSystemMode = currentSystemMode;
    }

    public int getHeatSetPoint() {
        return heatSetPoint;
    }

    public void setHeatSetPoint(int heatSetPoint) {
        this.heatSetPoint = heatSetPoint;
    }

    public int getCoolSetPoint() {
        return coolSetPoint;
    }

    public void setCoolSetPoint(int coolSetPoint) {
        this.coolSetPoint = coolSetPoint;
    }

    public String getDisplayUnits() { return  this.displayUnits; };

    public void setDisplayUnits(String displayUnits) { this.displayUnits = displayUnits; }



    public HoneywellThermostatFanMode getCurrentFanMode() {
        return currentFanMode;
    }

    public void setCurrentFanMode(HoneywellThermostatFanMode currentFanMode) {
        this.currentFanMode = currentFanMode;
    }
}
