package lakehouse.api.mapper;

import lakehouse.api.constant.Status;
import lakehouse.api.entities.configs.ScenarioAct;
import lakehouse.api.entities.configs.ScenarioActEdge;
import lakehouse.api.entities.configs.TaskTemplate;
import lakehouse.api.entities.tasks.*;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class Mapper {



    public ScheduleTaskInstance mapToScheduleTaskInstance(
            TaskTemplate taskTemplate,
            ScheduleScenarioActInstance scheduleScenarioActInstance) {
        ScheduleTaskInstance result = new ScheduleTaskInstance();
        result.setTaskExecutionServiceGroup(taskTemplate.getTaskExecutionServiceGroup());
        result.setExecutionModule(taskTemplate.getExecutionModule());
        result.setName(taskTemplate.getName());
        result.setScheduleScenarioActInstance(scheduleScenarioActInstance);
        return result;
    }

    public ScheduleScenarioActInstance mapToScheduleScenarioActInstance(
            ScenarioAct scenarioAct,
            ScheduleInstance scheduleInstance){
        ScheduleScenarioActInstance result = new ScheduleScenarioActInstance();
        result.setName(scenarioAct.getName());
        result.setScheduleInstance(scheduleInstance);
        result.setDataSet(scenarioAct.getDataSet());
        result.setStatus(Status.ScenarioAct.NEW.label);
        return result;
    }

    public ScheduleScenarioActInstanceDependency mapToScheduleScenarioActInstanceDependency(
            ScheduleScenarioActInstance from,
            ScheduleScenarioActInstance to
    ){
        ScheduleScenarioActInstanceDependency result = new ScheduleScenarioActInstanceDependency();
        result.setFrom(from);
        result.setTo(to);
        return result;
    }

    public ScheduleTaskInstanceDependency mapToScheduleTaskInstanceDependency(
            ScheduleTaskInstance from,
            ScheduleTaskInstance to
            ){
        ScheduleTaskInstanceDependency result = new ScheduleTaskInstanceDependency();
        result.setScheduleTaskInstance(to);
        result.setDepends(from);
        return result;

    }
    public ScheduleTaskInstanceDependency fromScenarioActEdge(ScenarioActEdge scenarioActEdge, Map<String, ScheduleTaskInstance> taskInstanceMap){
        ScheduleTaskInstanceDependency result = new ScheduleTaskInstanceDependency();
        result.setScheduleTaskInstance(taskInstanceMap.get(String.format("beginAct%s",scenarioActEdge.getToScheduleScenarioAct().getName())));
        result.setDepends(taskInstanceMap.get(String.format("endAct%s",scenarioActEdge.getFromScheduleScenarioAct().getName())));
        return result;
    }


}
