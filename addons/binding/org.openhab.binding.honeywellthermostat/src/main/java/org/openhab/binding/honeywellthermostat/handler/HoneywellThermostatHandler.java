/**
 * Copyright (c) 2014-2016 by the respective copyright holders.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.honeywellthermostat.handler;

import static org.openhab.binding.honeywellthermostat.HoneywellThermostatBindingConstants.*;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.smarthome.config.core.Configuration;
import org.eclipse.smarthome.core.library.types.DecimalType;
import org.eclipse.smarthome.core.library.types.StringType;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.ThingStatusDetail;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandler;
import org.eclipse.smarthome.core.types.Command;
import org.eclipse.smarthome.core.types.RefreshType;
import org.openhab.binding.honeywellthermostat.internal.data.HoneywellThermostatData;
import org.openhab.binding.honeywellthermostat.internal.data.HoneywellThermostatFanMode;
import org.openhab.binding.honeywellthermostat.internal.data.HoneywellThermostatSystemMode;
import org.openhab.binding.honeywellthermostat.internal.webapi.HoneywellWebsiteJetty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link HoneywellThermostatHandler} is responsible for handling thermostat commands
 *
 * @author Jeremy Freedman - Initial contribution
 */
public class HoneywellThermostatHandler extends BaseThingHandler {
    private Logger logger = LoggerFactory.getLogger(HoneywellThermostatHandler.class);

    HoneywellWebsiteJetty webapi;
    ScheduledFuture<?> refreshJob;

    private String deviceID = null;

    private HoneywellThermostatData thermodata;

    public HoneywellThermostatHandler(Thing thing) {
        super(thing);

    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        if (command instanceof RefreshType) {
            refreshThermostatData(thermodata);
            return;
        }
        HoneywellThermostatData updates = thermodata;
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
                        HoneywellThermostatSystemMode.getEnum(sysmode_val.toString()));
                break;
            case FAN_MODE:
                StringType fanmode_val = (StringType) command;
                updates.setCurrentFanMode(HoneywellThermostatFanMode.getEnum(fanmode_val.toString()));
                break;
        }

        if (!webapi.submitThermostatChange(deviceID, updates)) {
            logger.info("Failed to submit changes to honeywell site.");
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR,
                    "Failed to submit changes to Honeywell site.");
        } else {
            thermodata = updates;
            refreshThermostatData(thermodata);
        }
    }

    private void refreshThermostatData() {
        thermodata = webapi.getTherostatData(deviceID);
        refreshThermostatData(thermodata);
    }

    private void refreshThermostatData(HoneywellThermostatData data) {

        if(data!=null) {
            updateState(new ChannelUID(getThing().getUID(), CURRENT_TEMPERATURE),
                    new DecimalType(data.getCurrentTemperature()));
            updateState(new ChannelUID(getThing().getUID(), CURRENT_HUMIDITY),
                    new DecimalType(data.getCurrentHumidity()));
            updateState(new ChannelUID(getThing().getUID(), SYSTEM_MODE),
                    new StringType(String.valueOf(data.getCurrentSystemMode().getValue())));
            updateState(new ChannelUID(getThing().getUID(), HEAT_SETPOINT),
                    new DecimalType(data.getHeatSetPoint()));
            updateState(new ChannelUID(getThing().getUID(), COOL_SETPOINT),
                    new DecimalType(data.getCoolSetPoint()));
            updateState(new ChannelUID(getThing().getUID(), FAN_MODE),
                    new StringType(String.valueOf(data.getCurrentFanMode().getValue())));
        }
    }


    @Override
    public void initialize() {
        super.initialize();
        Configuration conf = this.getConfig();
        webapi = HoneywellWebsiteJetty.getInstance();

        if (conf.get(EMAIL) != null) {
            webapi.setUsername(String.valueOf(conf.get(EMAIL)));
        }
        if (conf.get(PASSWORD) != null) {
            webapi.setPassword(String.valueOf(conf.get(PASSWORD)));
        }
        if (conf.get(DEVICE_ID) != null) {
            deviceID = String.valueOf(conf.get(DEVICE_ID));
        }
        if (!webapi.isLoginValid()) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR,
                    "Login username/password invalid.");
        }
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                refreshThermostatData();
            }
        };
        refreshJob = scheduler.scheduleAtFixedRate(runnable, 0, 60, TimeUnit.SECONDS);
    }

    @Override
    public void dispose() {
        refreshJob.cancel(true);
        webapi.dispose();
        super.dispose();
    }
}
