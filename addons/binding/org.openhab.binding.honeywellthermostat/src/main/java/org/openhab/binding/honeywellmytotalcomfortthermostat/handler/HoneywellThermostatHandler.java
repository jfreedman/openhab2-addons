/**
 * Copyright (c) 2014-2016 by the respective copyright holders.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.honeywellmytotalcomfortthermostat.handler;

import static org.openhab.binding.honeywellmytotalcomfortthermostat.HoneywellThermostatBindingConstants.*;
import static org.openhab.binding.honeywellmytotalcomfortthermostat.internal.data.HoneywellThermostatSetPoint.TEMP_HOLD;

import java.util.Calendar;
import java.util.Set;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import com.google.common.collect.Sets;
import org.eclipse.smarthome.config.core.Configuration;
import org.eclipse.smarthome.core.library.types.DateTimeType;
import org.eclipse.smarthome.core.library.types.DecimalType;
import org.eclipse.smarthome.core.library.types.StringType;
import org.eclipse.smarthome.core.thing.*;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandler;
import org.eclipse.smarthome.core.thing.binding.ThingHandler;
import org.eclipse.smarthome.core.types.Command;
import org.eclipse.smarthome.core.types.RefreshType;
import org.openhab.binding.honeywellmytotalcomfortthermostat.internal.data.HoneywellThermostatData;
import org.openhab.binding.honeywellmytotalcomfortthermostat.internal.data.HoneywellThermostatFanMode;
import org.openhab.binding.honeywellmytotalcomfortthermostat.internal.data.HoneywellThermostatSetPoint;
import org.openhab.binding.honeywellmytotalcomfortthermostat.internal.data.HoneywellThermostatSystemMode;
import org.openhab.binding.honeywellmytotalcomfortthermostat.internal.webapi.MyTotalComfortClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link HoneywellThermostatHandler} is responsible for proxying thermostat updates to the web api,
 * as well as updating channels when thermostat data changes
 * It implements the ThermStatusListener so it can response to thermostat change events
 *
 * @author Jeremy Freedman - Initial contribution
 */
public class HoneywellThermostatHandler extends BaseThingHandler implements ThermStatusListener {

    private Logger logger = LoggerFactory.getLogger(HoneywellThermostatHandler.class);

    private MyTotalComfortClient webapi;
    private ScheduledFuture<?> refreshJob;

    private String deviceID = null;

    private HoneywellBridgeHandler bridgeHandler;

    private HoneywellThermostatData thermodata;

    private int failCount=0;

    public final static Set<ThingTypeUID> SUPPORTED_THING_TYPES_UIDS = Sets.newHashSet(
            HONEY_THERM_THING);

    /**
     * Constructor
     * @param thing thermostat tied to this handler
     */
    public HoneywellThermostatHandler(Thing thing) {
        super(thing);
    }

    @Override
    public void onThermostatStateChanged(ThingUID bridge,
                                         HoneywellThermostatData data) {
        if (data.getDeviceId().equals(deviceID)) {
            updateThermostatStatus(data);
        }
    }

    @Override
    public void onThermostatRemoved(HoneywellBridgeHandler bridge,
                                    HoneywellThermostatData data) {
        if (data.getDeviceId().equals(deviceID)) {
            bridgeHandler.unregisterThermostatStatusListener(this);
            bridgeHandler = null;
            updateStatus(ThingStatus.OFFLINE);
        }
    }

    @Override
    public void onThermostatAdded(Bridge bridge, HoneywellThermostatData data) {
        logger.debug("new thermostat discovered "+data.getDeviceId()+" by "+bridge);
    }


    @Override
    public void initialize() {
        super.initialize();
        Configuration conf = this.getConfig();
        webapi = getHoneywellBridgeHandler().getClient();
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                refreshThermostatData();
            }
        };
        deviceID =  (String) getThing().getProperties().get(DEVICE_ID);
        refreshJob = scheduler.scheduleAtFixedRate(runnable, 0, 60, TimeUnit.SECONDS);
    }

    @Override
    public void dispose() {
        refreshJob.cancel(true);
        super.dispose();
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        if (command instanceof RefreshType) {
            updateThermostatStatus(thermodata);
            return;
        }
        HoneywellThermostatData updates = thermodata;
        Calendar holdTime = Calendar.getInstance();
        switch (channelUID.getId()) {
            case COOL_SETPOINT:
                DecimalType dec = (DecimalType) command;
                updates.setCoolSetPoint(dec.intValue());
                break;
            case HEAT_SETPOINT:
                DecimalType heatSP_val = (DecimalType) command;
                updates.setHeatSetPoint(heatSP_val.intValue());
                break;
            case SYSTEM_MODE:
                StringType sysmode_val = (StringType) command;
                updates.setCurrentSystemMode(
                        HoneywellThermostatSystemMode.valueOf(sysmode_val.toString()));
                break;
            case FAN_MODE:
                StringType fanmode_val = (StringType) command;
                updates.setCurrentFanMode(HoneywellThermostatFanMode.valueOf(fanmode_val.toString()));
                break;
            case SCHEDULE_MODE:
                StringType sched = (StringType) command;
                HoneywellThermostatSetPoint setpoint = HoneywellThermostatSetPoint.valueOf(sched.toString());
                updates.setCurrentSetPoint(setpoint);
                break;
            case HOLD_UNTIL:
                DateTimeType holdDate = (DateTimeType) command;
                int period = getPeriodFromTime(holdDate.getCalendar());
                updates.setHoldUntilTime(period);
                Calendar holdUntil = Calendar.getInstance();
                holdUntil.set(Calendar.HOUR_OF_DAY, holdUntil.getActualMinimum(Calendar.HOUR_OF_DAY));
                holdUntil.set(Calendar.MINUTE,      holdUntil.getActualMinimum(Calendar.MINUTE));
                holdUntil.set(Calendar.SECOND,      holdUntil.getActualMinimum(Calendar.SECOND));
                holdUntil.set(Calendar.MILLISECOND, holdUntil.getActualMinimum(Calendar.MILLISECOND));
                logger.info("period:" + period);
                int ms = getMillisecondsFromPeriod(period);
                logger.info("ms:" + ms);
                holdUntil.add(Calendar.MILLISECOND,ms);
                updateState(new ChannelUID(getThing().getUID(),HOLD_UNTIL),new DateTimeType(holdUntil));
                break;
        }
        //todo: shouldn't update states until this is successful
        if (!webapi.submitThermostatChange(deviceID, updates)) {
            logger.info("Failed to submit changes to honeywell site.");
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR,
                    "Failed to submit changes to Honeywell site.");
        } else {
            thermodata = updates;
            updateThermostatStatus(thermodata);
        }
    }

    /**
     * finds number of minutes from 15 minute period
     * @param period the period number to use
     */
    private int getMillisecondsFromPeriod(int period) {
        return period*15*60000;
    }

    /**
     * finds period of a time
     * @param endTime the end time
     */
    private int getPeriodFromTime(Calendar endTime) {
        return (endTime.get(Calendar.HOUR_OF_DAY) * 4) + (int) (endTime.get(Calendar.MINUTE)/15);
    }

    /**
     * calls the web api to refersh a thermostat
     */
    private void refreshThermostatData() {
        try {
            thermodata = webapi.getThermostatData(deviceID);
            if(failCount>0) {
                failCount = 0;
                Runnable runnable = new Runnable() {
                    @Override
                    public void run() {
                        refreshThermostatData();
                    }
                };
                refreshJob.cancel(false);
                refreshJob = scheduler.scheduleAtFixedRate(runnable, 60, 60, TimeUnit.SECONDS);
            }
        } catch (Exception ex) {
            logger.error("error updating thermostat data");
            failCount++;
            Runnable runnable = new Runnable() {
                @Override
                public void run() {
                    refreshThermostatData();
                }
            };
            refreshJob.cancel(true);
            refreshJob = scheduler.scheduleAtFixedRate(runnable, 60 * failCount, 60 * failCount, TimeUnit.SECONDS);
        }
        updateThermostatStatus(thermodata);
    }

    /**
     * Updates channel information for thermostat after data has changed
     * @param data thermostat data to update
     */
    private void updateThermostatStatus(HoneywellThermostatData data) {

        if(data!=null) {
            updateStatus(ThingStatus.ONLINE);
            updateState(new ChannelUID(getThing().getUID(), CURRENT_TEMPERATURE),
                    new DecimalType(data.getCurrentTemperature()));
            updateState(new ChannelUID(getThing().getUID(), CURRENT_HUMIDITY),
                    new DecimalType(data.getCurrentHumidity()));
            updateState(new ChannelUID(getThing().getUID(), SYSTEM_MODE),
                    new StringType(data.getCurrentSystemMode().toString()));
            updateState(new ChannelUID(getThing().getUID(), HEAT_SETPOINT),
                    new DecimalType(data.getHeatSetPoint()));
            updateState(new ChannelUID(getThing().getUID(), COOL_SETPOINT),
                    new DecimalType(data.getCoolSetPoint()));
            updateState(new ChannelUID(getThing().getUID(), FAN_MODE),
                    new StringType(data.getCurrentFanMode().toString()));
            updateState(new ChannelUID(getThing().getUID(), SCHEDULE_MODE),
                    new StringType(data.getCurrentSetPoint().toString()));
            Calendar holdUntil = Calendar.getInstance();
            holdUntil.set(Calendar.HOUR_OF_DAY,0);
            holdUntil.set(Calendar.MINUTE,0);
            holdUntil.set(Calendar.SECOND,0);
            holdUntil.set(Calendar.MILLISECOND,0);
            holdUntil.add(Calendar.MILLISECOND,getMillisecondsFromPeriod(data.getHoldUntilTime()));
            updateState(new ChannelUID(getThing().getUID(), HOLD_UNTIL),
                        new DateTimeType(holdUntil));
        } else {
            updateStatus(ThingStatus.OFFLINE);
        }
    }

    /**
     * gets the bridge handler used by this thermostat, so we can get access to global web api shared by all thermostats
     * @return bridge handler used by thermostat
     */
    private synchronized HoneywellBridgeHandler getHoneywellBridgeHandler() {
        if (this.bridgeHandler == null) {
            Bridge bridge = getBridge();
            if (bridge == null) {
                logger.debug("Required bridge not defined for device {}.", deviceID);
                return null;
            }
            ThingHandler handler = bridge.getHandler();
            if (handler instanceof HoneywellBridgeHandler) {
                this.bridgeHandler = (HoneywellBridgeHandler) handler;
                this.bridgeHandler.registerThermostatStatusListener(this);
            } else {
                logger.debug(
                        "No available bridge handler found for device {} bridge {} .",
                        deviceID, bridge.getUID());
                return null;
            }
        }
        return this.bridgeHandler;
    }


}
