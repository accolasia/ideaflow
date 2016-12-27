package org.openmastery.publisher.api.ideaflow;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.openmastery.publisher.api.event.Event;
import org.openmastery.publisher.api.journey.TroubleshootingJourney;
import org.openmastery.publisher.api.metrics.TimelineMetrics;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubtaskTimelineOverview {

	private Event subtask;
	private IdeaFlowSubtaskTimeline subtaskTimeline;
	private TimelineMetrics subtaskMetrics;
	private List<TroubleshootingJourney> troubleshootingJourneys;

}