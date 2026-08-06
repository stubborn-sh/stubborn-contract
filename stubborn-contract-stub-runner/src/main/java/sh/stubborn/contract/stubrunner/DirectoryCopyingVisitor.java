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

package sh.stubborn.contract.stubrunner;

import java.io.IOException;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

class DirectoryCopyingVisitor extends SimpleFileVisitor<Path> {

	private static final List<String> FOLDERS_TO_DELETE = Arrays.asList("contracts", "mappings");

	private static final Log log = LogFactory.getLog(DirectoryCopyingVisitor.class);

	private final Path from;

	private final Path to;

	DirectoryCopyingVisitor(Path from, Path to) {
		this.from = from;
		this.to = to;
		if (log.isDebugEnabled()) {
			log.debug("Will copy from [" + from.toString() + "] to [" + to.toString() + "]");
		}
	}

	@Override
	public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
		Path relativePath = this.from.relativize(dir);
		if (".git".equals(relativePath.toString())) {
			return FileVisitResult.SKIP_SUBTREE;
		}
		Path targetPath = this.to.resolve(relativePath);
		if (!Files.exists(targetPath)) {
			if (log.isDebugEnabled()) {
				log.debug("Created a folder [" + targetPath.toString() + "]");
			}
			Files.createDirectory(targetPath);
		}
		else {
			if (log.isDebugEnabled()) {
				log.debug("Folder [" + targetPath.toString() + "] already exists");
			}
			if (FOLDERS_TO_DELETE.contains(targetPath.toFile().getName())) {
				if (log.isDebugEnabled()) {
					log.debug("Will remove the folder [" + targetPath.toString() + "]");
				}
				deleteRecursively(targetPath);
				Files.createDirectory(targetPath);
				if (log.isDebugEnabled()) {
					log.debug("Recreated folder [" + targetPath.toString() + "]");
				}
			}
		}
		return FileVisitResult.CONTINUE;
	}

	private boolean deleteRecursively(Path root) throws IOException {
		if (root == null) {
			return false;
		}
		if (!Files.exists(root)) {
			return false;
		}
		Files.walkFileTree(root, new SimpleFileVisitor<Path>() {
			@Override
			public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
				Files.delete(file);
				return FileVisitResult.CONTINUE;
			}

			@Override
			public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
				// a hack for Windows not to fail when directory is removed
				// related to
				// https://github.com/spring-cloud/spring-cloud-sleuth/issues/834
				if (exc == null) {
					int maxTries = 5;
					int count = 0;
					boolean deleted;
					do {
						deleted = this.isDeleted(dir);
						if (deleted) {
							if (log.isDebugEnabled()) {
								log.debug("Deleted [" + dir + "]");
							}
							break;
						}
						if (log.isDebugEnabled()) {
							log.debug("Failed to delete [" + dir + "]");
						}
						// wait a bit and try again
						count++;
						try {
							Thread.sleep(2);
						}
						catch (InterruptedException e1) {
							Thread.currentThread().interrupt();
							break;
						}

					}
					while (count < maxTries);
					if (!deleted) {
						if (log.isDebugEnabled()) {
							log.debug("Failed to delete [" + dir + "] after [" + maxTries + "] attempts to do it");
						}
						throw new DirectoryNotEmptyException(dir.toString());
					}
					return FileVisitResult.CONTINUE;
				}
				throw exc;
			}

			private boolean isDeleted(Path dir) throws IOException {
				try {
					Files.delete(dir);
					return true;
				}
				catch (DirectoryNotEmptyException ex) {
					// happens sometimes if Windows is too slow to remove children of a
					// directory
					return false;
				}
			}
		});
		return true;
	}

	@Override
	public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
		Path relativePath = this.to.resolve(this.from.relativize(file));
		Files.copy(file, relativePath, StandardCopyOption.REPLACE_EXISTING);
		if (log.isDebugEnabled()) {
			log.debug("Copied file from [" + file.toString() + "] to [" + relativePath.toString() + "]");
		}
		return FileVisitResult.CONTINUE;
	}

}
