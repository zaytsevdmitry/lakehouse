package lakehouse.api.service.tasks;

import jakarta.transaction.Transactional;
import lakehouse.api.constant.Status;
import lakehouse.api.entities.tasks.ScheduleInstance;
import lakehouse.api.entities.tasks.ScheduleInstanceRunning;
import lakehouse.api.exception.CronParceErrorException;
import lakehouse.api.repository.tasks.ScheduleInstanceLastRepository;
import lakehouse.api.repository.tasks.ScheduleInstanceRepository;
import lakehouse.api.repository.tasks.ScheduleInstanceRunningRepository;
import lakehouse.api.utils.DateTimeUtils;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class ScheduleInstanceRunnigService {

	private final ScheduleInstanceRunningRepository scheduleInstanceRunningRepository;
	private final ScheduleInstanceLastRepository scheduleInstanceLastRepository;
	private final ScheduleInstanceRepository scheduleInstanceRepository;
	private final ScheduleTaskInstanceService scheduleTaskInstanceService;

	public ScheduleInstanceRunnigService(ScheduleInstanceRunningRepository scheduleInstanceRunningRepository,
			ScheduleInstanceLastRepository scheduleInstanceLastRepository,
			ScheduleInstanceRepository scheduleInstanceRepository,
			ScheduleTaskInstanceService scheduleTaskInstanceService) {
		this.scheduleInstanceRunningRepository = scheduleInstanceRunningRepository;
		this.scheduleInstanceLastRepository = scheduleInstanceLastRepository;
		this.scheduleInstanceRepository = scheduleInstanceRepository;
		this.scheduleTaskInstanceService = scheduleTaskInstanceService;
	}

	private Optional<ScheduleInstance> getLastScheduleInstance(String scheduleName) {
		List<ScheduleInstance> scheduleInstanceList = scheduleInstanceRepository
				.findByScheduleNameOrderByTargetExecutionDateTimeDesc(scheduleName, Limit.of(1));

		if (!scheduleInstanceList.isEmpty()) {
			return Optional.ofNullable(scheduleInstanceList.get(0));
		}
		return Optional.empty();
	}

	@Transactional
	public void findAndRegisterNewSchedules() {
		scheduleInstanceLastRepository.findByScheduleEnabledNotRunning().stream().map(scheduleInstanceLast -> {
			ScheduleInstanceRunning result = new ScheduleInstanceRunning();
			result.setSchedule(scheduleInstanceLast.getSchedule());
			return result;
		}).forEach(scheduleInstanceRunningRepository::save);
	}

	private void runSchedule(ScheduleInstanceRunning scheduleInstanceRunning) {

		scheduleInstanceRunning.getScheduleInstance().setStatus(Status.Schedule.RUNNING.label);
		scheduleInstanceRepository.save(scheduleInstanceRunning.getScheduleInstance());
		scheduleInstanceRunningRepository.save(scheduleInstanceRunning);
		// scheduleTaskInstanceService.entryTasksQueued(scheduleInstanceRunning.getScheduleInstance());
	}

	private ScheduleInstanceRunning resolveScheduleInstance(ScheduleInstanceRunning scheduleInstanceRunning) {
		ScheduleInstanceRunning sir = new ScheduleInstanceRunning();
		sir.setId(scheduleInstanceRunning.getId());
		sir.setSchedule(scheduleInstanceRunning.getSchedule());
		sir.setScheduleInstance(scheduleInstanceRunning.getScheduleInstance());

		if (scheduleInstanceRunning.getScheduleInstance() == null) {
			List<ScheduleInstance> instanceList = scheduleInstanceRepository
					.findByScheduleNameNotSuccessOrderByTargetExecutionDateTimeAsc(
							scheduleInstanceRunning.getSchedule().getName(), Limit.of(1));
			if (!instanceList.isEmpty()) {
				ScheduleInstance si = instanceList.get(0);
				scheduleInstanceRunning.setScheduleInstance(si);
				sir.setScheduleInstance(si);
			}

		}
		if (sir.getScheduleInstance().getStatus().equals(Status.Schedule.SUCCESS.label)) {
			try {

				OffsetDateTime next = DateTimeUtils.getNextTargetExecutionDateTime(
						sir.getSchedule().getIntervalExpression(),
						sir.getScheduleInstance().getTargetExecutionDateTime());

				if (OffsetDateTime.now().isAfter(next)) {
					scheduleInstanceRepository.findByScheduleNameAndTargetDateTime(sir.getSchedule().getName(), next)
							.ifPresent(sir::setScheduleInstance);

				}

			} catch (CronParceErrorException e) {
				throw new RuntimeException(e);
			}
		}
		return sir;
	}

	@Transactional
	public void runSchedules() {
		scheduleInstanceRunningRepository.findByScheduleEnabled().forEach(scheduleInstanceRunning -> {
			ScheduleInstanceRunning sir = resolveScheduleInstance(scheduleInstanceRunning);

			if (!scheduleInstanceRunning.equals(sir))
				scheduleInstanceRunningRepository.save(sir);

			if (scheduleInstanceRunning.getScheduleInstance().getStatus().equals(Status.Schedule.NEW.label)) {
				runSchedule(scheduleInstanceRunning);
			}
		});
	}
}
