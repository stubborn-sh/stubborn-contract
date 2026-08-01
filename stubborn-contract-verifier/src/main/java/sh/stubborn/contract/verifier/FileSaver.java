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

package sh.stubborn.contract.verifier;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sh.stubborn.contract.verifier.util.NamesUtil;

class FileSaver {

	private static final Logger log = LoggerFactory.getLogger(FileSaver.class);

	private final File targetDirectory;

	private final String fileExtension;

	FileSaver(File targetDirectory, String fileExtension) {
		this.targetDirectory = targetDirectory;
		this.fileExtension = fileExtension;
	}

	void saveClassFile(Path classPath, byte[] classBytes) {
		log.info("Creating new class file [{}]", classPath);
		try {
			Files.write(classPath, classBytes, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
		}
		catch (IOException ex) {
			throw new RuntimeException(ex);
		}
	}

	protected Path pathToClass(Path testBaseDir, String fileName) {
		return Paths.get(testBaseDir.toString(), NamesUtil.capitalize(fileName) + this.fileExtension).toAbsolutePath();
	}

	protected Path generateTestBaseDir(String basePackageClass, String includedDirectoryRelativePath) {
		Path testBaseDir = Paths.get(this.targetDirectory.getAbsolutePath(),
				NamesUtil.packageToDirectory(basePackageClass),
				NamesUtil.beforeLast(includedDirectoryRelativePath, File.separator));
		try {
			Files.createDirectories(testBaseDir);
		}
		catch (IOException ex) {
			throw new RuntimeException(ex);
		}
		return testBaseDir;
	}

}
