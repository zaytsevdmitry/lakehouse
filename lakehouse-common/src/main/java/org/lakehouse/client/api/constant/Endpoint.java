package org.lakehouse.client.api.constant;

public class Endpoint {
    public final static String ROOT_API_V1_0 = "/v1_0";
    public final static String CONFIGS = ROOT_API_V1_0 + "/configs";

    public final static String PROJECTS = CONFIGS + "/projects";
    public final static String PROJECTS_NAME = PROJECTS + "/{name}";

    public final static String TASK_EXECUTION_SERVICE_GROUPS =  CONFIGS + "/taskexecutionservicegroups";
    public final static String TASK_EXECUTION_SERVICE_GROUPS_NAME = TASK_EXECUTION_SERVICE_GROUPS + "/{name}";


    public final static String SCRIPTS = CONFIGS + "/scripts";
    public final static String SCRIPT_BY_KEY= CONFIGS + "/scripts/{key}";

    public final static String DATA_SETS = CONFIGS + "/datasets";
    public final static String DATA_SETS_NAME = CONFIGS +  "/datasets" + "/{name}";

    public final static String DATA_STORES = CONFIGS +  "/datastores";
    public final static String DATA_STORES_NAME = DATA_STORES + "/{name}";

    public final static String SCHEDULES = CONFIGS +  "/schedules";
    public final static String SCHEDULES_NAME = SCHEDULES + "/{name}";
    public final static String EFFECTIVE_SCHEDULES_ROOT = CONFIGS +  "/effective/schedules";
    public final static String EFFECTIVE_SCHEDULES_FROM_DT = EFFECTIVE_SCHEDULES_ROOT  +  "/fromdt/{dt}";
    public final static String EFFECTIVE_SCHEDULES_NAME = EFFECTIVE_SCHEDULES_ROOT + "/name/{name}";
    public final static String EFFECTIVE_SCHEDULE_SCENARIOACT_TASK = EFFECTIVE_SCHEDULES_ROOT
            + "/schedule/{schedule}/scenarioact/{scenarioact}/task/{task}";

    public final static String SCENARIOS = CONFIGS + "/scenarios";
    public final static String SCENARIOS_NAME = SCENARIOS + "/{name}";


    public final static String TASKS = ROOT_API_V1_0 + "/tasks";
    public final static String SCHEDULED_TASKS = TASKS + "/scheduledtasks";
    public final static String SCHEDULED_TASKS_LOCK_BY_ID = SCHEDULED_TASKS + "/lock/taskId/{id}/service/{serviceId}";
    public final static String SCHEDULED_TASKS_LOCK_ID = SCHEDULED_TASKS + "/lock/{id}";
    public final static String SCHEDULED_TASKS_LOCKS = SCHEDULED_TASKS + "/locks";
    public final static String SCHEDULED_TASKS_RELEASE = SCHEDULED_TASKS + "/release";
    public final static String SCHEDULED_TASKS_LOCK_HEARTBEAT = SCHEDULED_TASKS + "/lock/heartbeat";
    public final static String SCHEDULED_TASKS_ID = SCHEDULED_TASKS + "/{id}";
}