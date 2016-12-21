/**
 * Copyright (c) 2014-2016 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.tcpconnected.handler;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.smarthome.config.core.Configuration;
import org.eclipse.smarthome.core.thing.*;
import org.eclipse.smarthome.core.thing.binding.BaseBridgeHandler;
import org.eclipse.smarthome.core.types.Command;
import org.eclipse.smarthome.core.types.RefreshType;
import org.openhab.binding.tcpconnected.TcpConnectedBindingConstants;
import org.openhab.binding.tcpconnected.internal.TcpConnectedClient;
import org.openhab.binding.tcpconnected.internal.items.LightConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.openhab.binding.tcpconnected.TcpConnectedBindingConstants.*;

/**
 * {@link TcpConnectedBridgeHandler} is the handler for a tcp connected gateway and
 * connects it to the framework.
 *
 */
public class TcpConnectedBridgeHandler extends BaseBridgeHandler {
	private Logger logger = LoggerFactory
			.getLogger(TcpConnectedBridgeHandler.class);

	public final static Set<ThingTypeUID> SUPPORTED_THING_TYPES_UIDS = Collections
			.singleton(TcpConnectedBindingConstants.BRIDGE_THING_TYPE);

	public String host = "";
	public int port = 80;

	private String accessKey = "";
	private String token = "";

	public int refreshInterval = 30000;

	private TcpConnectedClient client;

	private HashSet<String> lastActiveLights = new HashSet<String>();

	private ScheduledFuture<?> pollingJob;

	Runnable setupRunnable = new Runnable() {
		@Override
		public void run() {
			try {
				if(getThing()==null) {
					scheduler.schedule(setupRunnable, 1 , TimeUnit.SECONDS);
					return;
				}
				client = new TcpConnectedClient();
				client.setHost(host);
				token = getThing().getProperties().get(BRIDGE_PROPERTY_TOKEN);
				accessKey = getThing().getProperties().get(BRIDGE_PROPERTY_ACCESS_KEY);
				if (token == null || token.isEmpty() || token.equals("token")) {
					// initialize gateway
					try {
						accessKey = UUID.randomUUID().toString();
						token = client.generateToken(accessKey);
						logger.info("created token of:" + token);
						Map<String, String> properties = editProperties();
						properties.put(BRIDGE_PROPERTY_TOKEN, token);
						properties.put(BRIDGE_PROPERTY_ACCESS_KEY, accessKey);
						updateProperties(properties);
						updateThing(getThing());
						logger.info("persisted properties");
						updateStatus(ThingStatus.ONLINE);
						return;
					} catch (Exception ex) {
						updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.HANDLER_INITIALIZING_ERROR, "Cannot generate token, ensure you pressed the sync button on the gateway and try again");
						logger.info("error generating token", ex);
						Runnable connectRunnable = new Runnable() {
							@Override
							public void run() {
								initialize();
							}
						};
						scheduler.schedule(connectRunnable, 15, TimeUnit.SECONDS);
					}
				}

				client.setToken(token);
				client.setAccessKey(accessKey);
				client.update();
			/*
			List<LightConfig> lightConfigs = client.getItems();
			List<Thing> lights = getBridge().getThings();
			for(Thing light : lights) {
				boolean found = false;
				for(LightConfig lightConfig : lightConfigs) {
					if(light.getProperties().get(DID_PROPERTY).equals(lightConfig.getDid())) {
						found = true;
						break;
					}
				}
				if(!found) {
					light.setStatusInfo(new ThingStatusInfo(ThingStatus.OFFLINE,ThingStatusDetail.HANDLER_MISSING_ERROR,"light not found"));
				}
			} */
				updateStatus(ThingStatus.ONLINE);
				startAutomaticRefresh();


			} catch (Exception ex) {
				logger.info("Error connecting", ex);
				updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR, "could not connect to gateway");
			}
		}
	};

	private Runnable pollingRunnable = new Runnable() {
		@Override
		public void run() {
		    try {
                client.update();
                updateStatus(ThingStatus.ONLINE);
            } catch (Exception ex) {
		        updateStatus(ThingStatus.OFFLINE,ThingStatusDetail.COMMUNICATION_ERROR, "unable to fetch light data from gateway");
                logger.info("error fetching light status from gateway", ex);
                return;
            }
			for (LightConfig light : client.getItems()) {
				if (lastActiveLights != null
						&& lastActiveLights.contains(light.getDid())) {
					for (LightStatusListener lightStatusListener : lightStatusListeners) {
						try {
                            lightStatusListener.onLightStateChanged(
									getThing().getUID(), light);
						} catch (Exception e) {
							logger.error(
									"An exception occurred while calling the LightStatusListener",
									e);
						}
					}
				} else {
				    for (LightStatusListener lightStatusListener : lightStatusListeners) {
						try {
							lightStatusListener.onLightAdded(getThing(),
									light);
							lightStatusListener.onLightStateChanged(
									getThing().getUID(), light);
						} catch (Exception e) {
							logger.error(
									"An exception occurred while calling the LightStatusListener",
									e);
						}
						lastActiveLights.add(light.getDid());
					}
				}
			}
		}
	};

	private List<LightStatusListener> lightStatusListeners = new CopyOnWriteArrayList<>();

	public TcpConnectedBridgeHandler(Bridge bridge) {

		super(bridge);
	}

	@Override
	public void handleCommand(ChannelUID channelUID, Command command) {
		if (command instanceof RefreshType) {
		    try {
                client.update();
            } catch (Exception ex) {
                logger.info("error refreshing light list");
            }
		} else {
			logger.warn("received invalid command for tcp connected bridge '{}'.",
					host);
		}
	}

	private synchronized void startAutomaticRefresh() {
		if (pollingJob == null || pollingJob.isCancelled()) {
			pollingJob = scheduler.scheduleAtFixedRate(pollingRunnable, 0,
					refreshInterval, TimeUnit.MILLISECONDS);
		}
	}


	public LightConfig getLight(String did) {
		return client.getLight(did);
	}


	public TcpConnectedClient getClient() {
		return client;
	}

	@Override
	public void initialize() {
		logger.debug("Initializing Tcp Connected handler.");
		Configuration conf = this.getConfig();
		super.initialize();
		if (conf.get(BRIDGE_PARAMETER_HOST) != null) {
			this.host = String.valueOf(conf.get(BRIDGE_PARAMETER_HOST));
		}
		if (conf.get(BRIDGE_PARAMETER_REFRESH_INTERVAL) != null) {
			this.refreshInterval = ((BigDecimal) conf
					.get(BRIDGE_PARAMETER_REFRESH_INTERVAL)).intValue();
		}

		if (host != null && !host.isEmpty()) {

			scheduler.schedule(setupRunnable, 5, TimeUnit.SECONDS);
		} else {
			logger.warn(
					"Couldn't connect to TCP Connected Gateway because of missing connection parameters [Host '{}':'{}'].",
					host, port);
			updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR, "host not configured");
			Runnable connectRunnable = new Runnable() {
				@Override
				public void run() {
					initialize();
				}
			};
			scheduler.schedule(connectRunnable, 15, TimeUnit.SECONDS);
		}
	}

	@Override
	public void dispose() {
		try {
            pollingJob.cancel(true);

        } catch (Exception ex) {}
        try {
            client.disconnect();

        } catch (Exception ex) {}

		super.dispose();
	}

	public boolean registerLightStatusListener(
			LightStatusListener lightStatusListener) {
		if (lightStatusListener == null) {
			throw new IllegalArgumentException(
					"It's not allowed to pass a null lightStatusListener.");
		}
		return lightStatusListeners.add(lightStatusListener);
	}

	public boolean unregisterLightStatusListener(
			LightStatusListener lightStatusListener) {
		return lightStatusListeners.remove(lightStatusListener);
	}
}
