package com.smartresume.model;

import lombok.*;

/**
 * Embedded model representing one stage in a job's hiring pipeline.
 * Each stage has a custom email template the recruiter can generate or write.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HiringStage {

    /** Display name e.g. "Round 1: Aptitude Test", "Technical Interview" */
    private String stageName;

    /** Order in the pipeline (1-based) */
    private int stageOrder;

    /**
     * Stage type key used to match pipeline status.
     * Convention: STAGE_1, STAGE_2, STAGE_3 … or named like INTERVIEW, CODING_ROUND
     */
    private String stageKey;

    /** Email subject — supports {{candidateName}}, {{jobTitle}} placeholders */
    private String emailSubject;

    /**
     * Email body — supports {{candidateName}}, {{jobTitle}}, {{stageName}},
     * {{companyName}}, {{recruiterNotes}}
     */
    private String emailBody;

    /** Whether this stage has a custom template (false = use system default) */
    private boolean customTemplate;
}
