/*
 * Copyright (c) 2008-2019 Haulmont.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.jmix.reports.libintegration;

import com.haulmont.yarg.exception.ReportFormattingException;
import com.haulmont.yarg.formatters.impl.inline.AbstractInliner;
import io.jmix.core.*;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.regex.Pattern;

@Component("report_FileStorageContentInliner")
public class FileStorageContentInliner extends AbstractInliner {
    private final static String REGULAR_EXPRESSION = "\\$\\{imageFileId:([0-9]+?)x([0-9]+?)\\}";

    @Autowired
    protected Metadata metadata;

    @Autowired
    protected DataManager dataManager;

    @Autowired
    protected FileStorageLocator fileStorageLocator;

    protected FileStorage fileStorage;

    public FileStorageContentInliner() {
        tagPattern = Pattern.compile(REGULAR_EXPRESSION, Pattern.CASE_INSENSITIVE);
    }

    @Override
    public Pattern getTagPattern() {
        return tagPattern;
    }

    @Override
    protected byte[] getContent(Object paramValue) {
        try {
            //todo image format
            FileRef fileRef = null;
//            if (paramValue instanceof FileDescriptor) {
//                file = dataManager.load(new LoadContext(metadata.getClass(FileDescriptor.class)).setId(((FileDescriptor) paramValue).getId()));
//            } else {
//                file = dataManager.load(new LoadContext(metadata.getClass(FileDescriptor.class)).setId(UuidProvider.fromString(paramValue.toString())));
//            }
            byte[] bytes = IOUtils.toByteArray(getFileStorage().openStream(fileRef));
            return bytes;
        } catch (IOException e) {
            throw new ReportFormattingException(String.format("Unable to get image from file storage. File id [%s]", paramValue), e);
        }
    }

    protected FileStorage getFileStorage() {
        if (fileStorage == null) {
            fileStorage = fileStorageLocator.getDefault();
        }
        return fileStorage;
    }
}