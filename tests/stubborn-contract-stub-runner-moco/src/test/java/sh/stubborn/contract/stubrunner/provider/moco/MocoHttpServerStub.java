/*
 * Copyright 2013-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package sh.stubborn.contract.stubrunner.provider.moco;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.github.dreamhead.moco.bootstrap.arg.HttpArgs;
import com.github.dreamhead.moco.runner.JsonRunner;
import com.github.dreamhead.moco.runner.RunnerSetting;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sh.stubborn.contract.stubrunner.HttpServerStub;
import sh.stubborn.contract.stubrunner.HttpServerStubConfiguration;

public class MocoHttpServerStub implements HttpServerStub {

	private static final Logger log = LoggerFactory.getLogger(MocoHttpServerStub.class);

	private boolean started;

	private @Nullable JsonRunner runner;

	private int port;

	@Override
	public int port() {
		if (!isRunning()) {
			return -1;
		}
		return this.port;
	}

	@Override
	public boolean isRunning() {
		return this.started;
	}

	@Override
	public HttpServerStub start(HttpServerStubConfiguration configuration) {
		Integer configuredPort = configuration.port;
		this.port = (configuredPort != null) ? configuredPort : 0;
		return this;
	}

	@Override
	public HttpServerStub stop() {
		if (this.runner != null) {
			this.runner.stop();
		}
		return this;
	}

	@Override
	public HttpServerStub registerMappings(Collection<File> stubFiles) {
		List<RunnerSetting> settings = new ArrayList<>();
		for (File file : stubFiles) {
			if (!file.getName().endsWith("json")) {
				continue;
			}
			log.info("Trying to parse [{}]", file.getName());
			try {
				byte[] bytes = Files.readAllBytes(file.toPath());
				settings.add(RunnerSetting.aRunnerSetting().addStream(new ByteArrayInputStream(bytes)).build());
			}
			catch (Exception ex) {
				log.warn("Exception occurred while trying to parse file [" + file.getName() + "]", ex);
			}
		}
		this.runner = JsonRunner.newJsonRunnerWithSetting(settings, HttpArgs.httpArgs().withPort(this.port).build());
		this.runner.run();
		this.started = true;
		return this;
	}

	@Override
	public String registeredMappings() {
		return "";
	}

	@Override
	public boolean isAccepted(File file) {
		return file.getName().endsWith(".json");
	}

}
