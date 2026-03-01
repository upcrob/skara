/*
 * Copyright (c) 2019, 2024, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package org.openjdk.skara.jcheck;

import org.openjdk.skara.census.Census;
import org.openjdk.skara.vcs.Commit;
import org.openjdk.skara.vcs.Diff;
import org.openjdk.skara.vcs.Patch;
import org.openjdk.skara.vcs.openjdk.CommitMessage;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class PatchFileCheck extends CommitCheck {
    private final Set<String> patchFileTypes = Set.of(".orig", ".rej");

    @Override
    Iterator<Issue> check(Commit commit, CommitMessage message, JCheckConfiguration conf, Census census) {
        CommitIssue.Metadata metadata = CommitIssue.metadata(commit, message, conf, this);
        List<Issue> issues = new ArrayList<>();
        for (Diff diff : commit.parentDiffs()) {
            for (Patch patch : diff.patches()) {
                Path path = patch.target().path().get();
                if (hasPatchFileSuffix(path)) {
                    issues.add(new PatchFileIssue(path, metadata));
                }
            }
        }
        return issues.iterator();
    }

    private boolean hasPatchFileSuffix(Path path) {
        for (String suffix : patchFileTypes) {
            if (path.getFileName().toString().endsWith(suffix)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public String name() {
        return "patchfile";
    }

    @Override
    public String description() {
        return "Change must not include patch files ("
                + String.join(", ", patchFileTypes) + ")";
    }
}
