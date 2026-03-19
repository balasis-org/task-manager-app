package io.github.balasis.taskmanager.context.web.resource.taskanalysis.outbound;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.time.Instant;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class TaskAnalysisEstimateResource implements Serializable {

    private int commentCount;
    private long totalChars;
    private int analysisCredits;
    private int summaryCredits;
    private int egressCredits;
    private int fullCredits;
    private int budgetUsed;
    private int budgetMax;
    private int budgetRemaining;
    private Instant estimatedAt;
    private boolean stale;
}
