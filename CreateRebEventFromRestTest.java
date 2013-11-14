package com.xmatters.webdriver.tests;

import com.jayway.restassured.response.Response;
import org.json.JSONObject;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.xmatters.webdriver.tasks.Companies;
import com.xmatters.webdriver.tasks.RelevanceEngines;
import com.xmatters.webdriver.tasks.Users;
import com.xmatters.webui.helpers.RestInjectionHelper;
import com.xmatters.webui.pages.home.HomePage;
import com.xmatters.webui.pages.home.developer.DeveloperSubMenu;
import com.xmatters.webui.pages.home.developer.managerelevanceengines.ManageRelevanceEnginesScreen;
import com.xmatters.webui.pages.home.developer.managerelevanceengines.formdetails.LayoutScreen;
import com.xmatters.webui.pages.home.developer.managerelevanceengines.formdetails.layoutsections.CustomSectionScreen;
import com.xmatters.webui.pages.home.developer.managerelevanceengines.relevanceenginedetails.FormsScreen;
import com.xmatters.webui.pages.login.LoginPage;


public class CreateRebEventFromRestTest extends BaseTest {
    JSONObject environmentConfig;
    JSONObject companyConfig;
    JSONObject restRequest;
    String engineName;
    String formName;
    JSONObject company;
    JSONObject companyAdmin;
    JSONObject engine;
    private String port;
    private String webServerIP;
    private String hostName;

    public CreateRebEventFromRestTest() throws Exception {
        super();
        environmentConfig = this.getEnvironmentConfig();
        companyConfig = this.getCompanyConfig("CreateRebEventFromRestTest.json");
        webServerIP = this.getWebServerIP();
        if (webServerIP.contains(":")) {
            port = webServerIP.substring(webServerIP.indexOf(":") + 1, webServerIP.indexOf(":") + 5);
        } else {
            port = null;
        }
        webServerIP = webServerIP.substring(0, webServerIP.indexOf(":"));
    }

    @BeforeClass(enabled = true, groups = {"chrome", "reb"})
    public void createTestData() throws Exception {
        company = companyConfig.getJSONObject("company");
        companyAdmin = companyConfig.getJSONObject("companyAdmin");
        loadLoginPage();

        engine = companyConfig.getJSONArray("relevanceEngines").getJSONObject(0);
        //engine #1
        engineName = engine.getString("name");
        formName = engine.getJSONArray("forms").getJSONObject(0).getString("name");

        Companies.createCompany(driver, companyConfig, environmentConfig);
        Users.createCompanyAdmin(driver, companyConfig, environmentConfig);
        RelevanceEngines.createRelevanceEngines(driver, companyConfig);

        //Make form
        LoginPage loginPage = new LoginPage(driver);
        //login as company admin
        HomePage homePage = loginPage.doLogin(
                company.getString("companyName"),
                companyAdmin.getString("userId"),
                companyAdmin.getString("password"),
                false
        );

        engine = companyConfig.getJSONArray("relevanceEngines").getJSONObject(0);
        engineName = engine.getString("name");
        formName = engine.getJSONArray("forms").getJSONObject(0).getString("name");
        DeveloperSubMenu developerSubMenu = homePage.clickDeveloperTab();
        ManageRelevanceEnginesScreen manageRelevanceEnginesScreen = developerSubMenu.clickManageRelevanceEngines();
        manageRelevanceEnginesScreen.clickEditButtonForEngine(engineName);
        FormsScreen formScreen = manageRelevanceEnginesScreen.clickFormsButtonForEngine(engineName);
        formScreen.clickDeployFormAsWebService(formName);

        formScreen.clickEditButtonForForm(formName);
        LayoutScreen layoutScreen = formScreen.clickLayoutForForm(formName);
        String textPropertyName = engine.getJSONObject("properties").getJSONArray("textProperties").getJSONObject(0)
                .getString("name");
        layoutScreen.dragPropertyToCustomSection(textPropertyName);
        layoutScreen.clickSaveChangesButton();

        homePage.logout();

        hostName = (company.getString("hostname") + ".xmatters.com").toLowerCase();
        //Update host file
        RestInjectionHelper.setHostFile(hostName, webServerIP);
    }

    @Test(groups = {"chrome", "reb"})
    public void createEvent() throws Exception {
        String url;
        if (port != null) {
            url = "http://" + hostName + ":" + port + "/reapi/2012-03-01/engines/" + engineName + "/" + formName;
        } else {
            url = "http://" + hostName + "/reapi/2012-03-01/engines/" + engineName + "/" + formName;
        }

        restRequest = this.getCompanyConfig("Rest.json");
        //Inject Event
        Response response = RestInjectionHelper.eventInjectionNoSSL(url, companyAdmin.getString("userId"),
                companyAdmin.getString("password"), restRequest);
        //Get Response Status
        String status = RestInjectionHelper.getResponseStatus(response);
        //Get Response Message
        String responseBody = RestInjectionHelper.getResponseBody(response);
        //Verify Response is correct
        //   Assert.assertTrue(status.equalsIgnoreCase("200"), "Status Code is not correct");
        Assert.assertEquals(status, "200", "Expected status 200 does not match actual " + status);
        Assert.assertTrue(responseBody.contains("id"), "Event Id is Not present");
    }

    @Test(groups = {"chrome", "reb"})
    public void formDoesNotExist() throws Exception {
        String url;
        if (port != null) {
            url = "http://" + hostName + ":" + port + "/reapi/2012-03-01/engines/" + engineName + "/NoExist";
        } else {
            url = "http://" + hostName + "/reapi/2012-03-01/engines/" + engineName + "/NoExist";
        }
        restRequest = this.getCompanyConfig("Rest.json");
        //Inject Event
        Response response = RestInjectionHelper.eventInjectionNoSSL(url, companyAdmin.getString("userId"),
                companyAdmin.getString("password"), restRequest);
        //Get Response Status
        String status = RestInjectionHelper.getResponseStatus(response);
        //Get Response Message
        String responseBody = RestInjectionHelper.getResponseBody(response);
        //Verify Response is correct
        Assert.assertTrue(status.equalsIgnoreCase("404"), "Status Code is not correct");
        Assert.assertTrue(responseBody.contains("The engine or form is not available."), "Error is Not correct");
    }

    @Test(groups = {"chrome", "reb"})
    public void invalidCredentials() throws Exception {
        String url;
        if (port != null) {
            url = "http://" + hostName + ":" + port + "/reapi/2012-03-01/engines/" + engineName + "/" + formName;
        } else {
            url = "http://" + hostName + "/reapi/2012-03-01/engines/" + engineName + "/" + formName;
        }
        restRequest = this.getCompanyConfig("Rest.json");
        //Inject Event
        Response response = RestInjectionHelper.eventInjectionNoSSL(url, "admin", "tre", restRequest);
        //Get Response Status
        String status = RestInjectionHelper.getResponseStatus(response);
        //Get Response Message
        String responseBody = RestInjectionHelper.getResponseBody(response);
        //Verify Response is correct
        Assert.assertTrue(status.equalsIgnoreCase("401"), "Status Code is not correct");
        Assert.assertTrue(responseBody.contains("User could not be authenticated"), "Error is Not correct");
    }

    @Test(groups = {"chrome", "reb"})
    public void invalidRequest() throws Exception {
        String url;
        if (port != null) {
            url = "http://" + hostName + ":" + port + "/reapi/2012-03-01/engines/" + engineName + "/" + formName;
        } else {
            url = "http://" + hostName + "/reapi/2012-03-01/engines/" + engineName + "/" + formName;
        }
        restRequest = this.getCompanyConfig("invalidRequest.json");
        //Inject Event
        Response response = RestInjectionHelper.eventInjectionNoSSL(url, companyAdmin.getString("userId"),
                companyAdmin.getString("password"), restRequest);
        //Get Response Status
        String status = RestInjectionHelper.getResponseStatus(response);
        //Get Response Message
        String responseBody = RestInjectionHelper.getResponseBody(response);
        //Verify Response is correct
        Assert.assertTrue(status.equalsIgnoreCase("400"), "Status Code is not correct");
        Assert.assertTrue(responseBody.contains("Unrecognized field \\\"targetname\\\""), "Error is Not correct");
    }

    @Test(groups = {"chrome", "reb"})
    public void propertyDoesNotExist() throws Exception {
        String url;
        if (port != null) {
            url = "http://" + hostName + ":" + port + "/reapi/2012-03-01/engines/" + engineName + "/" + formName;
        } else {
            url = "http://" + hostName + "/reapi/2012-03-01/engines/" + engineName + "/" + formName;
        }
        restRequest = this.getCompanyConfig("propertyDoesNotExist.json");
        //Inject Event
        Response response = RestInjectionHelper.eventInjectionNoSSL(url, companyAdmin.getString("userId"),
                companyAdmin.getString("password"), restRequest);
        //Get Response Status
        String status = RestInjectionHelper.getResponseStatus(response);
        //Get Response Message
        String responseBody = RestInjectionHelper.getResponseBody(response);
        //Verify Response is correct
        Assert.assertTrue(status.equalsIgnoreCase("400"), "Status Code is not correct");
        Assert.assertTrue(responseBody.contains("The property does not exist."), "Error is Not correct");
    }

    @Test(groups = {"chrome", "reb"})
    public void requiredPropertyMissing() throws Exception {
        //Make form
        LoginPage loginPage = new LoginPage(driver);
        //login as company admin
        HomePage homePage = loginPage.doLogin(
                company.getString("companyName"),
                companyAdmin.getString("userId"),
                companyAdmin.getString("password"),
                false
        );

        DeveloperSubMenu developerSubMenu = homePage.clickDeveloperTab();
        ManageRelevanceEnginesScreen manageRelevanceEnginesScreen = developerSubMenu.clickManageRelevanceEngines();
        manageRelevanceEnginesScreen.clickEditButtonForEngine(engineName);
        FormsScreen formScreen = manageRelevanceEnginesScreen.clickFormsButtonForEngine(engineName);
        formScreen.clickEditButtonForForm(formName);
        LayoutScreen layoutScreen = formScreen.clickLayoutForForm(formName);

        String propertyName = engine.getJSONObject("properties").getJSONArray("textProperties").getJSONObject(0)
                .getString("name");

        CustomSectionScreen customSectionScreen = layoutScreen.getCustomSection();
        customSectionScreen.clickOptionsForProperty(propertyName);
        customSectionScreen.clickRequiredForProperty(propertyName);
        layoutScreen = layoutScreen.clickSaveChangesButton();
        String url;
        if (port != null) {
            url = "http://" + hostName + ":" + port + "/reapi/2012-03-01/engines/" + engineName + "/" + formName;
        } else {
            url = "http://" + hostName + "/reapi/2012-03-01/engines/" + engineName + "/" + formName;
        }
        restRequest = this.getCompanyConfig("requiredPropertyMissing.json");
        //Inject Event
        Response response = RestInjectionHelper.eventInjectionNoSSL(url, companyAdmin.getString("userId"),
                companyAdmin.getString("password"), restRequest);
        //Get Response Status
        String status = RestInjectionHelper.getResponseStatus(response);
        //Get Response Message
        String responseBody = RestInjectionHelper.getResponseBody(response);
        //Verify Response is correct
        //    Assert.assertTrue(status.equalsIgnoreCase("400"), "Status Code is not correct");
        Assert.assertEquals(status, "400", "Expected status code 400 does not match actual: " + status);
        Assert.assertTrue(responseBody.contains("\"properties/message\",\"details\":\"Required field\""), "Error is Not correct");

        customSectionScreen = layoutScreen.getCustomSection();
        customSectionScreen.clickOptionsForProperty(propertyName);
        customSectionScreen.clickRequiredForProperty(propertyName);
        layoutScreen.clickSaveChangesButton();
        homePage.logout();
    }

    @Test(groups = {"chrome", "reb"})
    public void formIsNotDeployedAsWebservice() throws Exception {
        //Make form
        LoginPage loginPage = new LoginPage(driver);
        //login as company admin
        HomePage homePage = loginPage.doLogin(
                company.getString("companyName"),
                companyAdmin.getString("userId"),
                companyAdmin.getString("password"),
                false
        );

        DeveloperSubMenu developerSubMenu = homePage.clickDeveloperTab();
        ManageRelevanceEnginesScreen manageRelevanceEnginesScreen = developerSubMenu.clickManageRelevanceEngines();
        manageRelevanceEnginesScreen.clickEditButtonForEngine(engineName);
        FormsScreen formScreen = manageRelevanceEnginesScreen.clickFormsButtonForEngine(engineName);
        //Undeploy form as webservice
        formScreen.clickDeployFormAsWebService(formName);

        String url;
        if (port != null) {
            url = "http://" + hostName + ":" + port + "/reapi/2012-03-01/engines/" + engineName + "/" + formName;
        } else {
            url = "http://" + hostName + "/reapi/2012-03-01/engines/" + engineName + "/" + formName;
        }
        restRequest = this.getCompanyConfig("Rest.json");
        //Inject Event
        Response response = RestInjectionHelper.eventInjectionNoSSL(url, companyAdmin.getString("userId"),
                companyAdmin.getString("password"), restRequest);
        //Get Response Status
        String status = RestInjectionHelper.getResponseStatus(response);
        //Get Response Message
        String responseBody = RestInjectionHelper.getResponseBody(response);
        //Verify Response is correct
        Assert.assertTrue(status.equalsIgnoreCase("404"), "Status Code is not correct");
        Assert.assertTrue(responseBody.contains("The engine or form is not available."), "Error is Not correct");

        formScreen.clickDeployFormAsWebService(formName);
        homePage.logout();
    }
}
