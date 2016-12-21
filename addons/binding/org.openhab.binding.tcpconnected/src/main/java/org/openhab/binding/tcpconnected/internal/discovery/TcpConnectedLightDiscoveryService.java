/**
 * Copyright (c) 2014-2016 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.tcpconnected.internal.discovery;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.eclipse.smarthome.config.discovery.AbstractDiscoveryService;
import org.eclipse.smarthome.config.discovery.DiscoveryResult;
import org.eclipse.smarthome.config.discovery.DiscoveryResultBuilder;
import org.eclipse.smarthome.core.thing.Bridge;
import org.eclipse.smarthome.core.thing.ThingTypeUID;
import org.eclipse.smarthome.core.thing.ThingUID;
import org.openhab.binding.tcpconnected.TcpConnectedBindingConstants;
import org.openhab.binding.tcpconnected.handler.LightStatusListener;
import org.openhab.binding.tcpconnected.handler.TcpConnectedBridgeHandler;
import org.openhab.binding.tcpconnected.handler.TcpConnectedHandler;
import org.openhab.binding.tcpconnected.internal.items.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link TcpConnectedLightDiscoveryService} class is used to discover Tcp Connected
 * lights on a TCP Connected gateway
 */
public class TcpConnectedLightDiscoveryService extends AbstractDiscoveryService implements LightStatusListener {

	private final static Logger logger = LoggerFactory.getLogger(TcpConnectedLightDiscoveryService.class);

	private TcpConnectedBridgeHandler tcpConnectedBridgeHandler;

	public TcpConnectedLightDiscoveryService(TcpConnectedBridgeHandler tcpConnectedBridgeHandler) {
		super(TcpConnectedHandler.SUPPORTED_THING_TYPES_UIDS, 10, true);
		this.tcpConnectedBridgeHandler = tcpConnectedBridgeHandler;
	}

	public TcpConnectedLightDiscoveryService() {
		super(TcpConnectedHandler.SUPPORTED_THING_TYPES_UIDS, 10, true);
	}

	public void activate() {
		tcpConnectedBridgeHandler.registerLightStatusListener(this);
	}

	public void deactivate() {
		tcpConnectedBridgeHandler.unregisterLightStatusListener(this);
	}

	@Override
	public Set<ThingTypeUID> getSupportedThingTypes() {
		return tcpConnectedBridgeHandler.SUPPORTED_THING_TYPES_UIDS;
	}

	@Override
	public void onLightAdded(Bridge bridge, LightConfig light) {
		String uidName = light.getDid();
		logger.debug("light {} found",light);
		ThingTypeUID thingType = TcpConnectedBindingConstants.LIGHT_THING_TYPE;
		Map<String,Object> properties = new HashMap<String,Object>();
		properties.put(TcpConnectedBindingConstants.DID_PROPERTY, uidName);
		logger.trace("Adding new light {} with name '{}' to smarthome inbox", light.getClass().getSimpleName(), uidName);
		ThingUID thingUID = new ThingUID(thingType,bridge.getUID(), String.valueOf(light.getDid()));
		DiscoveryResult discoveryResult = DiscoveryResultBuilder.create(thingUID)
				.withProperties(properties)
				.withBridge(bridge.getUID())
				.withLabel(light.getName())
				.build();
		thingDiscovered(discoveryResult);
	}

	@Override
	protected void startScan() {
		// this can be ignored here as we discover via the PulseaudioClient.update() mechanism
	}

	@Override
	public void onLightStateChanged(ThingUID bridge, LightConfig light) {
		// this can be ignored here
	}

	@Override
	public void onLightRemoved(TcpConnectedBridgeHandler bridge, LightConfig light) {
		// this can be ignored here
	}
}
