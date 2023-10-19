package api;

import api.models.*;
import com.google.gson.Gson;
import com.telerikacademy.testframework.PropertiesManager;
import com.telerikacademy.testframework.utils.Helpers;
import io.restassured.RestAssured;
import io.restassured.authentication.FormAuthConfig;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import org.testng.log4testng.Logger;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

import static api.utils.JSONRequests.*;
import static com.telerikacademy.testframework.utils.Constants.API;
import static com.telerikacademy.testframework.utils.Endpoints.*;
import static com.telerikacademy.testframework.utils.UserRoles.ROLE_ADMIN;
import static com.telerikacademy.testframework.utils.UserRoles.ROLE_USER;
import static io.restassured.RestAssured.given;
import static org.apache.http.HttpStatus.SC_MOVED_TEMPORARILY;
import static org.apache.http.HttpStatus.SC_OK;
import static org.testng.Assert.*;

public class WeAreApi {

    private final Helpers helpers = new Helpers();
    private static final Logger LOGGER = Logger.getLogger(WeAreApi.class);

    public RequestSpecification getRestAssured(String... args) {
        Gson deserializer = new Gson();
        String baseUri = PropertiesManager.PropertiesManagerEnum.INSTANCE.getConfigProperties()
                .getProperty("weare.baseUrl") + PropertiesManager.PropertiesManagerEnum.INSTANCE.getConfigProperties()
                .getProperty("weare.api");

        if (args.length > 0) {
            return RestAssured
                    .given()
                    .header("Content-Type", "application/json")
                    .baseUri(baseUri)
                    .auth()
                    .form(args[0], args[1],
                            new FormAuthConfig(AUTHENTICATE, "username", "password"));
        } else {
            return RestAssured
                    .given()
                    .header("Content-Type", "application/json")
                    .baseUri(baseUri);
        }

    }

    public UserModel registerUser(String authority) {

        String email = helpers.generateEmail();
        String generatedPassword = helpers.generatePassword();
        String generatedUsername = helpers.generateUsernameAsImplemented(authority);
        int categoryId = 100;
        String categoryName = "All";
        CategoryModel category = new CategoryModel();
        category.setName(categoryName);
        category.setId(categoryId);

        if (authority.equals(ROLE_ADMIN.toString())) {
            authority = String.format("\"%s\", \"%s\"", ROLE_USER, ROLE_ADMIN);
        } else if (authority.equals(ROLE_USER.toString())) {
            authority = String.format("\"%s\"", ROLE_USER);
        }

        Response response = given()
                .contentType("application/json")
                .body(String.format(userBody, authority, category.getId(), category.getName(), generatedPassword, email, generatedPassword, generatedUsername))
                .post(API + REGISTER_USER)
                .then()
                .assertThat()
                .statusCode(SC_OK)
                .extract().response();

        LOGGER.info(response.getBody().asPrettyString());

        int userId = Integer.parseInt(getUserId(response));

        assertEquals(response.body().asString(), String.format("User with name %s and id %s was created",
                generatedUsername, userId), "User was not registered");

        return extractUser(userId, generatedUsername, generatedPassword);
    }

    private UserModel extractUser(int userId, String username, String password) {

        UserModel userModel = new UserModel();
        UserByIdModel userByIdModel = getUserById(username, userId).as(UserByIdModel.class);
        userModel.setUserId(userByIdModel.getId());
        userModel.setUsername(userByIdModel.getUsername());
        userModel.setPassword(password);
        userModel.setEmail(userByIdModel.getEmail());
        String firstName = helpers.generateFirstName();
        PersonalProfileModel personalProfileModel = editPersonalProfileFirstName(userModel, firstName);
        userModel.setPersonalProfile(personalProfileModel);

        UserBySearchModel userBySearchModel = searchUser(userModel.getId(), userModel.getPersonalProfile().getFirstName());
        userModel.setExpertiseProfile(userBySearchModel.getExpertiseProfile());
        userModel.setAccountNonExpired(userBySearchModel.isAccountNonExpired());
        userModel.setAccountNonLocked(userBySearchModel.isAccountNonLocked());
        userModel.setCredentialsNonExpired(userBySearchModel.isCredentialsNonExpired());
        userModel.setEnabled(userBySearchModel.isEnabled());
        List<GrantedAuthorityModel> authorities = new ArrayList<>();
        RoleModel roleModel = new RoleModel();
        roleModel.setAuthority(ROLE_USER.toString());
        authorities.add(roleModel);
        if (userByIdModel.getAuthorities().length == 2) {
            roleModel.setAuthority(ROLE_ADMIN.toString());
            authorities.add(roleModel);
        }
        userModel.setAuthorities(authorities);
        assertNotNull(userModel.getExpertiseProfile().getCategory(), "User has no professional category");

        return userModel;
    }

    public PersonalProfileModel editPersonalProfile(UserModel user) {

        String birthYear = helpers.generateBirthdayDate();
        String firstName = user.getPersonalProfile().getFirstName();
        int id = user.getId();
        String lastName = helpers.generateLastName();
        String city = helpers.generateCity();
        String personalReview = helpers.generatePersonalReview();
        String picture = helpers.generatePicture();
        boolean picturePrivacy = true;
        String sex = "MALE";

        PersonalProfileModel personalProfileModel = new PersonalProfileModel();
        personalProfileModel.setBirthYear(birthYear);
        personalProfileModel.setFirstName(firstName);
        personalProfileModel.setId(id);
        personalProfileModel.setLastName(lastName);
        personalProfileModel.getLocation().getCity().setCity(city);
        personalProfileModel.setPersonalReview(personalReview);
        personalProfileModel.setPicture(picture);
        personalProfileModel.setPicturePrivacy(picturePrivacy);
        personalProfileModel.setSex(sex);

        String body = String.format(personalProfileBody, birthYear, firstName, id, lastName, city, personalReview, picture,
                picturePrivacy, sex);

        Response editProfileResponse = given()
                .auth()
                .form(user.getUsername(), user.getPassword(),
                        new FormAuthConfig(AUTHENTICATE, "username", "password"))
                .contentType("application/json")
                .queryParam("name", user.getUsername())
                .body(body)
                .post(String.format(API + UPGRADE_USER_PERSONAL_WITH_ID, user.getId()));

        int statusCode = editProfileResponse.getStatusCode();
        assertEquals(statusCode, SC_OK, "Incorrect status code. Expected 200.");

        Response userByIdResponse = getUserById(user.getUsername(), user.getId());

        assertEditPersonalProfile(userByIdResponse, personalProfileModel);
        user.setPersonalProfile(editProfileResponse.as(PersonalProfileModel.class));
        LOGGER.info(String.format("Personal profile of user %s with id %d was updated", user.getUsername(), user.getId()));

        return user.getPersonalProfile();

    }

    public PersonalProfileModel editPersonalProfileFirstName(UserModel user, String firstName) {

        int id = user.getId();

        String body = String.format(personalProfileBodyFirstName, firstName);

        Response editProfileResponse = given()
                .auth()
                .form(user.getUsername(), user.getPassword(),
                        new FormAuthConfig(AUTHENTICATE, "username", "password"))
                .contentType("application/json")
                .body(body)
                .post(String.format(API + UPGRADE_USER_PERSONAL_WITH_ID, id));

        int statusCode = editProfileResponse.getStatusCode();
        assertEquals(statusCode, SC_OK, "Incorrect status code. Expected 200.");

        user.setPersonalProfile(editProfileResponse.as(PersonalProfileModel.class));

        assertEquals(editProfileResponse.getBody().jsonPath().getString("firstName"), user.getPersonalProfile().getFirstName(),
                "User personal profile was not updated.");

        LOGGER.info(String.format("First name of user %s with id %d was set to %s.", user.getUsername(), user.getId(), firstName));

        return user.getPersonalProfile();

    }

    public ExpertiseProfileModel editExpertiseProfile(UserModel user) {

        int availability = 8;
        int categoryId = 100;
        String categoryName = "All";
        String skill1 = helpers.generateSkill();
        String skill2 = helpers.generateSkill();
        String skill3 = helpers.generateSkill();
        String skill4 = helpers.generateSkill();
        String skill5 = helpers.generateSkill();

        ExpertiseProfileModel expertiseProfileModel = new ExpertiseProfileModel();
        expertiseProfileModel.setAvailability(availability);
        expertiseProfileModel.getCategory().setId(categoryId);
        expertiseProfileModel.getCategory().setName(categoryName);
        for (int i = 0; i < 5; i++) {
            expertiseProfileModel.getSkills().add(new SkillModel());
        }
        expertiseProfileModel.getSkills().get(0).setSkill(skill1);
        expertiseProfileModel.getSkills().get(1).setSkill(skill2);
        expertiseProfileModel.getSkills().get(2).setSkill(skill3);
        expertiseProfileModel.getSkills().get(3).setSkill(skill4);
        expertiseProfileModel.getSkills().get(4).setSkill(skill5);

        String body = String.format(expertiseProfileBpdy, availability, categoryId, categoryName, skill1, skill2, skill3,
                skill4, skill5);

        Response response = given()
                .auth()
                .form(user.getUsername(), user.getPassword(),
                        new FormAuthConfig(AUTHENTICATE, "username", "password"))
                .contentType("application/json")
                .body(body)
                .post(String.format(API + UPGRADE_USER_EXPERTISE_WITH_ID, user.getId()))
                .then()
                .assertThat()
                .statusCode(SC_OK)
                .extract().response();

        UserBySearchModel userBySearchModel = searchUser(user.getId(), user.getPersonalProfile().getFirstName());

        assertEditExpertiseProfile(userBySearchModel, expertiseProfileModel);

        LOGGER.info(String.format("Expertise profile of user %s with id %d was updated", user.getUsername(), user.getId()));

        return response.as(ExpertiseProfileModel.class);
    }

    public UserBySearchModel searchUser(int userId, String firstname) {

        int index = 0;
        boolean next = true;
        String searchParam1 = "";
        String searchParam2 = firstname;
        int size = 1000000;

        String body = String.format(searchUsersBody, index, next, searchParam1, searchParam2, size);

        Response response = given()
                .contentType("application/json")
                .body(body)
                .post(API + USERS);

        int statusCode = response.getStatusCode();
        assertEquals(statusCode, SC_OK, "Incorrect status code. Expected 200.");

        UserBySearchModel[] foundUsers = new Gson().fromJson(response.getBody().asString(), UserBySearchModel[].class);
        for (UserBySearchModel userBySearchModel: foundUsers) {
            if (userBySearchModel.getUserId() == userId) {
                return userBySearchModel;
            }
        }

        return null;

    }

    public void disableUser(UserModel adminUser, int userId) {

        Response response = given()
                .auth()
                .form(adminUser.getUsername(), adminUser.getPassword(),
                        new FormAuthConfig(AUTHENTICATE, "username", "password"))
                .formParam("enable", false)
                .queryParam("userId", userId)
                .post(ADMIN_STATUS);

        int statusCode = response.getStatusCode();
        assertEquals(statusCode, SC_MOVED_TEMPORARILY, "Incorrect status code. Expected 200.");

        LOGGER.info(String.format("User with id %d disabled.", userId));
    }

    public void enableUser(UserModel adminUser, UserModel userToBeEnabled) {

        Response response = given()
                .auth()
                .form(adminUser.getUsername(), adminUser.getPassword(),
                        new FormAuthConfig(AUTHENTICATE, "username", "password"))
                .formParam("enable", true)
                .queryParam("userId", userToBeEnabled.getId())
                .post(ADMIN_STATUS);

        int statusCode = response.getStatusCode();
        assertEquals(statusCode, SC_MOVED_TEMPORARILY, "Incorrect status code. Expected 200.");

        LOGGER.info(String.format("User %s with id %d enabled.", userToBeEnabled.getUsername(), userToBeEnabled.getId()));

    }

    public void deleteUser(int userId) throws SQLException {

        String disableForeignKeyChecks = "SET FOREIGN_KEY_CHECKS = 0;";
        String deleteUser = "DELETE FROM users WHERE user_id=?;";
        String enableForeignKeyChecks = "SET FOREIGN_KEY_CHECKS = 1;";

//        String deleteUserRequestQuery = "DELETE FROM requests WHERE sender_user_id=1367;";
//        String deleteUserCommentsQuery = "DELETE FROM comments_table WHERE user_id=1367;";
//        String deleteUserPostsQuery = "DELETE FROM posts_Table WHERE user_id=1367";
//        String deleteUserQuery = "DELETE FROM users WHERE user_id=1367";

        Connection con = getConnection();


        try {

            Statement statementDisable = con.createStatement();
            statementDisable.executeUpdate(disableForeignKeyChecks);
            PreparedStatement myStmt = con.prepareStatement(deleteUser);
            myStmt.setInt(1, userId);
            myStmt.executeUpdate(deleteUser);
            Statement statementEnable = con.createStatement();
            statementEnable.executeUpdate(enableForeignKeyChecks);

            LOGGER.info(String.format("User with id %d was deleted.%n", userId));

            con.close();
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    protected Connection getConnection() throws SQLException {

        Connection conn = DriverManager.getConnection("jdbc:mysql://84.21.205.241:33061/telerik_project",
                "telerik", "nakovEtap");

        LOGGER.info("Connected to database");

        return conn;
    }

    protected UserModel readUserById(String username, int userId) {

        Response response = given()
//                .auth()
//                .form(user.getUsername(), user.getPassword(),
//                        new FormAuthConfig(AUTHENTICATE, "username", "password"))
                .contentType("application/json")
                .queryParam("principal", username)
                .get(String.format(API + USER_BY_ID, userId))
                .then()
                .assertThat()
                .statusCode(SC_OK)
                .extract()
                .response();

        System.out.println(response.getBody().asPrettyString());

        UserModel userModel = response.as(UserModel.class);

        return userModel;

    }

    public Response getUserById(String username, int userId) {

        Response response = given()
                .queryParam("principal", username)
                .get(String.format(API + USER_BY_ID, userId))
                .then()
                .assertThat()
                .statusCode(SC_OK)
                .extract().response();

        LOGGER.info(String.format("User with id %d found", userId));

        return response;
    }

    protected String getUserId(Response response) {
        return response.body().asString().replaceAll("\\D", "");
    }

    public PostModel[] findAllPosts() {

        Response response = given()
                .queryParam("name", "adminvHQOD")
                .get(API + POST)
                .then()
                .assertThat()
                .statusCode(SC_OK)
                .extract().response();

        PostModel[] foundPosts = response.as(PostModel[].class);

        return foundPosts;

    }

    public PostModel[] showProfilePosts(UserModel user) {

        int index = 0;
        boolean next = true;
        String searchParam1 = "";
        String searchParam2 = user.getPersonalProfile().getFirstName();
        int size = 1000000;

        String body = String.format(searchUsersBody, index, next, searchParam1, searchParam2, size);

        Response response = given()
                .auth()
                .form(user.getUsername(), user.getPassword(),
                        new FormAuthConfig(AUTHENTICATE, "username", "password"))
                .contentType("application/json")
                .body(body)
                .get(String.format(API + USER_POSTS_WITH_ID, user.getId()))
                .then()
                .assertThat()
                .statusCode(SC_OK)
                .extract().response();

        PostModel[] userPosts = new Gson().fromJson(response.getBody().asString(), PostModel[].class);

        return userPosts;

    }

    public PostModel createPost(UserModel userModel, boolean publicVisibility) {

        PostModel post = given()
                .auth()
                .form(userModel.getUsername(), userModel.getPassword(),
                        new FormAuthConfig(AUTHENTICATE, "username", "password"))
                .contentType("application/json")
                .body(helpers.generatePostBody(publicVisibility))
                .when()
                .post(API + CREATE_POST)
                .then()
                .assertThat()
                .statusCode(SC_OK)
                .extract()
                .response()
                .as(PostModel.class);

        if (publicVisibility) {
            LOGGER.info(String.format("Public post with id %d created by user %s.", post.getPostId(), userModel.getUsername()));
        } else {
            LOGGER.info(String.format("Private post with id %d created by user %s.", post.getPostId(), userModel.getUsername()));
        }

        return post;
    }

    public void editPost(UserModel user, PostModel post) {

        boolean visibility = post.isPublic();

        Response editedPostResponse = given()
                .auth()
                .form(user.getUsername(), user.getPassword(),
                        new FormAuthConfig(AUTHENTICATE, "username", "password"))
                .contentType("application/json")
                .queryParam("postId", post.getPostId())
                .body(String.format(helpers.generatePostBody(visibility)))
                .put(API + EDIT_POST)
                .then()
                .assertThat()
                .statusCode(SC_OK)
                .extract().response();

        if (post.isPublic()) {
            LOGGER.info(String.format("Public post with id %d edited.", post.getPostId()));
        } else {
            LOGGER.info(String.format("Private post with id %d edited.", post.getPostId()));
        }

    }

    public PostModel likePost(UserModel user, int postId) {
        Response response = given()
                .auth()
                .form(user.getUsername(), user.getPassword(),
                        new FormAuthConfig(AUTHENTICATE, "username", "password"))
                .queryParam("postId", postId)
                .post(API + LIKE_POST);

        int statusCode = response.getStatusCode();
        assertEquals(statusCode, SC_OK, "Incorrect status code. Expected 200.");

        return response.as(PostModel.class);

    }

    public void deletePost(UserModel user, int postId) {

        Response response = given()
                .auth()
                .form(user.getUsername(), user.getPassword(),
                        new FormAuthConfig(AUTHENTICATE, "username", "password"))
                .queryParam("postId", postId)
                .delete(API + DELETE_POST)
                .then()
                .assertThat()
                .statusCode(SC_OK)
                .extract().response();

        LOGGER.info(String.format("Post with id %d deleted.", postId));

    }

    public boolean publicPostExists(int postId) {

        PostModel[] posts = findAllPosts();

        for (PostModel post : posts) {
            if (post.getPostId() == postId) {
                return true;
            }
        }

        return false;

    }

    public boolean privatePostExists(UserModel user, int postId) {

        PostModel[] posts = showProfilePosts(user);

        for (PostModel post : posts) {
            if (post.getPostId() == postId) {
                return true;
            }
        }

        return false;

    }

    public CommentModel[] findCommentsOfAPost(int postId) {

        Response response = given()
                .queryParam("postId", postId)
                .get(API + COMMENTS_OF_POST);

        int statusCode = response.getStatusCode();
        assertEquals(statusCode, SC_OK, "Incorrect status code. Expected 200.");

        CommentModel[] postComments = new Gson().fromJson(response.getBody().asString(), CommentModel[].class);

        return postComments;
    }

    public CommentModel[] findAllComments() {

        Response response = given()
                .get(API + COMMENT_ALL)
                .then()
                .assertThat()
                .statusCode(SC_OK)
                .extract().response();

        CommentModel[] allComments = new Gson().fromJson(response.getBody().asString(), CommentModel[].class);

        return allComments;

    }

    public CommentModel createComment(UserModel user, PostModel post) {

        String commentContent = helpers.generateCommentContent();
        boolean deletedConfirmed = true;
        int postId = post.getPostId();
        int userId = user.getId();

        String body = String.format(commentBody, commentContent, deletedConfirmed, postId, userId);

        Response response = given()
                .auth()
                .form(user.getUsername(), user.getPassword(),
                        new FormAuthConfig(AUTHENTICATE, "username", "password"))
                .contentType("application/json")
                .body(body)
                .when()
                .post(API + CREATE_COMMENT);

        int statusCode = response.getStatusCode();

        if (statusCode == 500) {
            return null;
        }

        assertEquals(statusCode, SC_OK, "Incorrect status code. Expected 200.");
        assertEquals(response.jsonPath().getString("content"), commentContent, "Contents do not match.");
        CommentModel comment = response.as(CommentModel.class);

        comment.setUser(user);
        comment.setPost(post);

        LOGGER.info(String.format("Comment with id %d created.", comment.getCommentId()));

        return comment;

    }

    public void editComment(UserModel user, CommentModel commentToBeEdited) {

        String commentContent = helpers.generateCommentContent();

        Response editedCommentResponse = given()
                .auth()
                .form(user.getUsername(), user.getPassword(),
                        new FormAuthConfig(AUTHENTICATE, "username", "password"))
                .queryParam("commentId", commentToBeEdited.getCommentId())
                .queryParam("content", commentContent)
                .put(API + EDIT_COMMENT)
                .then()
                .assertThat()
                .statusCode(SC_OK)
                .extract().response();

        LOGGER.info(String.format("Comment with id %d edited.", commentToBeEdited.getCommentId()));

    }

    public CommentModel likeComment(UserModel user, int commentId) {

        Response response = given()
                .auth()
                .form(user.getUsername(), user.getPassword(),
                        new FormAuthConfig(AUTHENTICATE, "username", "password"))
                .queryParam("commentId", commentId)
                .post(API + LIKE_COMMENT);

        int statusCode = response.getStatusCode();
        assertEquals(statusCode, SC_OK, "Incorrect status code. Expected 200.");

        return response.as(CommentModel.class);

    }

    public void deleteComment(UserModel user, int commentId) {
        Response response = given()
                .auth()
                .form(user.getUsername(), user.getPassword(),
                        new FormAuthConfig(AUTHENTICATE, "username", "password"))
                .queryParam("commentId", commentId)
                .delete(API + DELETE_COMMENT);

        int statusCode = response.getStatusCode();
        assertEquals(statusCode, SC_OK, "Incorrect status code. Expected 200.");

        LOGGER.info(String.format("Comment with id %d deleted.", commentId));

    }

    public boolean commentExists(int commentId) {

        CommentModel[] comments = findAllComments();

        for (CommentModel comment : comments) {
            if (comment.getCommentId() == commentId) {
                return true;
            }
        }

        return false;

    }

    public CommentModel[] findAllCommentsOfAPost(UserModel user, int postId) {

        Response response = given()
                .auth()
                .form(user.getUsername(), user.getPassword(),
                        new FormAuthConfig(AUTHENTICATE, "username", "password"))
                .queryParam("postId", postId)
                .get(API + COMMENT_BY_POST);

        int statusCode = response.getStatusCode();
        assertEquals(statusCode, SC_OK, "Incorrect status code. Expected 200.");

        CommentModel[] postComments = new Gson().fromJson(response.getBody().asString(), CommentModel[].class);

        return postComments;
    }

    public CommentModel getCommentById(UserModel user, int commentId) {

        Response response = given()
                .auth()
                .form(user.getUsername(), user.getPassword(),
                        new FormAuthConfig(AUTHENTICATE, "username", "password"))
                .queryParam("commentId", commentId)
                .get(API + COMMENT_SINGLE);

        int statusCode = response.getStatusCode();
        assertEquals(statusCode, SC_OK, "Incorrect status code. Expected 200.");

        CommentModel comment = response.as(CommentModel.class);

        return comment;
    }

    public RequestModel sendRequest(UserModel sender, UserModel receiver) {

        Response response = given()
                .auth()
                .form(sender.getUsername(), sender.getPassword(),
                        new FormAuthConfig(AUTHENTICATE, "username", "password"))
                .contentType("application/json")
                .body(String.format(sendRequestBody, receiver.getId(), receiver.getUsername()))
                .post(API + REQUEST);

        LOGGER.info(response.getBody().asPrettyString());

        int statusCode = response.getStatusCode();
        assertEquals(statusCode, SC_OK, "Incorrect status code. Expected 200.");
        assertEquals(response.body().asString(), String.format("%s send friend request to %s",
                sender.getUsername(), receiver.getUsername()), "Connection request was not send");

        RequestModel request = new RequestModel();
        request.setSender(sender);
        request.setReceiver(receiver);
        String[] fields = getRequestBySenderAndReceiver(sender, receiver);
        request.setId(Integer.parseInt(fields[0]));
        request.setTimeStamp(fields[1]);

        return request;

    }

    public RequestModel[] getUserRequests(UserModel receiver) {
        Response response = given()
                .auth()
                .form(receiver.getUsername(), receiver.getPassword(),
                        new FormAuthConfig(AUTHENTICATE, "username", "password"))
                .get(String.format(API + USER_REQUEST_WITH_ID, receiver.getId()));

        int statusCode = response.getStatusCode();
        assertEquals(statusCode, SC_OK, "Incorrect status code. Expected 200.");

        RequestModel[] requests = new Gson().fromJson(response.getBody().asString(), RequestModel[].class);

        return requests;
    }

    protected String[] getRequestBySenderAndReceiver(UserModel sender, UserModel receiver) {
        Response response = given()
                .auth()
                .form(receiver.getUsername(), receiver.getPassword(),
                        new FormAuthConfig(AUTHENTICATE, "username", "password"))
                .get(String.format(API + USER_REQUEST_WITH_ID, receiver.getId()));

        int statusCode = response.getStatusCode();
        assertEquals(statusCode, SC_OK, "Incorrect status code. Expected 200.");

        RequestModel[] requests = new Gson().fromJson(response.getBody().asString(), RequestModel[].class);

        String[] fields = new String[2];

        fields[0] = String.valueOf(requests[0].getId());
        fields[1] = String.valueOf(requests[0].getTimeStamp());

        return fields;
    }

    public Response approveRequest(UserModel receiver, RequestModel request) {

        Response response = given()
                .auth()
                .form(receiver.getUsername(), receiver.getPassword(),
                        new FormAuthConfig(AUTHENTICATE, "username", "password"))
                .queryParam("requestId", request.getId())
                .post(String.format(API + APPROVE_REQUEST_WITH_ID, receiver.getId()));

        LOGGER.info(response.getBody().asPrettyString());

        int statusCode = response.getStatusCode();
        assertEquals(statusCode, SC_OK, "Incorrect status code. Expected 200.");

        return response;
    }

    public Response disconnectFromUser(UserModel sender, UserModel receiver) {

        Response response = given()
                .auth()
                .form(sender.getUsername(), sender.getPassword(),
                        new FormAuthConfig(AUTHENTICATE, "username", "password"))
                .contentType("application/json")
                .body(String.format(sendRequestBody, receiver.getId(), receiver.getUsername()))
                .post(API + REQUEST);

        LOGGER.info(response.getBody().asPrettyString());

        return response;

    }

    public void connectUsers(UserModel sender, UserModel receiver) {

        approveRequest(receiver, sendRequest(sender, receiver));

    }

    public SkillModel[] getAllSkills() {

        Response response = given()
                .get(API + FIND_SKILL);

        int statusCode = response.getStatusCode();
        assertEquals(statusCode, SC_OK, "Incorrect status code. Expected 200.");

        SkillModel[] skills = new Gson().fromJson(response.getBody().asString(), SkillModel[].class);

        return skills;

    }

    public SkillModel createSkill(UserModel user) {

        int categoryId = 100;
        String categoryName = "All";
        String skillService = helpers.generateSkill();

        String body = String.format(skillBody, categoryId, categoryName, skillService);

        Response response = given()
                .auth()
                .form(user.getUsername(), user.getPassword(),
                        new FormAuthConfig(AUTHENTICATE, "username", "password"))
                .contentType("application/json")
                .body(body)
                .post(API + CREATE_SKILL);

        int statusCode = response.getStatusCode();
        assertEquals(statusCode, SC_OK, "Incorrect status code. Expected 200.");

        SkillModel skill = response.as(SkillModel.class);

        LOGGER.info(String.format("Skill %s created in category %s.", skillService, categoryName));
        return skill;

    }

    public void deleteSkill(int skillId) {

        Response response = given()
                .queryParam("skillId", skillId)
                .put(API + DELETE_SKILL);

        int statusCode = response.getStatusCode();
        assertEquals(statusCode, SC_OK, "Incorrect status code. Expected 200.");

        LOGGER.info("Skill deleted.");
    }

    public SkillModel getSkillById(int skillId) {

        Response response = given()
                .queryParam("skillId", skillId)
                .get(API + GET_ONE_SKILL);

        int statusCode = response.getStatusCode();
        assertEquals(statusCode, SC_OK, "Incorrect status code. Expected 200.");

        return response.as(SkillModel.class);

    }

    public boolean skillExists(int skillId) {

        SkillModel[] skills = getAllSkills();

        for (SkillModel skill : skills) {
            if (skill.getSkillId() == skillId) {
                return true;
            }
        }

        return false;

    }

    public Response editSkill(int skillId) {

        String skillService = helpers.generateSkill();

        Response response = given()
                .queryParam("skill", skillService)
                .queryParam("skillId", skillId)
                .put(API + EDIT_SKILL);

        return response;

    }

    private void assertEditPersonalProfile(Response response, PersonalProfileModel personalProfile) {

        int statusCode = response.getStatusCode();
        assertEquals(statusCode, SC_OK, "Incorrect status code. Expected 200.");
        assertEquals(response.getBody().jsonPath().getString("firstName"), personalProfile.getFirstName(),
                "First names do not match.");
        assertEquals(response.getBody().jsonPath().getString("lastNAme"), personalProfile.getLastName(),
                "Last names do not match.");
        assertEquals(response.getBody().jsonPath().getString("birthYear"), personalProfile.getBirthYear(),
                "Birth years do not match.");
        assertEquals(response.getBody().jsonPath().getString("location.city.city"), personalProfile.getLocation().getCity().getCity(),
                "Cities do not match.");
        assertEquals(response.getBody().jsonPath().getString("personalReview"), personalProfile.getPersonalReview(),
                "Personal reviews do not match.");
        assertEquals(response.getBody().jsonPath().getString("picturePrivacy"), String.valueOf(personalProfile.getPicturePrivacy()),
                "Picture privacies do not match.");
    }

    private void assertEditExpertiseProfile(UserBySearchModel userBySearchModel, ExpertiseProfileModel expertiseProfileModel) {

        assertEquals(userBySearchModel.getExpertiseProfile().getCategory().getId(), expertiseProfileModel.getCategory().getId(),
                "Category ids do not match.");
        assertEquals(userBySearchModel.getExpertiseProfile().getCategory().getName(), expertiseProfileModel.getCategory().getName(),
                "Category names do not match.");
        assertEquals(userBySearchModel.getExpertiseProfile().getAvailability(), expertiseProfileModel.getAvailability(),
                "Availabilities do not match.");
        for (int i = 0; i < 5; i++) {
            assertEquals(userBySearchModel.getExpertiseProfile().getSkills().get(i).getSkill(),
                    expertiseProfileModel.getSkills().get(i).getSkill(), String.format("Skill%d do not match.", i));
        }

    }

    protected void assertEditPersonalProfileFirstName(Response response, PersonalProfileModel personalProfile) {

        int statusCode = response.getStatusCode();
        assertEquals(statusCode, SC_OK, "Incorrect status code. Expected 200.");
        assertEquals(response.getBody().jsonPath().getString("firstName"), personalProfile.getFirstName(),
                "User personal profile was not updated.");
    }

    public void assertPostCreation(PostModel post) {

        assertNotNull(post, "Post was not created.");
        assertNotNull(post.getPostId(), "Post was not created.");

    }

    public void assertEditedPublicPost(int postId, String postToBeEditedContent) {

        PostModel[] foundPosts = findAllPosts();

        for (PostModel post : foundPosts) {
            if (post.getPostId() == postId) {
                assertNotEquals(post.getContent(), postToBeEditedContent,
                        "Post contents are equal. Post was not edited");
                break;
            }
        }
    }

    public void assertEditedPrivatePost(UserModel user, int postId, String postToBeEditedContent) {

        PostModel[] foundPosts = showProfilePosts(user);

        for (PostModel post : foundPosts) {
            if (post.getPostId() == postId) {
                System.out.println(post.getPostId());
                System.out.println(postId);
                assertNotEquals(post.getContent(), postToBeEditedContent,
                        "Post contents are equal. Post was not edited");
                break;
            }
        }
    }

    public void assertEditedComment(UserModel user, int postId, int commentId, String contentToBeEdited) {

        CommentModel[] postComments = findAllCommentsOfAPost(user, postId);

        for (CommentModel postComment : postComments) {
            if (postComment.getCommentId() == commentId) {
                assertNotEquals(postComment.getContent(), contentToBeEdited, "Contents are the same.");
                break;
            }
        }
    }
}