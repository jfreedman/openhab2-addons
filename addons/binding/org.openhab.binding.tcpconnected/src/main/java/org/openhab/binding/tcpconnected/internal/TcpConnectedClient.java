/**
 * Copyright (c) 2014-2016 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.tcpconnected.internal;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.io.StringReader;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;


import org.apache.commons.lang.StringUtils;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.client.util.StringContentProvider;
import org.eclipse.jetty.util.Fields;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.openhab.binding.tcpconnected.internal.items.*;
import org.openhab.binding.tcpconnected.internal.items.LightConfig.State;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.eclipse.jetty.client.HttpClient;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

/**
 * The client connects to a tcp connected gateway via http. It reads the current state of the
 * tcp connected gateway for available lights and can send commands to the gateway.
 */
public class TcpConnectedClient {

    private final DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();


    private static final Logger logger = LoggerFactory.getLogger(TcpConnectedClient.class);

    private static TcpConnectedClient instance = null;
	private String host;
	private HttpClient client;
	private String accessKey;
	private String token;

	private List<LightConfig> items;
	

	public TcpConnectedClient() throws Exception {
		items = new ArrayList<LightConfig>();
        client = new HttpClient();
        SslContextFactory sslFactory = new SslContextFactory(true);
        sslFactory.setEndpointIdentificationAlgorithm(null);
        client = new HttpClient(sslFactory);
        client.setFollowRedirects(true);
        client.start();
	}

	public void setHost(String host) {
	    this.host = host;
    }
    public static TcpConnectedClient getInstance() throws Exception {
        if (instance == null) {
            instance = new TcpConnectedClient();
        }
        return instance;
    }

    public void dispose() {
        try {
            client.stop();
        } catch (Exception e) {

            logger.warn("error disposing", e);
        }
    }

    public String generateToken(String guid) throws Exception {
        String command = String.format("cmd=GWRLogin&data=<gip><version>1</version><email>%s</email><password>%s</password></gip>",guid, guid);

        Request request = client.POST("https://" + host + "/gwr/gop.php");
        request.header("Content-Type", "application/xml");
        request.content(new StringContentProvider(command));
        String result = request.send().getContentAsString();
        logger.error(result);
        DocumentBuilder db = dbf.newDocumentBuilder();
        Document doc = db.parse(new InputSource(new StringReader(result)));
        if (doc.getFirstChild().hasChildNodes() == false || !((Element)doc.getFirstChild()).getElementsByTagName("rc").item(0).getTextContent().equals("200")) {
            throw new Exception("Could not handle response");
        }
        return ((Element)doc.getFirstChild()).getElementsByTagName("token").item(0).getTextContent();
    }


	
	public boolean isConnected() {
		return client!=null;
	}

	/**
	 * updates the light states and levels
	 */
	public void update() throws Exception {
		logger.info("getting items");
		String command = String.format("cmd=GWRBatch&data=<gwrcmds><gwrcmd><gcmd>RoomGetCarousel</gcmd><gdata><gip><version>1</version><token>%s</token><fields>name,image,imageurl,control,power,product,class,realtype,status</fields></gip></gdata></gwrcmd></gwrcmds>&fmt=xml",token);
		Request request = client.POST("https://" + host + "/gwr/gop.php");
		request.header("Content-Type", "application/xml");
		request.content(new StringContentProvider(command));
		String result = request.send().getContentAsString();
		DocumentBuilder db = dbf.newDocumentBuilder();
		Document doc = db.parse(new InputSource(new StringReader(result)));
		logger.info(result);
		if (doc.getFirstChild().hasChildNodes() == false || !((Element)doc.getFirstChild()).getElementsByTagName("rc").item(0).getTextContent().equals("200")) {
			throw new Exception("Could not handle response");
		}
		NodeList devices = doc.getElementsByTagName("device");
		List<LightConfig> newItems = new ArrayList<LightConfig>();
		for(int i=0; i<devices.getLength(); i++) {
			Node device = devices.item(i);
			String did = ((Element)device).getElementsByTagName("did").item(0).getTextContent();
			String state = ((Element)device).getElementsByTagName("state").item(0).getTextContent();
			String level = "0";
			if(state.equals("1")) {
				level = ((Element)device).getElementsByTagName("level").item(0).getTextContent();
			}
			String status = ((Element)device).getElementsByTagName("known").item(0).getTextContent();
			String name = ((Element)device).getElementsByTagName("name").item(0).getTextContent();
			LightConfig config = new LightConfig(did, name, state.equals("1") ? State.ON : State.OFF, Integer.valueOf(level), status.equals("1"));
			newItems.add(config);
		}
		logger.info("got:" + newItems.size() + " items");
		items = newItems;
	}
	
	private String listLights() {
		return "";
	}

	public void setState(LightConfig light, boolean on) throws Exception {
		String command = String.format("cmd=DeviceSendCommand&data=<gip><version>1</version><token>%s</token><did>%s</did><value>%d</value></gip>&fmt=xml",token,light.getDid(),on ? 1 : 0);
		logger.info(command);
		Request request = client.POST("https://" + host + "/gwr/gop.php");
		request.header("Content-Type", "application/xml");
		request.content(new StringContentProvider(command));
		String result = request.send().getContentAsString();
		DocumentBuilder db = dbf.newDocumentBuilder();
		Document doc = db.parse(new InputSource(new StringReader(result)));
		logger.info("setting state response for:" + on + ":"+ result);
		if (doc.getFirstChild().hasChildNodes() == false || !((Element)doc.getFirstChild()).getElementsByTagName("rc").item(0).getTextContent().equals("200")) {
			throw new Exception("Could not handle response");
		}
		items.remove(light);
		light.setState(on ? State.ON : State.OFF);
		items.add(light);

	}

	public void setLevel(LightConfig light, int level) throws Exception {
		String command = String.format("cmd=DeviceSendCommand&data=<gip><version>1</version><token>%s</token><did>%s</did><value>%d</value><type>level</type></gip>&fmt=xml",token,light.getDid(),level);
		Request request = client.POST("https://" + host + "/gwr/gop.php");
		request.header("Content-Type", "application/xml");
		request.content(new StringContentProvider(command));
		String result = request.send().getContentAsString();
		DocumentBuilder db = dbf.newDocumentBuilder();
		Document doc = db.parse(new InputSource(new StringReader(result)));
		logger.info("setting level:" + level + ":"+ result);
		if (doc.getFirstChild().hasChildNodes() == false || !((Element)doc.getFirstChild()).getElementsByTagName("rc").item(0).getTextContent().equals("200")) {
			throw new Exception("Could not handle response");
		}
		items.remove(light);
		light.setLevel(level);
		items.add(light);

	}

	public void setAccessKey(String accessKey) {
		this.accessKey = accessKey;
	}

	public void setToken(String token) {
		this.token = token;
	}




	public LightConfig getLight(String did) {
		for(LightConfig item : items) {
			if (item.getDid().equals(did))
				return (LightConfig) item;
		}
		return null;
	}

	
	public List<LightConfig> getItems() {
		return items;
	}
	

	public void setLight(LightConfig item) {
		if (item==null) return;

	}
	


	private void checkConnection() throws Exception {
		if (client == null) {
			try {
				connect();
			} catch (IOException e) {
				logger.error(e.getLocalizedMessage(),e);
			}
		}
	}

	/**
	 * Connects to the pulseaudio server (timeout 500ms)
	 */
	private void connect() throws Exception {
	    getInstance();
	}
	
	/**
	 * Disconnects from the pulseaudio server
	 */
	public void disconnect() throws Exception {
		client.stop();
	}
	
}
