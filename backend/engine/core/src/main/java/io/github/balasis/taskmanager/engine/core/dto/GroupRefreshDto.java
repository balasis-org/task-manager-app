package io.github.balasis.taskmanager.engine.core.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Set;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@JsonPropertyOrder({"sn", "c", "mc", "n", "d", "an", "diu", "iu", "aen", "aaen", "ds", "lged", "op", "db", "udb", "sb", "usb", "mcf", "maf", "mfsb", "mt", "mm", "ddce", "ct", "dti"})
public class GroupRefreshDto {

    @JsonProperty("sn")   private Instant serverNow;

    @JsonProperty("c")    private boolean changed;

    @JsonProperty("mc")   private boolean membersChanged;

    @JsonProperty("n")    private String name;
    @JsonProperty("d")    private String description;
    @JsonProperty("an")   private String announcement;
    @JsonProperty("diu")  private String defaultImgUrl;
    @JsonProperty("iu")   private String imgUrl;
    @JsonProperty("aen")  private Boolean allowEmailNotification;
    @JsonProperty("lged") private Instant lastGroupEventDate;

    // owner plan & download budget
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

    @JsonProperty("ct")   private Set<TaskPreviewDto> changedTasks;

    @JsonProperty("dti")  private Set<Long> deletedTaskIds;
}
