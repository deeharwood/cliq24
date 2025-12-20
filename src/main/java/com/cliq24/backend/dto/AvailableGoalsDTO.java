package com.cliq24.backend.dto;

import java.util.List;
import java.util.Map;

public class AvailableGoalsDTO {
    private List<GoalOption> goals;

    public AvailableGoalsDTO(List<GoalOption> goals) {
        this.goals = goals;
    }

    public List<GoalOption> getGoals() {
        return goals;
    }

    public void setGoals(List<GoalOption> goals) {
        this.goals = goals;
    }

    public static class GoalOption {
        private String id;
        private String label;
        private String description;
        private String icon;

        public GoalOption(String id, String label, String description, String icon) {
            this.id = id;
            this.label = label;
            this.description = description;
            this.icon = icon;
        }

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getLabel() {
            return label;
        }

        public void setLabel(String label) {
            this.label = label;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public String getIcon() {
            return icon;
        }

        public void setIcon(String icon) {
            this.icon = icon;
        }
    }
}
