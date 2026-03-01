/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
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

import org.junit.jupiter.api.Test;
import org.openjdk.skara.vcs.*;
import org.openjdk.skara.vcs.openjdk.CommitMessage;
import org.openjdk.skara.vcs.openjdk.CommitMessageParsers;

import java.nio.file.Path;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PatchFileCheckTests {
    private static final JCheckConfiguration conf = JCheckConfiguration.parse(List.of(
        "[general]",
        "project = test",
        "[checks]",
        "error = patchfile"
    ));

    private static final JCheckConfiguration origConf = JCheckConfiguration.parse(List.of(
        "[general]",
        "project = test",
        "[checks]",
        "error = patchfile",
        "[checks \"patchfile\"]",
        "file_types=.orig"
    ));

    private static final JCheckConfiguration rejConf = JCheckConfiguration.parse(List.of(
            "[general]",
            "project = test",
            "[checks]",
            "error = patchfile",
            "[checks \"patchfile\"]",
            "file_types=.rej"
    ));

    private static Commit commit(Hash hash, List<String> message, List<Diff> parentDiffs) {
        var author = new Author("Foo Bar", "foo@bar.org");
        var parents = List.of(new Hash("12345789012345789012345678901234567890"));
        var authored = ZonedDateTime.now();
        var metadata = new CommitMetadata(hash, parents, author, authored, author, authored, message);
        return new Commit(metadata, parentDiffs);
    }

    private static CommitMessage message(Commit c) {
        return CommitMessageParsers.v1.parse(c);
    }

    private List<Issue> toList(Iterator<Issue> i) {
        var list = new ArrayList<Issue>();
        while (i.hasNext()) {
            list.add(i.next());
        }
        return list;
    }

    @Test
    void origSuffixFailsCheck() {
        var targetHash = "12345789012345789012345678901234567890";
        var tag = "skara-11+22";

        var hunk1 = new Hunk(new Range(1, 0), List.of(),
                            new Range(1, 1), List.of(targetHash + " " + tag));
        var patch1 = new TextualPatch(Path.of("test.orig"), FileType.fromOctal("100644"), Hash.zero(),
                               Path.of("test.orig"), FileType.fromOctal("100644"), Hash.zero(),
                               Status.from('A'), List.of(hunk1));
        var diff = new Diff(Hash.zero(), Hash.zero(), List.of(patch1));
        var diffs = List.of(diff);

        var commitHash = "1111222233334444555566667777888899990000";
        var lines = List.of("Added tag " + tag + " for changeset " + targetHash);
        var commit = commit(new Hash(commitHash), lines, diffs);

        var check = new PatchFileCheck();
        var issues = toList(check.check(commit, message(commit), conf, null));

        assertEquals(1, issues.size());
        var issue = (PatchFileIssue) issues.get(0);
        assertEquals(Severity.ERROR, issue.severity());
        assertEquals(commit, issue.commit());
        assertEquals(check, issue.check());
    }

    @Test
    void origSuffixFailsCheckWithOrigConf() {
        var targetHash = "12345789012345789012345678901234567890";
        var tag = "skara-11+22";

        var hunk1 = new Hunk(new Range(1, 0), List.of(),
                new Range(1, 1), List.of(targetHash + " " + tag));
        var patch1 = new TextualPatch(Path.of("test.orig"), FileType.fromOctal("100644"), Hash.zero(),
                Path.of("test.orig"), FileType.fromOctal("100644"), Hash.zero(),
                Status.from('A'), List.of(hunk1));
        var diff = new Diff(Hash.zero(), Hash.zero(), List.of(patch1));
        var diffs = List.of(diff);

        var commitHash = "1111222233334444555566667777888899990000";
        var lines = List.of("Added tag " + tag + " for changeset " + targetHash);
        var commit = commit(new Hash(commitHash), lines, diffs);

        var check = new PatchFileCheck();
        var issues = toList(check.check(commit, message(commit), origConf, null));

        assertEquals(1, issues.size());
        var issue = (PatchFileIssue) issues.get(0);
        assertEquals(Severity.ERROR, issue.severity());
        assertEquals(commit, issue.commit());
        assertEquals(check, issue.check());
    }

    @Test
    void origSuffixPassesCheckWithRejConf() {
        var targetHash = "12345789012345789012345678901234567890";
        var tag = "skara-11+22";

        var hunk1 = new Hunk(new Range(1, 0), List.of(),
                new Range(1, 1), List.of(targetHash + " " + tag));
        var patch1 = new TextualPatch(Path.of("test.orig"), FileType.fromOctal("100644"), Hash.zero(),
                Path.of("test.orig"), FileType.fromOctal("100644"), Hash.zero(),
                Status.from('A'), List.of(hunk1));
        var diff = new Diff(Hash.zero(), Hash.zero(), List.of(patch1));
        var diffs = List.of(diff);

        var commitHash = "1111222233334444555566667777888899990000";
        var lines = List.of("Added tag " + tag + " for changeset " + targetHash);
        var commit = commit(new Hash(commitHash), lines, diffs);

        var check = new PatchFileCheck();
        var issues = toList(check.check(commit, message(commit), rejConf, null));

        assertEquals(0, issues.size());
    }

    @Test
    void rejSuffixFailsCheck() {
        var targetHash = "12345789012345789012345678901234567890";
        var tag = "skara-11+22";

        var hunk1 = new Hunk(new Range(1, 0), List.of(),
                new Range(1, 1), List.of(targetHash + " " + tag));
        var patch1 = new TextualPatch(Path.of("test.rej"), FileType.fromOctal("100644"), Hash.zero(),
                Path.of("test.rej"), FileType.fromOctal("100644"), Hash.zero(),
                Status.from('A'), List.of(hunk1));
        var diff = new Diff(Hash.zero(), Hash.zero(), List.of(patch1));
        var diffs = List.of(diff);

        var commitHash = "1111222233334444555566667777888899990000";
        var lines = List.of("Added tag " + tag + " for changeset " + targetHash);
        var commit = commit(new Hash(commitHash), lines, diffs);

        var check = new PatchFileCheck();
        var issues = toList(check.check(commit, message(commit), conf, null));

        assertEquals(1, issues.size());
        var issue = (PatchFileIssue) issues.get(0);
        assertEquals(Severity.ERROR, issue.severity());
        assertEquals(commit, issue.commit());
        assertEquals(check, issue.check());
    }

    @Test
    void rejSuffixFailsCheckWithRejConf() {
        var targetHash = "12345789012345789012345678901234567890";
        var tag = "skara-11+22";

        var hunk1 = new Hunk(new Range(1, 0), List.of(),
                new Range(1, 1), List.of(targetHash + " " + tag));
        var patch1 = new TextualPatch(Path.of("test.rej"), FileType.fromOctal("100644"), Hash.zero(),
                Path.of("test.rej"), FileType.fromOctal("100644"), Hash.zero(),
                Status.from('A'), List.of(hunk1));
        var diff = new Diff(Hash.zero(), Hash.zero(), List.of(patch1));
        var diffs = List.of(diff);

        var commitHash = "1111222233334444555566667777888899990000";
        var lines = List.of("Added tag " + tag + " for changeset " + targetHash);
        var commit = commit(new Hash(commitHash), lines, diffs);

        var check = new PatchFileCheck();
        var issues = toList(check.check(commit, message(commit), rejConf, null));

        assertEquals(1, issues.size());
        var issue = (PatchFileIssue) issues.get(0);
        assertEquals(Severity.ERROR, issue.severity());
        assertEquals(commit, issue.commit());
        assertEquals(check, issue.check());
    }

    @Test
    void rejSuffixPassesCheckWithOrigConf() {
        var targetHash = "12345789012345789012345678901234567890";
        var tag = "skara-11+22";

        var hunk1 = new Hunk(new Range(1, 0), List.of(),
                new Range(1, 1), List.of(targetHash + " " + tag));
        var patch1 = new TextualPatch(Path.of("test.rej"), FileType.fromOctal("100644"), Hash.zero(),
                Path.of("test.rej"), FileType.fromOctal("100644"), Hash.zero(),
                Status.from('A'), List.of(hunk1));
        var diff = new Diff(Hash.zero(), Hash.zero(), List.of(patch1));
        var diffs = List.of(diff);

        var commitHash = "1111222233334444555566667777888899990000";
        var lines = List.of("Added tag " + tag + " for changeset " + targetHash);
        var commit = commit(new Hash(commitHash), lines, diffs);

        var check = new PatchFileCheck();
        var issues = toList(check.check(commit, message(commit), origConf, null));

        assertEquals(0, issues.size());
    }

    @Test
    void txtSuffixDoesNotFailCheck() {
        var targetHash = "12345789012345789012345678901234567890";
        var tag = "skara-11+22";

        var hunk1 = new Hunk(new Range(1, 0), List.of(),
                new Range(1, 1), List.of(targetHash + " " + tag));
        var patch1 = new TextualPatch(Path.of("test.txt"), FileType.fromOctal("100644"), Hash.zero(),
                Path.of("test.txt"), FileType.fromOctal("100644"), Hash.zero(),
                Status.from('M'), List.of(hunk1));
        var diff = new Diff(Hash.zero(), Hash.zero(), List.of(patch1));
        var diffs = List.of(diff);

        var commitHash = "1111222233334444555566667777888899990000";
        var lines = List.of("Added tag " + tag + " for changeset " + targetHash);
        var commit = commit(new Hash(commitHash), lines, diffs);

        var check = new PatchFileCheck();
        var issues = toList(check.check(commit, message(commit), conf, null));

        assertEquals(0, issues.size());
    }
}
