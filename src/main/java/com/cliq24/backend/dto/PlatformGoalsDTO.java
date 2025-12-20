package com.cliq24.backend.dto;

import java.util.List;
import java.util.Map;

public class PlatformGoalsDTO {
    private Map<String, List<String>> platformGoals;

    public PlatformGoalsDTO() {
    }

    public PlatformGoalsDTO(Map<String, List<String>> platformGoals) {
        this.platformGoals = platformGoals;
    }

    public Map<String, List<String>> getPlatformGoals() {
        return platformGoals;
    }

    public void setPlatformGoals(Map<String, List<String>> platformGoals) {
        this.platformGoals = platformGoals;
    }
}
