package org.jenkinsci.plugins.sonargerrit.review;

import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.Multimap;
import me.redaalaoui.gerrit_rest_java_client.thirdparty.com.google.gerrit.extensions.api.changes.ReviewInput;
import org.jenkinsci.plugins.sonargerrit.SonarToGerritPublisher;
import org.jenkinsci.plugins.sonargerrit.config.ReviewConfig;
import org.jenkinsci.plugins.sonargerrit.config.ScoreConfig;
import org.jenkinsci.plugins.sonargerrit.inspection.entity.Issue;
import org.jenkinsci.plugins.sonargerrit.inspection.entity.IssueAdapter;
import org.jenkinsci.plugins.sonargerrit.inspection.entity.Severity;
import org.jenkinsci.plugins.sonargerrit.test_infrastructure.jenkins.EnableJenkinsRule;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Project: Sonar-Gerrit Plugin Author: Tatiana Didik Created: 18.11.2017 12:41 $Id$ */
@EnableJenkinsRule
public class ReviewResultTest {
  protected Multimap<String, IssueAdapter> scoreIssues = LinkedListMultimap.create();
  protected Multimap<String, IssueAdapter> commentIssues = LinkedListMultimap.create();
  protected SonarToGerritPublisher publisher;

  @BeforeEach
  public final void initialize() {
    doInitialize();
  }

  protected void doInitialize() {
    publisher = buildPublisher();
  }

  @Test
  public void testNoScoreConfig() {
    publisher.setScoreConfig(null);
    ReviewInput reviewResult = getReviewResult();
    Assertions.assertNull(reviewResult.labels);
  }

  protected ReviewInput getReviewResult() {
    GerritReviewBuilder builder =
        new GerritReviewBuilder(
            commentIssues,
            scoreIssues,
            publisher.getReviewConfig(),
            publisher.getScoreConfig(),
            publisher.getNotificationConfig(),
            publisher.getInspectionConfig());
    return builder.buildReview();
  }

  protected ReviewConfig getReviewConfig() {
    return publisher.getReviewConfig();
  }

  public static class DummyIssue extends Issue implements IssueAdapter {
    @Override
    public Severity getSeverity() {
      return Severity.CRITICAL;
    }

    @Override
    public String getRule() {
      return "rule";
    }

    @Override
    public String getMessage() {
      return "message";
    }

    @Override
    public String getFilepath() {
      return getComponent();
    }

    @Override
    public void setFilepath(String path) {}
  }

  protected SonarToGerritPublisher buildPublisher() {
    SonarToGerritPublisher publisher = new SonarToGerritPublisher();
    publisher.setScoreConfig(new ScoreConfig());
    return publisher;
  }
}
