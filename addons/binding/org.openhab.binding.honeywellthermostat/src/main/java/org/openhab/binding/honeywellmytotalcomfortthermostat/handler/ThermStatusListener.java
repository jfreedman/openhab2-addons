/**
 * Copyright (c) 2014-2016 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.honeywellmytotalcomfortthermostat.handler;

import org.eclipse.smarthome.core.thing.Bridge;
import org.eclipse.smarthome.core.thing.ThingUID;
import org.openhab.binding.honeywellmytotalcomfortthermostat.internal.data.HoneywellThermostatData;

/**
 * The {@link ThermStatusListener} is notified when a thermostat status has changed
 * or a thermostat has been removed or added.
 *
 */
public interface ThermStatusListener {

	/**
	 * This method is called whenever the state of the given thermostat has changed.
	 * 
	 * @param bridge The bridge the changed therm is connected to.
	 * @param therm The therm which received the state update.
	 */
	void onThermostatStateChanged(ThingUID bridge, HoneywellThermostatData therm);

	/**
	 * This method us called whenever a thermostat is removed.
	 * 
	 * @param bridge The bridge the removed therm was connected to.
	 * @param therm The therm which is removed.
	 */
	void onThermostatRemoved(HoneywellBridgeHandler bridge, HoneywellThermostatData therm);

	/**
	 * This method us called whenever a therm is added.
	 *
	 * @param bridge The bridge the added therm is connected to.
	 * @param therm The therm which is added.
	 */
	void onThermostatAdded(Bridge bridge, HoneywellThermostatData therm);

}
