package com.fit2cloud.codedeploy2.client.model;

import java.io.Serializable;

public class TagValue implements Serializable {
    private String id;

    private String tagKey;

    private String tagValue;

    private String tagValueAlias;

    private Long createTime;


    private static final long serialVersionUID = 1L;


    public String getId() {
        return id;
    }


    public void setId(String id) {
        this.id = id == null ? null : id.trim();
    }


    public String getTagKey() {
        return tagKey;
    }


    public void setTagKey(String tagKey) {
        this.tagKey = tagKey == null ? null : tagKey.trim();
    }


    public String getTagValue() {
        return tagValue;
    }


    public void setTagValue(String tagValue) {
        this.tagValue = tagValue == null ? null : tagValue.trim();
    }


    public String getTagValueAlias() {
        return tagValueAlias;
    }


    public void setTagValueAlias(String tagValueAlias) {
        this.tagValueAlias = tagValueAlias == null ? null : tagValueAlias.trim();
    }


    public Long getCreateTime() {
        return createTime;
    }


    public void setCreateTime(Long createTime) {
        this.createTime = createTime;
    }
}