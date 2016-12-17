/**
 * Copyright (c) 2014-2016 by the respective copyright holders.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.honeywellthermostat.internal;

import static org.openhab.binding.honeywellthermostat.HoneywellThermostatBindingConstants.HONEY_THERM_THING;

import java.util.Collections;
import java.util.Set;

import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingTypeUID;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandlerFactory;
import org.eclipse.smarthome.core.thing.binding.ThingHandler;
import org.openhab.binding.honeywellthermostat.handler.HoneywellThermostatHandler;

/**
 * The {@link HoneywellThermostatHandlerFactory} is responsible for creating things and thing
 * handlers.
 *
 * @author Jeremy Freedman - Initial contribution
 */
public class HoneywellThermostatHandlerFactory extends BaseThingHandlerFactory {

    private final static Set<ThingTypeUID> SUPPORTED_THING_TYPES_UIDS = Collections.singleton(HONEY_THERM_THING);

    @Override
    public boolean supportsThingType(ThingTypeUID thingTypeUID) {
        return SUPPORTED_THING_TYPES_UIDS.contains(thingTypeUID);
    }

    @Override
    protected ThingHandler createHandler(Thing thing) {
        ThingTypeUID thingTypeUID = thing.getThingTypeUID();
        if (thingTypeUID.equals(HONEY_THERM_THING)) {
            return new HoneywellThermostatHandler(thing);
        }

        return null;
    }
}
