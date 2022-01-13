package org.jenkinsci.plugins.sonargerrit.gerrit;

import com.google.common.base.Function;
import com.google.common.collect.Collections2;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import me.redaalaoui.gerrit_rest_java_client.thirdparty.com.google.gerrit.extensions.api.changes.NotifyHandling;
import me.redaalaoui.gerrit_rest_java_client.thirdparty.com.google.gerrit.extensions.api.changes.ReviewInput;
import org.jenkinsci.plugins.sonargerrit.sonar.Issue;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;

/**
 * Project: Sonar-Gerrit Plugin Author: Tatiana Didik Created: 28.11.2017 14:03
 *
 * <p>$Id$
 */
@Restricted(NoExternalUse.class)
public class GerritReviewBuilder {
  private final Multimap<String, Issue> finalIssuesToComment;
  private final Multimap<String, Issue> finalIssuesToScore;
  private final ReviewConfig reviewConfig;
  private final ScoreConfig scoreConfig;
  private final NotificationConfig notificationConfig;

  public GerritReviewBuilder(
      Multimap<String, Issue> finalIssuesToComment,
      Multimap<String, Issue> finalIssuesToScore,
      ReviewConfig reviewConfig,
      ScoreConfig scoreConfig,
      NotificationConfig notificationConfig) {
    this.finalIssuesToComment = finalIssuesToComment;
    this.finalIssuesToScore = finalIssuesToScore;
    this.reviewConfig = reviewConfig;
    this.scoreConfig = scoreConfig;
    this.notificationConfig = notificationConfig;
  }

  public ReviewInput buildReview() {
    // review
    String reviewMessage = getReviewMessage(finalIssuesToComment);
    ReviewInput reviewInput = new ReviewInput().message(reviewMessage);
    reviewInput.comments = generateComments();

    // score
    int score = 0;
    if (postScore()) {
      score = getReviewMark(finalIssuesToScore.size());
      String category = scoreConfig.getCategory();

      reviewInput.label(category, score);
    }

    // notification
    reviewInput.notify = getNotificationSettings(finalIssuesToComment.size(), score);
    return reviewInput;
  }

  private boolean postScore() {
    return scoreConfig != null;
  }

  private String getReviewMessage(Multimap<String, Issue> finalIssues) {
    return new CustomReportFormatter(
            finalIssues.values(),
            reviewConfig.getSomeIssuesTitleTemplate(),
            reviewConfig.getNoIssuesTitleTemplate())
        .getMessage();
  }

  private int getReviewMark(int finalIssuesCount) {
    return finalIssuesCount > 0 ? scoreConfig.getIssuesScore() : scoreConfig.getNoIssuesScore();
  }

  private NotifyHandling getNotificationSettings(int commentsCount, int score) {
    if (score < 0) {
      return NotifyHandling.valueOf(notificationConfig.getNegativeScoreNotificationRecipient());
    } else if (commentsCount > 0) {
      return NotifyHandling.valueOf(notificationConfig.getCommentedIssuesNotificationRecipient());
    } else {
      return NotifyHandling.valueOf(notificationConfig.getNoIssuesNotificationRecipient());
    }
  }

  private Map<String, List<ReviewInput.CommentInput>> generateComments() {
    Map<String, List<ReviewInput.CommentInput>> file2comments = new HashMap<>();
    for (String file : finalIssuesToComment.keySet()) {
      Collection<Issue> issues = finalIssuesToComment.get(file);
      Collection<ReviewInput.CommentInput> comments =
          Collections2.transform(issues, new IssueToCommentTransformation());
      ArrayList<ReviewInput.CommentInput> commentList = Lists.newArrayList(comments);
      file2comments.put(file, commentList);
    }
    return file2comments;
  }

  private ReviewInput.CommentInput createComment(@Nullable Issue input) {
    if (input == null) {
      return null;
    }

    String commentTemplate = reviewConfig.getIssueCommentTemplate();
    String message = new CustomIssueFormatter(input, commentTemplate).getMessage();

    ReviewInput.CommentInput commentInput = new ReviewInput.CommentInput();
    commentInput.id = input.getKey();
    commentInput.line = input.getLine();
    commentInput.message = message;
    commentInput.unresolved = true;
    return commentInput;
  }

  private class IssueToCommentTransformation implements Function<Issue, ReviewInput.CommentInput> {
    @Nullable
    @Override
    public ReviewInput.CommentInput apply(@Nullable Issue input) {
      return createComment(input);
    }
  }
}