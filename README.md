# Tidewater Freight: Shipping Cost Service

A small service that works out the cost of a shipment from its weight, destination zone and service level.

- **[`ANALYSIS.md`](ANALYSIS.md):** what was wrong with the original code and tests, written before any change. Problems are referred to below by their IDs there (C1–C11 for the code, T1–T3 and S1–S5 for the tests).
- **This README:** how to run the service, how it's built, and why.

---

## Running it

You need JDK 21 and Maven.

```bash
mvn compile
mvn exec:java -Dexec.mainClass=com.tidewater.freight.Demo   # demo
mvn test                                                    # all tests
mvn verify                                                  # tests + coverage: target/site/jacoco/index.html
mvn test-compile org.pitest:pitest-maven:mutationCoverage   # mutation testing: target/pit-reports/index.html
```

Demo output:

```text
DOMESTIC 10kg STANDARD -> GHS 50.00
INTERNATIONAL 40kg EXPRESS -> GHS 967.50
MOON 10kg STANDARD -> refused: No rates are configured for zone 'MOON'
DOMESTIC -10kg STANDARD -> refused: Weight must be greater than 0 kg, got -10
DOMESTIC 10kg OVERNIGHT -> refused: Unknown service level 'OVERNIGHT'; expected one of [STANDARD, EXPRESS]
```

The original printed `0.0` for `MOON`, `-50.0` for −10 kg, and priced `OVERNIGHT` as standard.

---

## How it's built

```text
Demo ──creates──► InMemoryRateRepository ──injected into──► ShippingCalculator
                                                               │
                              Shipment (Zone, weight, ServiceLevel)
                                                               │
       1. ask the RateRepository for the zone's ZoneRate ──► missing? UnknownZoneException
       2. zone base cost = ZoneRate.baseCost(weight)
       3. + heavy-item surcharge (over 30 kg)
       4. × service-level multiplier
       5. round to pesewas (2 dp, half-up)
```

| Class | Its one job |
|---|---|
| `ShippingCalculator` | Combines the zone's rule with the rules every zone shares: heavy-item surcharge, service multiplier, rounding |
| `RateRepository` | Where zone rates come from (an interface: the injected seam) |
| `InMemoryRateRepository` | Tidewater's current rate table, supplied when it's created |
| `ZoneRate` | The pricing rule for one zone (an interface: the extension point for new zones) |
| `PerKilogramRate` | A zone priced per kg plus a fixed handling fee |
| `Shipment` | A valid shipment: zone, weight, service level. It also reads them from text. |
| `Zone` | A destination zone code, trimmed and case-insensitive |
| `ServiceLevel` | `STANDARD` or `EXPRESS`, each with its price multiplier |
| `ShippingException` | Parent of every refusal: `UnknownZoneException`, `UnknownServiceLevelException`, `InvalidShipmentException` |
| `Demo` | The edge of the program: wires the parts together and reports refusals |

All classes share the package `com.tidewater.freight`. The service is one small feature, and a single package avoids dependency cycles between packages.

---

## Design rationale

### 1. The rate source is injected (fixes C2)

`ShippingCalculator` receives a `RateRepository` through its constructor instead of building `new InMemoryRateRepository()` itself (§12.5, dependency inversion).

- The original already had a `RateRepository` interface, but the calculator bypassed it. I kept the interface and made it the seam.
- Tests pass in a Mockito mock. That lets them give the calculator any rates, no rates, or a rate-source outage, which the real in-memory table could never produce.

### 2. Each zone's rule lives in one object (fixes C3)

Originally, a zone's per-kg rate was in the repository and its handling fee was in a string `if`/`else` chain inside the calculator. Adding a zone meant editing both, and forgetting the second silently charged no fee.

Now `RateRepository` returns a `ZoneRate`, the zone's **complete** rule. `PerKilogramRate` (rate × kg + handling fee) covers all three current zones. The calculator never asks *which* zone it's pricing (§12.2, open-closed).

- **Rejected: an enum of zones with an `if` or `switch` on it.** That's the same edit-everywhere problem in a different shape.

### 3. Inputs that can't be wrong (fixes C7, C8)

| Type | Choice | Why |
|---|---|---|
| `Zone` | A **record**, not an enum | Zones are added by configuring rates. With an enum, every new zone means editing it. The record trims and upper-cases the code in one place, so `domestic` equals `DOMESTIC`. |
| `ServiceLevel` | An **enum** with a multiplier | A fixed set where each value has behaviour. An unknown code is now **refused**; before, it was silently priced as standard. |
| `Shipment` | A **record** that validates itself | Weight must be above 0, and every field is required. If a `Shipment` exists it's valid, so the calculator never checks input. It also bundles three parameters into one (§4.3). |

### 4. Failures are refused, never priced at zero (fixes C1, C10)

- **`ShippingCalculator` has no `try`/`catch`.** It can't do anything useful with a failure, so catching one would only hide it (§6.1). An unknown zone throws `UnknownZoneException`, and anything else, such as a rate-source outage, reaches the caller unchanged.
- **Three custom exceptions, one per refusal a customer can trigger,** under an abstract `ShippingException`. The edge catches the parent once, and each child names the exact reason.
- **They're unchecked:** refusals are handled at one place, and checked exceptions would force every layer in between to declare them.
- **`try`/`catch` appears only where it adds something:**
  - **Reading a service level from text:** wraps Java's `IllegalArgumentException` with the bad code, and keeps it as the cause (§6.3).
  - **Reading a weight from text:** wraps `NumberFormatException` with the bad text, and keeps it as the cause.
  - **`Demo`:** the edge of the program reports each refusal to the user. It catches only `ShippingException`, so real bugs still fail loudly.
- **Configuration mistakes are different.** A negative rate or a blank zone code is the configuring developer's mistake, not a customer's, so those throw `IllegalArgumentException`.

### 5. Money is exact (fixes C9)

All amounts are `BigDecimal`, and the final price is rounded once, to 2 decimal places, half-up. The original returned `42.599999999999994` for regional, 2.3 kg, express; this version returns `42.60`.

### 6. Small and honest code (fixes C4, C5, C6, C11)

- `calculateCost` went from 31 lines doing six jobs to 7 lines plus two named helpers.
- The calculator no longer prints. It returns a price, and the caller decides what to show.
- `30`, `20` and `1.5` became `HEAVY_ITEM_THRESHOLD_KG`, `HEAVY_ITEM_SURCHARGE` and the `EXPRESS` multiplier.
- Comments that narrated the code (`// express costs more`) or described flaws are gone. Names carry the meaning, and Javadoc states the contracts.

### What stayed the same

Every business rule, and the order they're applied in. For **6,000 valid shipments** (every zone, both service levels, 0.1 kg to 100 kg), the new code returns exactly the original price, rounded to 2 decimal places. The only behaviour changes are the ones the brief asked for: refusing what used to be silently mispriced.

---

## Adding the new zone

The brief says a new zone is coming "with its own rate rules". There are two cases.

**1. It's priced like the others** (a rate per kg plus a fee). Add one entry to the rate table in `InMemoryRateRepository.withStandardRates()`:

```java
new Zone("COASTAL"),
new PerKilogramRate(new BigDecimal("12.00"), new BigDecimal("15.00"))
```

**2. It has a different kind of rule** (for example, a minimum charge or weight tiers). Write one new class that implements `ZoneRate`, then add it to the table:

```java
public record MinimumChargeRate(BigDecimal ratePerKg, BigDecimal minimumCharge) implements ZoneRate {

    @Override
    public BigDecimal baseCost(BigDecimal weightKg) {
        return ratePerKg.multiply(weightKg).max(minimumCharge);
    }
}
```

**In both cases, nothing existing changes:** not `ShippingCalculator`, not the other zones, and not the surcharge or service-level rules. The test `newZoneWithItsOwnKindOfRuleNeedsNoCalculatorChange` proves this with a minimum-charge rule.

**The original would have needed:**
- a new entry in the repository constructor;
- a new `else if` branch in the calculator for the fee;
- retesting every zone, because they all shared one method.

---

## Testing

### How the suite is organised

| Test class | Type | What it proves |
|---|---|---|
| `ShippingCalculatorTest` | Unit, with the `RateRepository` mocked (Mockito) | The calculator's own rules: surcharge on both sides of 30 kg, express, rounding mode, unknown zone refused, outage not swallowed, a new kind of zone rule needing no change |
| `ShippingCalculatorIntegrationTest` | Integration, with the real rate table | The prices customers are actually quoted, end to end, from text input |
| `InMemoryRateRepositoryTest` | Unit | Lookup, unknown zone, and the table being copied |
| `PerKilogramRateTest` | Unit | Rate × kg + fee, exact decimals, zero allowed, negatives rejected |
| `ShipmentTest`, `ZoneTest`, `ServiceLevelTest` | Unit | Validation, parsing, and that original causes are kept |

### The rules the tests follow

- **Every expected price is worked out by hand** from the business rules, never copied from the code's own output. For example, international, 40 kg, express = (15 × 40 + 25 + 20) × 1.5 = **967.50**.
- **Boundaries are tested from both sides:** 30 kg and 30.01 kg; 0 kg rejected and 0.001 kg accepted; a rate of −0.01 rejected and 0 accepted.
- **Every failure path has a test,** and the tests that rethrow check that the original cause is kept.
- **Every test asserts a real result. None only checks that a mock was called.** Mocks are keyed by zone, so a calculator asking for the wrong zone fails the test anyway.
- Names say the expected behaviour, imports are explicit, and each test checks one behaviour (§9, §11).

## What the AI got wrong

| Part | Verdict | Why |
|---|---|---|
| `RateRepository` interface | **Kept**, reshaped | The seam was the right idea. It now returns the zone's whole rule, and `Optional` for an unknown zone. |
| `InMemoryRateRepository` | **Fixed** | The rates are now passed in, each zone's rule is complete, and an unknown zone returns empty instead of throwing a generic exception |
| `ShippingCalculator` | **Rewritten** | It swallowed every failure as a price of 0, built its own dependency, held zone fees in an `if` chain, printed, and used `double` for money |
| `Demo` | **Fixed** | It now wires the parts together and reports refusals |
| The business rules and their order | **Kept** | Proved unchanged across 6,000 prices |
| `pom.xml` (JUnit 5, Mockito, JaCoCo, PIT) | **Kept** | Already set up correctly |
| T1 `calculatesACost` | **Rewritten** | `assertTrue(cost > 0)` accepts any positive number. It's now an exact price. |
| T2 `calculatesExpressCost` | **Rewritten** | It mocked the class under test, so none of our code ran. The same 967.50 scenario now runs the real calculator. |
| T3 `returnsACostForAShipment` | **Rewritten** | `assertNotNull` on a primitive `double` can never fail. It's now an exact price. |