export type OfferStatus = "DRAFT" | "RECEIVED" | "ACCEPTED" | "REJECTED" | "EXPIRED";

export interface OfferLine {
  id: string;
  offerId: string;
  lineOrder: number;
  label: string;
  description: string | null;
  qty: number;
  unit: string;
  unitPrice: number;
  currencyCode: string;
  vatRate: number;
  lineTotalExclVat: number;
  lineTotalInclVat: number;
  wbsItemId: string | null;
  normalizedKey: string | null;
  notes: string | null;
}

export interface OfferSummary {
  id: string;
  projectId: string;
  vendorId: string;
  label: string | null;
  receivedAt: string | null;
  validUntil: string | null;
  currencyCode: string;
  totalAmountExclVat: number;
  totalAmountInclVat: number;
  status: OfferStatus;
  createdAt: string;
}

export interface Offer {
  id: string;
  projectId: string;
  vendorId: string;
  label: string | null;
  receivedAt: string | null;
  validUntil: string | null;
  currencyCode: string;
  totalAmountExclVat: number;
  totalAmountInclVat: number;
  status: OfferStatus;
  notes: string | null;
  createdAt: string;
  updatedAt: string;
  lines: OfferLine[];
}

export interface CreateOfferInput {
  projectId: string;
  vendorId: string;
  label?: string;
  receivedAt?: string;
  validUntil?: string;
  currencyCode?: string;
  notes?: string;
}

export interface OfferLineInput {
  label: string;
  description?: string;
  qty: number;
  unit?: string;
  unitPrice: number;
  currencyCode?: string;
  vatRate?: number;
  notes?: string;
  lineOrder?: number;
}

export const OFFER_STATUS_TRANSITIONS: Record<OfferStatus, OfferStatus[]> = {
  DRAFT: ["RECEIVED"],
  RECEIVED: ["ACCEPTED", "REJECTED", "EXPIRED"],
  ACCEPTED: ["EXPIRED"],
  REJECTED: ["DRAFT"],
  EXPIRED: [],
};
