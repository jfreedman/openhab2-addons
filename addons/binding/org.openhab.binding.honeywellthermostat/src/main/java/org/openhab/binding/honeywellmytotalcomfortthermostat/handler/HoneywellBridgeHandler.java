/**
 * Copyright (c) 2014-2016 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.honeywellmytotalcomfortthermostat.handler;

import org.eclipse.smarthome.config.core.Configuration;
import org.eclipse.smarthome.core.thing.*;
import org.eclipse.smarthome.core.thing.binding.BaseBridgeHandler;
import org.eclipse.smarthome.core.types.Command;
import org.openhab.binding.honeywellmytotalcomfortthermostat.internal.data.HoneywellThermostatData;
import org.openhab.binding.honeywellmytotalcomfortthermostat.internal.webapi.MyTotalComfortClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;

import static org.openhab.binding.honeywellmytotalcomfortthermostat.HoneywellThermostatBindingConstants.EMAIL;
import static org.openhab.binding.honeywellmytotalcomfortthermostat.HoneywellThermostatBindingConstants.HONEY_BRIDGE;
import static org.openhab.binding.honeywellmytotalcomfortthermostat.HoneywellThermostatBindingConstants.PASSWORD;

/**
 * {@link HoneywellBridgeHandler} is the bridge handler managing the webapi to the honeywell mytotalconnectcomfort web site
 * contains logic to find new thermostats for auto discover, and registers and unregisters event listeners for thermostat events
 */
public class HoneywellBridgeHandler extends BaseBridgeHandler {

	private Logger logger = LoggerFactory
			.getLogger(HoneywellBridgeHandler.class);

	private String email = "";
	private String password = "";

	private MyTotalComfortClient client;

	private HashSet<String> lastActiveThermostats = new HashSet<String>();

	private List<ThermStatusListener> thermStatusListeners = new CopyOnWriteArrayList<ThermStatusListener>();

	public final static Set<ThingTypeUID> SUPPORTED_THING_TYPES_UIDS = Collections
			.singleton(HONEY_BRIDGE);

	/**
	 * Constructor
	 * @param bridge the bridge used by this thermostat
	 */
	public HoneywellBridgeHandler(Bridge bridge) {
		super(bridge);
	}

	@Override
	public void initialize() {
		logger.debug("Initializing therm handler.");
		Configuration conf = this.getConfig();
		super.initialize();
		if (conf.get(EMAIL) != null) {
			this.email = String.valueOf(conf.get(EMAIL));
		}
		if (conf.get(PASSWORD) != null) {
			this.password = String.valueOf(conf.get(PASSWORD));
		}

		if (email!=null && password!=null && !email.isEmpty() && !password.isEmpty()) {
			client = new MyTotalComfortClient();
			client.setUsername(email);
			client.setPassword(password);
		} else {
			logger.info(
					"no email or password configured for bridge");
			updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR, "email or password not configured for honeywell mytotalconnectcomfort site.  Update bridge configuration with login information.");
			Runnable connectRunnable = new Runnable() {
				@Override
				public void run() {
					initialize();
				}
			};
			scheduler.schedule(connectRunnable, 60, TimeUnit.SECONDS);
		}
	}


	@Override
	public void handleCommand(ChannelUID channelUID, Command command) {
		//there are no commmands for the bridge, return
		return;
	}

	@Override
	public void dispose() {
		try {
			client.disconnect();

		} catch (Exception ex) {}
		super.dispose();
	}

	/**
	 * queries web api for thermostat data to find new thermostats added to the system
	 * This is used by auto discovery
	 */
	public void findNewThermostats() {
		List<HoneywellThermostatData> thermostats = new ArrayList<HoneywellThermostatData>();
		try {
			thermostats = client.getThermostatsData();
			updateStatus(ThingStatus.ONLINE);
		} catch (Exception ex) {
			updateStatus(ThingStatus.OFFLINE,ThingStatusDetail.COMMUNICATION_ERROR, "unable to authenticate or retrieve data from Honeywell My Total Connect Comfort site, make sure login info is correct and internet connection is active.");
			logger.info("error fetching therm data from gateway", ex);
			return;
		}
		for (HoneywellThermostatData therm : thermostats) {
			if (lastActiveThermostats != null
					&& lastActiveThermostats.contains(therm.getDeviceId())) {
				for (ThermStatusListener thermStatusListener : thermStatusListeners) {
					try {
						thermStatusListener.onThermostatStateChanged(
								getThing().getUID(), therm);
					} catch (Exception e) {
						logger.info(
								"An exception occurred while calling the ThermStatusListener",
								e);
					}
				}
			} else {
				for (ThermStatusListener thermStatusListener : thermStatusListeners) {
					try {
						thermStatusListener.onThermostatAdded(getThing(),
								therm);
						thermStatusListener.onThermostatStateChanged(
								getThing().getUID(), therm);
					} catch (Exception e) {
						logger.info(
								"An exception occurred while calling the ThremStatusListener",
								e);
					}
					lastActiveThermostats.add(therm.getDeviceId());
				}
			}
		}
	};


	/**
	 * gets cached thermostat data
	 * @param deviceId the device id of the thermostat
	 * @return thermostat data
	 */
	public HoneywellThermostatData getThermostat(String deviceId) {
		return client.getThermostat(deviceId);
	}

	/**
	 * gets the current web client
	 * @return api to interact with mytotalconnectcomfort
	 */
	public MyTotalComfortClient getClient() {
		return client;
	}


	/**
	 * Registers thermostat listener that will fire when a thermostat event occurs
	 * @param thermStatusListener the listener to register
	 * @return if the listener was registered
	 */
	public boolean registerThermostatStatusListener(
			ThermStatusListener thermStatusListener) {
		if (thermStatusListener == null) {
			throw new IllegalArgumentException(
					"It's not allowed to pass a null thermStatusListener.");
		}
		return thermStatusListeners.add(thermStatusListener);
	}

	/**
	 * Unrgisters thermostat listener
	 * @param thermStatusListener the listener to unregister
	 * @return if the listener was unregistered
	 */
	public boolean unregisterThermostatStatusListener(
			ThermStatusListener thermStatusListener) {
		return thermStatusListeners.remove(thermStatusListener);
	}
}
