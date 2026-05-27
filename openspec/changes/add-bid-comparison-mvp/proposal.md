## Why
This is the ASAP feature. Mihnea has not yet signed his GC contract; he needs to look at 3+ offers side by side, see what's missing from each, and evaluate whether quoted prices are sane. WBS taxonomy (Phase 2) is not yet available, so comparison relies on user-driven line matching: the user marks "this OfferLine in Offer A is the same scope as that OfferLine in Offer B." All other comparison features key off these matches. This proposal also lays the groundwork for the future WBS-driven comparison: the same comparison query backend will switch from MatchGroup to WBSItem as the primary matching primitive once Phase 2 ships, with no UI rewrite.

## What Changes
- **ADD**: `OfferLineMatchGroup` entity — `{id, project_id FK, label VARCHAR(512), notes, audit}` plus join table `match_group_line {match_group_id FK, offer_line_id FK UNIQUE}` enforcing one-group-per-line.
- **ADD**: comparison query at GET /api/projects/{id}/compare with optional `offer_ids` query param:
  - Returns row-major structure where each row is either a MatchGroup OR an unmatched OfferLine.
  - For each row, a map `{offerId → OfferLine | null}` (null = MISSING from that offer).
  - Per-Offer aggregates: `total_excl_vat`, `total_incl_vat`, `matched_count`, `missing_count`.
  - Per-row warning flags: `CURRENCY_MISMATCH` (lines span multiple currencies), `UNIT_MISMATCH` (lines span different units).
- **ADD**: side-by-side comparison view at `app/projects/[id]/compare`:
  - Frozen first column = MatchGroup label or unmatched-line label.
  - One column per selected Offer; cell shows qty × unit_price = line_total, plus unit chip; tooltip shows full description + VAT.
  - MISSING cells rendered red with "—".
  - Footer row: per-Offer totals + "(M missing of N rows)" badge.
- **ADD**: "Match mode" interaction:
  - User presses M (or toggle button) to enter match mode.
  - Clicking lines across different offers groups them into a MatchGroup.
  - Press Enter to finalize; the system creates the MatchGroup and labels it from the first selected line's text.
  - Esc exits match mode without changes.
- **ADD**: "Match by exact label text" one-shot helper:
  - Scans selected Offers for OfferLines with identical (case-insensitive, trimmed) labels.
  - Proposes draft MatchGroups; user accepts/rejects each proposal individually.
  - Auto-matches are NOT applied without user confirmation.
- **ADD**: **While-entering recommendations** — when the user is editing an Offer (DRAFT or RECEIVED status), a side panel lists OfferLines present in OTHER offers of the SAME Project that are NOT yet represented in the current offer. Detection sources, in order of confidence:
  1. **MatchGroup membership**: any MatchGroup that contains at least one line in another offer but no line in the current offer.
  2. **Label similarity**: OfferLines from other offers whose `label` matches no line in the current offer (case-insensitive, whitespace-trimmed; exact equality only — no fuzzy matching in Phase 1).
  - For each recommendation, the user can: (a) "Add to my offer" → copies the line into the current offer with qty/unit_price editable in a quick form, and creates/extends a MatchGroup linking the new line to the source(s); (b) "Mark as same" → if a similar line already exists in the current offer, group them without copying; (c) "Dismiss" → marks this suggestion as ignored for this offer (persisted as `dismissed_recommendation` row so it doesn't keep appearing).
- **ADD**: Unmatch action on grouped cells; groups with single members auto-dissolve.
- **ADD**: Mismatch warnings (currency, unit) on MatchGroups whose lines disagree — flag visibly; do NOT auto-convert.
- **NO LLM / fuzzy matching / automatic suggestion based on description.** Matching is fully user-driven; the auto-helper only handles exact label equality.
- **Future hook**: Phase 2 (`add-wbs-catalog`) introduces `wbs_item_id` on OfferLine. The comparison query SHALL prefer `wbs_item_id` as the primary grouping key when populated, falling back to MatchGroup otherwise. Migration: an admin action will offer to convert existing MatchGroups into WBS assignments where labels match.

## Impact
- **Affected specs**: NEW capability `bid-comparison`.
- **Affected code**:
  - Backend: new package `eu.px.genba.compare.*`; Liquibase changeset `db.changelog-genba-007-compare.xml` adding `offer_line_match_group` and `match_group_line`.
  - Frontend: new route `app/projects/[id]/compare/page.tsx` with a side-by-side grid component (frozen first column, dynamic offer columns); match-mode interaction layer.
- **Risk**: The match-interaction UX is the make-or-break of this MVP. If users find it tedious to manually pair lines across 3 offers, the feature fails. Mitigation:
  - Keyboard-first matching (M to enter match mode, click-click-Enter).
  - "Unmatched line counter" prominently displayed so users can drive to zero.
  - "Match by exact label text" auto-helper covers the easy cases.
  - Inline confidence: when the same vendor reuses the same label across offers (e.g., updated offers), exact-match works perfectly.
- **Risk**: MatchGroup state becomes inconsistent with later WBS data. Mitigation: backend comparison query treats both `wbs_item_id` (when present) and MatchGroup as sources; explicit migration action surfaces conflicts when Phase 2 ships.
