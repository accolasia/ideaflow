package org.ideaflow.publisher.core.ideaflow;

import org.ideaflow.publisher.core.TimeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class IdeaFlowStateMachineFactory {

	@Autowired
	private TimeService timeService;
	@Autowired
	private IdeaFlowPersistenceService ideaFlowPersistenceService;

	public IdeaFlowStateMachine createStateMachine(Long taskId) {
		return new IdeaFlowStateMachine(taskId, timeService, ideaFlowPersistenceService);
	}

}
