package lakehouse.api.service.tasks;

import lakehouse.api.repository.tasks.ScheduleInstanceLastRepository;
import lakehouse.api.repository.tasks.ScheduleTaskInstanceDependencyRepository;
import lakehouse.api.repository.tasks.ScheduleTaskInstanceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class ScheduleTaskInstanceService {
	private final Logger logger = LoggerFactory.getLogger(this.getClass());

	private final ScheduleTaskInstanceRepository scheduleTaskInstanceRepository;
	private final ScheduleTaskInstanceDependencyRepository scheduleTaskInstanceDependencyRepository;
	private final ScheduleInstanceLastRepository scheduleInstanceLastRepository;

	public ScheduleTaskInstanceService(ScheduleTaskInstanceRepository scheduleTaskInstanceRepository,
			ScheduleTaskInstanceDependencyRepository scheduleTaskInstanceDependencyRepository,
			ScheduleInstanceLastRepository scheduleInstanceLastRepository) {
		this.scheduleTaskInstanceRepository = scheduleTaskInstanceRepository;
		this.scheduleTaskInstanceDependencyRepository = scheduleTaskInstanceDependencyRepository;
		this.scheduleInstanceLastRepository = scheduleInstanceLastRepository;
	}
	/*
	 * private ScheduleTaskInstance mapToScheduleTaskInstance(TaskTemplate
	 * taskTemplate) { ScheduleTaskInstance result = new ScheduleTaskInstance();
	 * result.setTaskExecutionServiceGroup(taskTemplate.getTaskExecutionServiceGroup
	 * ()); result.setExecutionModule(taskTemplate.getExecutionModule());
	 * result.setName(taskTemplate.getName()); return result; }
	 * 
	 * public void entryTasksQueued(ScheduleInstance scheduleInstance){
	 * List<ScheduleTaskInstanceDependency> scheduleTaskInstanceDependencies =
	 * scheduleTaskInstanceDependencyRepository
	 * .findByScheduleInstanceId(scheduleInstance.getId());
	 * 
	 * Set<ScheduleTaskInstance> taskInstancesSet = new HashSet<>();
	 * scheduleTaskInstanceDependencies.forEach(scheduleTaskInstanceDependency -> {
	 * taskInstancesSet.add(scheduleTaskInstanceDependency.getScheduleTaskInstance()
	 * ); taskInstancesSet.add(scheduleTaskInstanceDependency.getDepends()); });
	 * 
	 * List<ScheduleTaskInstance> entryPoints = new ArrayList<>(taskInstancesSet);
	 * 
	 * entryPoints.removeAll( scheduleTaskInstanceDependencies .stream()
	 * .map(ScheduleTaskInstanceDependency::getScheduleTaskInstance) .toList());
	 * 
	 * entryPoints.forEach(scheduleTaskInstance -> {
	 * scheduleTaskInstance.setStatus(Status.Task.QUEUED.label);
	 * scheduleTaskInstanceRepository.save(scheduleTaskInstance); } );
	 * 
	 * }
	 * 
	 * private ScheduledTaskDTO mapScheduledTaskDTO(ScheduleTaskInstance
	 * scheduleTaskInstance){ ScheduledTaskDTO result = new ScheduledTaskDTO();
	 * result.setName(scheduleTaskInstance.getName());
	 * result.setImportance(scheduleTaskInstance.getName());
	 * result.setTaskExecutionServiceGroupName(scheduleTaskInstance.getName());
	 * result.setExecutionModule(scheduleTaskInstance.getName());
	 * result.setExecutionModuleArgs(null); //todo stub result.setDescription(
	 * String.format( "Schedule %s time %s task %s",
	 * scheduleTaskInstance.getScheduleInstance().getSchedule().getName(),
	 * scheduleTaskInstance.getName(),
	 * DateTimeUtils.formatDateTimeFormatWithTZ(scheduleTaskInstance.
	 * getScheduleInstance().getTargetExecutionDateTime())));
	 * 
	 * return result; }
	 */
	/*
	 * private List<ScheduleTaskInstance> getTaskInstances(ScheduleInstance
	 * scheduleInstance){
	 * 
	 * } public void runNewTasks() { scheduleInstanceLastRepository
	 * .findByScheduleEnabled() .stream() .filter(scheduleInstanceLast ->
	 * scheduleInstanceLast .getScheduleInstance() .getStatus()
	 * .equals(Status.Schedule.READY_ACTS.label)) .forEach(scheduleInstanceLast -> {
	 * try { SaveScenario( scheduleInstanceLast.getScheduleInstance(),
	 * getScenario(scheduleInstanceLast.getScheduleInstance())); } catch (Exception
	 * e) { logger.warn(e.getMessage()); throw new RuntimeException(e); } }); }
	 */
	/*
	 * public ScheduledTaskDTO takeTaskToExecute(TaskExecutionServiceGroup
	 * taskExecutionServiceGroup, String serviceId){
	 * scheduleTaskInstanceRepository.findQueuedBy(taskExecutionServiceGroup.getName
	 * ()); }
	 */

	/*
	 * private int getIndexByName(List<String> strings, String string){ for (int
	 * i=0; i < strings.size(); i++){ if (strings.get(i).equals(string)) return i; }
	 * return -1; } private void saveTasks(Schedule schedule, List<TaskTemplate>
	 * taskTemplates, List<DagTaskEdge> dagTaskEdges){ //Map<String,TaskTemplate>
	 * taskTemplateMap = new HashMap<>(); ///taskTemplates.forEach(taskTemplate ->
	 * taskTemplateMap.put(taskTemplate.getName(),taskTemplate)); Set<String>
	 * verticesHS = new HashSet<>(); taskTemplates.forEach(taskDTO ->
	 * verticesHS.add(taskDTO.getName()));
	 * 
	 * // diff between task list and "to" take entry vertices List<String>
	 * entryPoints = new ArrayList<>(verticesHS);
	 * entryPoints.removeAll(dagTaskEdges.stream().map(dagTaskEdge ->
	 * dagTaskEdge.getToTaskTemplate().getName()).toList()); //todo debug
	 * entryPoints.forEach(System.out::println);
	 * 
	 * // union entry vertices in zero start node List<String> vertices = new
	 * ArrayList<>(); vertices.add(0,"begin"); vertices.addAll(verticesHS); //todo
	 * debug vertices.forEach(System.out::println);
	 * 
	 * LinkedList<Integer>[] edges = new LinkedList[vertices.size()]; for (int i =
	 * 0; i < vertices.size(); ++i) edges[i] = new LinkedList<>();
	 * 
	 * dagTaskEdges.forEach(dagTaskEdge -> { edges.add() });
	 * 
	 * 
	 * }
	 */
}
