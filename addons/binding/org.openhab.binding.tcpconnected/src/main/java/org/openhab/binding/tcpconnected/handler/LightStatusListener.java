/**
 * Copyright (c) 2014-2016 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.tcpconnected.handler;

import org.eclipse.smarthome.core.thing.Bridge;
import org.eclipse.smarthome.core.thing.ThingUID;
import org.openhab.binding.tcpconnected.internal.items.LightConfig;

/**
 * The {@link LightStatusListener} is notified when a light status has changed
 * or a light has been removed or added.
 *
 */
public interface LightStatusListener {

	/**
	 * This method is called whenever the state of the given light has changed.
	 * 
	 * @param bridge
	 *            The tcp connected bridge the changed light is connected to.
	 * @param light
	 *            The light which received the state update.
	 */
	public void onLightStateChanged(ThingUID bridge, LightConfig light);

	/**
	 * This method us called whenever a light is removed.
	 * 
	 * @param bridge
	 *            The tcp connected bridge the removed light was connected to.
	 * @param light
	 *            The light which is removed.
	 */
	public void onLightRemoved(TcpConnectedBridgeHandler bridge, LightConfig light);

	/**
	 * This method us called whenever a light is added.
	 *
	 * @param bridge
	 *            The tcp connected bridge the added light is connected to.
	 * @param light
	 *            The light which is added.
	 */
	public void onLightAdded(Bridge bridge, LightConfig light);

}
