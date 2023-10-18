package test.cases.wearerestassuredtests.tests;

import api.models.CommentModel;
import api.models.PostModel;
import api.models.UserModel;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import test.cases.wearerestassuredtests.base.BaseWeareRestAssuredTest;

import static com.telerikacademy.testframework.utils.UserRoles.ROLE_USER;
import static org.testng.Assert.*;

public class RESTCommentControllerTest extends BaseWeareRestAssuredTest {

    UserModel commentUser;

    @BeforeClass
    public void setUpCommentTest() {
        commentUser = WEareApi.registerUser(ROLE_USER.toString());
    }

    @AfterClass
    public void tearDownCommentTest() {
        WEareApi.disableUser(globalRESTAdminUser, commentUser.getId());
    }

    @Test
    public void userCanCreateCommentOfAPublicPostWithValidData() {

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
    public void userCanCreateCommentOfAPrivatePostWithValidDataIfConnected() {

        UserModel sender = WEareApi.registerUser(ROLE_USER.toString());
        UserModel receiver = WEareApi.registerUser(ROLE_USER.toString());

        WEareApi.connectUsers(sender, receiver);

        boolean publicVisibility = false;
        PostModel post = WEareApi.createPost(sender, publicVisibility);

        CommentModel comment = WEareApi.createComment(receiver, post);

        assertNotNull(comment, "Comment was not made.");
        assertEquals(comment.getPost().getPostId(), post.getPostId(), "Comment is not made for the required post.");
        assertEquals(comment.getUser().getId(), receiver.getId(), "Comment is not made by the required user.");

        WEareApi.deleteComment(receiver, comment.getCommentId());
        assertFalse(WEareApi.commentExists(comment.getCommentId()), "Comment was not deleted.");

        WEareApi.deletePost(sender, post.getPostId());
        assertFalse(WEareApi.postExists(post.getPostId()), "Post was not deleted.");
    }

    @Test
    public void userCannotCreateCommentOfAPrivatePostWithValid_Data() {

        UserModel newUser = WEareApi.registerUser(ROLE_USER.toString());

        boolean publicVisibility = false;
        PostModel post = WEareApi.createPost(commentUser, publicVisibility);

        CommentModel comment = WEareApi.createComment(newUser, post);

        assertNull(comment, "Comment was made.");

        WEareApi.deletePost(commentUser, post.getPostId());
        assertFalse(WEareApi.postExists(post.getPostId()), "Post was not deleted.");

    }

    @Test
    public void userCanFindAllComments() {

        CommentModel[] comments = WEareApi.findAllComments();

        for (CommentModel comment : comments) {
            assertNotNull(comment.getCommentId(), "There are no comments found");
        }
    }

    @Test
    public void userCanEditCommentOfAPostWithValid_Data() {

        UserModel newUser = WEareApi.registerUser(ROLE_USER.toString());

        boolean publicVisibility = true;
        PostModel post = WEareApi.createPost(commentUser, publicVisibility);
        CommentModel comment = WEareApi.createComment(newUser, post);

        String contentToBeEdited = comment.getContent();

        WEareApi.editComment(newUser, comment);

        WEareApi.assertEditedComment(newUser, post.getPostId(), comment.getCommentId(), contentToBeEdited);

        WEareApi.deleteComment(newUser, comment.getCommentId());
        assertFalse(WEareApi.commentExists(comment.getCommentId()), "Comment was not deleted.");

        WEareApi.deletePost(commentUser, post.getPostId());
        assertFalse(WEareApi.postExists(post.getPostId()), "Post was not deleted.");

    }

    @Test
    public void adminUserCanEditCommentOfAPublicPostWithValidData() {

        UserModel newUser = WEareApi.registerUser(ROLE_USER.toString());

        boolean publicVisibility = true;
        PostModel post = WEareApi.createPost(commentUser, publicVisibility);
        CommentModel comment = WEareApi.createComment(newUser, post);

        String contentToBeEdited = comment.getContent();

        WEareApi.editComment(globalRESTAdminUser, comment);

        WEareApi.assertEditedComment(commentUser, post.getPostId(), comment.getCommentId(), contentToBeEdited);

        WEareApi.deleteComment(newUser, comment.getCommentId());
        assertFalse(WEareApi.commentExists(comment.getCommentId()), "Comment was not deleted.");

        WEareApi.deletePost(commentUser, post.getPostId());
        assertFalse(WEareApi.postExists(post.getPostId()), "Post was not deleted.");

    }

    @Test
    public void adminUserCanEditCommentOfAPrivatePostWithValidData() {

        boolean publicVisibility = false;
        PostModel post = WEareApi.createPost(commentUser, publicVisibility);
        CommentModel comment = WEareApi.createComment(commentUser, post);

        String contentToBeEdited = comment.getContent();

        WEareApi.editComment(globalRESTAdminUser, comment);

        WEareApi.assertEditedComment(commentUser, post.getPostId(), comment.getCommentId(), contentToBeEdited);

        WEareApi.deleteComment(globalRESTAdminUser, comment.getCommentId());
        assertFalse(WEareApi.commentExists(comment.getCommentId()), "Comment was not deleted.");

        WEareApi.deletePost(commentUser, post.getPostId());
        assertFalse(WEareApi.postExists(post.getPostId()), "Post was not deleted.");
    }

    @Test
    public void userCanLikeCommentOfAPublicPost() {

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
    public void userCanDeleteCommentOfAPublicPost() {

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
    public void adminUserCanDeleteCommentOfAPublicPost() {

        UserModel newUser = WEareApi.registerUser(ROLE_USER.toString());

        boolean publicVisibility = true;
        PostModel post = WEareApi.createPost(commentUser, publicVisibility);

        CommentModel commentToBeDeleted = WEareApi.createComment(newUser, post);
        int commentToBeDeletedId = commentToBeDeleted.getCommentId();

        WEareApi.deleteComment(globalRESTAdminUser, commentToBeDeletedId);
        assertFalse(WEareApi.commentExists(commentToBeDeletedId), "Comment was not deleted.");

        WEareApi.deletePost(commentUser, post.getPostId());
        assertFalse(WEareApi.postExists(post.getPostId()), "Post was not deleted.");

    }

    @Test
    public void adminUserCanDeleteCommentOfAPrivatePost() {

        UserModel newUser = WEareApi.registerUser(ROLE_USER.toString());

        WEareApi.connectUsers(commentUser, newUser);

        boolean publicVisibility = false;
        PostModel post = WEareApi.createPost(commentUser, publicVisibility);

        CommentModel commentToBeDeleted = WEareApi.createComment(newUser, post);

        int commentToBeDeletedId = commentToBeDeleted.getCommentId();

        WEareApi.deleteComment(globalRESTAdminUser, commentToBeDeletedId);
        assertFalse(WEareApi.commentExists(commentToBeDeletedId), "Comment was not deleted.");

        WEareApi.deletePost(commentUser, post.getPostId());
        assertFalse(WEareApi.postExists(post.getPostId()), "Post was not deleted.");

    }

    @Test
    public void userCanFindAllCommentsOfAPost() {

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
    public void userCanFindACommentById() {

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