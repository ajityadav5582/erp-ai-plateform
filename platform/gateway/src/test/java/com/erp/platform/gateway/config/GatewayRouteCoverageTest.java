package com.erp.platform.gateway.config;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.gateway.config.GatewayProperties;
import org.springframework.test.context.TestPropertySource;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Stops a service from being deployed and unreachable.
 *
 * <p><strong>The incident this exists because of.</strong> The procurement route
 * was present in {@code application.yml} and {@code application-local.yml} in the
 * repository, but the deployed gateway image had been built before it was added
 * and registered only three routes: inventory, identity and the health
 * passthrough. Every call to {@code /api/v1/procurement/**} therefore matched no
 * route at all.
 *
 * <p>The symptom was a CORS error, which sends people looking at CORS
 * configuration that is already correct. What actually happened is that Spring
 * WebFlux answers a request matching no route with {@code 403 Forbidden}, and
 * that 403 is produced before any route's CORS handling runs - so it carries
 * {@code Vary} but no {@code Access-Control-Allow-Origin}. A browser reports a
 * missing allow-origin header as a CORS failure regardless of what caused it.
 * The error named the wrong subsystem entirely.
 *
 * <p><strong>Why this test rather than per-service CORS config.</strong> The
 * tempting fix is to let each service emit its own CORS headers, so it works
 * whether or not the gateway knows about it. That trades one real problem for two
 * worse ones: the browser now has to satisfy two hops' policies, and a service
 * reachable both through the gateway and directly ends up with two competing
 * policies and no defined precedence. CORS stays on the gateway, which is the one
 * origin the browser ever talks to.
 *
 * <p>What is centralised here is not CORS - it is the <em>route table</em>. This
 * test derives the routes that must exist from {@code docker-compose.yml}, the
 * file that already declares what is deployed, instead of from a list somebody has
 * to remember to update by hand. Adding a service to compose and forgetting the
 * gateway now fails {@code ./gradlew build} with the service named, rather than
 * producing a 403 that a browser misreports as CORS.
 *
 * <p>Covers every present and future service automatically. There is nothing to
 * add here per module, which is the point: the day a new service needs this, the
 * guard already applies to it.
 */
@SpringBootTest
@TestPropertySource(properties = {
        "spring.config.import=optional:classpath:application.yml",
        "spring.profiles.active=local"
})
class GatewayRouteCoverageTest {

    @Autowired
    private GatewayProperties gatewayProperties;

    private static Set<String> deployedServices;

    /**
     * Reads the deployed service names once.
     *
     * <p>Walks up from the working directory rather than hard-coding a relative
     * path, because a Gradle test's working directory is the module directory and
     * the depth from there to the repository root is a layout detail that should
     * not be duplicated into an assertion.
     */
    @BeforeAll
    static void readComposeFile() {
        Path compose = locateRepositoryRoot().resolve("docker-compose.yml");
        assertThat(compose)
                .as("docker-compose.yml must exist; it is the source of truth for which "
                        + "services are deployed and therefore must be routed")
                .exists();

        Map<String, Object> root;
        try {
            root = new Yaml().load(Files.readString(compose));
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot read " + compose, e);
        }

        Object services = root.get("services");
        assertThat(services).as("docker-compose.yml must declare services").isInstanceOf(Map.class);

        deployedServices = ((Map<?, ?>) services).keySet().stream()
                .map(String::valueOf)
                // The gateway routes to services; it does not route to itself. Infrastructure
                // containers such as postgres do not carry the -service suffix.
                .filter(name -> name.endsWith("-service"))
                .collect(Collectors.toCollection(LinkedHashSet::new));

        assertThat(deployedServices)
                .as("at least one deployable service must be found in docker-compose.yml")
                .isNotEmpty();
    }

    @Test
    @DisplayName("every deployed service has a gateway route")
    void everyDeployedServiceIsRouted() {
        Set<String> missing = deployedServices.stream()
                .map(GatewayRouteCoverageTest::expectedPathFor)
                .filter(path -> !configuredPaths().contains(path))
                .collect(Collectors.toCollection(LinkedHashSet::new));

        assertThat(missing)
                .as("""
                        Every service in docker-compose.yml must be reachable through the gateway.

                        These paths have no route, so requests to them are answered with 403 by \
                        WebFlux - which a browser reports as a CORS error, because the 403 is \
                        emitted before any CORS header is attached.

                        Add the route to BOTH application.yml and application-local.yml under \
                        spring.cloud.gateway.routes, or drop the service from compose if it is \
                        not meant to be exposed.
                        """)
                .isEmpty();
    }

    @Test
    @DisplayName("every gateway route points at a service that is actually deployed")
    void everyRoutePointsAtADeployedService() {
        Set<String> unroutableHosts = gatewayProperties.getRoutes().stream()
                .map(route -> route.getUri().getHost())
                .filter(host -> host != null)
                .filter(host -> !host.equals("localhost") && !host.equals("127.0.0.1"))
                .filter(host -> !deployedServices.contains(host))
                .collect(Collectors.toCollection(LinkedHashSet::new));

        assertThat(unroutableHosts)
                .as("""
                        These routes name a host that is not a service in docker-compose.yml, so \
                        they resolve to nothing and fail at request time instead of at build time.

                        Either the host is misspelled, or the service needs adding to compose.
                        """)
                .isEmpty();
    }

    @Test
    @DisplayName("the local profile routes every service, since that is what containers run")
    void localProfileRoutesEveryService() {
        // The deployed gateway runs with SPRING_PROFILES_ACTIVE=local. Spring merges
        // profile-specific YAML over the default one, and a `routes:` list is
        // replaced wholesale rather than merged element-wise - so the local file
        // must repeat every route, not just the ones that differ. A route added
        // only to application.yml never reaches a deployed gateway, which is exactly
        // how procurement was present in the repository and absent from the running
        // service.
        assertThat(configuredPaths())
                .as("paths routed under the local profile, which is what the container runs")
                .containsAll(deployedServices.stream()
                        .map(GatewayRouteCoverageTest::expectedPathFor)
                        .collect(Collectors.toSet()));
    }

    /** {@code procurement-service} is exposed at {@code /api/v1/procurement/**}. */
    private static String expectedPathFor(String serviceName) {
        return "/api/v1/" + serviceName.substring(0, serviceName.length() - "-service".length()) + "/**";
    }

    /** Every path the configured routes will match, read off their predicates. */
    private Set<String> configuredPaths() {
        return gatewayProperties.getRoutes().stream()
                .flatMap(route -> route.getPredicates().stream())
                .flatMap(predicate -> predicate.getArgs().values().stream())
                .map(String::valueOf)
                .filter(value -> value.startsWith("/api/v1/"))
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private static Path locateRepositoryRoot() {
        Path candidate = Path.of(System.getProperty("user.dir")).toAbsolutePath();
        while (candidate != null && !Files.exists(candidate.resolve("docker-compose.yml"))) {
            candidate = candidate.getParent();
        }
        assertThat(candidate)
                .as("could not walk up from the working directory to the repository root")
                .isNotNull();
        return candidate;
    }
}
