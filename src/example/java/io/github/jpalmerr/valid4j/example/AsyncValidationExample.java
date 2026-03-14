package io.github.jpalmerr.valid4j.example;

import io.github.jpalmerr.valid4j.NonEmptyList;
import io.github.jpalmerr.valid4j.Validated;
import io.github.jpalmerr.valid4j.ValidatedNel;
import java.time.Duration;
import java.time.Instant;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;

/**
 * Demonstrates concurrent validation with Java 21 virtual threads.
 *
 * <p>valid4j is synchronous by design — {@code combine} takes already-computed {@link Validated}
 * values and merges them. When validations involve I/O, Java evaluates method arguments
 * left-to-right before calling {@code combine}, so I/O-based validations run sequentially by
 * default. Virtual threads solve this: submit each validation to an executor, let them run
 * concurrently, then pass the results to {@code combine}.
 *
 * <p>Run with: {@code ./gradlew runAsyncExample}
 */
public class AsyncValidationExample {

  // -------------------------------------------------------------------------
  // Domain types
  // -------------------------------------------------------------------------

  record Registration(String name, String email, String address) {}

  // -------------------------------------------------------------------------
  // Simulated I/O validators
  //
  // In production these would call a database, HTTP service, etc.
  // Thread.sleep simulates the latency to prove concurrency.
  // -------------------------------------------------------------------------

  /** Simulates a database uniqueness check (~400ms). */
  static Validated<NonEmptyList<String>, String> validateEmailUnique(String email) {
    sleep(400);
    Set<String> taken = Set.of("taken@example.com", "admin@example.com");
    if (taken.contains(email.toLowerCase())) {
      return ValidatedNel.invalidNel("Email already registered: " + email);
    }
    return ValidatedNel.validNel(email);
  }

  /** Simulates an HTTP address verification service (~300ms). */
  static Validated<NonEmptyList<String>, String> validateAddress(String raw) {
    sleep(300);
    if (raw == null || raw.isBlank()) {
      return ValidatedNel.invalidNel("Address verification failed: empty input");
    }
    if (!raw.contains(",")) {
      return ValidatedNel.invalidNel("Address must include city (use comma separator)");
    }
    return ValidatedNel.validNel(raw.trim());
  }

  /** Pure validation — no I/O, instant. */
  static Validated<NonEmptyList<String>, String> validateName(String name) {
    if (name == null || name.isBlank()) return ValidatedNel.invalidNel("Name is required");
    if (name.length() < 2) return ValidatedNel.invalidNel("Name must be at least 2 characters");
    return ValidatedNel.validNel(name.trim());
  }

  // -------------------------------------------------------------------------
  // Sequential (baseline) — I/O validators run one after another
  // -------------------------------------------------------------------------

  static Validated<NonEmptyList<String>, Registration> registerSequential(
      String name, String email, String address) {
    return ValidatedNel.combine(
        validateName(name),
        validateEmailUnique(email),
        validateAddress(address),
        Registration::new);
  }

  // -------------------------------------------------------------------------
  // Concurrent — virtual threads run I/O validators in parallel
  // -------------------------------------------------------------------------

  static Validated<NonEmptyList<String>, Registration> registerConcurrent(
      String name, String email, String address) throws InterruptedException, ExecutionException {
    try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
      var nameResult = executor.submit(() -> validateName(name));
      var emailResult = executor.submit(() -> validateEmailUnique(email));
      var addressResult = executor.submit(() -> validateAddress(address));

      return ValidatedNel.combine(
          nameResult.get(), emailResult.get(), addressResult.get(), Registration::new);
    }
  }

  // -------------------------------------------------------------------------
  // Main — compare sequential vs concurrent
  // -------------------------------------------------------------------------

  public static void main(String[] args) throws Exception {
    String name = "";
    String email = "taken@example.com";
    String address = "bad";

    // --- Sequential ---
    System.out.println("=== Sequential validation ===");
    Instant seqStart = Instant.now();
    var seqResult = registerSequential(name, email, address);
    Duration seqTime = Duration.between(seqStart, Instant.now());
    printResult(seqResult);
    System.out.println("Time: " + seqTime.toMillis() + "ms (expect ~700ms: 400 + 300)");

    System.out.println();

    // --- Concurrent ---
    System.out.println("=== Concurrent validation (virtual threads) ===");
    Instant conStart = Instant.now();
    var conResult = registerConcurrent(name, email, address);
    Duration conTime = Duration.between(conStart, Instant.now());
    printResult(conResult);
    System.out.println("Time: " + conTime.toMillis() + "ms (expect ~400ms: max of parallel calls)");

    System.out.println();

    // --- Concurrent, all valid ---
    System.out.println("=== Concurrent validation (all valid) ===");
    var validResult = registerConcurrent("Alice", "alice@example.com", "123 Main St, London");
    printResult(validResult);

    System.out.println();

    // --- Errors accumulate across both sync and async validators ---
    System.out.println("=== All three fail concurrently ===");
    var allBad = registerConcurrent("", "admin@example.com", "");
    printResult(allBad);
  }

  // -------------------------------------------------------------------------
  // Helpers
  // -------------------------------------------------------------------------

  private static void printResult(Validated<NonEmptyList<String>, Registration> result) {
    switch (result) {
      case Validated.Valid<NonEmptyList<String>, Registration>(var reg) ->
          System.out.println("Valid: " + reg.name() + " <" + reg.email() + "> at " + reg.address());
      case Validated.Invalid<NonEmptyList<String>, Registration>(var errors) -> {
        System.out.println("Invalid (" + errors.size() + " errors):");
        for (String error : errors) {
          System.out.println("  - " + error);
        }
      }
    }
  }

  private static void sleep(long millis) {
    try {
      Thread.sleep(millis);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }
}
