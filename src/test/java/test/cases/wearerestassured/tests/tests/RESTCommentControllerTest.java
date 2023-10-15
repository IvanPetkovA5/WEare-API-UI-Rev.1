package test.cases.wearerestassured.tests.tests;

import test.cases.BaseTestSetup;
import com.telerikacademy.testframework.models.CommentModel;
import com.telerikacademy.testframework.models.PostModel;
import com.telerikacademy.testframework.models.UserModel;
import io.restassured.response.Response;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import test.cases.wearerestassured.tests.base.BaseWeareRestAssuredTest;

import static com.telerikacademy.testframework.utils.UserRoles.*;
import static org.apache.http.HttpStatus.*;
import static org.testng.Assert.*;

public class RESTCommentControllerTest extends BaseWeareRestAssuredTest {
    private UserModel commentUser;

    @BeforeClass
    private void setUp() {
        commentUser = WEareApi.registerUser(ROLE_USER.toString());
    }

    @AfterClass
    public void clear() {
        WEareApi.disableUser(globalAdminUser, commentUser.getId());
    }

    @Test
    public void user_Can_Create_Comment_Of_A_Public_Post_With_Valid_Data() {

        UserModel newUser = WEareApi.registerUser(ROLE_USER.toString());

        boolean publicVisibility = true;
        PostModel post = WEareApi.createPost(commentUser, publicVisibility);

        CommentModel comment = WEareApi.createComment(newUser, post);

        assertNotNull(comment, "Comment was not made.");
        assertEquals(comment.getPost().getPostId(), post.getPostId(), "Comment is not made for the required post.");
        assertEquals(comment.getUser().getId(), newUser.getId(), "Comment is not made by the required user.");

        WEareApi.deleteComment(newUser, comment.getCommentId());
        assertFalse(WEareApi.commentExists(comment.getCommentId()), "Comment was not deleted.");

        WEareApi.deletePost(commentUser, post.getPostId());
        assertFalse(WEareApi.postExists(post.getPostId()), "Post was not deleted.");

    }

    @Test
    public void user_Can_Create_Comment_Of_A_Private_Post_With_Valid_Data_If_Connected() {

        UserModel newUser = WEareApi.registerUser(ROLE_USER.toString());

        WEareApi.connectUsers(commentUser, newUser);

        boolean publicVisibility = false;
        PostModel post = WEareApi.createPost(commentUser, publicVisibility);

        CommentModel comment = WEareApi.createComment(newUser, post);

        assertNotNull(comment, "Comment was not made.");
        assertEquals(comment.getPost().getPostId(), post.getPostId(), "Comment is not made for the required post.");
        assertEquals(comment.getUser().getId(), newUser.getId(), "Comment is not made by the required user.");

        WEareApi.deleteComment(newUser, comment.getCommentId());
        assertFalse(WEareApi.commentExists(comment.getCommentId()), "Comment was not deleted.");

        WEareApi.deletePost(commentUser, post.getPostId());
        assertFalse(WEareApi.postExists(post.getPostId()), "Post was not deleted.");
    }

    @Test
    public void user_Cannot_Create_Comment_Of_A_Private_Post_With_Valid_Data() {

        UserModel newUser = WEareApi.registerUser(ROLE_USER.toString());

        boolean publicVisibility = false;
        PostModel post = WEareApi.createPost(commentUser, publicVisibility);

        CommentModel comment = WEareApi.createComment(newUser, post);

        assertNull(comment, "Comment was made.");

        WEareApi.deletePost(commentUser, post.getPostId());
        assertFalse(WEareApi.postExists(post.getPostId()), "Post was not deleted.");

    }

    @Test
    public void user_Can_Find_All_Comments() {

        CommentModel[] comments = WEareApi.findAllComments();

        for (CommentModel comment : comments) {
            assertEquals(comment.getClass(), CommentModel.class, "There are no comments found");
            assertNotNull(comment.getCommentId(), "There are no comments found");
        }
    }

    @Test
    public void user_Can_Edit_Comment_Of_A_Post_With_Valid_Data() {

        boolean publicVisibility = true;
        PostModel post = WEareApi.createPost(commentUser, publicVisibility);
        CommentModel comment = WEareApi.createComment(commentUser, post);

        Response editedCommentResponse = WEareApi.editComment(commentUser, comment);

        int statusCode = editedCommentResponse.getStatusCode();
        assertEquals(statusCode, SC_OK, "Incorrect status code. Expected 200.");

        WEareApi.deleteComment(commentUser, comment.getCommentId());
        assertFalse(WEareApi.commentExists(comment.getCommentId()), "Comment was not deleted.");

        WEareApi.deletePost(commentUser, post.getPostId());
        assertFalse(WEareApi.postExists(post.getPostId()), "Post was not deleted.");

    }

    @Test
    public void admin_User_Can_Edit_Comment_Of_A_Public_Post_With_Valid_Data() {

        UserModel adminUser = WEareApi.registerUser(ROLE_ADMIN.toString());
        UserModel newUser = WEareApi.registerUser(ROLE_ADMIN.toString());

        boolean publicVisibility = true;
        PostModel post = WEareApi.createPost(commentUser, publicVisibility);
        CommentModel comment = WEareApi.createComment(newUser, post);

        Response editedCommentResponse = WEareApi.editComment(adminUser, comment);

        int statusCode = editedCommentResponse.getStatusCode();
        assertEquals(statusCode, SC_OK, "Incorrect status code. Expected 200.");

        WEareApi.deleteComment(newUser, comment.getCommentId());
        assertFalse(WEareApi.commentExists(comment.getCommentId()), "Comment was not deleted.");

        WEareApi.deletePost(commentUser, post.getPostId());
        assertFalse(WEareApi.postExists(post.getPostId()), "Post was not deleted.");

    }

    @Test
    public void admin_User_Can_Edit_Comment_Of_A_Private_Post_With_Valid_Data() {

        UserModel adminUser = WEareApi.registerUser(ROLE_ADMIN.toString());

        boolean publicVisibility = false;
        PostModel post = WEareApi.createPost(commentUser, publicVisibility);
        CommentModel comment = WEareApi.createComment(commentUser, post);

        Response editedCommentResponse = WEareApi.editComment(adminUser, comment);

        int statusCode = editedCommentResponse.getStatusCode();
        assertEquals(statusCode, SC_OK, "Incorrect status code. Expected 200.");

        WEareApi.deleteComment(adminUser, comment.getCommentId());
        assertFalse(WEareApi.commentExists(comment.getCommentId()), "Comment was not deleted.");

        WEareApi.deletePost(commentUser, post.getPostId());
        assertFalse(WEareApi.postExists(post.getPostId()), "Post was not deleted.");
    }

    @Test
    public void user_Can_Like_Comment_Of_A_Public_Post() {

        UserModel newUser = WEareApi.registerUser(ROLE_USER.toString());
        UserModel userToLikeComment = WEareApi.registerUser(ROLE_USER.toString());

        boolean publicVisibility = true;
        PostModel post = WEareApi.createPost(commentUser, publicVisibility);

        CommentModel commentToBeLiked = WEareApi.createComment(newUser, post);
        CommentModel likedComment = WEareApi.likeComment(userToLikeComment, commentToBeLiked.getCommentId());

        int likedCommentLikesToHave = commentToBeLiked.getLikes().size() + 1;
        assertEquals(likedComment.getLikes().size(), likedCommentLikesToHave, "Comment was not liked.");
        assertEquals(commentToBeLiked.getCommentId(), likedComment.getCommentId(), "Liked comment is different.");

        WEareApi.deleteComment(newUser, commentToBeLiked.getCommentId());
        assertFalse(WEareApi.commentExists(commentToBeLiked.getCommentId()), "Comment was not deleted.");

        WEareApi.deletePost(commentUser, post.getPostId());
        assertFalse(WEareApi.postExists(post.getPostId()), "Post was not deleted.");
    }

    @Test
    public void user_Can_Delete_Comment_Of_A_Public_Post() {

        UserModel newUser = WEareApi.registerUser(ROLE_USER.toString());

        boolean publicVisibility = true;
        PostModel post = WEareApi.createPost(commentUser, publicVisibility);

        CommentModel commentToBeDeleted = WEareApi.createComment(newUser, post);

        int commentToBeDeletedId = commentToBeDeleted.getCommentId();

        WEareApi.deleteComment(newUser, commentToBeDeletedId);
        assertFalse(WEareApi.commentExists(commentToBeDeletedId), "Comment was not deleted.");

        WEareApi.deletePost(commentUser, post.getPostId());
        assertFalse(WEareApi.postExists(post.getPostId()), "Post was not deleted.");

    }

    @Test
    public void admin_User_Can_Delete_Comment_Of_A_Public_Post() {

        UserModel adminUser = WEareApi.registerUser(ROLE_ADMIN.toString());
        UserModel newUser = WEareApi.registerUser(ROLE_USER.toString());

        boolean publicVisibility = true;
        PostModel post = WEareApi.createPost(commentUser, publicVisibility);

        CommentModel commentToBeDeleted = WEareApi.createComment(newUser, post);
        int commentToBeDeletedId = commentToBeDeleted.getCommentId();

        WEareApi.deleteComment(adminUser, commentToBeDeletedId);
        assertFalse(WEareApi.commentExists(commentToBeDeletedId), "Comment was not deleted.");

        WEareApi.deletePost(commentUser, post.getPostId());
        assertFalse(WEareApi.postExists(post.getPostId()), "Post was not deleted.");

    }

    @Test
    public void admin_User_Can_Delete_Comment_Of_A_Private_Post() {

        UserModel adminUser = WEareApi.registerUser(ROLE_ADMIN.toString());
        UserModel newUser = WEareApi.registerUser(ROLE_USER.toString());

        WEareApi.connectUsers(commentUser, newUser);

        boolean publicVisibility = false;
        PostModel post = WEareApi.createPost(commentUser, publicVisibility);

        CommentModel commentToBeDeleted = WEareApi.createComment(newUser, post);

        int commentToBeDeletedId = commentToBeDeleted.getCommentId();

        WEareApi.deleteComment(adminUser, commentToBeDeletedId);
        assertFalse(WEareApi.commentExists(commentToBeDeletedId), "Comment was not deleted.");

        WEareApi.deletePost(commentUser, post.getPostId());
        assertFalse(WEareApi.postExists(post.getPostId()), "Post was not deleted.");

    }

    @Test
    public void user_Can_Find_All_Comments_Of_A_Post() {

        UserModel newUser = WEareApi.registerUser(ROLE_USER.toString());

        boolean publicVisibility = true;
        PostModel post = WEareApi.createPost(commentUser, publicVisibility);

        int commentCount = 3;

        for (int i = 0; i < commentCount; i++) {
            WEareApi.createComment(newUser, post);
        }

        CommentModel[] postComments = WEareApi.findAllCommentsOfAPost(commentUser, post.getPostId());

        assertEquals(postComments.length, commentCount, "Wrong post comments count");

        for (CommentModel comment : postComments) {
            assertEquals(comment.getClass(), CommentModel.class, "Wrong type of comment");
            assertNotNull(comment, "Comment is null");
        }

        for (CommentModel comment : postComments) {
            WEareApi.deleteComment(newUser, comment.getCommentId());
            assertFalse(WEareApi.commentExists(comment.getCommentId()), "Comment was not deleted.");
        }

        WEareApi.deletePost(commentUser, post.getPostId());
        assertFalse(WEareApi.postExists(post.getPostId()), "Post was not deleted.");

    }

    @Test
    public void user_Can_Find_A_Comment_By_Id() {

        UserModel newUser = WEareApi.registerUser(ROLE_USER.toString());

        boolean publicVisibility = true;
        PostModel post = WEareApi.createPost(commentUser, publicVisibility);

        CommentModel comment = WEareApi.createComment(newUser, post);
        int commentId = comment.getCommentId();

        CommentModel foundComment = WEareApi.getCommentById(commentUser, commentId);
        assertEquals(foundComment.getCommentId(), commentId, "Comments do not match.");

        WEareApi.deleteComment(newUser, comment.getCommentId());
        assertFalse(WEareApi.commentExists(comment.getCommentId()), "Comment was not deleted.");

        WEareApi.deletePost(commentUser, post.getPostId());
        assertFalse(WEareApi.postExists(post.getPostId()), "Post was not deleted.");

    }
}