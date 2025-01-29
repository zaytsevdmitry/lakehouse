package org.lakehouse.scheduler.service;

import jakarta.transaction.Transactional;

import org.lakehouse.client.api.dto.configs.ScheduleEffectiveDTO;
import org.lakehouse.client.api.utils.DateTimeUtils;
import org.lakehouse.scheduler.entities.ScheduleInstance;
import org.lakehouse.scheduler.entities.ScheduleInstanceLastBuild;
import org.lakehouse.scheduler.repository.ScheduleInstanceLastBuildRepository;
import org.lakehouse.scheduler.repository.ScheduleInstanceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ScheduleInstanceLastBuildService {
	private final ScheduleInstanceLastBuildRepository scheduleInstanceLastBuildRepository;
	private final ScheduleInstanceRepository scheduleInstanceRepository;
	private final Logger logger = LoggerFactory.getLogger(this.getClass());
	public ScheduleInstanceLastBuildService(
			ScheduleInstanceLastBuildRepository scheduleInstanceLastBuildRepository,
			ScheduleInstanceRepository scheduleInstanceRepository) {
        this.scheduleInstanceLastBuildRepository = scheduleInstanceLastBuildRepository;
		this.scheduleInstanceRepository = scheduleInstanceRepository;
	}

	private Optional<ScheduleInstance> getLastScheduleInstance(String scheduleName) {
		List<ScheduleInstance> scheduleInstanceList = scheduleInstanceRepository
				.findByScheduleNameOrderByTargetExecutionDateTimeDesc(scheduleName, Limit.of(1));

		if (!scheduleInstanceList.isEmpty()) {
			return Optional.ofNullable(scheduleInstanceList.get(0));
		}
		return Optional.empty();
	}

	private ScheduleInstanceLastBuild mapDTOToScheduleInstanceLastBuild(
			ScheduleInstanceLastBuild instanceLastBuild,
			ScheduleEffectiveDTO scheduleEffectiveDTO){

		ScheduleInstanceLastBuild result = instanceLastBuild;

		result.setConfigScheduleKeyName(scheduleEffectiveDTO.getName());
		result.setEnabled(scheduleEffectiveDTO.isEnabled());
		result.setLastChangeNumber(scheduleEffectiveDTO.getLastChangeNumber());
		result.setLastChangedDateTime(DateTimeUtils.parceDateTimeFormatWithTZ(scheduleEffectiveDTO.getLastChangedDateTime()));
		// check if already scheduled but not present in Last
		getLastScheduleInstance(scheduleEffectiveDTO.getName()).ifPresent(result::setScheduleInstance);
		result.setLastUpdateDateTime(DateTimeUtils.now());

		return result;
	}

	@Transactional
	public void findAndRegisterNewSchedule(ScheduleEffectiveDTO scheduleDTO) {
		scheduleInstanceLastBuildRepository.findByConfigScheduleKeyName(scheduleDTO.getName())
				.ifPresentOrElse(
						scheduleInstanceLastBuild -> {
							if (scheduleInstanceLastBuild.getLastChangeNumber() < scheduleDTO.getLastChangeNumber())
								scheduleInstanceLastBuildRepository.save(
										mapDTOToScheduleInstanceLastBuild(scheduleInstanceLastBuild, scheduleDTO));
						},
						() -> scheduleInstanceLastBuildRepository.save(
								mapDTOToScheduleInstanceLastBuild(new ScheduleInstanceLastBuild(), scheduleDTO))
				);
	}

	@Transactional
	public void findAndRegisterNewSchedules(List<ScheduleEffectiveDTO> scheduleDTOs) {
		Map<String, ScheduleInstanceLastBuild> silMap =
				scheduleInstanceLastBuildRepository
					.findAll()
					.stream()
					.collect(Collectors.toMap(ScheduleInstanceLastBuild::getConfigScheduleKeyName, sil -> sil));

		List<ScheduleInstanceLastBuild> disabledByConfig = scheduleDTOs
				.stream()
				.filter(scheduleDTO -> silMap.containsKey(scheduleDTO.getName()))
				.filter(scheduleDTO -> silMap.get(scheduleDTO.getName()).isEnabled() != scheduleDTO.isEnabled())
				.map(scheduleDTO -> {
					ScheduleInstanceLastBuild result = silMap.get(scheduleDTO.getName());
					result.setEnabled(scheduleDTO.isEnabled());
					return result;
				})
				.toList();

		logger.info("Schedules disabled by config %d", disabledByConfig.size());

		List<ScheduleInstanceLastBuild> newInConfig = scheduleDTOs
				.stream()
				.filter(scheduleDTO -> !silMap.containsKey(scheduleDTO.getName()))
				.map(scheduleDTO -> {
						ScheduleInstanceLastBuild result = new ScheduleInstanceLastBuild();
					result.setConfigScheduleKeyName(scheduleDTO.getName());
						// check if already scheduled but not present in Last
					getLastScheduleInstance(scheduleDTO.getName()).ifPresent(result::setScheduleInstance);
					return result;})
				.toList();

		logger.info("Schedules new in config %d", newInConfig.size());

		Map<String,ScheduleEffectiveDTO> scheduleDTOMap = scheduleDTOs
				.stream()
				.collect(Collectors.toMap(ScheduleEffectiveDTO::getName,scheduleDTO -> scheduleDTO));

		List<ScheduleInstanceLastBuild> removedFromConfig = silMap.values()
				.stream()
				.filter(scheduleInstanceLastBuild ->
						!scheduleDTOMap.containsKey(scheduleInstanceLastBuild.getConfigScheduleKeyName()))
				.peek(scheduleInstanceLastBuild -> scheduleInstanceLastBuild.setEnabled(false))
				.toList();

		logger.info("Schedules remooved from config %d", removedFromConfig.size());

		List<ScheduleInstanceLastBuild> changes = new ArrayList<>();
		changes.addAll(disabledByConfig);
		changes.addAll(newInConfig);
		changes.addAll(removedFromConfig);

		logger.info("Schedules total changes %d", changes.size());

		changes.forEach(scheduleInstanceLastBuildRepository::save);

	}
}
