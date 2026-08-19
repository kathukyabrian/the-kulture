# User Creation and Crew Assignment Journey

## Goal

When an admin creates or edits a nganya, they should assign existing users as crew. If a person does not yet have an account, the admin can create one in the same flow. The new user receives an email link to set their password and can then sign in.

Users should also be movable from one nganya to another without creating duplicate people or losing their account.

## Required user details

- Name
- Email address
- Mobile number
- Account role

Mobile number is the primary person-matching identifier and must be unique. Email must also be unique because it is used for password setup and login.

Phone numbers must be normalized before lookup or storage. For example, `0712 345 678`, `+254712345678`, and `254712345678` should resolve to one canonical E.164 value: `+254712345678`.

## How this fits the current model

The existing model is not yet sufficient for this journey.

### What can be reused

- `Vehicle` remains the nganya being staffed.
- The current `CrewRole` values, `DRIVER` and `CONDUCTOR`, can describe a person's job on a particular nganya.
- `CrewMember.rating`, `active`, and timestamps can remain assignment-related fields.
- The admin nganya create/edit form is the right place to search for and assign crew.
- Existing crew vehicle operations can remain under `/api/crew/vehicles`, but they must eventually authorize access using the signed-in user's active assignment.

### Current gaps

- `CrewMember` stores only `displayName`; it has no user account, mobile number, email, or credentials.
- A crew row belongs directly to one vehicle and cannot represent an unassigned user.
- `VehicleService.replaceCrew` deletes all crew rows and recreates them on every update. This loses identity and assignment history.
- The current Angular `AuthService` uses hard-coded demo accounts in browser storage. There is no backend authentication or user table.
- Crew APIs accept any vehicle ID and do not verify that the signed-in user belongs to that vehicle.
- `CrewAdminRequest` accepts free-text display details instead of a user ID.

## Recommended domain model

Keep account authorization separate from the role performed on a vehicle.

### `users`

| Field | Purpose |
|---|---|
| `id` | UUID primary key |
| `name` | User's full/display name |
| `email` | Normalized login email; unique, case-insensitive |
| `phone_number` | Canonical E.164 mobile number; unique |
| `account_role` | `ADMIN`, `CREW`, or `TRAVELLER` |
| `password_hash` | Nullable until password setup is completed |
| `status` | `INVITED`, `ACTIVE`, `SUSPENDED` |
| `email_verified_at` | Set after the setup link is completed |
| `created_at` | Audit timestamp |
| `updated_at` | Audit timestamp |

`TRAVELLER` replaces the current frontend role named `nganya`. `CREW` grants access to crew functionality. `DRIVER` should not be an account-level permission because it is a job on a specific vehicle.

### `crew_assignments`

This should replace the current vehicle-owned meaning of `crew_members`.

| Field | Purpose |
|---|---|
| `id` | UUID primary key |
| `user_id` | Required foreign key to `users` |
| `vehicle_id` | Required foreign key to `vehicles` |
| `crew_role` | `DRIVER` or `CONDUCTOR` |
| `rating` | Assignment/crew rating, if still required |
| `started_at` | When the assignment began |
| `ended_at` | Nullable; set when moved or removed |
| `created_at` | Audit timestamp |
| `updated_at` | Audit timestamp |

Only one active assignment (`ended_at IS NULL`) should exist per user. Enforce this with a database constraint or partial unique index. A vehicle can have multiple active crew assignments.

This separation allows one persistent user account to move between nganyas while preserving assignment history.

### `password_setup_tokens`

| Field | Purpose |
|---|---|
| `id` | UUID primary key |
| `user_id` | User receiving the invitation |
| `token_hash` | Hash of the one-time token; never store the raw token |
| `expires_at` | Short expiry, such as 24 hours |
| `used_at` | Set after successful password creation |
| `created_at` | Audit timestamp |

The emailed URL contains the raw one-time token. The database stores only its hash. Completing setup sets `password_hash`, activates the user, verifies the email, and invalidates the token.

## Role mapping

There are two different role concepts:

- Account role: `ADMIN`, `CREW`, `TRAVELLER`
- Vehicle assignment role: `DRIVER`, `CONDUCTOR`

The original requirement listed `DRIVER`, `CREW`, and `TRAVELLER` together. Splitting these avoids ambiguity: both a driver and conductor sign in with the `CREW` account role, while their assignment records specify what they do on a particular nganya.

If conductors are intentionally called "crew" in the UI, show the label `CREW` while persisting the current `CONDUCTOR` assignment value, or rename that enum in a dedicated migration.

## Admin journey when creating or editing a nganya

1. The admin opens the crew section of the nganya form.
2. The admin searches by mobile number.
3. The API normalizes the number and searches `users.phone_number`.
4. If the user exists:
   - Show their name, masked email, account status, and current nganya assignment.
   - Let the admin select the assignment role (`DRIVER` or `CONDUCTOR`).
   - If assigned elsewhere, clearly ask the admin to confirm the move.
5. If the user does not exist:
   - Show fields for name, email, and mobile number.
   - Create the user with account role `CREW` and status `INVITED`.
   - Create a password setup token and send the setup email.
6. Assign the user to the nganya.
7. Save the vehicle and assignments in one transaction where practical.

Creating a new user and sending email should not leave inconsistent data. Commit the user and assignment first, then send mail through an outbox/background job so a temporary email failure can be retried.

## Admin users tab

The admin area should have a dedicated **Users** tab alongside Dashboard, Nganyas, and Routes. It provides one place to find and manage every account, including users who are not currently assigned to a nganya.

### List and filters

The page should provide:

- Search by name, email, or mobile number.
- Account-role filter: `ALL`, `ADMIN`, `CREW`, or `TRAVELLER`.
- Account-status filter: `ALL`, `INVITED`, `ACTIVE`, or `SUSPENDED`.
- Optional crew assignment-role filter: `DRIVER`, `CONDUCTOR`, or `UNASSIGNED`.
- Server-side pagination and a stable default sort, such as newest users first.

Account role and assignment role must remain separate filters. For example, a driver has account role `CREW` and active assignment role `DRIVER`.

Each row should show:

- Name
- Email
- Mobile number
- Account role
- Account status
- Current nganya and assignment role, when applicable
- Invitation/setup state
- Created date

### Admin actions

From the user detail or row actions, an admin should be able to:

- View the user's profile and assignment history.
- Resend an expired or unused password setup invitation.
- Assign an unassigned crew user to a nganya.
- Move a crew user to another nganya with confirmation.
- Change a crew assignment role.
- Suspend or reactivate an account.
- End a crew assignment without deleting the account.

Changing account roles is security-sensitive. In particular, promotion to `ADMIN` should use a separate confirmation and audit trail. The last active admin must not be demoted or suspended.

## Moving a user between nganyas

Moving must update assignments, not the user account:

1. Find the user's active assignment.
2. If it already points to the requested vehicle, update only the assignment role if needed.
3. Otherwise, set the old assignment's `ended_at`.
4. Create a new active assignment for the destination vehicle.
5. Perform steps 3 and 4 in one database transaction.

The UI must display the current nganya and require explicit confirmation before moving the user.

## Self-sign-up journey

Normal users can register themselves as travellers:

1. Submit name, email, mobile number, and password.
2. Normalize and check both email and mobile number for uniqueness.
3. Create the account with role `TRAVELLER`.
4. Verify email and/or mobile number before activating sensitive functionality.
5. Sign in using email and password.

Self-sign-up must never allow the caller to choose `ADMIN` or `CREW`. Those roles require an admin-controlled invitation or role change.

## Suggested API surface

### Admin users and assignments

- `GET /api/admin/users?q={query}&role={role}&status={status}&assignmentRole={assignmentRole}&page={page}&size={size}` — paginated Users tab data.
- `GET /api/admin/users/{userId}` — user details and assignment history.
- `GET /api/admin/users/by-phone?phone={number}` — find a user for crew selection.
- `POST /api/admin/users/invite-crew` — create/invite a crew account.
- `POST /api/admin/users/{userId}/resend-invitation` — issue a new setup token and queue another email.
- `PATCH /api/admin/users/{userId}/status` — suspend or reactivate an account.
- `PATCH /api/admin/users/{userId}/role` — change account role with elevated authorization and auditing.
- `GET /api/admin/vehicles/{vehicleId}/crew` — list active assignments.
- `POST /api/admin/vehicles/{vehicleId}/crew` — assign a user to a vehicle.
- `PATCH /api/admin/crew-assignments/{assignmentId}` — change assignment role.
- `POST /api/admin/crew-assignments/{assignmentId}/move` — move to another vehicle.
- `DELETE /api/admin/crew-assignments/{assignmentId}` — end an assignment; do not delete the user.

### Authentication

- `POST /api/auth/register` — traveller self-sign-up.
- `POST /api/auth/login` — email/password login.
- `POST /api/auth/password/setup` — consume invitation token and set initial password.
- `POST /api/auth/password/forgot` — request a reset email.
- `POST /api/auth/password/reset` — consume reset token.

Do not expose whether arbitrary email addresses exist in public password-reset responses.

## Request model changes

The current vehicle payload embeds crew as:

```json
{
  "displayName": "Example",
  "role": "DRIVER",
  "rating": 0
}
```

It should reference persistent users instead:

```json
{
  "userId": "9a7c...",
  "role": "DRIVER"
}
```

Prefer dedicated assignment endpoints over deleting and replacing the entire crew list during every vehicle update. Vehicle profile edits and crew staffing have different lifecycle and authorization rules.

## Migration path from the existing model

1. Add `users`, `crew_assignments`, and password-token/outbox tables.
2. Create placeholder `CREW` users for existing `crew_members` records. Existing data has no email or phone, so these accounts must remain incomplete until reconciled by an admin.
3. Copy each current crew row into a `crew_assignments` row, preserving vehicle, role, rating, active state, and timestamps.
4. Update API responses and the admin form to use `userId` and assignment IDs.
5. Replace the browser-only demo authentication with backend login and server-issued secure sessions or tokens.
6. Enforce assignment-based authorization on crew vehicle operations.
7. Stop calling `deleteByVehicleId` from vehicle updates.
8. After migration and verification, remove the old `crew_members` table or rename the new assignment table into its place.

## Important constraints and security rules

- Unique normalized phone number.
- Unique normalized email address, case-insensitive.
- Passwords stored only as strong hashes, never plaintext.
- Setup/reset tokens must be random, hashed, expiring, single-use, and revocable.
- A crew user can have at most one active vehicle assignment.
- Moving/removing crew must not delete their account.
- Crew endpoints must derive the user from the authenticated session and verify an active assignment.
- Admin actions for invitations, moves, role changes, and removals should be audited.
- Rate-limit login, user lookup, invitations, and password reset endpoints.

## Recommended implementation order

1. Add backend users and real authentication.
2. Add crew assignments linked to users.
3. Add phone lookup and invite flows.
4. Add the paginated, filterable admin Users tab.
5. Update the nganya form to search, create, assign, and move users.
6. Add password setup email delivery with retries.
7. Add traveller self-registration.
8. Enforce assignment-aware crew authorization and remove the legacy crew model.
