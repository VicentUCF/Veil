# Veil agent instructions

Read `docs/PROJECT_CONTEXT.md` before planning or changing product behavior. It is the local source of truth for the product direction, v0.1 scope, non-goals, and definition of done.

The repository contains the initial v0.1 implementation. Keep future work within the explicit user request and the scope boundaries in the product context.

Keep the project as a single `app` module. Prefer Android APIs, AndroidX, and Compose over third-party dependencies. Do not introduce persistence, backend services, analytics, dependency-injection frameworks, or speculative abstractions.

The product name is `Veil`. The initial namespace and application ID are `dev.vicent.veil` unless the user requests a different publishing identity.
