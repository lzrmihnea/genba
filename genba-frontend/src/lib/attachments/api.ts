import { apiClient } from "@/lib/api/client";
import type {
  AttachableType,
  Attachment,
  AttachmentSourceChannel,
  CreateChannelInput,
  CreateLinkAttachmentInput,
  CreateTextAttachmentInput,
} from "@/lib/attachments/types";

export const attachmentChannelsApi = {
  async list(): Promise<AttachmentSourceChannel[]> {
    const response = await apiClient.get<AttachmentSourceChannel[]>("/attachment-channels");
    return response.data;
  },

  async create(input: CreateChannelInput): Promise<AttachmentSourceChannel> {
    const response = await apiClient.post<AttachmentSourceChannel>("/attachment-channels", input);
    return response.data;
  },

  async delete(id: string): Promise<void> {
    await apiClient.delete(`/attachment-channels/${id}`);
  },
};

export const attachmentsApi = {
  async list(type: AttachableType, id: string): Promise<Attachment[]> {
    const response = await apiClient.get<Attachment[]>("/attachments", {
      params: { type, id },
    });
    return response.data;
  },

  async createFile(params: {
    attachableType: AttachableType;
    attachableId: string;
    sourceChannelId: string;
    file: File;
  }): Promise<Attachment> {
    const form = new FormData();
    form.append("attachableType", params.attachableType);
    form.append("attachableId", params.attachableId);
    form.append("sourceChannelId", params.sourceChannelId);
    form.append("file", params.file);
    const response = await apiClient.post<Attachment>("/attachments", form, {
      headers: { "Content-Type": "multipart/form-data" },
    });
    return response.data;
  },

  async createText(input: CreateTextAttachmentInput): Promise<Attachment> {
    const response = await apiClient.post<Attachment>("/attachments/text", input);
    return response.data;
  },

  async createLink(input: CreateLinkAttachmentInput): Promise<Attachment> {
    const response = await apiClient.post<Attachment>("/attachments/link", input);
    return response.data;
  },

  async delete(id: string): Promise<void> {
    await apiClient.delete(`/attachments/${id}`);
  },

  downloadUrl(id: string): string {
    const base = process.env.NEXT_PUBLIC_API_BASE_URL ?? "http://localhost:8086/api";
    return `${base}/attachments/${id}/download`;
  },
};
