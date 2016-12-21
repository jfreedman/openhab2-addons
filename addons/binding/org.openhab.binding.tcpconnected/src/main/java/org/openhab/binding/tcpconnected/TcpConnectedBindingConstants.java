/**
 * Copyright (c) 2014-2016 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.tcpconnected;

import org.eclipse.smarthome.core.thing.ThingTypeUID;

/**
 * The {@link TcpConnectedBindingConstants} class defines common constants, which are
 * used across the whole binding.
 */
public class TcpConnectedBindingConstants {

    public static final String BINDING_ID = "tcpconnected";
    
    // List of all Thing Type UIDs
    public final static ThingTypeUID LIGHT_THING_TYPE= new ThingTypeUID(BINDING_ID, "light");
    
    public final static ThingTypeUID BRIDGE_THING_TYPE= new ThingTypeUID(BINDING_ID, "bridge");

    // List of all Channel ids
    public final static String LEVEL_CHANNEL = "level";
    public final static String NAME_CHANNEL = "name";

    // List of all Parameters
    public final static String BRIDGE_PARAMETER_HOST = "host";
    public final static String BRIDGE_PARAMETER_REFRESH_INTERVAL = "refreshInterval";

    // Properties
    public final static String BRIDGE_PROPERTY_TOKEN = "token";
    public final static String BRIDGE_PROPERTY_ACCESS_KEY = "accessKey";
    public final static String DID_PROPERTY = "did";


}
