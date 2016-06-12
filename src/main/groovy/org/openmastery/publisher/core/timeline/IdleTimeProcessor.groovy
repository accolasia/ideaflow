package org.openmastery.publisher.core.timeline

import org.openmastery.publisher.api.timeline.TimeBand
import org.openmastery.publisher.api.ideaflow.IdeaFlowBand
import org.openmastery.publisher.api.timeline.IdleTimeBand
import org.openmastery.publisher.api.timeline.TimeBandGroup
import org.openmastery.publisher.core.activity.IdleTimeBandEntity

class IdleTimeProcessor {

	private TimeBandIdleCalculator timeBandCalculator = new TimeBandIdleCalculator()

	public void collapseIdleTime(BandTimelineSegment timelineSegment, List<IdleTimeBandEntity> idleActivities) {
		for (IdleTimeBandEntity idle : idleActivities) {
			addIdleDuration(timelineSegment.ideaFlowBands, idle)

			for (TimeBandGroup group : timelineSegment.timeBandGroups) {
				addIdleDuration(group.linkedTimeBands, idle)
			}
		}
	}

	private void addIdleDuration(List<TimeBand> timeBands, IdleTimeBandEntity idleEntity) {
		for (TimeBand timeBand : timeBands) {
			if (timeBand instanceof IdeaFlowBand) {
				IdleTimeBand idle = toIdleTimeBand(idleEntity)
				IdleTimeBand splitIdle = timeBandCalculator.getIdleForTimeBandOrNull(timeBand, idle)
				if (splitIdle != null) {
					timeBand.addIdleBand(splitIdle)
					addIdleDuration(timeBand.nestedBands, idleEntity)
				}
			}
		}
	}

	private IdleTimeBand toIdleTimeBand(IdleTimeBandEntity entity) {
		// TODO: use dozer
		IdleTimeBand.builder()
				.id(entity.id)
				.taskId(entity.taskId)
				.start(entity.start)
				.end(entity.end)
				.comment(entity.comment)
				.auto(entity.auto)
				.build()
	}

}