# Traveller

- a traveller should have:
    - default route - this is basically where they live
    - temporary route - to help in sampling

- sampling is where a traveller decides to visit another route to sample nganyas from there

- a traveller should be able to enter sampling mode - here they will choose the route they want to sample - that route should go to temporary route

- on landing they should have the following tabs;
    - dashboard
    - my nganyas
    - nganyas
## Dashboard
- should show the traveller info 
- should show default route info
- should have a button to enter/quit sampling mode
- if sampling mode is on, show the temporary/sampling route

## My Nganyas
- should filter out nganyas by default/sampling route depending on what they have set


## Nganyas
- show all nganyas without a filter

# Anonymous users
- they should be able to see the public page without the custom filtering

---

# Implementation notes

## Current application fit

- Traveller accounts already use the shared `users` table with `account_role = TRAVELLER`.
- Registration, login, account status checks, public routes, public nganya summaries, and nganya profile APIs already exist.
- `UserAccount` does not currently store a default route, temporary route, or sampling state.
- The Angular landing page currently uses `LiveMapComponent` and is restricted to authenticated travellers even though its backing route and vehicle APIs are public.
- The existing vehicle summaries contain route information, so filtering can be performed efficiently by route ID once that ID is included in the summary response or accepted as an API filter.

## Data model

Add the following nullable columns to `users` through a new Liquibase migration:

- `default_route_id`, foreign key to `routes.id`.
- `temporary_route_id`, foreign key to `routes.id`.
- `sampling_enabled`, non-null boolean with a default of `false`.

These fields only have meaning for `TRAVELLER` accounts. Admin and crew responses should not expose or depend on them.

The active route is derived rather than stored separately:

```text
sampling_enabled && temporary_route_id != null
    ? temporary_route_id
    : default_route_id
```

This avoids a fourth route-state field becoming inconsistent with the other three.

## State rules

- A traveller may have no default route immediately after registration. The dashboard should show a setup prompt rather than treating this as an error.
- A default route must exist and be active when it is selected.
- Sampling can only be enabled with an active temporary route.
- Entering sampling mode sets `temporary_route_id` and `sampling_enabled = true` atomically.
- Leaving sampling mode sets `sampling_enabled = false` and clears `temporary_route_id`.
- Changing the default route does not affect an active sampling session.
- If a selected route is later deactivated, it remains visible in account context for explanation, but cannot be selected again. Personalized results should return an empty state until the traveller selects an active route.
- Sampling state is persisted in the database and therefore survives logout, browser restarts, and use on another device.

## Backend API

Add a traveller context endpoint:

`GET /api/traveller/me`

```json
{
  "user": {
    "id": "uuid",
    "displayName": "Traveller Name",
    "email": "traveller@example.com",
    "role": "traveller"
  },
  "defaultRoute": null,
  "temporaryRoute": null,
  "activeRoute": null,
  "samplingEnabled": false
}
```

Add focused preference operations:

- `PUT /api/traveller/default-route` with `{ "routeId": "uuid" }`.
- `POST /api/traveller/sampling` with `{ "routeId": "uuid" }`.
- `DELETE /api/traveller/sampling` to leave sampling mode.

All `/api/traveller/**` endpoints require an authenticated, active `TRAVELLER` account. They must return `403` for admin or crew accounts and must resolve the user from the authenticated principal rather than accepting a user ID from the client.

Extend public vehicle discovery to accept an optional route filter:

`GET /api/vehicles?routeId={uuid}`

The response must continue to include only public/listed vehicles. The server should perform route filtering; the client should not download the entire fleet merely to implement My Nganyas.

## Frontend routes and navigation

Use a shared traveller shell with three tabs:

| Tab | Route | Purpose |
| --- | --- | --- |
| Dashboard | `/traveller` | Profile, route preferences, and sampling controls |
| My Nganyas | `/traveller/my-nganyas` | Nganyas on the active route |
| Nganyas | `/traveller/nganyas` | All public nganyas |

Nganya detail pages opened from either list should use `/traveller/nganyas/:id` and reuse the existing public `NganyaProfileComponent`.

The traveller guard should protect the three personalized routes. Public discovery should remain available separately at `/` and `/nganyas/:id` without authentication. If an authenticated traveller opens `/`, the application may redirect them to `/traveller`.

## Dashboard behavior

- Load all dashboard state from `GET /api/traveller/me`.
- Display the traveller's name and email.
- Show the default route, or a clear “Choose your route” action when none exists.
- Use the existing route search/select behavior instead of a large basic select list.
- When sampling is off, show an “Enter sampling mode” action that opens the route picker.
- When sampling is on, prominently show the temporary route and provide a confirmed “Quit sampling” action.
- Explain that My Nganyas currently uses the sampling route while sampling mode is active.
- Disable duplicate submissions while a preference request is in progress and refresh context after success.

## My Nganyas behavior

- Load traveller context first.
- If no active route exists, show a link to configure the default route on the dashboard.
- Otherwise request public nganyas using the active route ID.
- Clearly label whether results are based on the default route or sampling route.
- Reuse the public nganya cards, loading state, error state, and empty state.

“My Nganyas” means route-matched nganyas in this feature. It does not mean favourites. Favourites should be treated as a separate future feature to avoid mixing two different concepts.

## Nganyas and anonymous behavior

- The Nganyas tab uses the existing unfiltered public vehicle endpoint and public profile component.
- Anonymous users can browse the public landing page, all public nganyas, and public nganya profiles.
- Anonymous users do not see Dashboard or My Nganyas and do not receive personalized filtering.
- Hidden or suspended listings must never appear in either authenticated or anonymous discovery.

## Registration and onboarding

Keep the current traveller registration fields. After successful registration or first login, a traveller without a default route should be directed to the dashboard route picker. Route selection should not block account creation because users may need to search the route catalogue after signing in.

## Errors and empty states

- `401`: session is missing or expired; redirect to login for personalized pages.
- `403`: the account is suspended or does not have the traveller role.
- `404`: a submitted route ID does not exist.
- `409`: the selected route is inactive or cannot be used for sampling.
- Network errors should preserve the last successfully loaded screen where possible and offer retry.
- Empty route results are a valid state and should say that no public nganyas are currently listed on that route.

## Recommended execution order

1. Add traveller preference columns and domain mappings.
2. Add traveller context and preference APIs with role and route validation tests.
3. Add optional server-side route filtering to public vehicle discovery.
4. Make the existing public landing and profile routes anonymous-accessible.
5. Add the shared traveller shell and its three routes.
6. Implement dashboard setup and sampling controls.
7. Implement active-route My Nganyas filtering.
8. Reuse public discovery and profile components in the traveller shell.
9. Add responsive, authorization, sampling-state, empty-state, and navigation tests.

## Acceptance criteria

- A traveller can select and later change their default route.
- A traveller can enter sampling mode by selecting a different active route.
- Sampling state persists across sessions and can be explicitly cleared.
- Dashboard accurately identifies the default, temporary, and active routes.
- My Nganyas uses the temporary route during sampling and the default route otherwise.
- Nganyas always shows the complete public catalogue without personalized filtering.
- Anonymous users can browse public nganyas and profiles without creating an account.
- Admin and crew users cannot read or mutate traveller preferences.
- Inactive routes cannot be newly selected.
- Loading, missing-route, empty-result, expired-session, and network-error states are handled explicitly.
