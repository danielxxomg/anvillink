# equipment-repair Specification

## Purpose

Define the bounded, deterministic equipment repair plan used by paid repair signs without touching storage inventory.

## Requirements

### Requirement: Exact equipment target modes
The plugin MUST resolve `HAND` to the main-hand slot only. It MUST resolve `ALL` to exactly six slots in fixed order: main hand, off hand, helmet, chestplate, leggings, and boots. Neither mode MAY include storage inventory slots.

#### Scenario: HAND excludes other equipment
- GIVEN damaged items exist in main hand, off hand, and helmet
- WHEN a `HAND` plan is created
- THEN the plan contains only the main-hand slot

#### Scenario: ALL excludes storage
- GIVEN all six equipment slots and one storage slot contain damaged items
- WHEN an `ALL` plan is created
- THEN the plan contains exactly the six equipment slots and leaves storage out

### Requirement: Damaged repairable eligibility
The plugin MUST plan an item only when it is non-empty, its meta implements `Damageable`, its damage is positive, and it is not unbreakable. Every other item MUST be skipped.

#### Scenario: Mixed targets select only eligible items
- GIVEN targets include empty, undamaged, unbreakable, non-`Damageable`, and non-unbreakable `Damageable` items with positive damage
- WHEN a plan is created
- THEN only the non-empty eligible `Damageable` items appear in the plan

#### Scenario: No eligible target creates no plan
- GIVEN every resolved equipment slot is empty or fails an eligibility condition
- WHEN a plan is created
- THEN the plan has zero entries and no item state changes

### Requirement: Deterministic planning and snapshots
The plugin MUST resolve and validate the complete ordered plan before payment. Each planned slot MUST capture its full pre-repair item state for restoration, and planning MUST NOT mutate any item.

#### Scenario: Repeated planning is stable
- GIVEN the same player equipment snapshot and repair mode are supplied twice
- WHEN both plans are created
- THEN both plans contain the same eligible slots in the specified order and equivalent snapshots

### Requirement: Payment-gated server-thread application
The plugin MUST NOT mutate an item before successful payment. After payment succeeds, it SHALL set each planned `Damageable` meta damage to zero on the server thread. If application fails, it MUST attempt restoration for every mutated slot from its snapshot; if any attempt fails, it MUST defer to the repair-economy failed-restoration recovery contract before reporting failure.

#### Scenario: Payment failure preserves equipment
- GIVEN a validated plan and a failed payment
- WHEN the transaction ends
- THEN no planned item is mutated

#### Scenario: Apply failure restores snapshots
- GIVEN payment succeeds and an unexpected failure occurs while applying the plan
- WHEN recovery runs
- THEN restoration is attempted for every previously mutated equipment slot; any failed attempt follows the repair-economy failed-restoration recovery contract, and storage remains unchanged
