/**
 * Copyright 2015 New Iron Group, Inc.
 * <p>
 * Licensed under the GNU GENERAL PUBLIC LICENSE, Version 3 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.gnu.org/licenses/gpl-3.0.en.html
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openmastery.publisher;

import groovyx.net.http.RESTClient;
import org.openmastery.time.MockTimeService;
import org.openmastery.publisher.client.ActivityClient;
import org.openmastery.publisher.client.EventClient;
import org.openmastery.publisher.client.IdeaFlowClient;
import org.openmastery.publisher.client.TaskClient;
import org.openmastery.publisher.client.TimelineClient;
import org.openmastery.time.TimeService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.net.URISyntaxException;

@Configuration
public class IfmPublisherTestConfig {

	@Value("${test-server.base_url:http://localhost}")
	private String serverBaseUrl;
	@Value("${test-server.base_url:http://localhost}:${server.port}")
	private String hostUri;

	@Bean
	public IdeaFlowClient ideaFlowClient() {
		return new IdeaFlowClient(hostUri);
	}

	@Bean
	public EventClient eventClient() {
		return new EventClient(hostUri);
	}

	@Bean
	public ActivityClient activityClient() {
		return new ActivityClient(hostUri);
	}

	@Bean
	public TaskClient taskClient() {
		return new TaskClient(hostUri);
	}

	@Bean
	public TimelineClient timelineClient() {
		return new TimelineClient(hostUri);
	}

	@Bean
	@Primary
	public TimeService timeService() {
		return new MockTimeService();
	}

	@Bean
	@Primary
	public RESTClient restClient() throws URISyntaxException {
		RESTClient client = new RESTClient(hostUri);
		return client;
	}

	@Bean
	public RESTClient managementRestClient(@Value("${management.port}") String managementPort) throws URISyntaxException {
		RESTClient client = new RESTClient(serverBaseUrl + ":" + managementPort);
		return client;
	}

}