import { apiClient } from "@/lib/api/client";
import type { CreateVendorInput, Vendor } from "@/lib/vendors/types";

export const vendorsApi = {
  async list(projectId?: string): Promise<Vendor[]> {
    const response = await apiClient.get<Vendor[]>("/vendors", {
      params: projectId ? { project_id: projectId } : undefined,
    });
    return response.data;
  },

  async create(input: CreateVendorInput): Promise<Vendor> {
    const response = await apiClient.post<Vendor>("/vendors", input);
    return response.data;
  },

  async delete(id: string): Promise<void> {
    await apiClient.delete(`/vendors/${id}`);
  },
};
