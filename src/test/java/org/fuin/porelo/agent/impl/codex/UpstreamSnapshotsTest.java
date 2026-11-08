package org.fuin.porelo.agent.impl.codex;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Stream;
import javax.xml.parsers.DocumentBuilderFactory;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.yaml.snakeyaml.Yaml;

/**
 * Where snapshots come from on a forge whose registry holds only its organization's packages: a
 * second, anonymous repository the forge names, which never gets the forge's token. The other
 * adapters' copy also guards a tag's build; nothing builds a tag here yet.
 */
class UpstreamSnapshotsTest {

    /** The server id that carries the forge's read-only token. */
    private static final String FORGE = "porelo-forge";

    private static final String FORGE_URL = "${env.MAVEN_SNAPSHOTS_URL}";

    private static final String UPSTREAM_URL = "${env.MAVEN_UPSTREAM_SNAPSHOTS_URL}";

    /** Maven's id for a profile or a server that names none. */
    private static final String DEFAULT_ID = "default";

    private static final String SETTINGS_HEAD =
            "<settings xmlns=\"http://maven.apache.org/SETTINGS/1.0.0\"><servers><server><id>"
                    + FORGE
                    + "</id></server><server><id>central-sonatype</id></server></servers>";

    private static final String FORGE_PROFILE =
            "<profile><id>forge</id><repositories><repository><id>"
                    + FORGE
                    + "</id><url>"
                    + FORGE_URL
                    + "</url><snapshots><enabled>true</enabled></snapshots></repository>"
                    + "</repositories></profile>";

    private static final String NO_REPOSITORIES_POM =
            "<project><modelVersion>4.0.0</modelVersion></project>";

    @Test
    void theForgesTokenGoesToTheForgesRegistryAlone() throws Exception {

        // Maven sends a server's credential to every repository and mirror of the same id, wherever
        // its URL points: a second one under the forge's id would hand its token to that host.
        final Document settings = parse(RepositoryDocuments.root().resolve("settings.xml"));
        final Document pom = parse(RepositoryDocuments.root().resolve("pom.xml"));

        assertThat(serverIds(settings))
                .as("the servers settings.xml gives credentials for")
                .containsExactlyInAnyOrder("central-sonatype", FORGE);
        assertThat(urls(settings, FORGE))
                .as("the URLs the forge's token is sent to")
                .containsOnly(FORGE_URL)
                .hasSize(2);
        assertThat(urls(settings, "porelo-upstream-snapshots"))
                .as("the URLs of the upstream repository and plugin repository")
                .containsExactly(UPSTREAM_URL, UPSTREAM_URL);
        assertThat(credentialLeaks(settings, pom))
                .as("where a credential could go astray")
                .isEmpty();
    }

    @Test
    void seesEveryWayACredentialGoesAstray() throws Exception {

        assertThat(credentialLeaks(settings(FORGE_PROFILE), pom(NO_REPOSITORIES_POM)))
                .as("the forge's repository alone")
                .isEmpty();
        assertThat(
                        credentialLeaks(
                                settings(
                                        "<profile><id>x</id><repositories><repository><id>"
                                                + FORGE
                                                + "</id><url>https://elsewhere.example.org/</url>"
                                                + "</repository></repositories></profile>"),
                                pom(NO_REPOSITORIES_POM)))
                .as("a second repository under the forge's id")
                .isNotEmpty();
        assertThat(
                        credentialLeaks(
                                settings(
                                        "<profile><id>x</id><repositories><repository><id>"
                                                + "central-sonatype</id><url>"
                                                + FORGE_URL
                                                + "</url></repository></repositories></profile>"),
                                pom(NO_REPOSITORIES_POM)))
                .as("another server's credential sent to the forge")
                .isNotEmpty();
        assertThat(
                        credentialLeaks(
                                settings(
                                        "<profile><repositories><repository><id>"
                                                + FORGE
                                                + "</id><url>https://elsewhere.example.org/</url>"
                                                + "</repository></repositories><id>x</id></profile>"),
                                pom(NO_REPOSITORIES_POM)))
                .as("a repository read before its profile's id")
                .isNotEmpty();
        assertThat(
                        credentialLeaks(
                                settings(
                                        "</profiles><mirrors><mirror><id>"
                                                + FORGE
                                                + "</id><mirrorOf>*</mirrorOf>"
                                                + "<url>https://elsewhere.example.org/</url>"
                                                + "</mirror></mirrors><profiles>"),
                                pom(NO_REPOSITORIES_POM)))
                .as("a mirror")
                .isNotEmpty();
        assertThat(
                        credentialLeaks(
                                settings(FORGE_PROFILE),
                                pom(
                                        "<project><repositories><repository><id>"
                                                + FORGE
                                                + "</id><url>https://elsewhere.example.org/</url>"
                                                + "</repository></repositories></project>")))
                .as("a repository in pom.xml")
                .isNotEmpty();
    }

    @Test
    void noTagIsBuiltHere() throws Exception {

        // The other adapters' tag guard is not here, since nothing builds a tag yet: a release
        // and its -P!standard,!forge,!upstream come with the first tag release. Until then a
        // tag's build would read these profiles.
        final Document settings = parse(RepositoryDocuments.root().resolve("settings.xml"));
        assertThat(snapshotProfiles(settings))
                .as("the profiles of settings.xml that read snapshots")
                .containsExactly("forge", "standard", "upstream");

        final Path dir = RepositoryDocuments.root().resolve(".github/workflows");
        final List<Path> workflows;
        try (Stream<Path> paths = Files.list(dir)) {
            workflows =
                    paths.filter(
                                    p ->
                                            p.toString().endsWith(".yml")
                                                    || p.toString().endsWith(".yaml"))
                            .sorted()
                            .toList();
        }
        assertThat(workflows.stream().map(p -> p.getFileName().toString()))
                .as("the workflows read")
                .containsExactly("build.yml", "shared-rules.yml", "update.yml", "workflows.yml");
        for (final Path workflow : workflows) {
            assertThat(tagTriggers(Files.readString(workflow)))
                    .as("what runs %s on a tag", workflow.getFileName())
                    .isEmpty();
        }
    }

    @Test
    void seesEveryWayATagIsBuilt() throws Exception {

        assertThat(
                        tagTriggers(
                                "on:\n    push:\n        branches: ['**']\n    workflow_dispatch:\n"))
                .as("a push to branches only")
                .isEmpty();
        assertThat(tagTriggers("on:\n    schedule:\n        - cron: '1 1 * * 1'\n"))
                .as("a schedule")
                .isEmpty();
        for (final String on :
                List.of(
                        "on:\n    push:\n        tags: ['v*']\n",
                        "on:\n    push:\n        branches: ['**']\n        tags-ignore: ['x']\n",
                        "on:\n    push:\n        branches: ['**']\n        \"tags\": ['v*']\n",
                        "on: push\n",
                        "on: [push, workflow_dispatch]\n",
                        "on:\n    push:\n        paths-ignore: ['doc/**']\n",
                        "on:\n    push:\n",
                        "on: release\n",
                        "on:\n    create:\n",
                        "\"on\":\n    release:\n        types: [published]\n")) {
            assertThat(tagTriggers(on)).as("a workflow with %s", on).isNotEmpty();
        }

        final String head = "<settings xmlns=\"http://maven.apache.org/SETTINGS/1.0.0\"><profiles>";
        assertThat(
                        snapshotProfiles(
                                parse(
                                        head
                                                + "<profile><id>a</id><repositories><repository>"
                                                + "<id>r</id></repository></repositories></profile>"
                                                + "<profile><id>b</id><repositories><repository>"
                                                + "<id>r</id><snapshots><updatePolicy>always"
                                                + "</updatePolicy></snapshots></repository>"
                                                + "</repositories></profile>"
                                                + "<profile><pluginRepositories><pluginRepository>"
                                                + "<id>c</id></pluginRepository>"
                                                + "</pluginRepositories></profile>"
                                                + "<profile><id>d</id><repositories><repository>"
                                                + "<id>r</id><snapshots><enabled>false</enabled>"
                                                + "</snapshots></repository></repositories>"
                                                + "</profile></profiles></settings>")))
                .as("snapshots by Maven's default, a profile without an id, and one switched off")
                .containsExactly("a", "b", DEFAULT_ID);
    }

    /** What runs a workflow on a tag: a push not limited to branches, a tag filter, a release. */
    private static List<String> tagTriggers(final String workflow) {
        final List<String> found = new ArrayList<>();
        final Map<?, ?> loaded = new Yaml().load(workflow);
        // YAML 1.1 reads a bare on as true.
        final Object on = loaded.containsKey("on") ? loaded.get("on") : loaded.get(Boolean.TRUE);
        final Map<Object, Object> events = new LinkedHashMap<>();
        if (on instanceof String event) {
            events.put(event, null);
        } else if (on instanceof List<?> list) {
            list.forEach(event -> events.put(event, null));
        } else if (on instanceof Map<?, ?> map) {
            events.putAll(map);
        }
        events.forEach(
                (event, filter) -> {
                    if ("release".equals(event) || "create".equals(event)) {
                        found.add(event + " runs on a tag or a release");
                    }
                    if (filter instanceof Map<?, ?> map
                            && (map.containsKey("tags") || map.containsKey("tags-ignore"))) {
                        found.add(event + " filters by tag");
                    }
                    if ("push".equals(event)
                            && !(filter instanceof Map<?, ?> map
                                    && (map.containsKey("branches")
                                            || map.containsKey("branches-ignore")))) {
                        found.add("push without a branch filter runs on every tag");
                    }
                });
        return found;
    }

    /** What could send a server's credential to a host other than the forge's registry. */
    private static List<String> credentialLeaks(final Document settings, final Document pom) {
        final List<String> leaks = new ArrayList<>(pomRepositories(pom));
        final Set<String> servers = serverIds(settings);
        if (settings.getElementsByTagName("mirror").getLength() > 0) {
            leaks.add("settings.xml has a mirror, which takes over a repository's id and URL");
        }
        for (final Element repository : repositories(settings.getDocumentElement())) {
            final String id = child(repository, "id", DEFAULT_ID);
            final String url = child(repository, "url", "");
            if (servers.contains(id) && !(FORGE.equals(id) && FORGE_URL.equals(url))) {
                leaks.add(
                        "repository " + id + " at " + url + " gets server " + id + "'s credential");
            }
        }
        return leaks;
    }

    /** A pom's own repositories, which no profile switch of settings.xml turns off. */
    private static List<String> pomRepositories(final Document pom) {
        final List<String> found = new ArrayList<>();
        for (final Element repository : repositories(pom.getDocumentElement())) {
            found.add("pom.xml names repository " + child(repository, "id", DEFAULT_ID));
        }
        return found;
    }

    /** The ids of the profiles with a repository that reads snapshots. */
    private static Set<String> snapshotProfiles(final Document settings) {
        final Set<String> ids = new TreeSet<>();
        final NodeList profiles = settings.getElementsByTagName("profile");
        for (int i = 0; i < profiles.getLength(); i++) {
            final Element profile = (Element) profiles.item(i);
            if (repositories(profile).stream().anyMatch(UpstreamSnapshotsTest::takesSnapshots)) {
                ids.add(child(profile, "id", DEFAULT_ID));
            }
        }
        return ids;
    }

    /**
     * Snapshots are on unless the repository's own {@code snapshots} says {@code enabled} false.
     */
    private static boolean takesSnapshots(final Element repository) {
        final Element snapshots = childElement(repository, "snapshots");
        return snapshots == null || !"false".equalsIgnoreCase(child(snapshots, "enabled", "true"));
    }

    private static Set<String> serverIds(final Document settings) {
        final Set<String> ids = new TreeSet<>();
        final NodeList servers = settings.getElementsByTagName("server");
        for (int i = 0; i < servers.getLength(); i++) {
            ids.add(child((Element) servers.item(i), "id", DEFAULT_ID));
        }
        return ids;
    }

    /** The URLs of every repository and plugin repository of one id, in document order. */
    private static List<String> urls(final Document settings, final String id) {
        return repositories(settings.getDocumentElement()).stream()
                .filter(repository -> id.equals(child(repository, "id", DEFAULT_ID)))
                .map(repository -> child(repository, "url", ""))
                .toList();
    }

    /** Every repository and plugin repository below an element, in document order. */
    private static List<Element> repositories(final Element root) {
        final List<Element> found = new ArrayList<>();
        for (final String tag : List.of("repository", "pluginRepository")) {
            final NodeList nodes = root.getElementsByTagName(tag);
            for (int i = 0; i < nodes.getLength(); i++) {
                final Element each = (Element) nodes.item(i);
                // A pom's distributionManagement deploys; it is not read from.
                if (!"distributionManagement".equals(each.getParentNode().getNodeName())) {
                    found.add(each);
                }
            }
        }
        return found;
    }

    /** The text of an element's own child of that name, stripped, or the fallback without one. */
    private static String child(final Element parent, final String name, final String fallback) {
        final Element found = childElement(parent, name);
        return found == null ? fallback : found.getTextContent().strip();
    }

    private static Element childElement(final Element parent, final String name) {
        final NodeList children = parent.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            final Node node = children.item(i);
            if (node instanceof Element element && name.equals(element.getTagName())) {
                return element;
            }
        }
        return null;
    }

    private static Document settings(final String profiles) throws Exception {
        return parse(SETTINGS_HEAD + "<profiles>" + profiles + "</profiles></settings>");
    }

    private static Document pom(final String text) throws Exception {
        return parse(text);
    }

    private static Document parse(final Path file) throws Exception {
        return parse(Files.readString(file));
    }

    private static Document parse(final String text) throws Exception {
        final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        return factory.newDocumentBuilder()
                .parse(new ByteArrayInputStream(text.getBytes(StandardCharsets.UTF_8)));
    }
}
