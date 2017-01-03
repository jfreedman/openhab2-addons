/**
 * Copyright (c) 2014-2016 by the respective copyright holders.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.honeywellmytotalcomfortthermostat.internal.data;

/**
 * Represents the properties of a thermostat, which are retrieved or updated using the web api
 */
public class HoneywellThermostatData {
    private int currentTemperature = 70;
    private int currentHumidity = 35;
    private HoneywellThermostatSystemMode currentSystemMode = HoneywellThermostatSystemMode.OFF;
    private int heatSetPoint = 70;
    private int coolSetPoint = 70;
    private String displayUnits = "";
    private String deviceId = "";
    private int holdUntilTime = 0;
    private HoneywellThermostatSetPoint setPoint = HoneywellThermostatSetPoint.SCHEDULE;
    private HoneywellThermostatFanMode currentFanMode = HoneywellThermostatFanMode.SCHEDULE;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        HoneywellThermostatData that = (HoneywellThermostatData) o;

        return deviceId.equals(that.deviceId);
    }

    @Override
    public int hashCode() {
        return deviceId.hashCode();
    }

    /**
     * gets thermostat temp
     * @return current temp
     */
    public int getCurrentTemperature() {
        return currentTemperature;
    }

    /**
     * sets the current temp
     * @param currentTemperature the current temp
     */
    public void setCurrentTemperature(int currentTemperature) {
        this.currentTemperature = currentTemperature;
    }

    /**
     * gets current indoor humidity
     * @return indoor humidity
     */
    public int getCurrentHumidity() {
        return this.currentHumidity;
    }

    /**
     * sets current humidity
     * @param currentHumidity current humidity
     */
    public void setCurrentHumidity(int currentHumidity) {
        this.currentHumidity=currentHumidity;
    }

    /**
     * returns the current mode the system is in (off, heat, cool)
     * @return curent system mode
     */
    public HoneywellThermostatSystemMode getCurrentSystemMode() {
        return currentSystemMode;
    }

    /**
     * sets what the system mode should be updated to (off, heat, cool)
     * @param currentSystemMode the system mode to set
     */
    public void setCurrentSystemMode(HoneywellThermostatSystemMode currentSystemMode) {
        this.currentSystemMode = currentSystemMode;
    }

    /**
     * gets the temp the heat is set at
     * @return heat set at
     */
    public int getHeatSetPoint() {
        return heatSetPoint;
    }

    /**
     * updates thermostat to set heat value
     * @param heatSetPoint heat value to be used
     */
    public void setHeatSetPoint(int heatSetPoint) {
        this.heatSetPoint = heatSetPoint;
    }

    /**
     * gets the temp the ac is set at
     * @return ac set at
     */
    public int getCoolSetPoint() {
        return coolSetPoint;
    }

    /**
     * updates thermostat to set ac value
     * @param coolSetPoint cool value to be used
     */
    public void setCoolSetPoint(int coolSetPoint) {
        this.coolSetPoint = coolSetPoint;
    }

    /**
     * gets if temp is displayed in celsius or fahrenheit
     * @return F or C
     */
    public String getDisplayUnits() { return  this.displayUnits; };

    /**
     * sets the display units used
     * @param displayUnits C or F
     */
    public void setDisplayUnits(String displayUnits) { this.displayUnits = displayUnits; }

    /**
     * gets the device id of the thermostat, which is the unique identifier
     * @return id of thermostat
     */
    public String getDeviceId() { return  this.deviceId; };

    /**
     * sets the device ID of the thermostat
     * @param deviceId the id to set
     */
    public void setDeviceId(String deviceId) { this.deviceId = deviceId; }

    /**
     * gets current fan mode (off, on, circulate, schedule)
     * @return fan mode
     */
    public HoneywellThermostatFanMode getCurrentFanMode() {
        return currentFanMode;
    }

    /**
     * sets fan mode
     * @param currentFanMode fan mode to set (off, on, circulate, schedule)
     */
    public void setCurrentFanMode(HoneywellThermostatFanMode currentFanMode) {
        this.currentFanMode = currentFanMode;
    }

    /**
     * get time in minutes until temp hold is released
     * @return minutes until temp hold is released
     */
    public int getHoldUntilTime() {
        return holdUntilTime;
    }

    /**
     * sets how many 15 minute spans to hold temp for
     * @param holdUntilTime the number 15 min spans
     */
    public void setHoldUntilTime(int holdUntilTime) {
        this.holdUntilTime = holdUntilTime;
    }

    /**
     * gets schedule mode (schedule, temp hold, perm hold)
     * @return the schedule mode
     */
    public HoneywellThermostatSetPoint getCurrentSetPoint() {
        return setPoint;
    }

    /**
     * sets schedule mode (schedule, temp hold, perm hold) - CurrentSetpointStatus
     * @param setPoint the schedule mode
     */
    public void setCurrentSetPoint(HoneywellThermostatSetPoint setPoint) {
        this.setPoint = setPoint;
    }

}
