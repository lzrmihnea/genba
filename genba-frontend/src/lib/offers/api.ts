import { apiClient } from "@/lib/api/client";
import type {
  CreateOfferInput,
  Offer,
  OfferLine,
  OfferLineInput,
  OfferStatus,
  OfferSummary,
} from "@/lib/offers/types";

export const offersApi = {
  async list(projectId: string, status?: OfferStatus): Promise<OfferSummary[]> {
    const response = await apiClient.get<OfferSummary[]>("/offers", {
      params: { project_id: projectId, ...(status ? { status } : {}) },
    });
    return response.data;
  },

  async get(id: string): Promise<Offer> {
    const response = await apiClient.get<Offer>(`/offers/${id}`);
    return response.data;
  },

  async create(input: CreateOfferInput): Promise<Offer> {
    const response = await apiClient.post<Offer>("/offers", input);
    return response.data;
  },

  async changeStatus(id: string, status: OfferStatus): Promise<Offer> {
    const response = await apiClient.patch<Offer>(`/offers/${id}/status`, { status });
    return response.data;
  },

  async clone(id: string, input: { vendorId?: string; label?: string; receivedAt?: string }): Promise<Offer> {
    const response = await apiClient.post<Offer>(`/offers/${id}/clone`, input);
    return response.data;
  },

  async delete(id: string): Promise<void> {
    await apiClient.delete(`/offers/${id}`);
  },

  async addLine(offerId: string, input: OfferLineInput): Promise<OfferLine> {
    const response = await apiClient.post<OfferLine>(`/offers/${offerId}/lines`, input);
    return response.data;
  },

  async addBulk(offerId: string, inputs: OfferLineInput[]): Promise<OfferLine[]> {
    const response = await apiClient.post<OfferLine[]>(`/offers/${offerId}/lines/bulk`, inputs);
    return response.data;
  },

  async updateLine(lineId: string, offerId: string, input: OfferLineInput): Promise<OfferLine> {
    const response = await apiClient.put<OfferLine>(`/lines/${lineId}`, input, {
      params: { offer_id: offerId },
    });
    return response.data;
  },

  async deleteLine(lineId: string, offerId: string): Promise<void> {
    await apiClient.delete(`/lines/${lineId}`, { params: { offer_id: offerId } });
  },
};
