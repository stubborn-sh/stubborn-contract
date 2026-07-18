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

package sh.stubborn.contract.maven.verifier.stubrunner;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.maven.project.MavenProject;
import org.apache.maven.settings.Settings;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import sh.stubborn.contract.stubrunner.AetherStubDownloader;
import sh.stubborn.contract.stubrunner.StubDownloader;
import sh.stubborn.contract.stubrunner.StubDownloaderBuilder;
import sh.stubborn.contract.stubrunner.StubRunnerOptions;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Builds {@link StubDownloaderBuilder} for a Maven project.
 *
 * @author Mariusz Smykula
 * @author Eddú Meléndez
 */
@Component
public class AetherStubDownloaderFactory {

	private static final Log log = LogFactory.getLog(AetherStubDownloaderFactory.class);

	private final MavenProject project;

	private final RepositorySystem repoSystem;

	private final Settings settings;

	@Autowired
	public AetherStubDownloaderFactory(RepositorySystem repoSystem, MavenProject project, Settings settings) {
		this.repoSystem = repoSystem;
		this.project = project;
		this.settings = settings;
	}

	public StubDownloaderBuilder build(final RepositorySystemSession repoSession) {
		return new StubDownloaderBuilder() {
			@Override
			public StubDownloader build(StubRunnerOptions stubRunnerOptions) {
				log.info("Will download contracts using current build's Maven repository setup");
				return new AetherStubDownloader(AetherStubDownloaderFactory.this.repoSystem,
						AetherStubDownloaderFactory.this.project.getRemoteProjectRepositories(), repoSession,
						AetherStubDownloaderFactory.this.settings);
			}

		};
	}

}
