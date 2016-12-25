/**
 * Copyright (c) 2014-2016 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.honeywellmytotalcomfortthermostat.internal.discovery;

import org.eclipse.smarthome.config.discovery.AbstractDiscoveryService;
import org.eclipse.smarthome.config.discovery.DiscoveryResult;
import org.eclipse.smarthome.config.discovery.DiscoveryResultBuilder;
import org.eclipse.smarthome.core.thing.Bridge;
import org.eclipse.smarthome.core.thing.ThingTypeUID;
import org.eclipse.smarthome.core.thing.ThingUID;
import org.openhab.binding.honeywellmytotalcomfortthermostat.HoneywellThermostatBindingConstants;
import org.openhab.binding.honeywellmytotalcomfortthermostat.handler.HoneywellBridgeHandler;
import org.openhab.binding.honeywellmytotalcomfortthermostat.handler.HoneywellThermostatHandler;
import org.openhab.binding.honeywellmytotalcomfortthermostat.handler.ThermStatusListener;
import org.openhab.binding.honeywellmytotalcomfortthermostat.internal.data.HoneywellThermostatData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * The {@link HoneywellThermDiscoveryService} class is used to discover Thermostats managed by the Honeywell MyTotalConnectComfort web site
 */
public class HoneywellThermDiscoveryService extends AbstractDiscoveryService implements ThermStatusListener {

	private final static Logger logger = LoggerFactory.getLogger(HoneywellThermDiscoveryService.class);

	private HoneywellBridgeHandler honeywellBridgeHandler;

	/**
	 * constructor
	 * @param honeywellBridgeHandler bridge handler for discovery service
	 */
	public HoneywellThermDiscoveryService(HoneywellBridgeHandler honeywellBridgeHandler) {
		super(HoneywellBridgeHandler.SUPPORTED_THING_TYPES_UIDS, 10, true);
		this.honeywellBridgeHandler = honeywellBridgeHandler;
	}

	/**
	 * constructor
	 */
	public HoneywellThermDiscoveryService() {
		super(HoneywellThermostatHandler.SUPPORTED_THING_TYPES_UIDS, 10, true);
	}

	@Override
	public Set<ThingTypeUID> getSupportedThingTypes() {
		return honeywellBridgeHandler.SUPPORTED_THING_TYPES_UIDS;
	}

	@Override
	public void onThermostatAdded(Bridge bridge, HoneywellThermostatData therm) {
		String uidName = therm.getDeviceId();
		ThingTypeUID thingType = HoneywellThermostatBindingConstants.HONEY_THERM_THING;
		Map<String,Object> properties = new HashMap<String,Object>();
		properties.put(HoneywellThermostatBindingConstants.DEVICE_ID, uidName);
		logger.debug("Adding new therm {} with device id '{}' to smarthome inbox", therm.getClass().getSimpleName(), uidName);
		ThingUID thingUID = new ThingUID(thingType,bridge.getUID(), therm.getDeviceId());
		DiscoveryResult discoveryResult = DiscoveryResultBuilder.create(thingUID)
				.withProperties(properties)
				.withBridge(bridge.getUID())
				.withLabel("Thermostat - " + therm.getDeviceId())
				.build();
		thingDiscovered(discoveryResult);
	}

	@Override
	protected void startScan() {
		honeywellBridgeHandler.findNewThermostats();
	}

	@Override
	public void onThermostatStateChanged(ThingUID bridge, HoneywellThermostatData therm) {
		// this can be ignored here
	}

	@Override
	public void onThermostatRemoved(HoneywellBridgeHandler bridge, HoneywellThermostatData therm) {
		// this can be ignored here
	}

	/**
	 * registers this as thermostat listener
	 */
	public void activate() {
		honeywellBridgeHandler.registerThermostatStatusListener(this);
	}

	/**
	 * unregister thermostat data
	 */
	public void deactivate() {
		honeywellBridgeHandler.unregisterThermostatStatusListener(this);
	}


}
