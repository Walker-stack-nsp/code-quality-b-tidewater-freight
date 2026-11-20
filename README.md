# Tidewater Freight: Shipping Cost Service

A small service that works out the cost of a shipment from its weight, destination zone and service level, and shows how that price is made up.

- **[`ANALYSIS.md`](ANALYSIS.md):** what was wrong with the original code and tests, written before any change. Problems are referred to below by their IDs there: C1–C11 for the code, T1–T3 for the tests.
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
    zone cost                 50.00
    heavy-item surcharge       0.00
    service-level charge       0.00
INTERNATIONAL 40kg EXPRESS -> GHS 967.50
    zone cost                625.00
    heavy-item surcharge      20.00
    service-level charge     322.50
MOON 10kg STANDARD -> refused: No rates are configured for zone 'MOON'
DOMESTIC -10kg STANDARD -> refused: Weight must be greater than 0 kg, got -10
DOMESTIC 10kg OVERNIGHT -> refused: Unknown service level 'OVERNIGHT'; expected one of [STANDARD, EXPRESS]
```

The original printed `0.0` for `MOON`, `-50.0` for −10 kg, and priced `OVERNIGHT` as standard.

---

## Project structure

```text
src/main/java/com/tidewater/freight/
├── Demo.java                          entry point: wires the parts together, prints quotes and refusals
├── service/
│   ├── ShippingCalculator.java        works out a shipment's price and its breakdown
│   └── InMemoryRateRepository.java    Tidewater's current rate table
├── interfaces/
│   ├── RateRepository.java            where zone rates come from (the injected seam)
│   └── ZoneRate.java                  the pricing rule for one zone (the extension point)
├── model/
│   ├── Shipment.java                  a valid shipment: zone, weight, service level
│   ├── Zone.java                      a destination zone code
│   ├── PerKilogramRate.java           a zone priced per kg plus a handling fee
│   └── Quote.java                     a price broken down by rule; the lines always add up
├── enums/
│   └── ServiceLevel.java              STANDARD or EXPRESS, each with its price multiplier
└── exceptions/
    ├── ShippingException.java         abstract parent of every refusal
    ├── UnknownZoneException.java
    ├── UnknownServiceLevelException.java
    └── InvalidShipmentException.java

src/test/java/com/tidewater/freight/   all test classes
```

The packages group classes by their role:

| Package | Role | Why it's separate |
|---|---|---|
| `service` | The work: pricing, and the rate source it's wired with | The classes that do something with the model |
| `interfaces` | The two abstractions the design depends on | `RateRepository` is what the calculator depends on (and what tests mock). `ZoneRate` is what a new zone implements. Keeping them together makes the extension points easy to find. |
| `model` | Domain values, all immutable records | Data that validates itself and never changes |
| `enums` | Fixed sets of values | `ServiceLevel` is a closed set with behaviour |
| `exceptions` | Every reason a quote can be refused | Callers find all the refusals in one place |

### How a price is worked out

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
       6. return a Quote: zone cost, surcharge, service-level charge, total
```

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

- The pricing method went from 31 lines doing six jobs to 8 lines (`calculateQuote`) plus small named helpers. `calculateCost` is now a one-liner that returns the quote's total.
- The calculator no longer prints. It returns a price, and the caller decides what to show.
- `30`, `20` and `1.5` became the private constants `HEAVY_ITEM_THRESHOLD_KG` and `HEAVY_ITEM_SURCHARGE`, and the `EXPRESS` multiplier.
- Comments that narrated the code (`// express costs more`) or described flaws are gone. Names carry the meaning, and Javadoc states the contracts.

### What stayed the same

Every business rule, and the order they're applied in. For **6,000 valid shipments** (every zone, both service levels, 0.1 kg to 100 kg), the new code returns exactly the original price, rounded to 2 decimal places. The only behaviour changes are the ones the brief asked for: refusing what used to be silently mispriced.

---

## Adding the new zone

The brief says a new zone is coming "with its own rate rules". There are two cases.

**1. It's priced like the others** (a rate per kg plus a fee). Add one entry to the rate table in `service/InMemoryRateRepository.withStandardRates()`:

```java
new Zone("COASTAL"),
new PerKilogramRate(new BigDecimal("12.00"), new BigDecimal("15.00"))
```

**2. It has a different kind of rule** (for example, a minimum charge or weight tiers). Add one new record to `model/` that implements `interfaces/ZoneRate`, then add it to the table:

```java
public record MinimumChargeRate(BigDecimal ratePerKg, BigDecimal minimumCharge) implements ZoneRate {

    @Override
    public BigDecimal baseCost(BigDecimal weightKg) {
        return ratePerKg.multiply(weightKg).max(minimumCharge);
    }
}
```

**In both cases, nothing existing changes:** not `ShippingCalculator`, not the other zones, and not the surcharge or service-level rules. The test `newZoneWithItsOwnKindOfRuleNeedsNoCalculatorChange` proves this with a minimum-charge rule. The new zone's cost also appears as its own line in every quote.

**The original would have needed:**
- a new entry in the repository constructor;
- a new `else if` branch in the calculator for the fee;
- retesting every zone, because they all shared one method.

---

## Improvement beyond the brief: itemised quotes

**What:** besides the total, the calculator now returns a `Quote` showing how the price is made up: the zone cost, the heavy-item surcharge, the service-level charge, and the total.

```java
Quote quote = calculator.calculateQuote(Shipment.of("INTERNATIONAL", "40", "EXPRESS"));
// zone cost 625.00 + heavy-item surcharge 20.00 + service-level charge 322.50 = total 967.50
```

**Why it adds value for this code:**
- **Every price can be explained.** When a customer asks "why 967.50?", customer service can answer line by line instead of a developer redoing the sum by hand.
- **It's ready for invoices,** which normally list the base charge, surcharges and service upgrades separately.
- **A broken rule is easier to find.** Tests now check each line of a quote, so when a rule breaks, the failing line points straight at it. A single total only says *something* is wrong.
- **Rule changes are auditable.** When the new zone or a new surcharge arrives, each quote shows exactly what every rule contributed.

**Design choices:**
- **The lines always add up to the total.** `Quote` checks this in its constructor, so a quote that contradicts itself can't be created, the same "valid by construction" idea as `Shipment`.
- **Every existing price is unchanged.** The total is still rounded once, as before. The service-level charge is whatever is left after the zone cost and surcharge (total − zone cost − surcharge). Rounding each line separately and adding them up would sometimes put the total a pesewa off today's price.
- **One calculation, not two.** `calculateCost` returns `calculateQuote(...).total()`, so the total and the breakdown can never disagree (§2.2, DRY).
- **The line is named `serviceLevelCharge`, not `expressCharge`,** so it still fits if a new service level is added. For `STANDARD` it's `0.00`.

**Checked:** across 6,000 valid shipments, every quote's total equals the original price, every quote adds up, and every standard shipment's service-level charge is exactly `0.00`.

---

## Assumptions

The original code left these business rules unclear. I kept the existing behaviour and recorded each one here.

1. **A parcel of exactly 30 kg pays no heavy-item surcharge** (the rule is "over 30 kg").
2. **Express ×1.5 also multiplies the zone fee and the surcharge**, e.g. (600 + 25 + 20) × 1.5 = 967.50.
3. **Zone and service-level codes are case-insensitive.**
4. **Prices are rounded to 2 decimal places, half-up.**
5. **A shipment must weigh more than 0 kg.**

---

## Testing

### How the suite is organised

| Test class | Type | What it proves |
|---|---|---|
| `ShippingCalculatorTest` | Unit, with the `RateRepository` mocked (Mockito) | The calculator's own rules: surcharge on both sides of 30 kg, express, rounding mode, unknown zone refused, outage not swallowed, a new kind of zone rule needing no change, and each line of the quote |
| `ShippingCalculatorIntegrationTest` | Integration, with the real rate table | The prices and itemised quotes customers are actually given, end to end, from text input |
| `QuoteTest` | Unit | A quote is accepted only when its lines add up to the total |
| `InMemoryRateRepositoryTest` | Unit | Lookup, unknown zone, and the table being copied |
| `PerKilogramRateTest` | Unit | Rate × kg + fee, exact decimals, zero allowed, negatives rejected |
| `ShipmentTest`, `ZoneTest`, `ServiceLevelTest` | Unit | Validation, parsing, and that original causes are kept |
| `DemoTest` | Unit | The demo's full output: itemised quotes, and the reason for each refusal |


### Evidence that the tests catch bugs

During the refactor, deliberate bugs were injected one at a time to check that the suite fails for each one. This is mutation testing done by hand.

| Point | Deliberate bugs caught |
|---|---|
| Original AI tests | **2 of 9** (T2 and T3 could never fail) |
| After replacing the original tests | **9 of 9** |
| Value types (`Zone`, `ServiceLevel`, `Shipment`) | **14 of 14** |
| Zone rules (`PerKilogramRate`) | **8 of 8** |
| Rewired calculator, including putting the old `catch`-and-return-0 back | **15 of 15** |
| `Quote` record | **5 of 5** |
| `calculateQuote`, including rounding each line separately | **8 of 8** |
| `Demo` output, including a swallowed refusal | **5 of 5** |

---

## What the AI got wrong

| Part | Verdict | Why |
|---|---|---|
| `RateRepository` interface | **Kept**, reshaped | The seam was the right idea. It now returns the zone's whole rule, and `Optional` for an unknown zone. |
| `InMemoryRateRepository` | **Fixed** | The rates are now passed in, each zone's rule is complete, and an unknown zone returns empty instead of throwing a generic exception |
| `ShippingCalculator` | **Rewritten** | It swallowed every failure as a price of 0, built its own dependency, held zone fees in an `if` chain, printed, and used `double` for money |
| `Demo` | **Fixed** | It now wires the parts together, prints itemised quotes, and reports refusals |
| The business rules and their order | **Kept** | Proved unchanged across 6,000 prices |
| `pom.xml` (JUnit 5, Mockito, JaCoCo, PIT) | **Kept** | Already set up correctly |
| T1 `calculatesACost` | **Rewritten** | `assertTrue(cost > 0)` accepts any positive number. It's now an exact price. |
| T2 `calculatesExpressCost` | **Rewritten** | It mocked the class under test, so none of our code ran. The same 967.50 scenario now runs the real calculator. |
| T3 `returnsACostForAShipment` | **Rewritten** | `assertNotNull` on a primitive `double` can never fail. It's now an exact price. |