package org.lakehouse.cli.api.dto.tasks;

import java.util.Map;

public class ScheduledTaskDTO {
	private Long id;
    private String name;
    private String taskExecutionServiceGroupName;
    private String executionModule;
    private Map<String,String> executionModuleArgs;
    private String scenarioActName;
    private String scheduleName;
    private String scheduleTargetTimestamp;
    private String status;

    public ScheduledTaskDTO() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getTaskExecutionServiceGroupName() {
        return taskExecutionServiceGroupName;
    }

    public void setTaskExecutionServiceGroupName(String taskExecutionServiceGroupName) {
        this.taskExecutionServiceGroupName = taskExecutionServiceGroupName;
    }

    public String getExecutionModule() {
        return executionModule;
    }

    public void setExecutionModule(String executionModule) {
        this.executionModule = executionModule;
    }


    public Map<String, String> getExecutionModuleArgs() {
        return executionModuleArgs;
    }

    public void setExecutionModuleArgs(Map<String, String> executionModuleArgs) {
        this.executionModuleArgs = executionModuleArgs;
    }

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getScenarioActName() {
		return scenarioActName;
	}

	public void setScenarioActName(String scenarioActName) {
		this.scenarioActName = scenarioActName;
	}

	public String getScheduleName() {
		return scheduleName;
	}

	public void setScheduleName(String scheduleName) {
		this.scheduleName = scheduleName;
	}

	public String getScheduleTargetTimestamp() {
		return scheduleTargetTimestamp;
	}

	public void setScheduleTargetTimestamp(String scheduleTargetTimestamp) {
		this.scheduleTargetTimestamp = scheduleTargetTimestamp;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}
}
