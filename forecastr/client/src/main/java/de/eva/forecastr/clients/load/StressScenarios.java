package de.eva.forecastr.clients.load;

import com.fasterxml.jackson.databind.JsonNode;
import de.eva.forecastr.rest.CommunicationHandler;
import de.eva.forecastr.rest.RestResponse;
import java.io.PrintStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.http.WebSocket;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.LockSupport;
import java.util.function.IntFunction;

public final class StressScenarios {
  private final CommunicationHandler communicationHandler;
  private final List<Long> seededUsers;
  private final List<Long> seededEvents;
  private final StressExecutor stressExecutor;
  private final StressReportWriter reportWriter;
  private final AtomicLong resolutionChanges;
  private final PrintStream output;
  private String lastFailure;

  public StressScenarios(CommunicationHandler communicationHandler) {
    this(communicationHandler, System.out);
  }

  public StressScenarios(CommunicationHandler communicationHandler, PrintStream output) {
    this.communicationHandler = communicationHandler;
    this.seededUsers = new ArrayList<>();
    this.seededEvents = new ArrayList<>();
    this.stressExecutor = new StressExecutor();
    this.reportWriter = new StressReportWriter();
    this.resolutionChanges = new AtomicLong();
    this.output = output;
  }

  public boolean executeCommand(
      String command, List<String> tokens, int requestCount, int concurrency, Path reportPath)
      throws Exception {
    lastFailure = null;
    return switch (command) {
      case "seed" -> seed(Integer.parseInt(tokens.get(1)), Integer.parseInt(tokens.get(2)));
      case "search" -> simple(command, requestCount, concurrency, reportPath, this::search);
      case "feed" ->
          simple(
              command,
              requestCount,
              concurrency,
              reportPath,
              index -> communicationHandler.get("/feed?limit=" + (10 + index % 40)));
      case "bets" -> bets(Long.parseLong(tokens.get(1)), requestCount, concurrency, reportPath);
      case "overbet" ->
          overbet(
              Long.parseLong(tokens.get(1)),
              Long.parseLong(tokens.get(2)),
              requestCount,
              concurrency,
              reportPath);
      case "resolve" ->
          resolve(Long.parseLong(tokens.get(1)), requestCount, concurrency, reportPath);
      case "accounts" -> accounts(requestCount, concurrency, reportPath);
      case "stats" -> stats(requestCount, concurrency, reportPath);
      case "mixed" -> mixed(requestCount, concurrency, reportPath);
      case "rate" ->
          rate(Integer.parseInt(tokens.get(1)), Integer.parseInt(tokens.get(2)), reportPath);
      case "sockets" -> sockets(requestCount);
      case "threads" -> threads();
      default -> throw new IllegalArgumentException("Unknown command: " + command);
    };
  }

  public String expandToken(String token) {
    if (token.equals("$user")) {
      if (seededUsers.isEmpty()) {
        throw new IllegalStateException("Run seed first");
      }
      return Long.toString(seededUsers.getLast());
    }
    if (token.equals("$event")) {
      if (seededEvents.isEmpty()) {
        throw new IllegalStateException("Run seed first");
      }
      return Long.toString(seededEvents.getLast());
    }
    return token;
  }

  public String getLastFailure() {
    return lastFailure;
  }

  private boolean seed(int userCount, int eventCount) {
    RestResponse response =
        communicationHandler.send(
            "POST",
            "/admin/seed",
            Map.of(
                "users", userCount, "events", eventCount, "balance", new BigDecimal("100000.00")),
            Duration.ofMinutes(5));
    if (!response.isSuccess()) {
      lastFailure = "HTTP " + response.status() + ": " + response.body();
      output.println("FAIL seed: HTTP " + response.status() + " " + response.body());
      return false;
    }
    JsonNode body = communicationHandler.tree(response);
    body.path("userIds").forEach(node -> seededUsers.add(node.asLong()));
    body.path("eventIds").forEach(node -> seededEvents.add(node.asLong()));
    output.printf(
        "PASS seed: %d users, %d events; $user=%s $event=%s%n",
        userCount,
        eventCount,
        seededUsers.isEmpty() ? "n/a" : seededUsers.getLast(),
        seededEvents.isEmpty() ? "n/a" : seededEvents.getLast());
    return true;
  }

  private RestResponse search(int index) {
    String query =
        switch (index % 5) {
          case 0 -> "?name=heute";
          case 1 -> "?status=OPEN";
          case 2 -> "?name=Leipzig&status=OPEN";
          case 3 -> "?status=OPEN&endsBefore=" + Instant.now().plusSeconds(86_400);
          default -> "?name=load&status=OPEN";
        };
    return communicationHandler.get("/events" + query);
  }

  private boolean simple(
      String command,
      int requestCount,
      int concurrency,
      Path reportPath,
      IntFunction<RestResponse> action) {
    List<StressMeasurement> measurements = new ArrayList<>();
    StressReport report =
        stressExecutor.execute(command, requestCount, concurrency, action, measurements);
    reportWriter.write(reportPath, measurements);
    boolean isSuccessful = report.successes() == requestCount;
    printResult(report, isSuccessful, "all requests returned 2xx");
    return isSuccessful;
  }

  private boolean bets(long eventId, int requestCount, int concurrency, Path reportPath) {
    seed(requestCount, 0);
    List<Long> userIds = seededUsers.subList(seededUsers.size() - requestCount, seededUsers.size());
    List<StressMeasurement> measurements = new ArrayList<>();
    StressReport report =
        stressExecutor.execute(
            "bets",
            requestCount,
            concurrency,
            index ->
                communicationHandler.post(
                    "/events/" + eventId + "/bets",
                    Map.of(
                        "userId",
                        userIds.get(index),
                        "outcome",
                        index % 2 == 0 ? "YES" : "NO",
                        "stake",
                        new BigDecimal("1.00"))),
            measurements);
    reportWriter.write(reportPath, measurements);
    BigDecimal finalBalances = BigDecimal.ZERO;
    for (Long userId : userIds) {
      RestResponse balanceResponse = communicationHandler.get("/users/" + userId + "/balance");
      if (balanceResponse.isSuccess()) {
        finalBalances =
            finalBalances.add(
                communicationHandler.tree(balanceResponse).path("balance").decimalValue());
      }
    }
    BigDecimal deducted =
        new BigDecimal("100000.00")
            .multiply(BigDecimal.valueOf(requestCount))
            .subtract(finalBalances);
    boolean isSuccessful =
        report.successes() + report.clientErrors() == requestCount
            && deducted.compareTo(BigDecimal.valueOf(report.successes()).setScale(2)) == 0;
    printResult(
        report,
        isSuccessful,
        "deducted="
            + deducted
            + ", accepted stakes="
            + report.successes()
            + ".00, accepted+rejected="
            + (report.successes() + report.clientErrors()));
    return isSuccessful;
  }

  private boolean overbet(
      long userId, long eventId, int requestCount, int concurrency, Path reportPath) {
    BigDecimal initialBalance = balance(userId);
    BigDecimal stake =
        initialBalance
            .divide(BigDecimal.valueOf(Math.max(2, requestCount / 2L)), 2, RoundingMode.HALF_EVEN)
            .max(new BigDecimal("0.01"));
    List<StressMeasurement> measurements = new ArrayList<>();
    StressReport report =
        stressExecutor.execute(
            "overbet",
            requestCount,
            concurrency,
            index ->
                communicationHandler.post(
                    "/events/" + eventId + "/bets",
                    Map.of("userId", userId, "outcome", "YES", "stake", stake)),
            measurements);
    reportWriter.write(reportPath, measurements);
    BigDecimal finalBalance = balance(userId);
    BigDecimal acceptedStakes = stake.multiply(BigDecimal.valueOf(report.successes()));
    boolean isSuccessful =
        finalBalance.signum() >= 0
            && acceptedStakes.compareTo(initialBalance) <= 0
            && initialBalance.subtract(finalBalance).compareTo(acceptedStakes) == 0;
    printResult(
        report,
        isSuccessful,
        "start="
            + initialBalance
            + ", accepted stakes="
            + acceptedStakes
            + ", final="
            + finalBalance);
    return isSuccessful;
  }

  private boolean resolve(long eventId, int requestCount, int concurrency, Path reportPath) {
    RestResponse beforeResponse = communicationHandler.get("/admin/events/" + eventId + "/audit");
    if (!beforeResponse.isSuccess()) {
      throw new IllegalStateException("Audit endpoint requires the test profile");
    }
    JsonNode before = communicationHandler.tree(beforeResponse);
    List<StressMeasurement> measurements = new ArrayList<>();
    StressReport report =
        stressExecutor.execute(
            "resolve",
            requestCount,
            concurrency,
            index -> {
              RestResponse response =
                  communicationHandler.post(
                      "/admin/events/" + eventId + "/resolve", Map.of("outcome", "YES"));
              if (response.isSuccess()
                  && communicationHandler.tree(response).path("changed").asBoolean()) {
                resolutionChanges.incrementAndGet();
              }
              return response;
            },
            measurements);
    reportWriter.write(reportPath, measurements);
    long changedBodies = resolutionChanges.getAndSet(0);
    RestResponse afterResponse = communicationHandler.get("/admin/events/" + eventId + "/audit");
    JsonNode after = communicationHandler.tree(afterResponse);
    JsonNode event = after.path("event");
    String status = event.path("status").asText();
    boolean isResolved = status.startsWith("RESOLVED");
    boolean hasExactPayouts =
        isResolved
            && verifyPayouts(
                before.path("bets"), after.path("bets"), status.endsWith("YES") ? "YES" : "NO");
    boolean isSuccessful =
        report.successes() == requestCount && changedBodies == 1 && isResolved && hasExactPayouts;
    printResult(
        report,
        isSuccessful,
        "changed=true responses="
            + changedBodies
            + ", resolved="
            + isResolved
            + ", exact gross/fee/net and one final state per bet="
            + hasExactPayouts);
    return isSuccessful;
  }

  private boolean verifyPayouts(JsonNode before, JsonNode after, String winningOutcome) {
    if (before.size() != after.size()) {
      return false;
    }
    Map<Long, JsonNode> finalBets = new HashMap<>();
    after.forEach(bet -> finalBets.put(bet.path("id").asLong(), bet));
    List<JsonNode> winners = new ArrayList<>();
    BigDecimal totalPool = BigDecimal.ZERO;
    BigDecimal winningPool = BigDecimal.ZERO;
    for (JsonNode bet : before) {
      BigDecimal stake = bet.path("stake").decimalValue();
      totalPool = totalPool.add(stake);
      if (bet.path("outcome").asText().equals(winningOutcome)) {
        winners.add(bet);
        winningPool = winningPool.add(stake);
      }
    }
    winners.sort(Comparator.comparingLong(bet -> bet.path("id").asLong()));
    if (winners.isEmpty()) {
      return after.findValuesAsText("status").stream().allMatch("LOST"::equals);
    }
    record Allocation(JsonNode bet, BigDecimal exact, BigDecimal gross) {}
    List<Allocation> allocations = new ArrayList<>();
    BigDecimal allocated = new BigDecimal("0.00");
    for (JsonNode bet : winners) {
      BigDecimal exact =
          totalPool
              .multiply(bet.path("stake").decimalValue())
              .divide(winningPool, 12, RoundingMode.HALF_EVEN);
      BigDecimal gross = exact.setScale(2, RoundingMode.DOWN);
      allocations.add(new Allocation(bet, exact, gross));
      allocated = allocated.add(gross);
    }
    int remainderCents =
        totalPool
            .setScale(2, RoundingMode.HALF_EVEN)
            .subtract(allocated)
            .movePointRight(2)
            .intValueExact();
    List<Allocation> remainderOrder =
        allocations.stream()
            .sorted(
                Comparator.comparing(
                        (Allocation allocation) -> allocation.exact().subtract(allocation.gross()))
                    .reversed()
                    .thenComparing(
                        allocation -> allocation.bet().path("id").asLong(),
                        Comparator.reverseOrder()))
            .toList();
    Map<Long, BigDecimal> grossByBet = new HashMap<>();
    allocations.forEach(
        allocation -> grossByBet.put(allocation.bet().path("id").asLong(), allocation.gross()));
    for (int i = 0; i < remainderCents; i++) {
      long betId = remainderOrder.get(i).bet().path("id").asLong();
      grossByBet.compute(betId, (key, gross) -> gross.add(new BigDecimal("0.01")));
    }
    Map<Long, BigDecimal[]> expected = new HashMap<>();
    for (JsonNode bet : winners) {
      BigDecimal stake = bet.path("stake").decimalValue();
      BigDecimal gross = grossByBet.get(bet.path("id").asLong());
      BigDecimal fee =
          gross
              .subtract(stake)
              .max(BigDecimal.ZERO)
              .multiply(new BigDecimal("0.05"))
              .setScale(2, RoundingMode.HALF_EVEN);
      expected.put(
          bet.path("id").asLong(), new BigDecimal[] {gross.subtract(fee).setScale(2), fee});
    }
    for (JsonNode original : before) {
      JsonNode actual = finalBets.get(original.path("id").asLong());
      if (actual == null) {
        return false;
      }
      boolean hasWon = original.path("outcome").asText().equals(winningOutcome);
      if (hasWon) {
        BigDecimal[] expectedAmounts = expected.get(original.path("id").asLong());
        if (!actual.path("status").asText().equals("WON")
            || actual.path("payoutAmount").decimalValue().compareTo(expectedAmounts[0]) != 0
            || actual.path("feeAmount").decimalValue().compareTo(expectedAmounts[1]) != 0) {
          return false;
        }
      } else if (!actual.path("status").asText().equals("LOST")) {
        return false;
      }
    }
    return true;
  }

  private boolean accounts(int requestCount, int concurrency, Path reportPath) {
    String nonce = Long.toUnsignedString(System.nanoTime());
    List<StressMeasurement> measurements = new ArrayList<>();
    AtomicReference<String> firstFailure = new AtomicReference<>();
    StressReport report =
        stressExecutor.execute(
            "accounts",
            requestCount,
            concurrency,
            index -> {
              RestResponse createResponse =
                  communicationHandler.post(
                      "/users",
                      Map.of(
                          "username",
                          "cycle-" + nonce + "-" + index,
                          "initialBalance",
                          new BigDecimal("10.00")));
              if (!createResponse.isSuccess()) {
                rememberFailure(firstFailure, "create", createResponse);
                return createResponse;
              }
              long userId = communicationHandler.tree(createResponse).path("id").asLong();
              RestResponse readResponse = communicationHandler.get("/users/" + userId);
              if (!readResponse.isSuccess()) {
                rememberFailure(firstFailure, "read", readResponse);
                return readResponse;
              }
              RestResponse updateResponse =
                  communicationHandler.put(
                      "/users/" + userId, Map.of("username", "cycled-" + nonce + "-" + index));
              if (!updateResponse.isSuccess()) {
                rememberFailure(firstFailure, "update", updateResponse);
                return updateResponse;
              }
              RestResponse depositResponse =
                  communicationHandler.post(
                      "/users/" + userId + "/deposit", Map.of("amount", new BigDecimal("5.00")));
              if (!depositResponse.isSuccess()) {
                rememberFailure(firstFailure, "deposit", depositResponse);
                return depositResponse;
              }
              RestResponse withdrawResponse =
                  communicationHandler.post(
                      "/users/" + userId + "/withdraw", Map.of("amount", new BigDecimal("3.00")));
              if (!withdrawResponse.isSuccess()) {
                rememberFailure(firstFailure, "withdraw", withdrawResponse);
                return withdrawResponse;
              }
              RestResponse balanceResponse =
                  communicationHandler.get("/users/" + userId + "/balance");
              if (!balanceResponse.isSuccess()) {
                rememberFailure(firstFailure, "balance", balanceResponse);
                return new RestResponse(
                    500, "inconsistent final balance", balanceResponse.latencyNanos());
              }
              BigDecimal finalBalance =
                  communicationHandler.tree(balanceResponse).path("balance").decimalValue();
              if (finalBalance.compareTo(new BigDecimal("12.00")) != 0) {
                firstFailure.compareAndSet(
                    null, "balance was EUR " + finalBalance + " instead of EUR 12.00");
                return new RestResponse(
                    500, "inconsistent final balance", balanceResponse.latencyNanos());
              }
              return balanceResponse;
            },
            measurements);
    reportWriter.write(reportPath, measurements);
    boolean isSuccessful = report.successes() == requestCount;
    String check =
        isSuccessful
            ? "every cycle ended at EUR 12.00"
            : String.format(
                Locale.ROOT,
                "completed=%d/%d, failed=%d; first failure: %s",
                report.successes(),
                requestCount,
                requestCount - report.successes(),
                firstFailure.get() == null ? "unknown" : firstFailure.get());
    printResult(report, isSuccessful, check);
    return isSuccessful;
  }

  private void rememberFailure(
      AtomicReference<String> firstFailure, String operation, RestResponse response) {
    String body = response.body() == null ? "" : response.body().replaceAll("\\s+", " ").trim();
    firstFailure.compareAndSet(
        null,
        operation
            + " returned HTTP "
            + response.status()
            + (body.isEmpty() ? "" : " (" + body + ")"));
  }

  private boolean stats(int requestCount, int concurrency, Path reportPath) {
    if (seededEvents.isEmpty()) {
      seed(200, 1);
    }
    long eventId = seededEvents.getLast();
    List<Long> userIds =
        seededUsers.subList(
            Math.max(0, seededUsers.size() - Math.min(200, seededUsers.size())),
            seededUsers.size());
    try (ExecutorService backgroundExecutor = Executors.newVirtualThreadPerTaskExecutor()) {
      for (int i = 0; i < Math.min(1000, requestCount); i++) {
        int requestIndex = i;
        backgroundExecutor.submit(
            () ->
                communicationHandler.post(
                    "/events/" + eventId + "/bets",
                    Map.of(
                        "userId",
                        userIds.get(requestIndex % userIds.size()),
                        "outcome",
                        "YES",
                        "stake",
                        new BigDecimal("0.01"))));
      }
      List<StressMeasurement> measurements = new ArrayList<>();
      StressReport report =
          stressExecutor.execute(
              "stats",
              requestCount,
              concurrency,
              index -> checkedStats(communicationHandler.get("/stats")),
              measurements);
      reportWriter.write(reportPath, measurements);
      boolean isSuccessful = report.successes() == requestCount;
      printResult(
          report,
          isSuccessful,
          "stats stayed available and internally consistent during background bets");
      return isSuccessful;
    }
  }

  private RestResponse checkedStats(RestResponse response) {
    if (!response.isSuccess()) {
      return response;
    }
    JsonNode stats = communicationHandler.tree(response);
    long eventCount = stats.path("events").asLong(-1);
    long betCount = stats.path("bets").asLong(-1);
    long statusCount = 0;
    long userBetCount = 0;
    for (JsonNode value : stats.path("eventsByStatus")) {
      statusCount += value.asLong();
    }
    for (JsonNode value : stats.path("perUserBetCount")) {
      userBetCount += value.asLong();
    }
    boolean isConsistent =
        eventCount >= 0
            && betCount >= 0
            && statusCount == eventCount
            && userBetCount == betCount
            && stats.path("payouts").decimalValue().signum() >= 0
            && stats.path("fees").decimalValue().signum() >= 0;
    return isConsistent
        ? response
        : new RestResponse(500, "inconsistent stats snapshot", response.latencyNanos());
  }

  private boolean mixed(int requestCount, int concurrency, Path reportPath) {
    if (seededUsers.size() < Math.max(100, requestCount / 4)) {
      seed(Math.max(100, requestCount / 4), 1);
    } else {
      seed(0, 1);
    }
    long eventId = seededEvents.getLast();
    String nonce = Long.toUnsignedString(System.nanoTime());
    List<StressMeasurement> measurements = new ArrayList<>();
    StressReport report =
        stressExecutor.execute(
            "mixed",
            requestCount,
            concurrency,
            index -> {
              int bucket = index % 100;
              if (bucket < 70) {
                return bucket % 2 == 0
                    ? communicationHandler.get("/feed?limit=20")
                    : communicationHandler.get("/events?status=OPEN");
              }
              if (bucket < 95) {
                return communicationHandler.post(
                    "/events/" + eventId + "/bets",
                    Map.of(
                        "userId",
                        seededUsers.get(index % seededUsers.size()),
                        "outcome",
                        index % 2 == 0 ? "YES" : "NO",
                        "stake",
                        new BigDecimal("0.01")));
              }
              return communicationHandler.post(
                  "/users",
                  Map.of(
                      "username",
                      "mixed-" + nonce + "-" + index,
                      "initialBalance",
                      BigDecimal.ZERO));
            },
            measurements);
    reportWriter.write(reportPath, measurements);
    boolean isSuccessful = report.errorRate() < 0.1;
    printResult(
        report,
        isSuccessful,
        String.format(Locale.ROOT, "error rate %.4f%% (target <0.1%%)", report.errorRate()));
    return isSuccessful;
  }

  private boolean rate(int requestsPerSecond, int seconds, Path reportPath) {
    RestResponse warmup = warmUpRateConnection();
    if (!warmup.isSuccess()) {
      lastFailure = "warm-up returned HTTP " + warmup.status() + ": " + warmup.body();
      output.println("FAIL rate: " + lastFailure);
      return false;
    }
    int requestCount = Math.multiplyExact(requestsPerSecond, seconds);
    List<StressMeasurement> measurements =
        Collections.synchronizedList(new ArrayList<>(requestCount));
    long startedAt = System.nanoTime();
    try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
      List<Future<?>> futures = new ArrayList<>(requestCount);
      for (int i = 0; i < requestCount; i++) {
        long target = startedAt + (long) (i * (1_000_000_000d / requestsPerSecond));
        int index = i;
        futures.add(
            executor.submit(
                () -> {
                  long wait = target - System.nanoTime();
                  if (wait > 0) {
                    LockSupport.parkNanos(wait);
                  }
                  RestResponse response =
                      index % 4 == 0
                          ? communicationHandler.get("/stats")
                          : communicationHandler.get("/feed?limit=20");
                  measurements.add(
                      new StressMeasurement(
                          index, response.status(), response.latencyNanos(), "rate"));
                }));
      }
      awaitRateRequests(futures);
    }
    StressReport report =
        stressExecutor.report("rate", measurements, System.nanoTime() - startedAt);
    reportWriter.write(reportPath, measurements);
    long slowResponses =
        Arrays.stream(report.latencies()).filter(latency -> latency > 200_000_000L).count();
    boolean isSuccessful = report.errorRate() < 0.1 && slowResponses == 0;
    printResult(
        report,
        isSuccessful,
        "responses >200ms="
            + slowResponses
            + ", error rate="
            + String.format(Locale.ROOT, "%.4f%%", report.errorRate()));
    return isSuccessful;
  }

  private RestResponse warmUpRateConnection() {
    RestResponse response = null;
    for (int index = 0; index < 10; index++) {
      response =
          index % 4 == 0
              ? communicationHandler.get("/stats")
              : communicationHandler.get("/feed?limit=20");
      if (!response.isSuccess()) {
        return response;
      }
    }
    return response;
  }

  private boolean sockets(int subscriberCount) throws Exception {
    if (seededUsers.isEmpty()) {
      seed(1, 0);
    }
    long userId = seededUsers.getLast();
    CountDownLatch connected = new CountDownLatch(subscriberCount * 2);
    CountDownLatch feedMessages = new CountDownLatch(subscriberCount);
    CountDownLatch notifications = new CountDownLatch(subscriberCount);
    List<Long> arrivals = Collections.synchronizedList(new ArrayList<>());
    List<CompletableFuture<WebSocket>> connections = new ArrayList<>(subscriberCount * 2);
    for (int i = 0; i < subscriberCount; i++) {
      connections.add(
          communicationHandler.subscribe(
              "/topic/feed",
              message -> {
                if (message.contains("SUBSCRIBED")) {
                  connected.countDown();
                }
                if (message.contains("\"type\":\"FEED\"")) {
                  arrivals.add(System.nanoTime());
                  feedMessages.countDown();
                }
              }));
      connections.add(
          communicationHandler.subscribe(
              "/topic/users/" + userId,
              message -> {
                if (message.contains("SUBSCRIBED")) {
                  connected.countDown();
                }
                if (message.contains("\"type\":\"NOTIFICATION\"")) {
                  arrivals.add(System.nanoTime());
                  notifications.countDown();
                }
              }));
    }
    CompletableFuture.allOf(connections.toArray(CompletableFuture[]::new))
        .get(20, TimeUnit.SECONDS);
    List<WebSocket> sockets = connections.stream().map(CompletableFuture::join).toList();
    if (!connected.await(20, TimeUnit.SECONDS)) {
      throw new IllegalStateException("Subscribers did not connect");
    }
    long startedAt = System.nanoTime();
    seed(0, 1);
    long eventId = seededEvents.getLast();
    communicationHandler.post(
        "/events/" + eventId + "/bets",
        Map.of("userId", userId, "outcome", "YES", "stake", new BigDecimal("1.00")));
    communicationHandler.post("/admin/events/" + eventId + "/resolve", Map.of("outcome", "YES"));
    boolean isFeedTimely = feedMessages.await(1, TimeUnit.SECONDS);
    boolean isNotificationTimely = notifications.await(2, TimeUnit.SECONDS);
    for (WebSocket socket : sockets) {
      socket.sendClose(WebSocket.NORMAL_CLOSURE, "done");
    }
    long maximumLatency =
        arrivals.stream().mapToLong(arrival -> arrival - startedAt).max().orElse(Long.MAX_VALUE);
    boolean isSuccessful = isFeedTimely && isNotificationTimely && maximumLatency <= 2_000_000_000L;
    if (!isSuccessful) {
      lastFailure =
          "subscribers="
              + subscriberCount
              + ", feed<=1s="
              + isFeedTimely
              + ", notification<=2s="
              + isNotificationTimely
              + ", max="
              + String.format(Locale.ROOT, "%.2fms", maximumLatency / 1e6);
    }
    output.printf(
        "%s sockets: subscribers=%d feed<=1s=%s notification<=2s=%s max=%.2fms%n",
        isSuccessful ? "PASS" : "FAIL",
        subscriberCount,
        isFeedTimely,
        isNotificationTimely,
        maximumLatency / 1e6);
    return isSuccessful;
  }

  private boolean threads() {
    RestResponse response = communicationHandler.get("/logs?limit=10000");
    if (!response.isSuccess()) {
      lastFailure = response.body();
      output.println("FAIL threads: " + response.body());
      return false;
    }
    Set<String> threadNames = new TreeSet<>();
    communicationHandler
        .array(response)
        .forEach(
            node -> {
              String threadName = node.path("threadName").asText();
              if (threadName.startsWith("http-") || threadName.startsWith("forecastr-worker-")) {
                threadNames.add(threadName);
              }
            });
    boolean isSuccessful = threadNames.size() > 1;
    if (!isSuccessful) {
      lastFailure = "distinct server threads=" + threadNames.size() + " (expected >1)";
    }
    output.println(
        (isSuccessful ? "PASS" : "FAIL")
            + " threads: distinct="
            + threadNames.size()
            + " sample="
            + threadNames.stream().limit(10).toList());
    return isSuccessful;
  }

  private BigDecimal balance(long userId) {
    RestResponse response = communicationHandler.get("/users/" + userId + "/balance");
    if (!response.isSuccess()) {
      throw new IllegalStateException(response.body());
    }
    return communicationHandler.tree(response).path("balance").decimalValue();
  }

  private void awaitRateRequests(List<Future<?>> futures) {
    for (Future<?> future : futures) {
      try {
        future.get();
      } catch (InterruptedException exception) {
        Thread.currentThread().interrupt();
        throw new IllegalStateException("Rate test was interrupted", exception);
      } catch (Exception exception) {
        throw new IllegalStateException("Rate request failed", exception);
      }
    }
  }

  private void printResult(StressReport report, boolean isSuccessful, String check) {
    if (!isSuccessful) {
      lastFailure = check;
    }
    output.println(report.summary());
    output.println((isSuccessful ? "PASS" : "FAIL") + " post-condition: " + check);
  }
}
