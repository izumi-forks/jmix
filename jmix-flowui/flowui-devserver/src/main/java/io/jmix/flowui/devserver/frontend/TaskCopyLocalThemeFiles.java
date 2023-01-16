/*
 * Copyright 2000-2022 Vaadin Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package io.jmix.flowui.devserver.frontend;

import com.vaadin.flow.server.frontend.FallibleCommand;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * Copies theme files from the given local frontend folder.
 */
public class TaskCopyLocalThemeFiles implements FallibleCommand {

    private final File frontendThemeFolder;
    private final File localThemeFolder;

    /**
     * Copy project local theme files from defined frontendResourcesDirectory
     * (by default 'frontend/themes'). This enables running jar projects locally.
     *
     * @param frontendThemeFolder target directory for the discovered files
     * @param localThemeFolder    local folder containing resources to copy
     */
    TaskCopyLocalThemeFiles(File frontendThemeFolder,
                            File localThemeFolder) {
        this.frontendThemeFolder = frontendThemeFolder;
        this.localThemeFolder = localThemeFolder;
    }

    @Override
    public void execute() {
        createTargetFolder(frontendThemeFolder);

        if (localThemeFolder != null
                && localThemeFolder.isDirectory()) {
            String startMessage = "Copying project local frontend resources...";
            log().info(startMessage);
            FrontendUtils.logInFile(startMessage);
            copyLocalResources(localThemeFolder, frontendThemeFolder);
            String completeMessage = "Copying frontend directory completed.";
            log().info(completeMessage);
            FrontendUtils.logInFile(completeMessage);
        } else {
            log().debug("Found no local frontend resources for the project");
        }
    }

    static void copyLocalResources(File source, File target) {
        if (!source.isDirectory() || !target.isDirectory()) {
            return;
        }
        try {
            FileUtils.copyDirectory(source, target);
            try (Stream<Path> fileStream = Files
                    .walk(Paths.get(target.getPath()))) {
                // used with try-with-resources as defined in walk API note
                fileStream.filter(file -> !Files.isWritable(file)).forEach(
                        filePath -> filePath.toFile().setWritable(true));
            }
        } catch (IOException e) {
            throw new UncheckedIOException(String.format(
                    "Failed to copy project frontend resources from '%s' to '%s'",
                    source, target), e);
        }
    }

    static void createTargetFolder(File target) {
        try {
            FileUtils.forceMkdir(Objects.requireNonNull(target));
        } catch (IOException e) {
            throw new UncheckedIOException(
                    String.format("Failed to create directory '%s'", target),
                    e);
        }
    }

    private Logger log() {
        return LoggerFactory.getLogger(this.getClass());
    }
}