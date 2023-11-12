package test.cases.wearerestassuredtests.tests;

import restassuredapi.models.models.CommentModel;
import restassuredapi.models.models.PostModel;
import restassuredapi.models.models.UserModel;
import org.testng.annotations.*;
import restassuredapi.CommentApi;
import restassuredapi.PostApi;
import restassuredapi.UserApi;
import test.cases.wearerestassuredtests.base.BaseWeareRestAssuredTest;

import java.util.ArrayList;
import java.util.List;

import static com.telerikacademy.testframework.utils.UserRoles.*;
import static org.testng.Assert.*;
import static restassuredapi.PostApi.showProfilePosts;

public class RESTPostControllerTest extends BaseWeareRestAssuredTest {

    @Test
    public void publicPostCreated_When_ValidDataProvided() {

        boolean publicVisibility = true;
        PostModel post = PostApi.createPost(globalRestApiUser, publicVisibility);

        assertTrue(PostApi.publicPostExists(post.getPostId()), "Post not created.");

        PostApi.deletePost(globalRestApiUser, post.getPostId());
        assertFalse(PostApi.publicPostExists(post.getPostId()), "Post was not deleted.");
    }

    @Test
    public void privatePostCreated_When_ValidDataProvided() {

        boolean publicVisibility = false;
        PostModel post = PostApi.createPost(globalRestApiUser, publicVisibility);
        assertTrue(PostApi.privatePostExists(globalRestApiUser, post.getPostId()), "Post not created.");

        PostApi.deletePost(globalRestApiUser, post.getPostId());
        assertFalse(PostApi.privatePostExists(globalRestApiUser, post.getPostId()), "Post was not deleted.");
    }

    @Test
    public void publicPostEdited_By_Author() {

        boolean publicVisibility = true;

        PostModel postToBeEdited = PostApi.createPost(globalRestApiUser, publicVisibility);
        assertTrue(PostApi.publicPostExists(postToBeEdited.getPostId()), "Post not created.");

        String postToBeEditedContent = postToBeEdited.getContent();

        PostApi.editPost(globalRestApiUser, postToBeEdited);
        PostApi.assertEditedPublicPost(postToBeEdited.getPostId(), postToBeEditedContent);

        PostApi.deletePost(globalRestApiUser, postToBeEdited.getPostId());
        assertFalse(PostApi.publicPostExists(postToBeEdited.getPostId()), "Post was not deleted.");
    }

    @Test
    public void privatePostEdited_By_Author() {

        boolean publicVisibility = false;

        PostModel postToBeEdited = PostApi.createPost(globalRestApiUser, publicVisibility);
        assertTrue(PostApi.privatePostExists(globalRestApiUser, postToBeEdited.getPostId()), "Post not created.");

        String postToBeEditedContent = postToBeEdited.getContent();

        PostApi.editPost(globalRestApiUser, postToBeEdited);
        PostApi.assertEditedPrivatePost(globalRestApiUser, postToBeEdited.getPostId(), postToBeEditedContent);

        PostApi.deletePost(globalRestApiUser, postToBeEdited.getPostId());
        assertFalse(PostApi.privatePostExists(globalRestApiUser, postToBeEdited.getPostId()), "Post was not deleted.");

    }

    @Test
    public void publicPostEdited_By_AdminUser_When_NotAuthor() {

        boolean publicVisibility = true;

        PostModel postToBeEdited = PostApi.createPost(globalRestApiUser, publicVisibility);
        assertTrue(PostApi.publicPostExists(postToBeEdited.getPostId()), "Post not created.");

        String postToBeEditedContent = postToBeEdited.getContent();

        PostApi.editPost(globalRestApiAdminUser, postToBeEdited);
        PostApi.assertEditedPublicPost(postToBeEdited.getPostId(), postToBeEditedContent);

        PostApi.deletePost(globalRestApiUser, postToBeEdited.getPostId());
        assertFalse(PostApi.publicPostExists(postToBeEdited.getPostId()), "Post was not deleted.");
    }

    @Test
    public void privatePostEdited_By_AdminUser_When_NotAuthor() {

        boolean publicVisibility = false;

        PostModel postToBeEdited = PostApi.createPost(globalRestApiUser, publicVisibility);
        assertTrue(PostApi.privatePostExists(globalRestApiUser, postToBeEdited.getPostId()), "Post not created.");

        String postToBeEditedContent = postToBeEdited.getContent();

        PostApi.editPost(globalRestApiAdminUser, postToBeEdited);
        PostApi.assertEditedPrivatePost(globalRestApiUser, postToBeEdited.getPostId(), postToBeEditedContent);

        PostApi.deletePost(globalRestApiUser, postToBeEdited.getPostId());
        assertFalse(PostApi.privatePostExists(globalRestApiUser, postToBeEdited.getPostId()), "Post was not deleted.");

    }

    @Test
    public void allPostsListed_When_Requested_By_User() {

        List<Integer> postIds = new ArrayList<>();

        boolean publicVisibility = true;

        int postCount = 5;

        for (int i = 0; i < postCount; i++) {
            PostModel post = PostApi.createPost(globalRestApiUser, publicVisibility);
            assertTrue(PostApi.publicPostExists(post.getPostId()), "Post not created.");
            postIds.add(post.getPostId());
        }
        publicVisibility = false;
        for (int i = 0; i < postCount; i++) {
            PostModel post = PostApi.createPost(globalRestApiUser, publicVisibility);
            assertTrue(PostApi.privatePostExists(globalRestApiUser, post.getPostId()), "Post not created.");
            postIds.add(post.getPostId());
        }

        PostModel[] foundPosts = PostApi.findAllPosts();

        PostModel[] foundPrivatePosts = showProfilePosts(globalRestApiUser);

        for (PostModel foundPost : foundPrivatePosts) {
            if (postIds.contains(foundPost.getPostId())) {
                assertNotNull(foundPost, "Post is null");
                PostApi.deletePost(globalRestApiUser, foundPost.getPostId());
                postIds.remove(foundPost.getPostId());
                assertFalse(PostApi.publicPostExists(foundPost.getPostId()), "Post was not deleted.");
            }
        }


        assertEquals(postIds.size(), 0, "Some posts were not received");

    }

    @Test
    public void postLiked_By_User() {

        boolean publicVisibility = true;
        PostModel postToBeLiked = PostApi.createPost(globalRestApiUser, publicVisibility);
        assertTrue(PostApi.publicPostExists(postToBeLiked.getPostId()), "Post not created.");


        int likedPostLikesToHave = postToBeLiked.getLikes().size() + 1;

        PostApi.likePost(globalRestApiUser, postToBeLiked);

        assertEquals(postToBeLiked.getLikes().size(), likedPostLikesToHave, "Post was not liked.");

        PostApi.deletePost(globalRestApiUser, postToBeLiked.getPostId());
        assertFalse(PostApi.publicPostExists(postToBeLiked.getPostId()), "Post was not deleted.");

    }

    @Test
    public void privatePostDeleted_By_Author() {

        boolean publicVisibility = false;

        PostModel postToBeDeleted = PostApi.createPost(globalRestApiUser, publicVisibility);
        int postId = postToBeDeleted.getPostId();
        assertTrue(PostApi.privatePostExists(globalRestApiUser, postId), "Post not created.");

        PostApi.deletePost(globalRestApiUser, postId);
        assertFalse(PostApi.privatePostExists(globalRestApiUser, postId), "Post was not deleted.");

    }

    @Test
    public void privatePostDeleted_By_AdminUser_When_NotAuthor() {

        boolean publicVisibility = false;
        PostModel postToBeDeleted = PostApi.createPost(globalRestApiUser, publicVisibility);
        int postId = postToBeDeleted.getPostId();
        assertTrue(PostApi.privatePostExists(globalRestApiUser, postId), "Post not created.");

        PostApi.deletePost(globalRestApiAdminUser, postId);
        assertFalse(PostApi.privatePostExists(globalRestApiUser, postId), "Post was not deleted.");

    }

    @Test
    public void publicPostDeleted_By_AdminUser_When_NotAuthor() {

        boolean publicVisibility = true;
        PostModel postToBeDeleted = PostApi.createPost(globalRestApiUser, publicVisibility);
        int postId = postToBeDeleted.getPostId();
        assertTrue(PostApi.publicPostExists(postId), "Post not created.");

        PostApi.deletePost(globalRestApiAdminUser, postId);
        assertFalse(PostApi.publicPostExists(postId), "Post was not deleted.");

    }

    @Test
    public void postCommentsListed_When_Requested_By_User() {

        List<Integer> commentIds = new ArrayList<>();

        UserModel newUser = new UserModel();
        UserApi.register(newUser, ROLE_USER.toString());

        boolean publicVisibility = true;

        PostModel post = PostApi.createPost(globalRestApiUser, publicVisibility);
        assertTrue(PostApi.publicPostExists(post.getPostId()), "Post not created.");

        int commentCount = 3;

        for (int i = 0; i < commentCount; i++) {
            CommentModel comment = CommentApi.createComment(newUser, post);
            assertTrue(CommentApi.commentExists(comment.getCommentId()), "Comment not created.");
            commentIds.add(comment.getCommentId());
        }

        CommentModel[] postComments = PostApi.findCommentsOfAPost(post);

        assertEquals(postComments.length, commentCount, "Wrong post comments count");

        for (CommentModel comment : postComments) {
            if (commentIds.contains(comment.getCommentId())) {
                assertNotNull(comment, "Comment is null");
                CommentApi.deleteComment(newUser, comment.getCommentId());
                assertFalse(CommentApi.commentExists(comment.getCommentId()), "Post was not deleted.");
            }
        }

        PostApi.deletePost(globalRestApiUser, post.getPostId());
        assertFalse(PostApi.publicPostExists(post.getPostId()), "Post was not deleted.");

        UserApi.disableUser(globalRestApiAdminUser, newUser);
    }

}
