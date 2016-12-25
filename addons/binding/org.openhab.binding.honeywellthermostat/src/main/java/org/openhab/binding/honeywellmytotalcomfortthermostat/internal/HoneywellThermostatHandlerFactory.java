/**
 * Copyright (c) 2014-2016 by the respective copyright holders.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.honeywellmytotalcomfortthermostat.internal;

import java.util.*;

import com.google.common.collect.Sets;
import org.eclipse.smarthome.config.core.Configuration;
import org.eclipse.smarthome.config.discovery.DiscoveryService;
import org.eclipse.smarthome.core.thing.Bridge;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingTypeUID;
import org.eclipse.smarthome.core.thing.ThingUID;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandlerFactory;
import org.eclipse.smarthome.core.thing.binding.ThingHandler;
import org.openhab.binding.honeywellmytotalcomfortthermostat.HoneywellThermostatBindingConstants;
import org.openhab.binding.honeywellmytotalcomfortthermostat.handler.HoneywellBridgeHandler;
import org.openhab.binding.honeywellmytotalcomfortthermostat.handler.HoneywellThermostatHandler;
import org.openhab.binding.honeywellmytotalcomfortthermostat.internal.discovery.HoneywellThermDiscoveryService;
import org.osgi.framework.ServiceRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link HoneywellThermostatHandlerFactory} is responsible for creating things and thing
 * handlers.
 *
 * @author Jeremy Freedman - Initial contribution
 */
public class HoneywellThermostatHandlerFactory extends BaseThingHandlerFactory {

    private Logger logger = LoggerFactory
            .getLogger(HoneywellThermostatHandlerFactory.class);

    private final static Set<ThingTypeUID> SUPPORTED_THING_TYPES_UIDS = Sets.union(HoneywellThermostatHandler.SUPPORTED_THING_TYPES_UIDS, HoneywellBridgeHandler.SUPPORTED_THING_TYPES_UIDS);

    private Map<ThingHandler,ServiceRegistration<?>> discoveryServiceReg = new HashMap<ThingHandler,ServiceRegistration<?>>();

    @Override
    public boolean supportsThingType(ThingTypeUID thingTypeUID) {
        return SUPPORTED_THING_TYPES_UIDS.contains(thingTypeUID);
    }

    @Override
    public Thing createThing(ThingTypeUID thingTypeUID, Configuration configuration, ThingUID thingUID,
                             ThingUID bridgeUID) {
        if (HoneywellBridgeHandler.SUPPORTED_THING_TYPES_UIDS.contains(thingTypeUID)) {
            return super.createThing(thingTypeUID, configuration, thingUID, null);
        }
        if (HoneywellThermostatHandler.SUPPORTED_THING_TYPES_UIDS.contains(thingTypeUID)) {
            ThingUID deviceUID = getThremostatUID(thingTypeUID, thingUID, configuration, bridgeUID);
            return super.createThing(thingTypeUID, configuration, deviceUID, bridgeUID);
        }
        throw new IllegalArgumentException("The thing type " + thingTypeUID + " is not supported by the binding.");
    }

    @Override
    protected void removeHandler(ThingHandler thingHandler) {
        if (this.discoveryServiceReg.containsKey(thingHandler)) {
            HoneywellThermDiscoveryService service = (HoneywellThermDiscoveryService) bundleContext
                    .getService(discoveryServiceReg.get(thingHandler).getReference());
            service.deactivate();
            discoveryServiceReg.get(thingHandler).unregister();
            discoveryServiceReg.remove(thingHandler);
        }
        super.removeHandler(thingHandler);
    }

    @Override
    protected ThingHandler createHandler(Thing thing) {
        ThingTypeUID thingTypeUID = thing.getThingTypeUID();
        if (HoneywellBridgeHandler.SUPPORTED_THING_TYPES_UIDS.contains(thingTypeUID)) {
            HoneywellBridgeHandler handler = new HoneywellBridgeHandler((Bridge)thing);
            registerDeviceDiscoveryService(handler);
            return handler;
        }
        else if (HoneywellThermostatHandler.SUPPORTED_THING_TYPES_UIDS.contains(thingTypeUID)) {
            return new HoneywellThermostatHandler(thing);
        }

        return null;
    }

    /**
     * Registers the discovery service
     * @param bridgeHandler bridge handler register discovery with
     */
    private void registerDeviceDiscoveryService(HoneywellBridgeHandler bridgeHandler) {
        HoneywellThermDiscoveryService discoveryService = new HoneywellThermDiscoveryService(bridgeHandler);
        discoveryService.activate();
        this.discoveryServiceReg.put(bridgeHandler,bundleContext.registerService(DiscoveryService.class.getName(), discoveryService,
                new Hashtable<String, Object>()));
    }

    /**
     * gets thermostat thingUID for new thermostat
     * @param thingTypeUID the uid of the thing type
     * @param thingUID the thing uid
     * @param configuration the configuration of the thing
     * @param bridgeUID the bridge uid
     * @return the ThingUID matching the thermostat
     */
    private ThingUID getThremostatUID(ThingTypeUID thingTypeUID, ThingUID thingUID, Configuration configuration,
                                             ThingUID bridgeUID) {
        if (thingUID == null) {
            String name = (String) configuration.get(HoneywellThermostatBindingConstants.DEVICE_ID);
            thingUID = new ThingUID(thingTypeUID, name, bridgeUID.getId());
        }
        return thingUID;
    }


}
