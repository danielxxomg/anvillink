# repair-signs Specification

## Purpose

Define front-side signs with PDC identity.

## Requirements

### Requirement: Exact authorized creation
The plugin MUST register only when a player with create permission sets front-side line 1 exactly to case-insensitive `[repair]` and line 2 to `HAND` or `ALL`. It MUST normalize case, render blue canonical `[repair]` and uppercase mode, reject other locations or modes, and ignore back-side text in the MVP.

#### Scenario: Case-insensitive canonical creation
- GIVEN a permitted player submits front `[RePaIr]` and `hAnD`
- WHEN processed
- THEN it registers as blue `[repair]` and `HAND`

#### Scenario: Unauthorized creation is rejected
- GIVEN a player lacks create permission
- WHEN a valid front-side sign is submitted
- THEN no record or authority is created

#### Scenario: Wrong location is rejected
- GIVEN an authorized player puts `[repair]` on front line 2
- WHEN processed
- THEN no repair-sign record is created

### Requirement: Permanent PDC identity and creation authorization
The plugin MUST store a versioned PDC record containing schema version, canonical mode, creator UUID, and an authorized-create marker. Its namespace/key/schema MUST be permanent and brand-independent. Only a complete supported record MAY authorize activation; missing or malformed data MUST fail closed. Player and inventory data MUST remain server-local.

#### Scenario: Visible text alone has no authority
- GIVEN text displays `[repair]` and `HAND` but PDC is incomplete
- WHEN activated
- THEN no charge, repair, or sign treatment occurs

#### Scenario: Missing creation authorization fails closed
- GIVEN PDC lacks its creator UUID or create marker
- WHEN targeted
- THEN invalid identity is reported and no repair or charge occurs

### Requirement: Reloadable messages and MiniMessage presentation
The plugin MUST load reloadable messages/defaults and render presentation through MiniMessage. A valid reload MUST atomically replace active configuration. An invalid reload MUST retain the last valid configuration, report an operator failure, and apply no partial values; invalid initial configuration MUST disable repair activation.

#### Scenario: Valid reload changes presentation
- GIVEN valid active and replacement defaults/messages
- WHEN reloaded
- THEN replacement activates atomically and presentation uses MiniMessage

#### Scenario: Invalid reload fails deterministically
- GIVEN valid active configuration and an invalid replacement
- WHEN reloaded
- THEN the prior configuration remains, no partial value applies, and failure is reported

#### Scenario: Invalid initial configuration fails closed
- GIVEN startup configuration is invalid
- WHEN initialization completes
- THEN repair activation is disabled and an operator failure is reported

### Requirement: Tamper detection and managed re-render
The plugin MUST compare registered front-side text with its valid PDC record before activation; tampering MUST block activation. A manage-authorized administrator MUST be able to inspect validity and re-render canonical blue `[repair]` plus uppercase mode.

#### Scenario: Tampered text is rejected
- GIVEN a valid record says `ALL` but visible line 2 says `HAND`
- WHEN activated
- THEN it fails closed with no charge

#### Scenario: Valid identity is re-rendered
- GIVEN a manager targets a valid record with altered text
- WHEN inspect/re-render is requested
- THEN validity is reported and canonical front text is restored

### Requirement: Protected lifecycle and use authorization
The plugin MUST require manage permission to edit or break a registered sign and use permission before activation. An unauthorized lifecycle action MUST be cancelled without changing the record; a manage-authorized action MAY proceed.

#### Scenario: Unauthorized edit or break is cancelled
- GIVEN a registered sign and a player without manage permission
- WHEN it is edited or broken
- THEN the action is cancelled and PDC remains unchanged

### Requirement: Duplicate-hand interaction filtering
The plugin MUST ignore the duplicate off-hand interaction and allow only the main-hand event for one sign right-click to enter the transaction boundary.

#### Scenario: Duplicate events do not double-charge
- GIVEN one right-click produces main-hand and off-hand events for a permitted player
- WHEN both arrive
- THEN only the main-hand event enters activation
