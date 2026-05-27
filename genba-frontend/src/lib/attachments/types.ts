export type AttachmentKind = "FILE" | "TEXT" | "LINK";

export type AttachableType =
  | "PROJECT"
  | "OFFER"
  | "OFFER_LINE"
  | "VENDOR"
  | string;

export interface AttachmentSourceChannel {
  id: string;
  code: string;
  labelEn: string;
  labelRo: string;
  systemManaged: boolean;
  sortOrder: number;
}

export interface CreateChannelInput {
  code: string;
  labelEn: string;
  labelRo: string;
  sortOrder?: number;
}

export interface Attachment {
  id: string;
  attachableType: AttachableType;
  attachableId: string;
  kind: AttachmentKind;
  sourceChannelId: string;
  fileUrl: string | null;
  rawText: string | null;
  originalFilename: string | null;
  mimeType: string | null;
  byteSize: number | null;
  uploadedBy: string | null;
  uploadedAt: string;
}

export interface CreateTextAttachmentInput {
  attachableType: AttachableType;
  attachableId: string;
  sourceChannelId: string;
  rawText: string;
}

export interface CreateLinkAttachmentInput {
  attachableType: AttachableType;
  attachableId: string;
  sourceChannelId: string;
  url: string;
}
