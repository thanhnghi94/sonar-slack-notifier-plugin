package com.koant.sonar.slacknotifier.extension.task;

import com.github.seratch.jslack.api.model.Attachment;
import com.github.seratch.jslack.api.model.Field;
import com.github.seratch.jslack.api.webhook.Payload;
import com.koant.sonar.slacknotifier.common.component.ProjectConfig;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.sonar.api.utils.System2;
import org.sonar.core.i18n.DefaultI18n;
import org.sonar.core.platform.PluginRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Created by ak on 18/10/16.
 * Modified by poznachowski
 */
public class ProjectAnalysisPayloadBuilderTest {
    private static final boolean QG_FAIL_ONLY = true;
    CaptorPostProjectAnalysisTask postProjectAnalysisTask;
    DefaultI18n i18n;

    @Before
    public void before() {
        postProjectAnalysisTask = new CaptorPostProjectAnalysisTask();

        // org/sonar/l10n/core.properties
        PluginRepository pluginRepository = Mockito.mock(PluginRepository.class);
        System2 system2 = Mockito.mock(System2.class);
        i18n = new DefaultI18n(pluginRepository, system2);
        i18n.start();
    }

    @Test
    public void testI18nBundle() {
        assertThat(i18n.message(Locale.ENGLISH, "metric.new_sqale_debt_ratio.short_name", null)).isEqualTo("Debt Ratio on new code");
    }

    @Test
    public void execute_is_passed_a_non_null_ProjectAnalysis_object() {
        Analyses.simple(postProjectAnalysisTask);
        assertThat(postProjectAnalysisTask.getProjectAnalysis()).isNotNull();
    }

//    @Test
    public void testPayloadBuilder() {
        Analyses.qualityGateOk4Conditions(postProjectAnalysisTask);
        ProjectConfig projectConfig = new ProjectConfig("key", "#channel", false);
        Payload payload = ProjectAnalysisPayloadBuilder.of(postProjectAnalysisTask.getProjectAnalysis())
                .projectConfig(projectConfig)
                .i18n(i18n)
                .projectUrl("http://localhist:9000/dashboard?id=project:key")
                .username("CKSSlackNotifier")
                .build();
        assertThat(payload).isEqualTo(expected());
    }

    private Payload expected() {
        List<Attachment> attachments = new ArrayList<>();

        attachments.add(Attachment.builder()
            .title("Security Rating on New Code: 1 :sonarqube_ok:")
            .color("good")
            .build());
        attachments.add(Attachment.builder()
            .title("Reliability Rating on New Code: 1 :sonarqube_ok:")
            .color("good")
            .build());
        attachments.add(Attachment.builder()
            .title("New Bugs: 0 :sonarqube_ok:")
            .color("good")
            .build());
        attachments.add(Attachment.builder()
            .title("New Code Smells: 4 :sonarqube_ok:")
            .color("good")
            .build());
        attachments.add(Attachment.builder()
            .title("Security Rating on New Code: 0 :sonarqube_ok:")
            .color("good")
            .build());
        attachments.add(Attachment.builder()
            .title("Technical Debt Ratop: 0.2 :sonarqube_ok:")
            .color("good")
            .build());


        return Payload.builder()
                .text("Project [Project Name] analyzed. See http://localhist:9000/dashboard?id=project:key. Quality gate status: :sonarqube_ok:")
                .channel("#channel")
                .username("CKSSlackNotifier")
                .attachments(attachments)
                .build();
    }

//    @Test
    public void shouldShowOnlyExceededConditionsIfProjectConfigReportOnlyOnFailedQualityGateWay() throws Exception {
        Analyses.qualityGateError2Of3ConditionsFailed(postProjectAnalysisTask);
        ProjectConfig projectConfig = new ProjectConfig("key", "#channel", QG_FAIL_ONLY);
        Payload payload = ProjectAnalysisPayloadBuilder.of(postProjectAnalysisTask.getProjectAnalysis())
                .projectConfig(projectConfig)
                .i18n(i18n)
                .projectUrl("http://localhist:9000/dashboard?id=project:key")
                .username("CKSSlackNotifier")
                .build();

        assertThat(payload.getAttachments())
                .hasSize(1)
                .flatExtracting(Attachment::getFields)
                .hasSize(2)
                .extracting(Field::getTitle)
                .contains("Functions: WARN", "Issues: ERROR");
    }

    @Test
    public void buildPayloadWithoutQualityGateWay() throws Exception {
        Analyses.noQualityGate(postProjectAnalysisTask);
        ProjectConfig projectConfig = new ProjectConfig("key", "#channel", false);
        Payload payload = ProjectAnalysisPayloadBuilder.of(postProjectAnalysisTask.getProjectAnalysis())
                .projectConfig(projectConfig)
                .i18n(i18n)
                .projectUrl("http://localhist:9000/dashboard?id=project:key")
                .username("CKSSlackNotifier")
                .build();

        assertThat(payload.getAttachments()).isNull();
        assertThat(payload.getText()).doesNotContain("Quality Gate status");
    }
}
