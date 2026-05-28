export interface Vendor {
  id: string;
  orgId: string;
  projectId: string | null;
  name: string;
  contactName: string | null;
  phone: string | null;
  email: string | null;
  vatId: string | null;
  defaultRetentionPct: number | null;
  notes: string | null;
}

export interface CreateVendorInput {
  name: string;
  contactName?: string;
  phone?: string;
  email?: string;
  vatId?: string;
  defaultRetentionPct?: number;
  notes?: string;
  projectId?: string;
}
