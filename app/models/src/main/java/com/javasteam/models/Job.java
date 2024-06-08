package com.javasteam.models;

import java.util.Optional;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

/**
 * Wrapper class to hold a message and additional information about the job. This wrapper class does
 * not enforce that the header type supports jobs.
 */
@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Job {
  private static final Long DEFAULT_JOB_ID = -1L;
  @Setter private Long sourceJobId;
  private Long targetJobId;
  private String jobName;
  private Integer realm;

  public Optional<String> getJobNameOptional() {
    return Optional.ofNullable(jobName);
  }

  public Optional<Integer> getRealmOptional() {
    return Optional.ofNullable(realm);
  }

  public static Job of(String jobName, Integer realm) {
    return new Job(DEFAULT_JOB_ID, DEFAULT_JOB_ID, jobName, realm);
  }
}
