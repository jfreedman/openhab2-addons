/**
 * Copyright (c) 2014-2016 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.honeywellmytotalcomfortthermostat.internal.webapi;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.client.util.StringContentProvider;
import org.eclipse.jetty.util.Fields;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.openhab.binding.honeywellmytotalcomfortthermostat.internal.data.HoneywellThermostatData;
import org.openhab.binding.honeywellmytotalcomfortthermostat.internal.data.HoneywellThermostatFanMode;
import org.openhab.binding.honeywellmytotalcomfortthermostat.internal.data.HoneywellThermostatSetPoint;
import org.openhab.binding.honeywellmytotalcomfortthermostat.internal.data.HoneywellThermostatSystemMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * Web api used to for autodiscovery of new thermostats, as well as fetching/updating data for individual thermostats
 */
public class MyTotalComfortClient {

    private static MyTotalComfortClient instance = null;
    private Logger logger = LoggerFactory.getLogger(MyTotalComfortClient.class);

    private String username;
    private String password;

    private List<HoneywellThermostatData> thermostats = new ArrayList<HoneywellThermostatData>();

    private HttpClient httpclient;

    public MyTotalComfortClient() {
    }

    /**
     * gets list of thermostats managed by mytotalconnectcomfort site login
     * @return list of thermostats
     */
    public List<HoneywellThermostatData> getThermostats() {
        return thermostats;
    }

    public HoneywellThermostatData getThermostat(String deviceId) {
        for(HoneywellThermostatData item : thermostats) {
            if (item.getDeviceId().equals(deviceId))
                return item;
        }
        return null;
    }

    /**
     * Gets an instance of the webapi, that may already be authenticated
     * @return instance of webapi
     */
    public static MyTotalComfortClient getInstance() {
        if (instance == null) {
            instance = new MyTotalComfortClient();
        }
        return instance;
    }

    /**
     * disposes of http client
     */
    public void dispose() {
        try {
            httpclient.stop();
        } catch (Exception e) {
        }
    }

    /**
     * Checks if logged in web session is still valid, and if not, logs into mytotalconnectcomfort site and verifies login
     * @return if login is valid
     */
    public boolean isLoginValid() {
        if (username == null || password == null) {
            return false;
        }
        if (httpclient == null) {
            tryLogin();
        }
        try {
            ContentResponse cr = httpclient.GET("https://mytotalconnectcomfort.com/portal/Locations/");
            if (cr.getStatus() == 401) {
                logger.debug("401 response, authenticating again");
                httpclient.stop();
                tryLogin();
                cr = httpclient.GET("https://mytotalconnectcomfort.com/portal/Locations/");
            }
            if (cr.getStatus() != 200) {
                logger.info("non 200 response when fetching locations:" + cr.getStatus());
                return false;
            }
        } catch (Exception e) {
            logger.info("error checking login", e);
            return false;
        }
        logger.debug("login successful");
        return true;
    }

    /**
     * attempts to login to mytotalconnectcomfort site
     * @return was login successful
     */
    private boolean tryLogin() {
        SslContextFactory sslFactory = new SslContextFactory();
        sslFactory.setExcludeProtocols(new String[]{"TLS", "TLSv1.2", "TLSv1.1"});

        httpclient = new HttpClient(sslFactory);
        httpclient.setFollowRedirects(true);
        try {
            httpclient.start();

            Fields fields = new Fields();
            fields.add("timeOffset", "0");
            fields.add("UserName", username);
            fields.add("Password", password);

            ContentResponse cr = httpclient.FORM("https://mytotalconnectcomfort.com/portal/", fields);

            String result = cr.getContentAsString();
            if (result.contains("Login was unsuccessful.")) {
                logger.info("cannot login to honeywell site");
                return false;
            }
        } catch (Exception e) {
            logger.info("error logging into Honeywell total Connect Comfort website", e);
            return false;
        }

        logger.debug("Successfully logged into Honeywell Total Connect Comfort website.");
        return true;
    }

    /**
     * submits updated thermostat data to mytotalconnectcomfort web site
     * @param deviceID the device to update
     * @param thermodata the thermostat data to update
     * @return was update successful
     */
    public boolean submitThermostatChange(String deviceID, HoneywellThermostatData thermodata) {
        String minutesHold = "null";
        if(thermodata.getCurrentSetPoint()== HoneywellThermostatSetPoint.TEMP_HOLD) {
           minutesHold = String.valueOf(thermodata.getHoldUntilTime());
        }
        String jsonData = "{"
                + "\"DeviceID\":" + deviceID.toString()
                + ",\"SystemSwitch\":" + thermodata.getCurrentSystemMode().getValue()
                + ",\"HeatSetpoint\":" + Integer.toString(thermodata.getHeatSetPoint())
                + ",\"CoolSetpoint\":" + Integer.toString(thermodata.getCoolSetPoint())
                + ",\"HeatNextPeriod\":" + minutesHold
                + ",\"CoolNextPeriod\":" + minutesHold
                + ",\"StatusHeat\":" + thermodata.getCurrentSetPoint().getValue()
                + ",\"StatusCool\":" + thermodata.getCurrentSetPoint().getValue()
                + ",\"FanMode\":" + thermodata.getCurrentFanMode().getValue()
                + "}";
        if (!isLoginValid()) {
            logger.info("could not submit thermostat data, login not valid");
            return false;
        }
        try {
            logger.debug(jsonData);
            ContentResponse cr = httpclient
                    .POST("https://mytotalconnectcomfort.com/portal/Device/SubmitControlScreenChanges")
                    .content(new StringContentProvider("application/json", jsonData, Charset.forName("UTF-8")))
                    .send();
            if (!cr.getContentAsString().equals("{\"success\":1}")) {
                logger.info("Failed to submit thermostat data.");
                return false;
            }
        } catch (Exception ex) {
            logger.info("error updating thermostat data:" + jsonData, ex);
            return false;
        }
        return true;
    }

    /**
     * calls web api to get updated thermostat data for all thermostats, used for discovery
     * @return list of all thermostats found
     * @throws Exception if error
     */
    public List<HoneywellThermostatData> getThermostatsData() throws Exception {
        List<HoneywellThermostatData> thermodata = new ArrayList<HoneywellThermostatData>();
        if (!isLoginValid()) {
            throw new Exception("error logging into site");
        }
        String jsonData;
        try {
            ContentResponse cr = httpclient.POST(
                    "https://mytotalconnectcomfort.com/portal/Location/GetLocationListData")
                    .header("X-Requested-With", "XMLHttpRequest").header("Content-Type","application/json; charset=utf-8").param("page","1").param("filter","").send();
            if (cr.getStatus() != 200) {
                logger.info("Non 200 response when retrieving thermostat list data.");
                return thermodata;
            }
            jsonData = cr.getContentAsString();
            JsonArray locations = (JsonArray) new JsonParser().parse(jsonData);
            for(JsonElement el : locations) {
                JsonObject obj = el.getAsJsonObject();
                JsonArray devices = obj.getAsJsonArray("Devices");
                for(JsonElement device : devices) {
                    JsonObject deviceObj = device.getAsJsonObject();
                    String deviceId = String.valueOf(deviceObj.get("DeviceID").getAsBigInteger());
                    thermodata.add(getThermostatData(deviceId));
                }
            }
        } catch (Exception ex) {
            logger.info("error reading thermostat data", ex);
        }
        logger.debug("found:" + thermodata.size() + " thermostats");
        return thermodata;
    }

    /**
     * gets details from web api of an individual thermostat
     * @param deviceID device to fetch data for
     * @return thermostat data
     */
    public HoneywellThermostatData getThermostatData(String deviceID) throws Exception {
        HoneywellThermostatData thermodata = new HoneywellThermostatData();
        if (isLoginValid()) {
            String jsonData;
            ContentResponse cr = httpclient.newRequest(
                    "https://mytotalconnectcomfort.com/portal/Device/CheckDataSession/" + deviceID)
                    .header("X-Requested-With", "XMLHttpRequest")
                    .send();
            if (cr.getStatus() != 200) {
                logger.info("non 200 response when retrieving thermostat data.");
                return null;
            }
            jsonData = cr.getContentAsString();
            JsonObject obj = (JsonObject) new JsonParser().parse(jsonData);
            thermodata.setDeviceId(deviceID);
            JsonObject uiData = obj.getAsJsonObject("latestData").getAsJsonObject("uiData");
            thermodata.setCurrentTemperature(
                    uiData.get("DispTemperature").getAsInt());
            thermodata.setDisplayUnits(
                    uiData.get("DisplayUnits").getAsString());
            thermodata.setCurrentHumidity(
                    uiData.get("IndoorHumidity").getAsInt());
            thermodata.setHeatSetPoint(
                    uiData.get("HeatSetpoint").getAsInt());
            thermodata.setCoolSetPoint(
                    uiData.get("CoolSetpoint").getAsInt());

            thermodata.setCurrentSystemMode(HoneywellThermostatSystemMode.getEnum(uiData.get("SystemSwitchPosition").getAsInt()));

            thermodata.setCurrentFanMode(HoneywellThermostatFanMode.getEnum(obj.getAsJsonObject("latestData").getAsJsonObject("fanData").get("fanMode").getAsInt()));

            thermodata.setCurrentSetPoint(HoneywellThermostatSetPoint.getEnum(uiData.get("CurrentSetpointStatus").getAsInt()));

            thermodata.setHoldUntilTime(uiData.get("HeatNextPeriod").getAsInt());
        }
        for(HoneywellThermostatData data : thermostats) {
            if(data.getDeviceId().equals(deviceID)) {
                thermostats.remove(data);
                break;
            }
        }
        thermostats.add(thermodata);
        return thermodata;
    }

    /**
     * sets username for web api
     * @param username username of user
     */
    public void setUsername(String username) {
        this.username = username;
    }

    /**
     * sets password for web api
     * @param password password of user
     */
    public void setPassword(String password) {
        this.password = password;
    }

    /**
     * disconnects http client and loses sessoin
     * @throws Exception if error
     */
    public void disconnect() throws Exception {
        httpclient.stop();
    }

}