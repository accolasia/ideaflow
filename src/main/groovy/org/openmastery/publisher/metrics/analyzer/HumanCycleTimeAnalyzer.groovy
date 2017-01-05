/*
 * Copyright 2017 New Iron Group, Inc.
 *
 * Licensed under the GNU GENERAL PUBLIC LICENSE, Version 3 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * 	http://www.gnu.org/licenses/gpl-3.0.en.html
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openmastery.publisher.metrics.analyzer

import org.openmastery.publisher.api.ideaflow.IdeaFlowTimeline
import org.openmastery.publisher.api.journey.DiscoveryCycle
import org.openmastery.publisher.api.journey.ExperimentCycle
import org.openmastery.publisher.api.journey.TroubleshootingJourney
import org.openmastery.publisher.api.metrics.DurationInSeconds
import org.openmastery.publisher.api.metrics.GraphPoint
import org.openmastery.publisher.api.metrics.MetricType
import org.openmastery.storyweb.api.MetricThreshold

class HumanCycleTimeAnalyzer extends AbstractTimelineAnalyzer<DurationInSeconds> {

	HumanCycleTimeAnalyzer() {
		super(MetricType.AVG_HUMAN_CYCLE_RATIOS)
	}

	@Override
	GraphPoint<DurationInSeconds> analyzeTimelineAndJourneys(IdeaFlowTimeline timeline, List<TroubleshootingJourney> journeys) {

		List<GraphPoint<DurationInSeconds>> allPoints = journeys.collect { TroubleshootingJourney journey ->
			GraphPoint<DurationInSeconds> journeyPoint = createPointFromMeasurableContext("/journey", journey)
			journeyPoint.childPoints = generatePointsForDiscoveryCycles(journey.discoveryCycles)
			journeyPoint.frequency = getSumOfFrequency(journeyPoint.childPoints)
			journeyPoint.value = getWeightedAverage(journeyPoint.childPoints)
			journeyPoint.danger = isOverThreshold(journeyPoint.value)
			return journeyPoint
		}

		GraphPoint<DurationInSeconds> timelinePoint = createTimelinePoint(timeline, journeys)
		timelinePoint.value = getWeightedAverage(allPoints)
		timelinePoint.danger = isOverThreshold(timelinePoint.value)
		timelinePoint.childPoints = allPoints

		return timelinePoint
	}

	List<GraphPoint<DurationInSeconds>> generatePointsForDiscoveryCycles(List<DiscoveryCycle> discoveryCycles) {
		discoveryCycles.collect { DiscoveryCycle discoveryCycle ->
			GraphPoint<DurationInSeconds> discoveryPoint = createPointFromMeasurableContext("/discovery", discoveryCycle)
			discoveryPoint.value = calculateWeightedAverage(discoveryCycle)
			discoveryPoint.danger = isOverThreshold(discoveryPoint.value)
			return discoveryPoint
		}
	}

	DurationInSeconds calculateWeightedAverage(DiscoveryCycle discoveryCycle) {
		Long sum = 0
		int frequency = 0
		discoveryCycle.experimentCycles.each { ExperimentCycle experimentCycle ->
			println "Experiment Cycle ["+experimentCycle.relativePositionInSeconds + "] : "+experimentCycle.durationInSeconds
			sum += experimentCycle.durationInSeconds
			frequency++
		}
		println "Calculated average for discovery cycle = "+ sum + "/" +frequency
		return new DurationInSeconds((long)sum / frequency)
	}

	@Override
	MetricThreshold<DurationInSeconds> getDangerThreshold() {
		return createMetricThreshold(new DurationInSeconds(10 * 60))
	}

}
