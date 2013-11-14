package com.xmatters.webdriver.tests;

import org.json.JSONException;
import org.json.JSONObject;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.xmatters.webdriver.tasks.Companies;
import com.xmatters.webui.pages.home.HomePage;
import com.xmatters.webui.pages.home.admin.AdminSubMenu;
import com.xmatters.webui.pages.home.admin.companies.CompanyAdminSelectYourCompanyScreen;
import com.xmatters.webui.pages.home.admin.companies.CompanyAdministratorsChangeWebLoginScreen;
import com.xmatters.webui.pages.home.admin.companies.CompanyAdministratorsDetailsScreen;
import com.xmatters.webui.pages.home.admin.companies.CompanyAdministratorsScreen;
import com.xmatters.webui.pages.login.LoginPage;


public class CreateCompanyAdminTest extends BaseTest {

    public CreateCompanyAdminTest() throws JSONException {
        environmentConfig = this.getEnvironmentConfig();
        companyConfig = this.getCompanyConfig("CreateCompanyAdmin.json");
    }

    private final JSONObject environmentConfig;
    private final JSONObject companyConfig;

    @BeforeClass
    public void createTestData() throws JSONException {

        loadLoginPage();
        Companies.createCompany(driver, companyConfig, environmentConfig);
    }

    @Test
    public void createCompanyAdmin() throws JSONException {
        //creates company administrator
        JSONObject company = companyConfig.getJSONObject("company");
        JSONObject companyAdmin = companyConfig.getJSONObject("companyAdmin");
        LoginPage login = new LoginPage(driver);
        HomePage home = login.doLogin(null, "root", "tree", false);
        AdminSubMenu adminSubMenu = home.clickAdminTab();
        CompanyAdminSelectYourCompanyScreen companyAdminSelectYourCompanyScreen = adminSubMenu.clickCompanyAdmins();
        companyAdminSelectYourCompanyScreen.selectCompany(company.getString("companyName"));
        CompanyAdministratorsScreen companyAdministratorsScreen =
                companyAdminSelectYourCompanyScreen.clickContinueButton();

        //delete company admin if exists
        if (companyAdministratorsScreen.companyAdminNameExists(companyAdmin.getString("userId"))) {
            companyAdministratorsScreen.selectCompanyAdminRow(companyAdmin.getString("userId"), true);
            companyAdministratorsScreen.clickRemoveSelectedButton();
            Assert.assertFalse(
                    companyAdministratorsScreen.companyAdminNameExists(companyAdmin.getString("userId")),
                    "Company administrator was not deleted"
            );
        }

        CompanyAdministratorsDetailsScreen companyAdministratorsDetailsScreen =
                companyAdministratorsScreen.clickAddNew();
        //fill out form form company administrator
        companyAdministratorsDetailsScreen.checkActiveCheckBox(true);
        companyAdministratorsDetailsScreen.typeUserID(companyAdmin.getString("userId"));
        companyAdministratorsDetailsScreen.typeFirstName(companyAdmin.getString("firstName"));
        companyAdministratorsDetailsScreen.typeLastName(companyAdmin.getString("lastName"));
        CompanyAdministratorsChangeWebLoginScreen companyAdministratorsChangeWebLoginScreen =
                companyAdministratorsDetailsScreen.clickSave();
        companyAdministratorsChangeWebLoginScreen.typeWebLoginID(companyAdmin.getString("userId"));
        companyAdministratorsChangeWebLoginScreen.typeNewPassword(companyAdmin.getString("password"));
        companyAdministratorsChangeWebLoginScreen.typeVerifyNewPassword(companyAdmin.getString("password"));
        companyAdministratorsScreen = companyAdministratorsChangeWebLoginScreen.clickSave();

        Assert.assertTrue(
                companyAdministratorsScreen.companyAdminNameExists(companyAdmin.getString("userId")),
                "Company administrator was not created");

        home.logout();
    }

}
