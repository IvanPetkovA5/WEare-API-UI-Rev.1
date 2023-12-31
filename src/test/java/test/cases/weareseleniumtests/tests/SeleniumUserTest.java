package test.cases.weareseleniumtests.tests;

import com.telerikacademy.testframework.utils.Helpers;
import restassuredapi.models.models.UserByIdModel;
import restassuredapi.models.models.UserModel;
import org.testng.Assert;
import org.testng.annotations.*;
import pages.weare.HomePage;
import pages.weare.LoginPage;
import pages.weare.RegisterPage;
import restassuredapi.UserApi;
import test.cases.weareseleniumtests.base.BaseWeareSeleniumTest;

import static com.telerikacademy.testframework.utils.UserRoles.ROLE_USER;

public class SeleniumUserTest extends BaseWeareSeleniumTest {

    private Integer registeredUserId;

    @AfterMethod
    public void cleanUp() {
        HomePage homePage = new HomePage(actions.getDriver());
        homePage.navigateToPage();
        if (homePage.isLoggedIn()) {
            homePage.logout();
        }
    }

    @Test
    public void userRegistered_When_ValidDataProvided() {

        // Generate a random username and password
        String username = Helpers.generateUsernameAsImplemented(ROLE_USER.toString());
        String password = Helpers.generatePassword();
        String email = Helpers.generateEmail();

        RegisterPage registerPage = new RegisterPage(actions.getDriver());
        registerPage.registerUser(username, email, password);
        registeredUserId = Integer.parseInt(registerPage.extractUserId());

        UserByIdModel registeredUser = UserApi.getUserById(username, registeredUserId).as(UserByIdModel.class);

        Assert.assertEquals(registeredUser.getUsername(), username);
        Assert.assertEquals(registeredUser.getEmail(), email);
        Assert.assertEquals(registeredUser.getId(), registeredUserId);
    }

    @Test
    public void userLoggedIn_When_ValidCredentialsProvided() {

        UserModel registeredUser = new UserModel();
        UserApi.register(registeredUser, ROLE_USER.toString());
        registeredUserId = registeredUser.getId();

        LoginPage loginPage = new LoginPage(actions.getDriver());
        loginPage.loginUser(registeredUser.getUsername(), registeredUser.getPassword());

        HomePage homePage = new HomePage(actions.getDriver());
        homePage.assertUserHasLoggedIn();
    }

}
