import type { OfferLine } from "@/lib/offers/types";

export type CompareRowKind = "GROUP" | "UNMATCHED";

export interface CompareRow {
  rowKey: string;
  kind: CompareRowKind;
  label: string;
  /** keyed by offerId; null value = that offer is MISSING this row's scope */
  cells: Record<string, OfferLine | null>;
  warnings: string[];
}

export interface CompareAggregate {
  totalExclVat: number;
  totalInclVat: number;
  matchedCount: number;
  missingCount: number;
}

export interface CompareResult {
  offerIds: string[];
  rows: CompareRow[];
  perOfferAggregate: Record<string, CompareAggregate>;
}

export interface MatchSuggestion {
  label: string;
  candidateLineIds: string[];
}

export interface Recommendation {
  sourceLineId: string;
  sourceOfferId: string;
  sourceVendorName: string;
  label: string;
  qty: number;
  unit: string;
  unitPrice: number;
  currencyCode: string;
  vatRate: number;
  sourceMatchGroupId: string | null;
  source: "MATCH_GROUP" | "LABEL";
  similarCount: number;
}
