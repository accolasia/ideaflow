package org.ideaflow.publisher.core.timeline

import org.ideaflow.publisher.api.event.EventType
import org.ideaflow.publisher.api.timeline.Timeline
import org.ideaflow.publisher.api.timeline.TimelineSegment
import org.springframework.beans.factory.annotation.Autowired
import spock.lang.Specification

import java.time.Duration
import java.time.LocalDateTime

import static org.ideaflow.publisher.api.ideaflow.IdeaFlowStateType.CONFLICT
import static org.ideaflow.publisher.api.ideaflow.IdeaFlowStateType.LEARNING
import static org.ideaflow.publisher.api.ideaflow.IdeaFlowStateType.PROGRESS
import static org.ideaflow.publisher.api.ideaflow.IdeaFlowStateType.REWORK

class TimelineGeneratorSpec extends Specification {

	@Autowired
	TimelineGenerator generator
	TimelineSegmentValidator validator = new TimelineSegmentValidator()
	TimelineTestSupport testSupport = new TimelineTestSupport()
	LocalDateTime start

	def setup() {
		start = testSupport.now()
		testSupport.startTaskAndAdvanceHours(1)
	}

	private Timeline createTaskTimeline() {
		TimelineGenerator generator = new TimelineGenerator()
		generator.persistenceService = testSupport.persistenceService
		generator.createTaskTimeline(testSupport.taskId)
	}

	def "SHOULD end active band at last activity"() {
		given:
		testSupport.editor()

		when:
		Timeline timeline = createTaskTimeline()

		then:
		List<TimelineSegment> segments = timeline.timelineSegments
		validator.assertTimeBand(segments[0].ideaFlowBands, 0, PROGRESS, Duration.ofHours(1), 0)
		validator.assertValidationComplete(segments, 1)
	}

	def "SHOULD end subtask at last activity"() {
		given:
		testSupport.startSubtaskAndAdvanceHours(2)
		testSupport.editor()

		when:
		Timeline timeline = createTaskTimeline()

		then:
		List<TimelineSegment> segments = timeline.timelineSegments
		validator.assertTimeBand(segments[0].ideaFlowBands, 0, PROGRESS, Duration.ofHours(1), 0)
		validator.assertEvent(segments[1], 0, EventType.SUBTASK, start.plusHours(1))
		validator.assertTimeBand(segments[1].ideaFlowBands, 0, PROGRESS, Duration.ofHours(2), Duration.ofHours(1).seconds)
		validator.assertValidationComplete(segments, 2)
	}

	def "WHEN subtask splits between idle bands SHOULD split idle time between the time segments"() {
		given:
		testSupport.startBandAndAdvanceHours(LEARNING, 2)
		testSupport.idle(3)
		testSupport.startSubtaskAndAdvanceHours(1)
		testSupport.idle(2)
		testSupport.editor()

		when:
		Timeline timeline = createTaskTimeline()

		then:
		List<TimelineSegment> segments = timeline.timelineSegments
		validator.assertTimeBand(segments[0].ideaFlowBands, 0, PROGRESS, Duration.ofHours(1), 0)
		validator.assertTimeBand(segments[0].ideaFlowBands, 1, LEARNING, Duration.ofHours(2), Duration.ofHours(3), Duration.ofHours(1).seconds)
		validator.assertEvent(segments[1], 0, EventType.SUBTASK, start.plusHours(6))
		validator.assertTimeBand(segments[1].ideaFlowBands, 0, LEARNING, Duration.ofHours(1), Duration.ofHours(2), Duration.ofHours(3).seconds)
		validator.assertValidationComplete(segments, 2)
	}

	def "WHEN subtask splits linked bands SHOULD include idle"() {
		given:
		testSupport.startBandAndAdvanceHours(LEARNING, 2)
		testSupport.idle(1)
		testSupport.startSubtaskAndAdvanceHours(1)
		testSupport.startBandAndAdvanceHours(REWORK, 3)
		testSupport.idle(2)
		testSupport.editor()

		when:
		Timeline timeline = createTaskTimeline()

		then:
		List<TimelineSegment> segments = timeline.timelineSegments
		validator.assertTimeBand(segments[0].ideaFlowBands, 0, PROGRESS, Duration.ofHours(1), 0)
		validator.assertLinkedTimeBand(segments[0].timeBandGroups[0].linkedTimeBands, 0, LEARNING, Duration.ofHours(2), Duration.ofHours(1), Duration.ofHours(1).seconds)
		validator.assertEvent(segments[1], 0, EventType.SUBTASK, start.plusHours(4))
		validator.assertLinkedTimeBand(segments[1].timeBandGroups[0].linkedTimeBands, 0, LEARNING, Duration.ofHours(1), Duration.ofHours(3).seconds)
		validator.assertLinkedTimeBand(segments[1].timeBandGroups[0].linkedTimeBands, 1, REWORK, Duration.ofHours(3), Duration.ofHours(2), Duration.ofHours(4).seconds)
		validator.assertValidationComplete(segments, 2)
	}

	def "SHOULD handle nested bands within linked bands"() {
		given:
		testSupport.startBandAndAdvanceHours(CONFLICT, 1)
		testSupport.idle(1)
		testSupport.startBandAndAdvanceHours(LEARNING, 2)
		testSupport.idle(2)
		testSupport.advanceHours(1)
		testSupport.startBandAndAdvanceHours(CONFLICT, 1)
		testSupport.idle(1)
		testSupport.startSubtaskAndAdvanceHours(2)
		testSupport.editor()

		when:
		Timeline timeline = createTaskTimeline()

		then:
		List<TimelineSegment> segments = timeline.timelineSegments
		validator.assertTimeBand(segments[0].ideaFlowBands, 0, PROGRESS, Duration.ofHours(1), 0)
		validator.assertLinkedTimeBand(segments[0].timeBandGroups[0].linkedTimeBands, 0, CONFLICT, Duration.ofHours(1), Duration.ofHours(1), Duration.ofHours(1).seconds)
		validator.assertLinkedTimeBand(segments[0].timeBandGroups[0].linkedTimeBands, 1, LEARNING, Duration.ofHours(4), Duration.ofHours(3), Duration.ofHours(2).seconds)
		validator.assertNestedTimeBand(segments[0].timeBandGroups[0].linkedTimeBands[1].nestedBands, 0, CONFLICT, Duration.ofHours(1), Duration.ofHours(1), Duration.ofHours(5).seconds)
		validator.assertEvent(segments[1], 0, EventType.SUBTASK, start.plusHours(10))
		validator.assertLinkedTimeBand(segments[1].timeBandGroups[0].linkedTimeBands, 0, LEARNING, Duration.ofHours(2), Duration.ofHours(6).seconds)
		validator.assertNestedTimeBand(segments[1].timeBandGroups[0].linkedTimeBands[0].nestedBands, 0, CONFLICT, Duration.ofHours(2), Duration.ofHours(6).seconds)
		validator.assertValidationComplete(segments, 2)
	}

	def "SHOULD not complete state if duration would be zero"() {
		given:
		testSupport.startBand(CONFLICT)

		when:
		Timeline timeline = createTaskTimeline()

		then:
		List<TimelineSegment> segments = timeline.timelineSegments
		validator.assertTimeBand(segments[0].ideaFlowBands, 0, PROGRESS, Duration.ofHours(1), 0)
		validator.assertValidationComplete(segments, 1)
	}

}
