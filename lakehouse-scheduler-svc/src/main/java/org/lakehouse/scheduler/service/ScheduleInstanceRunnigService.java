package org.lakehouse.scheduler.service;

import jakarta.transaction.Transactional;
import org.lakehouse.client.api.constant.Status;
import org.lakehouse.client.api.dto.configs.ScheduleDTO;
import org.lakehouse.client.api.exception.CronParceErrorException;
import org.lakehouse.client.api.utils.DateTimeUtils;
import org.lakehouse.client.rest.config.component.ConfigRestClientApi;
import org.lakehouse.scheduler.entities.ScheduleInstance;
import org.lakehouse.scheduler.entities.ScheduleInstanceRunning;
import org.lakehouse.scheduler.repository.ScheduleInstanceLastBuildRepository;
import org.lakehouse.scheduler.repository.ScheduleInstanceRepository;
import org.lakehouse.scheduler.repository.ScheduleInstanceRunningRepository;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class ScheduleInstanceRunnigService {

	private final ScheduleInstanceRunningRepository scheduleInstanceRunningRepository;
	private final ScheduleInstanceLastBuildRepository scheduleInstanceLastBuildRepository;
	private final ScheduleInstanceRepository scheduleInstanceRepository;
	private final ConfigRestClientApi configRestClientApi;
	public ScheduleInstanceRunnigService(ScheduleInstanceRunningRepository scheduleInstanceRunningRepository,
                                         ScheduleInstanceLastBuildRepository scheduleInstanceLastBuildRepository,
                                         ScheduleInstanceRepository scheduleInstanceRepository,
                                         ScheduleTaskInstanceService scheduleTaskInstanceService, ConfigRestClientApi configRestClientApi) {
		this.scheduleInstanceRunningRepository = scheduleInstanceRunningRepository;
		this.scheduleInstanceLastBuildRepository = scheduleInstanceLastBuildRepository;
		this.scheduleInstanceRepository = scheduleInstanceRepository;
        this.configRestClientApi = configRestClientApi;
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
		scheduleInstanceLastBuildRepository
				.findByScheduleEnabledNotRunning()
				.stream()
				.map(scheduleInstanceLast -> {
			ScheduleInstanceRunning result = new ScheduleInstanceRunning();
			result.setConfigScheduleKeyName(scheduleInstanceLast.getConfigScheduleKeyName());
			return result;
		}).forEach(scheduleInstanceRunningRepository::save);
	}

	private void runSchedule(ScheduleInstanceRunning scheduleInstanceRunning) {

		scheduleInstanceRunning.getScheduleInstance().setStatus(Status.Schedule.RUNNING.label);
		scheduleInstanceRepository.save(scheduleInstanceRunning.getScheduleInstance());
		scheduleInstanceRunningRepository.save(scheduleInstanceRunning);
	}

	private ScheduleInstanceRunning resolveScheduleInstance(ScheduleInstanceRunning scheduleInstanceRunning) {
		ScheduleInstanceRunning sir = new ScheduleInstanceRunning();
		sir.setId(scheduleInstanceRunning.getId());
		sir.setConfigScheduleKeyName(scheduleInstanceRunning.getConfigScheduleKeyName());
		sir.setScheduleInstance(scheduleInstanceRunning.getScheduleInstance());

		if (scheduleInstanceRunning.getScheduleInstance() == null) {
			List<ScheduleInstance> instanceList = scheduleInstanceRepository
					.findByScheduleNameNotSuccessOrderByTargetExecutionDateTimeAsc(
							scheduleInstanceRunning.getConfigScheduleKeyName(), Limit.of(1));
			if (!instanceList.isEmpty()) {
				ScheduleInstance si = instanceList.get(0);
				scheduleInstanceRunning.setScheduleInstance(si);
				sir.setScheduleInstance(si);
			}

		}
		if (scheduleInstanceRunning.getScheduleInstance() != null
			&&	sir.getScheduleInstance().getStatus().equals(Status.Schedule.SUCCESS.label)) {
			try {
				ScheduleDTO scheduleDTO = configRestClientApi.getScheduleDTO(sir.getConfigScheduleKeyName());
				OffsetDateTime next = DateTimeUtils.getNextTargetExecutionDateTime(
						scheduleDTO.getIntervalExpression(),
						sir.getScheduleInstance().getTargetExecutionDateTime());

				if (OffsetDateTime.now().isAfter(next)) {
					scheduleInstanceRepository
							.findByScheduleNameAndTargetDateTime(sir.getConfigScheduleKeyName(), next)
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
	@Transactional
	public void successSchedule(ScheduleInstanceRunning sir) {
		ScheduleInstance si = sir.getScheduleInstance();
		si.setStatus(Status.Schedule.SUCCESS.label);
		scheduleInstanceRepository.save(si);
		sir.setScheduleInstance(null);
		scheduleInstanceRunningRepository.save(sir);
	}
	
	public int sucsessSchedules() {
		List<ScheduleInstanceRunning> l = scheduleInstanceRunningRepository.findByScheduleReadyToSuccess();
		l.forEach(this::successSchedule);
		return l.size();
	}
}
