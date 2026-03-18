package io.github.balasis.taskmanager.engine.core.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Set;

// full group snapshot returned when the user first opens a group. same
// minified property names as GroupRefreshDto but includes the full task
// preview set and budget counters for the tier UI. this is the "initial load"
// payload; subsequent updates come through GroupRefreshDto polls.
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@JsonPropertyOrder({"i", "n", "d", "diu", "iu", "oi", "on", "an", "aen", "aaen", "ca", "op", "db", "udb", "sb", "usb", "mcf", "maf", "mfsb", "mt", "mm", "ddce", "ue", "eq", "uac", "acq", "tp"})
public class GroupWithPreviewDto {
    @JsonProperty("i")   private Long id;
    @JsonProperty("n")   private String name;
    @JsonProperty("d")   private String description;
    @JsonProperty("diu") private String defaultImgUrl;
    @JsonProperty("iu")  private String imgUrl;
    @JsonProperty("oi")  private Long ownerId;
    @JsonProperty("on")  private String ownerName;
    @JsonProperty("an")  private String announcement;
    @JsonProperty("aen") private Boolean allowEmailNotification;
    @JsonProperty("ca")  private Instant createdAt;

    // owner plan & download budget so the frontend can display tier/budget per group
    @JsonProperty("op")  private String ownerPlan;
    @JsonProperty("db")  private Long downloadBudgetBytes;
    @JsonProperty("udb") private Long usedDownloadBytesMonth;

    // owner storage budget
    @JsonProperty("sb")  private Long storageBudgetBytes;
    @JsonProperty("usb") private Long usedStorageBytes;

    // file & task limits derived from the group owner's subscription plan
    @JsonProperty("mcf")  private Integer maxCreatorFiles;
    @JsonProperty("maf")  private Integer maxAssigneeFiles;
    @JsonProperty("mfsb") private Long maxFileSizeBytes;
    @JsonProperty("mt")   private Integer maxTasks;
    @JsonProperty("mm")   private Integer maxMembers;
    @JsonProperty("ddce") private Boolean dailyDownloadCapEnabled;
    @JsonProperty("aaen") private Boolean allowAssigneeEmailNotification;
    @JsonProperty("ds")   private Boolean downgradeShielded;
    @JsonProperty("ue")   private Integer usedEmailsMonth;
    @JsonProperty("eq")   private Integer emailQuotaPerMonth;

    // analysis credit budget (TEAMS_PRO)
    @JsonProperty("uac")  private Integer usedTaskAnalysisCreditsMonth;
    @JsonProperty("acq")  private Integer taskAnalysisCreditsPerMonth;

    @JsonProperty("tp")  private Set<TaskPreviewDto> taskPreviews;
}
