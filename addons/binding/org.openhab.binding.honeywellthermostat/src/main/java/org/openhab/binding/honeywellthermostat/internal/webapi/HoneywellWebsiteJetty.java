package org.openhab.binding.honeywellthermostat.internal.webapi;

import java.nio.charset.Charset;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.HttpRequestException;
import org.eclipse.jetty.client.HttpResponseException;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.client.util.StringContentProvider;
import org.eclipse.jetty.util.Fields;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.openhab.binding.honeywellthermostat.internal.data.HoneywellThermostatData;
import org.openhab.binding.honeywellthermostat.internal.data.HoneywellThermostatFanMode;
import org.openhab.binding.honeywellthermostat.internal.data.HoneywellThermostatSystemMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class HoneywellWebsiteJetty {

    private static HoneywellWebsiteJetty instance = null;
    private Logger logger = LoggerFactory.getLogger(HoneywellWebsiteJetty.class);

    private String username;
    private String password;

    private HttpClient httpclient;

    protected HoneywellWebsiteJetty() {
    }

    public static HoneywellWebsiteJetty getInstance() {
        if (instance == null) {
            instance = new HoneywellWebsiteJetty();
        }
        return instance;
    }

    public void dispose() {
        try {
            httpclient.stop();
        } catch (Exception e) {

            logger.warn("error disposing", e);
        }
    }

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
                httpclient.stop();
                tryLogin();
                cr = httpclient.GET("https://mytotalconnectcomfort.com/portal/Locations/");
            }
            if (cr.getStatus() != 200) {
                logger.info("non 200 response of:" + cr.getStatus());
                return false;
            }
        } catch (Exception e) {
            logger.error("error checking login", e);
            return false;
        }
        logger.debug("login successful");
        return true;
    }

    private boolean tryLogin() {
        logger.debug("setting up new ssl conection");
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
                logger.debug("cannot login to honeywell site");
                return false;
            }
        } catch (Exception e) {
            logger.error("error loging into Honeywell total Connect Comfort website", e);
            return false;
        }

        logger.debug("Successfully logged into Honeywell Total Connect Comfort website.");
        return true;
    }

    public boolean submitThermostatChange(String deviceID, HoneywellThermostatData thermodata) {
        String jsonData = "{\"DeviceID\":" + deviceID.toString() + ",\"SystemSwitch\":"
                + thermodata.getCurrentSystemMode().getValue() + ",\"HeatSetpoint\":"
                + Integer.toString(thermodata.getHeatSetPoint()) + ",\"CoolSetpoint\":"
                + Integer.toString(thermodata.getCoolSetPoint())
                + ",\"HeatNextPeriod\":null,\"CoolNextPeriod\":null,\"StatusHeat\":null,\"StatusCool\":null,\"FanMode\":"
                + thermodata.getCurrentFanMode().getValue() + "}";
        if (isLoginValid()) {
            try {
                ContentResponse cr = httpclient
                        .POST("https://mytotalconnectcomfort.com/portal/Device/SubmitControlScreenChanges")
                        .content(new StringContentProvider("application/json", jsonData, Charset.forName("UTF-8")))
                        .send();
                if (!cr.getContentAsString().equals("{\"success\":1}")) {
                    logger.debug("Failed to sumbit thermostat data.");
                    logger.debug(jsonData);
                    return false;
                }
            } catch (InterruptedException | TimeoutException | ExecutionException e) {
                logger.info("error updating thermostat data:" + jsonData, e);
                return false;
            }
            return true;
        } else {
            logger.debug("could not submit thermostat data");
            return false;
        }
    }

    public HoneywellThermostatData getTherostatData(String deviceID) {
        HoneywellThermostatData thermodata = new HoneywellThermostatData();
        if (isLoginValid()) {
            String jsonData;
            try {
                ContentResponse cr = httpclient.newRequest(
                        "https://mytotalconnectcomfort.com/portal/Device/CheckDataSession/" + deviceID.toString())
                        .header("X-Requested-With", "XMLHttpRequest").send();
                if (cr.getStatus() != 200) {
                    logger.info("Failed to retrieve thermostat data.");
                    return null;
                }
                jsonData = cr.getContentAsString();
                JsonObject obj = (JsonObject) new JsonParser().parse(jsonData);
                thermodata.setCurrentTemperature(
                        obj.getAsJsonObject("latestData").getAsJsonObject("uiData").get("DispTemperature").getAsInt());
                thermodata.setDisplayUnits(
                        obj.getAsJsonObject("latestData").getAsJsonObject("uiData").get("DisplayUnits").getAsString());
                thermodata.setCurrentHumidity(
                        obj.getAsJsonObject("latestData").getAsJsonObject("uiData").get("IndoorHumidity").getAsInt());
                thermodata.setHeatSetPoint(
                        obj.getAsJsonObject("latestData").getAsJsonObject("uiData").get("HeatSetpoint").getAsInt());
                thermodata.setCoolSetPoint(
                        obj.getAsJsonObject("latestData").getAsJsonObject("uiData").get("CoolSetpoint").getAsInt());

                switch (obj.getAsJsonObject("latestData").getAsJsonObject("uiData").get("SystemSwitchPosition")
                        .getAsInt()) {
                    case 1:
                        thermodata.setCurrentSystemMode(HoneywellThermostatSystemMode.HEAT);
                        break;

                    case 2:
                        thermodata.setCurrentSystemMode(HoneywellThermostatSystemMode.OFF);
                        break;

                    case 3:
                        thermodata.setCurrentSystemMode(HoneywellThermostatSystemMode.COOL);
                        break;

                    default:
                        thermodata.setCurrentSystemMode(HoneywellThermostatSystemMode.OFF);
                        break;
                }

                switch (obj.getAsJsonObject("latestData").getAsJsonObject("fanData").get("fanMode").getAsInt()) {
                    case 0:
                        thermodata.setCurrentFanMode(HoneywellThermostatFanMode.AUTO);
                        break;
                    case 1:
                        thermodata.setCurrentFanMode(HoneywellThermostatFanMode.ON);
                        break;
                    case 2:
                        thermodata.setCurrentFanMode(HoneywellThermostatFanMode.SCHEDULE);
                        break;
                    case 3:
                        thermodata.setCurrentFanMode(HoneywellThermostatFanMode.CIRCULATE);
                        break;
                    default:
                        thermodata.setCurrentFanMode(HoneywellThermostatFanMode.AUTO);
                }

            } catch (InterruptedException | TimeoutException | ExecutionException | HttpResponseException e) {
                logger.info("error retrieving thermostat data", e);
            }
        }
        return thermodata;
    }

    public void setUsername(String username) {
        this.username = username;
    }


    public void setPassword(String password) {
        this.password = password;
    }

}