# Tidewater Freight: Analysis (before refactoring)

Written before any change to the code, for the analysis check-in.

**Code:** `ShippingCalculator`, `RateRepository`, `InMemoryRateRepository`, `Demo`.
**Tests:** `ShippingCalculatorTest`.

What the current code returns:
```
Shipping DOMESTIC (10.0kg, STANDARD): GHS 50.0
Result DOMESTIC 10kg STANDARD       -> 50.0
Shipping INTERNATIONAL (40.0kg, EXPRESS): GHS 967.5
Result INTERNATIONAL 40kg EXPRESS   -> 967.5
Result unknown zone MOON 10kg STD   -> 0.0
```

## Part 1: The code

### C1. Every failure is swallowed and becomes a price of 0

- **Where:** `ShippingCalculator` inside `calculateCost` : catch (Exception e) { return 0; }`
- **Rule broken:** do not ignore caught exceptions.
- **What happens:** An unknown zone, a `null` input, or any bug in the method itself (even a `NullPointerException`) is caught and turned into `0.0`. Nobody is told, nothing is logged, and the caller cannot tell "free" from "failed".

### C2. The rate source is hard-wired

- **Where:** `ShippingCalculator` : `private final RateRepository repository = new InMemoryRateRepository();`
- **Rule broken:** Dependency inversion. Depend on the abstraction and have it passed in.
- **What happens:** a `RateRepository` interface exists, but the calculator ignores that seam and builds the concrete class itself. No caller or test can supply different rates, so the calculator can't be tested on its own.

### C3. A zone's rules are split across two classes and a string `if`/`else` chain

- **Where:** the per-kg rate is in `InMemoryRateRepository` (lines 11–13). The handling fee is in `ShippingCalculator` (lines 14–20).
- **Rules broken:** Open-closed principle; DRY (one piece of knowledge in one place).
- **What happens:**
  - Adding the planned new zone means editing two classes, including the existing calculation method.
  - The chain has no `else`, so if someone adds the rate but forgets the fee, the new zone silently gets no fee.

### C4. One method does six jobs

- **Where:** `calculateCost` inside `ShippingCalculator` class.
- **Rules broken:** Small methods, rarely over 20 lines; §4.2 and §12.1, one job each.
- **What it does:** it looks up the rate, adds the zone fee, adds the weight surcharge, applies the service multiplier, prints, and decides the error policy. Each one is a separate reason to change the same method.

### C5. The calculation prints as a hidden side effect

- **Where:** Inside `calculateCost` method `System.out.println(...)`
- **Rule broken:** No side effects.
- **What happens:** every price calculation writes to the console. Callers, including tests and any future service, can't turn it off. When I ran the mutation check, the calculator's prints were mixed into the test output. Deciding what to display is the caller's job.

### C6. Magic numbers

- **Where:** `10`, `25`, `30`, `20`, `1.5` in `ShippingCalculator`; `5.0`, `8.0`, `15.0` in `InMemoryRateRepository`.
- **Rule broken:** Use named constants.
- **What happens:** nothing tells the reader that `30` is a weight threshold in kg, or that `20` is a surcharge in GHS. The values can't be searched for or changed in one place.

### C7. Zone and service level are free-text strings

- **Where:** The parameters `String zone` and `String serviceLevel`, and the checks `zone.equals(...)` and `serviceLevel.equals("EXPRESS")`.
- **What happens:**
  - The checks are case-sensitive.
  - Any service level that isn't `"EXPRESS"`, including a typo or `"OVERNIGHT"`, is silently priced as standard.
  - `null` throws inside the method and then disappears through C1.
  - The compiler can't catch any of these mistakes.

### C8. Weight is never validated

- **Where:** `calculateCost` accepts any `double`.
- **What happens:**
  - −10 kg gives a **negative price** (−50.0).
  - 0 kg gives 0.0 for `DOMESTIC`, but 10.0 (the fee alone) for `REGIONAL`.
  - `NaN` gives a `NaN` price.
  - No rule says what a valid weight is.

### C9. Money is a `double`

- **Where:** rates, fees and the return type.
- **What happens:** binary floating point can't hold many decimal amounts exactly. `REGIONAL`, 2.3 kg, `EXPRESS` returns `42.599999999999994`, and nothing rounds the result to pesewas. Customers would be quoted, and could be invoiced, amounts that aren't real money.

### C10. "Unknown zone" has no exception type of its own

- **Where:** Inside `InMemoryRateRepository`, `findRate` method `throw new IllegalArgumentException("Unknown zone: " + zone);`
- **Rule broken:** Add context and use meaningful exceptions.
- **What happens:** an unknown zone is a normal business situation, but it's reported with the same generic type Java uses for programming mistakes. A caller can't catch "unknown zone" and handle it on its own. C1 hides it anyway.

### C11. Comments narrate the code or excuse it

- **Where:** `// zone-specific fees`, `// heavy-item surcharge`, `// express costs more`, `// no extra fee` (inside the empty branch), plus the comments on lines 5 and 35 that describe flaws.
- **Rule broken:** Comment only the *why*.
- **What happens:** the comments stand in for names. A constant such as `HEAVY_ITEM_SURCHARGE` would make most of them unnecessary.

### SOLID summary

| Principle | Verdict |
|---|---|
| S: single responsibility | **Violated** (C4) |
| O: open-closed | **Violated** (C3) |
| L: Liskov substitution | Not violated; there is no inheritance |
| I: interface segregation | Not violated; `RateRepository` has one small method |
| D: dependency inversion | **Violated** (C2) |

### The most serious problem: C1

This is a **pricing** service, so its worst possible failure is a wrong price that looks valid. `0.0` looks exactly like that.

- A typo, an unconfigured zone, or a crash inside the method each turns into free shipping, with no error and no log.
- It makes the coming change dangerous: if the new zone is added to the calculator but not to the rate repository, it ships free and nobody notices.
- It hides every other bug in the method, and it is the reason test T3 below cannot fail.

C2 comes next: it isn't a direct business loss, but until it's fixed none of the other problems can be tested properly.

---

## Part 2: The tests

### T1. `calculatesACost`: the assertion is too weak

- `assertTrue(cost > 0)` accepts any positive number.
- The correct answer, 50.0, is never checked, so a wrong rate, fee or multiplier all pass.
- **Verdict: fix.** Keep the scenario (domestic, standard) and assert the exact amount.

### T2. `calculatesExpressCost`: tests Mockito, not our code

- It mocks `ShippingCalculator`, the class under test. So none of our code runs; the test only checks that Mockito returns the value it was just told to return.
- `967.5` happens to be the correct real answer, which makes the test look meaningful when it isn't.
- **Verdict: rewrite.** Keep the scenario (international, 40 kg, express, 967.5) as a real expectation against the real calculator, with only the rate source mocked.

### T3. `returnsACostForAShipment`: cannot fail at all

- `assertNotNull(cost)` on a primitive `double` autoboxes it to a `Double`, which can never be `null`.
- The method never throws (C1), so nothing else can make the test fail either. It passes for every possible implementation.
- **Verdict: rewrite.** Regional, 5 kg, standard should equal 50.0 exactly (8 × 5 + 10).