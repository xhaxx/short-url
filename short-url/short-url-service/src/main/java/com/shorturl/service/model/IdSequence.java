package com.shorturl.service.model;

import jakarta.persistence.*;

/**
 * 发号器序列表
 */
@Entity
@Table(name = "id_sequence")
public class IdSequence {

    @Id
    @Column(name = "biz_tag", nullable = false, length = 32)
    private String bizTag;

    @Column(name = "max_id", nullable = false)
    private Long maxId;

    @Column(nullable = false)
    private Integer step;

    // --- Getters / Setters ---

    public String getBizTag() { return bizTag; }
    public void setBizTag(String bizTag) { this.bizTag = bizTag; }

    public Long getMaxId() { return maxId; }
    public void setMaxId(Long maxId) { this.maxId = maxId; }

    public Integer getStep() { return step; }
    public void setStep(Integer step) { this.step = step; }
}
