package org.fuin.porelo.agent.impl.codex;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.fuin.porelo.agent.impl.codex.WorkflowSteps.Run;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Runs the build workflow's publishing steps against a stub {@code curl}, with the repository's own
 * {@code artifactory.sh} beside them, so what they upload and what they believe is seen, not looked
 * for.
 */
class PublishStepsTest {

    private static final String POOL = "porelo-dist-deb/pool/main/p/porelo-agent-codex/";

    private static final String DEB = "porelo-agent-codex_0.1.0~snapshot.7_amd64.deb";

    private static final String RPM = "porelo-agent-codex-0.1.0~snapshot.7-1.x86_64.rpm";

    private static final String OLDER =
            "Package: porelo-agent-codex\nVersion: 0.1.0~snapshot.6\n"
                    + "Filename: pool/main/p/porelo-agent-codex/"
                    + "porelo-agent-codex_0.1.0~snapshot.6_amd64.deb\n\n";

    private static final String THIS =
            "Package: porelo-agent-codex\nVersion: 0.1.0~snapshot.7\n"
                    + "Filename: pool/main/p/porelo-agent-codex/"
                    + DEB
                    + "\n\n";

    @Test
    void thePackagesAndTheirBillReachOnlyTheSnapshotChannel(@TempDir final Path dir)
            throws Exception {
        final Run run = WorkflowSteps.run(publish(), dir, stubs(dir, OLDER), WorkflowSteps.LIMIT);
        assertThat(run.exit()).as("exit of the publish step:%n%s", run.output()).isZero();
        final List<String> uploads = calls(dir).stream().filter(PublishStepsTest::upload).toList();
        assertThat(uploads)
                .as("what the publish step uploads, and where")
                .hasSize(4)
                .anyMatch(
                        c ->
                                c.contains(" -T target/" + DEB + " ")
                                        && c.contains(
                                                POOL
                                                        + DEB
                                                        + ";deb.distribution=snapshots;"
                                                        + "deb.component=main;"
                                                        + "deb.architecture=amd64"))
                .anyMatch(
                        c ->
                                c.contains(" -T target/" + RPM + " ")
                                        && c.endsWith("porelo-dist-rpm/snapshots/" + RPM))
                .anyMatch(c -> c.endsWith(POOL + "porelo-agent-codex.cdx.json"))
                .anyMatch(c -> c.endsWith("porelo-dist-rpm/snapshots/porelo-agent-codex.cdx.json"))
                .noneMatch(c -> c.contains("releases"));
    }

    @Test
    void aReleaseVersionIsNeverPublishedAsASnapshot(@TempDir final Path dir) throws Exception {
        final Run release = snapshotOnly(dir, "0.1.0");
        assertThat(release.exit())
                .as("exit for a release version on main:%n%s", release.output())
                .isNotZero();
        assertThat(release.output())
                .as("the refusal names the version")
                .contains("0.1.0 on main would publish a release as a snapshot");

        final Run snapshot = snapshotOnly(dir, "0.1.0-SNAPSHOT");
        assertThat(snapshot.exit())
                .as("exit for a snapshot version on main:%n%s", snapshot.output())
                .isZero();
    }

    @Test
    void aPackageIsIndexedOnlyWithItsOwnVersion(@TempDir final Path dir) throws Exception {
        final List<String> steps =
                WorkflowSteps.scripts("build.yml", "The package is indexed, not merely stored");
        assertThat(steps).as("the index step of the build job").hasSize(1);
        final Run older =
                WorkflowSteps.run(steps.getFirst(), dir, stubs(dir, OLDER), WorkflowSteps.LIMIT);
        assertThat(older.exit())
                .as("exit with only an older version indexed:%n%s", older.output())
                .isEqualTo(1);
        assertThat(calls(dir))
                .as("the index the step reads")
                .isNotEmpty()
                .allMatch(
                        c ->
                                c.endsWith(
                                        "https://artifactory.invalid/artifactory/porelo-dist-deb/"
                                                + "dists/snapshots/main/binary-amd64/Packages"));

        final Run indexed =
                WorkflowSteps.run(
                        steps.getFirst(), dir, stubs(dir, OLDER + THIS), WorkflowSteps.LIMIT);
        assertThat(indexed.exit())
                .as("exit with this version indexed:%n%s", indexed.output())
                .isZero();
    }

    @Test
    void anUnreadableIndexIsNamedWithWhatCurlSaid(@TempDir final Path dir) throws Exception {
        final List<String> steps =
                WorkflowSteps.scripts("build.yml", "The package is indexed, not merely stored");
        final Map<String, String> env = stubs(dir, OLDER);
        env.put("CURL_EXIT", "22");
        final Run run = WorkflowSteps.run(steps.getFirst(), dir, env, WorkflowSteps.LIMIT);
        assertThat(run.exit()).as("exit with the index unreadable:%n%s", run.output()).isNotZero();
        assertThat(run.output())
                .as("what the step asked and what curl answered")
                .contains("dists/snapshots/main/binary-amd64/Packages")
                .contains("curl exited 22");
    }

    @Test
    void anEmptyRepositoryAddressIsRefusedBeforeAnyCall(@TempDir final Path dir) throws Exception {
        for (final String step :
                List.of(
                        publish(),
                        WorkflowSteps.scripts(
                                        "build.yml", "The package is indexed, not merely stored")
                                .getFirst())) {
            final Map<String, String> env = stubs(dir, OLDER + THIS);
            env.put("JF_URL", "");
            final Run run = WorkflowSteps.run(step, dir, env, WorkflowSteps.LIMIT);
            assertThat(run.exit()).as("exit without JF_URL:%n%s", run.output()).isNotZero();
            assertThat(run.output()).as("the refusal").contains("JF_URL is empty");
            assertThat(calls(dir)).as("curl calls without JF_URL").isEmpty();
        }
    }

    @Test
    void aBranchNameIsNeverRunAsShell(@TempDir final Path dir) throws Exception {
        final String ref = "refs/heads/x'$(touch ran)'";
        final List<String> steps = WorkflowSteps.scripts("build.yml", "Not publishing");
        assertThat(steps).as("the step that says why nothing is published").hasSize(1);
        // As GitHub expands an expression into the script before bash reads it.
        final String script =
                steps.getFirst()
                        .replace("${{ github.event_name }}", "push")
                        .replace("${{ github.ref }}", ref);
        final Run run =
                WorkflowSteps.run(
                        script, dir, Map.of("EVENT", "push", "REF", ref), WorkflowSteps.LIMIT);
        assertThat(run.exit()).as("exit of the step:%n%s", run.output()).isZero();
        assertThat(dir.resolve("ran")).as("a file the branch's name created").doesNotExist();
        assertThat(run.output()).as("the branch named as it is").contains(ref);
    }

    private static Run snapshotOnly(final Path dir, final String version)
            throws IOException, InterruptedException {
        final List<String> steps =
                WorkflowSteps.scripts("build.yml", "Only a snapshot is published");
        assertThat(steps).as("the step that holds main to snapshots").hasSize(1);
        Files.writeString(
                dir.resolve("pom.xml"),
                "<project>\n    <parent>\n        <version>0.2.1-SNAPSHOT</version>\n"
                        + "    </parent>\n    <version>"
                        + version
                        + "</version>\n</project>\n");
        return WorkflowSteps.run(steps.getFirst(), dir, Map.of(), WorkflowSteps.LIMIT);
    }

    private static String publish() throws IOException {
        final List<String> steps = WorkflowSteps.scripts("build.yml", "Publish the packages");
        assertThat(steps).as("the publishing step of the build job").hasSize(1);
        return steps.getFirst();
    }

    private static boolean upload(final String call) {
        return call.startsWith("curl ") && call.contains(" -T ");
    }

    private static List<String> calls(final Path dir) throws IOException {
        final Path calls = dir.resolve("calls");
        return Files.exists(calls) ? Files.readAllLines(calls) : List.of();
    }

    private static Map<String, String> stubs(final Path dir, final String index)
            throws IOException {
        final Path bin = Files.createDirectories(dir.resolve("bin"));
        final Path target = Files.createDirectories(dir.resolve("target"));
        Files.writeString(target.resolve(DEB), "deb");
        Files.writeString(target.resolve(RPM), "rpm");
        Files.writeString(target.resolve("porelo-agent-codex.cdx.json"), "{}");
        Files.writeString(dir.resolve("index"), index);
        Files.deleteIfExists(dir.resolve("calls"));
        final Path scripts = Files.createDirectories(dir.resolve(".github/scripts"));
        Files.copy(
                RepositoryDocuments.root().resolve(".github/scripts/artifactory.sh"),
                scripts.resolve("artifactory.sh"),
                StandardCopyOption.REPLACE_EXISTING);
        script(
                bin.resolve("curl"),
                """
                echo "curl $*" >> "$CALLS"
                [ "${CURL_EXIT:-0}" = 0 ] || exit "$CURL_EXIT"
                out=
                while [ $# -gt 0 ]; do
                    case "$1" in -o) out="$2"; shift ;; esac
                    shift
                done
                if [ -n "$out" ]; then cat "$INDEX" > "$out"; else cat "$INDEX"; fi
                """);
        script(bin.resolve("sleep"), "exit 0\n");
        final Map<String, String> env = new HashMap<>();
        env.put("PATH", bin + ":" + System.getenv("PATH"));
        env.put("CALLS", dir.resolve("calls").toString());
        env.put("INDEX", dir.resolve("index").toString());
        env.put("JF_URL", "https://artifactory.invalid");
        env.put("JF_ACCESS_TOKEN", "not-a-token");
        return env;
    }

    private static void script(final Path file, final String body) throws IOException {
        Files.writeString(file, "#!/bin/sh\n" + body);
        Files.setPosixFilePermissions(file, PosixFilePermissions.fromString("rwxr-xr-x"));
    }
}
