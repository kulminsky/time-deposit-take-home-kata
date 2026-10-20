package org.ikigaidigital;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.json.JsonCompareMode;
import org.springframework.test.web.servlet.client.RestTestClient;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import org.yaml.snakeyaml.Yaml;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * End to end: real HTTP calls against the running application and Postgres (Testcontainers).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureRestTestClient
@Import(TestcontainersConfiguration.class)
@Sql({"/db/clean.sql", "/db/test-data.sql"})
class TimeDepositApiIT {

    private static final Set<String> HTTP_METHODS = Set.of("get", "put", "post", "delete", "patch", "head", "options", "trace");

    @Autowired
    private RestTestClient client;

    @Autowired
    private RequestMappingHandlerMapping handlerMapping;

    @Autowired
    private ApplicationContext applicationContext;

    @Test
    void listsAllTimeDepositsWithTheirWithdrawals() {
        client.get().uri("/time-deposits")
            .exchange()
            .expectStatus().isOk()
            .expectBody().json("""
                [
                  {"id": 1, "planType": "basic", "balance": 1000.00, "days": 31, "withdrawals": [
                    {"id": 1, "amount": 50.25, "date": "2026-01-15"},
                    {"id": 2, "amount": 100.00, "date": "2026-02-01"},
                    {"id": 4, "amount": 10.00, "date": "2026-02-01"}]},
                  {"id": 2, "planType": "student", "balance": 1000.00, "days": 365, "withdrawals": []},
                  {"id": 3, "planType": "student", "balance": 1000.00, "days": 366, "withdrawals": []},
                  {"id": 4, "planType": "premium", "balance": 1000.00, "days": 45, "withdrawals": []},
                  {"id": 5, "planType": "premium", "balance": 1000.00, "days": 46, "withdrawals": [
                    {"id": 3, "amount": 200.00, "date": "2026-03-01"}]}
                ]
                """, JsonCompareMode.STRICT);
    }

    @Test
    void updateAddsOneMonthOfInterestToEveryBalance() {
        client.post().uri("/time-deposits/balance-updates")
            .exchange()
            .expectStatus().isNoContent()
            .expectBody().isEmpty();

        client.get().uri("/time-deposits")
            .exchange()
            .expectStatus().isOk()
            .expectBody()
            .jsonPath("$[0].balance").isEqualTo(1000.83)  // basic, 31 days
            .jsonPath("$[1].balance").isEqualTo(1002.50)  // student, 365 days
            .jsonPath("$[2].balance").isEqualTo(1000.00)  // student, 366 days: no interest after one year
            .jsonPath("$[3].balance").isEqualTo(1000.00)  // premium, 45 days: interest starts after 45 days
            .jsonPath("$[4].balance").isEqualTo(1004.17)  // premium, 46 days
            .jsonPath("$[0].withdrawals.length()").isEqualTo(3);
    }

    @Test
    void everyUpdateAddsAnotherMonth() {
        client.post().uri("/time-deposits/balance-updates").exchange().expectStatus().isNoContent();
        client.post().uri("/time-deposits/balance-updates").exchange().expectStatus().isNoContent();

        client.get().uri("/time-deposits")
            .exchange()
            .expectBody()
            .jsonPath("$[0].balance").isEqualTo(1001.66)
            .jsonPath("$[4].balance").isEqualTo(1008.35);
    }

    @Test
    void applicationHasExactlyTheTwoEndpointsOfTheContract() {
        List<String> applicationEndpoints = handlerMapping.getHandlerMethods().entrySet().stream()
            .filter(entry -> isApplicationHandler(entry.getValue()))
            .flatMap(entry -> endpoints(entry.getKey()))
            .sorted()
            .toList();

        assertThat(applicationEndpoints).containsExactly(
            "GET /time-deposits",
            "POST /time-deposits/balance-updates");
        assertThat(applicationContext.getBeanNamesForType(RouterFunction.class)).isEmpty();
    }

    @Test
    @SuppressWarnings("unchecked")
    void contractDefinesExactlyTwoOperations() {
        String contract = client.get().uri("/openapi/time-deposits.yaml")
            .exchange()
            .expectStatus().isOk()
            .returnResult(String.class).getResponseBody();

        Map<String, Map<String, Object>> paths = (Map<String, Map<String, Object>>) new Yaml().<Map<String, Object>>load(contract).get("paths");
        long operations = paths.values().stream()
            .flatMap(pathItem -> pathItem.keySet().stream())
            .filter(HTTP_METHODS::contains)
            .count();
        assertThat(operations).isEqualTo(2);
    }

    @Test
    void swaggerUiShowsTheContract() {
        String page = client.get().uri("/swagger-ui/index.html")
            .exchange()
            .expectStatus().isOk()
            .returnResult(String.class).getResponseBody();
        assertThat(page).contains("url: '../openapi/time-deposits.yaml'");

        client.get().uri("/webjars/swagger-ui/swagger-ui-bundle.js").exchange().expectStatus().isOk();
        client.get().uri("/webjars/swagger-ui/swagger-ui.css").exchange().expectStatus().isOk();
    }

    @Test
    void doesNotServeADocumentGeneratedFromTheCode() {
        client.get().uri("/v3/api-docs").exchange().expectStatus().isNotFound();
    }

    private static boolean isApplicationHandler(HandlerMethod handlerMethod) {
        return handlerMethod.getBeanType().getPackageName().startsWith("org.ikigaidigital");
    }

    // One entry per HTTP method and path of the mapping; a mapping without methods accepts any method.
    private static Stream<String> endpoints(RequestMappingInfo mapping) {
        Set<RequestMethod> methods = mapping.getMethodsCondition().getMethods();
        List<String> methodNames = methods.isEmpty()
            ? List.of("ANY")
            : methods.stream().map(RequestMethod::name).toList();
        return methodNames.stream().flatMap(method -> mapping.getPathPatternsCondition().getPatternValues().stream()
            .map(path -> method + " " + path));
    }
}
