package com.mongodb.migratecluster.commandline;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Created by shyamarjarapu on 4/13/17.
 */
public class ApplicationOptions {
    private String sourceCluster;
    private String targetCluster;
    private String oplogStore;
    private String configFilePath;
    private boolean showHelp;
    private boolean dropTarget;

    public ApplicationOptions() {
        sourceCluster = "";
        targetCluster = "";
        oplogStore = "";
        configFilePath = "";
        showHelp = false;
        dropTarget = false;
    }


    @JsonProperty("sourceCluster")
    public String getSourceCluster() {
        return sourceCluster;
    }

    public void setSourceCluster(String sourceCluster) {
        this.sourceCluster = sourceCluster;
    }

    @JsonProperty("targetCluster")
    public String getTargetCluster() {
        return targetCluster;
    }

    public void setTargetCluster(String targetCluster) {
        this.targetCluster = targetCluster;
    }

    @JsonProperty("oplogStore")
    public String getOplogStore() {
        return oplogStore;
    }

    public void setOplogStore(String oplogStore) {
        this.oplogStore = oplogStore;
    }

    @JsonProperty("configFilePath")
    public String getConfigFilePath() {
        return configFilePath;
    }

    public void setConfigFilePath(String configFilePath) {
        this.configFilePath = configFilePath;
    }

    public boolean isShowHelp() {
        return showHelp;
    }

    public void setShowHelp(boolean showHelp) {
        this.showHelp = showHelp;
    }

    @JsonProperty("dropTarget")
    public boolean isDropTarget() {
        return dropTarget;
    }

    public void setDropTarget(boolean dropTarget) {
        this.dropTarget = dropTarget;
    }

    @Override
    public String toString() {
        return String.format("{ showHelp : %s, configFilePath: \"%s\", " +
                " sourceCluster: \"%s\", targetCluster: \"%s\", oplog: \"%s\", drop: %s }",
                this.isShowHelp(), this.getConfigFilePath(), this.getSourceCluster(),
                this.getTargetCluster(), this.getOplogStore(), this.isDropTarget());
    }
}
