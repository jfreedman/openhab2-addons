/**
 * Copyright (c) 2014-2016 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.tcpconnected.handler;

import static org.openhab.binding.tcpconnected.TcpConnectedBindingConstants.*;

import java.util.*;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang.StringUtils;
import org.eclipse.smarthome.config.core.Configuration;
import org.eclipse.smarthome.config.discovery.DiscoveryService;
import org.eclipse.smarthome.core.library.types.DecimalType;
import org.eclipse.smarthome.core.library.types.IncreaseDecreaseType;
import org.eclipse.smarthome.core.library.types.OnOffType;
import org.eclipse.smarthome.core.library.types.PercentType;
import org.eclipse.smarthome.core.library.types.StringType;
import org.eclipse.smarthome.core.thing.Bridge;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.ThingTypeUID;
import org.eclipse.smarthome.core.thing.ThingUID;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandler;
import org.eclipse.smarthome.core.thing.binding.ThingHandler;
import org.eclipse.smarthome.core.types.Command;
import org.eclipse.smarthome.core.types.RefreshType;
import org.eclipse.smarthome.core.types.State;
import org.eclipse.smarthome.core.types.UnDefType;
import org.openhab.binding.tcpconnected.TcpConnectedBindingConstants;
import org.openhab.binding.tcpconnected.internal.discovery.TcpConnectedLightDiscoveryService;
import org.openhab.binding.tcpconnected.internal.items.LightConfig;
import org.osgi.framework.ServiceRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Sets;

/**
 * The {@link TcpConnectedHandler} is responsible for handling commands, which are
 * sent to one of the channels.
 *
 */
public class TcpConnectedHandler extends BaseThingHandler implements
		LightStatusListener {

	public final static Set<ThingTypeUID> SUPPORTED_THING_TYPES_UIDS = Sets.newHashSet( 
			LIGHT_THING_TYPE);
			

	private int refresh = 60; // refresh every minute as default
	ScheduledFuture<?> refreshJob;

	private TcpConnectedBridgeHandler bridgeHandler;

	private Logger logger = LoggerFactory.getLogger(TcpConnectedHandler.class);

	private Map<ThingHandler,ServiceRegistration<?>> discoveryServiceReg = new HashMap<ThingHandler,ServiceRegistration<?>>();


	private String name;

	private String did;

	private LightConfig.State state = LightConfig.State.OFF;

	private int level = 0;

	public TcpConnectedHandler(Thing thing) {
		super(thing);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void initialize() {
		logger.info("init light");
		Configuration config = getThing().getConfiguration();
		name = (String) config
				.get(TcpConnectedBindingConstants.NAME_CHANNEL);
		did = (String)getThing().getProperties().get(TcpConnectedBindingConstants.DID_PROPERTY);

		// until we get an update put the Thing offline
		updateStatus(ThingStatus.OFFLINE);
		lightOnlineWatchdog();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.smarthome.core.thing.binding.BaseThingHandler#dispose()
	 */
	@Override
	public void dispose() {
		if (refreshJob != null && !refreshJob.isCancelled()) {
			refreshJob.cancel(true);
			refreshJob = null;
		}
		updateStatus(ThingStatus.OFFLINE);
		bridgeHandler = null;
		logger.trace("Thing {} {} disposed.", getThing().getUID(), name);
		super.dispose();
	}

	private void lightOnlineWatchdog() {
		Runnable runnable = new Runnable() {
			public void run() {
				try {
					logger.info("checking light");
					TcpConnectedBridgeHandler bridgeHandler = getTcpConnectedBridgeHandler();
					if (bridgeHandler != null) {
						if (bridgeHandler.getLight(did) == null) {
							logger.info("bridge handler can't find light");
							updateStatus(ThingStatus.OFFLINE);
							bridgeHandler = null;
						} else {
							logger.info("light is online");
							updateStatus(ThingStatus.ONLINE);
						}

					} else {
						logger.info(
								"Bridge for tcp connected light {} not found.",
								name);
						updateStatus(ThingStatus.OFFLINE);
					}

				} catch (Exception e) {
					logger.debug("Exception occurred during execution: {}",
							e.getMessage(), e);
					bridgeHandler = null;
				}

			}
		};

		refreshJob = scheduler.scheduleAtFixedRate(runnable, 0, refresh,
				TimeUnit.SECONDS);
	}

	private synchronized TcpConnectedBridgeHandler getTcpConnectedBridgeHandler() {

		if (this.bridgeHandler == null) {
			Bridge bridge = getBridge();
			if (bridge == null) {
				logger.debug("Required bridge not defined for device {}.", name);
				return null;
			}
			ThingHandler handler = bridge.getHandler();
			if (handler instanceof TcpConnectedBridgeHandler) {
				this.bridgeHandler = (TcpConnectedBridgeHandler) handler;
				this.bridgeHandler.registerLightStatusListener(this);
			} else {
				logger.debug(
						"No available bridge handler found for device {} bridge {} .",
						name, bridge.getUID());
				return null;
			}
		}
		return this.bridgeHandler;
	}

	@Override
	public void handleCommand(ChannelUID channelUID, Command command) {
		TcpConnectedBridgeHandler bridge = getTcpConnectedBridgeHandler();
		if (bridge == null) {
			logger.warn("tcp connected bridge handler not found. Cannot handle command without bridge.");
			return;
		}
		if (command instanceof RefreshType) {
			logger.info("doing refresh");
			bridge.handleCommand(channelUID, command);
			return;
		}
		LightConfig light = bridge.getLight(did);

			if (light == null) {
				logger.warn("light {} not found", did);
				updateStatus(ThingStatus.OFFLINE);
				bridgeHandler = null;
				return;
			} else {

				State updateState = UnDefType.UNDEF;
				try {
				if (channelUID.getId().equals(
						TcpConnectedBindingConstants.LEVEL_CHANNEL)) {

					if (command instanceof IncreaseDecreaseType) {
						// refresh to get the current level
						try {
							bridge.getClient().update();
						} catch (Exception ex) {
						}
						light = bridge.getLight(did);
						int level = light.getLevel();
						if (command.equals(IncreaseDecreaseType.INCREASE))
							level = Math.min(100, level + 5);
						if (command.equals(IncreaseDecreaseType.DECREASE))
							level = Math.max(0, level - 5);
						bridge.getClient().setLevel(light, level);
						logger.info("set value of:" + level);
						this.level = level;
						updateState = new PercentType(level);
					} else if (command instanceof PercentType) {
						DecimalType level = (DecimalType) command;
						bridge.getClient().setLevel(light, level.intValue());
						logger.info("set value of:" + level);
						this.level = level.intValue();
						updateState = (PercentType) command;
					} else if (command instanceof DecimalType) {
						// set volume
						DecimalType level = (DecimalType) command;
						bridge.getClient().setLevel(light, level.intValue());
						logger.info("set value of:" + level);
						this.level = level.intValue();
						updateState = (DecimalType) command;
					} else if (command instanceof OnOffType) {
						bridge.getClient().setState(light,
								OnOffType.ON.equals(command));
						logger.info("set on/off of:" + command);
						state = OnOffType.ON.equals(command) ? LightConfig.State.ON : LightConfig.State.OFF;
						updateState = (OnOffType) command;
					}
				}
			} catch (Exception ex) {
					logger.info("error updating light state", ex);
				}
			logger.trace("updating " + channelUID + " to " + updateState);
			if (!updateState.equals(UnDefType.UNDEF)) {
				logger.info("firing updatestate after handling command");
				updateState(channelUID, updateState);

			}
		}
	}

	@Override
	public void onLightStateChanged(ThingUID bridge,
			LightConfig light) {
		if (light.getDid().equals(did)) {
			updateStatus(ThingStatus.ONLINE);
			if(light.getLevel()!=level || light.getState()!=state)
			logger.error("state change occurred: level:" + light.getLevel() + " state:" + light.getState());
			updateState(TcpConnectedBindingConstants.LEVEL_CHANNEL, new PercentType(light.getLevel()));
		}
	}

	@Override
	public void onLightRemoved(TcpConnectedBridgeHandler bridge,
			LightConfig light) {
		if (light.getDid().equals(did)) {
			bridgeHandler.unregisterLightStatusListener(this);
			bridgeHandler = null;
			updateStatus(ThingStatus.OFFLINE);
		}
	}

	@Override
	public void onLightAdded(Bridge bridge, LightConfig light) {
		logger.trace("new light discovered "+light+" by "+bridge);
	}


}
