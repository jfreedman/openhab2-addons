/**
 * Copyright (c) 2014-2016 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.tcpconnected.internal.items;

/**
 * light configuration
 *
 */
public class LightConfig {

	protected String did;
	protected String name;
	protected State state;
	protected int level;
	protected boolean found;
	protected boolean offline;

	public LightConfig(String did, String name, State state, int level, boolean found, boolean offine) {
		this.did = did;
		this.name = name;
		this.state=state;
		this.level=level;
		this.found = found;
		this.offline = offine;
	}

	
	public enum State {
		ON, OFF
	}

	public boolean getFound() { return found; }

	public void setFound(boolean found) { this.found=found; }

	public String getDid() {
		return did;
	}

	public String getName() {
		return name;
	}

	public int getLevel() { return level; }

	public void setLevel(int level) {
		this.level = level;
	}

	public boolean getOffline() { return offline; }

	public void setOffline(boolean offline) {
		this.offline = offline;
	}

	public State getState() {
		return state;
	}

	public void setState(State state) {
		this.state = state;
	}


	public String toString() {
		return this.getClass().getSimpleName() + " #" + did + ", name: " + name + ", state: "
				+ state + ", level: " + level;
	}
	
}
