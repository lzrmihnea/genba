"use client";

import { LinkOutlined, MessageOutlined, UploadOutlined } from "@ant-design/icons";
import { Alert, Button, Input, Segmented, Space, Upload } from "antd";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { isAxiosError } from "axios";
import { useLocale, useTranslations } from "next-intl";
import { useState } from "react";
import { ChannelSelector } from "@/components/ChannelSelector";
import { attachmentsApi } from "@/lib/attachments/api";
import type { AttachableType } from "@/lib/attachments/types";
import type { ApiError } from "@/lib/auth/types";

const { TextArea } = Input;

interface AttachmentUploadProps {
  type: AttachableType;
  id: string;
}

type Mode = "FILE" | "TEXT" | "LINK";

export function AttachmentUpload({ type, id }: AttachmentUploadProps): React.ReactElement {
  const t = useTranslations();
  const locale = useLocale();
  const queryClient = useQueryClient();

  const [mode, setMode] = useState<Mode>("TEXT");
  const [channelId, setChannelId] = useState<string | undefined>(undefined);
  const [textValue, setTextValue] = useState("");
  const [linkValue, setLinkValue] = useState("");
  const [error, setError] = useState<string | null>(null);

  const invalidate = (): void => {
    void queryClient.invalidateQueries({ queryKey: ["attachments", type, id] });
  };

  const fileMutation = useMutation({
    mutationFn: (file: File) =>
      attachmentsApi.createFile({ attachableType: type, attachableId: id, sourceChannelId: channelId!, file }),
    onSuccess: invalidate,
    onError: handleError,
  });

  const textMutation = useMutation({
    mutationFn: () =>
      attachmentsApi.createText({
        attachableType: type,
        attachableId: id,
        sourceChannelId: channelId!,
        rawText: textValue,
      }),
    onSuccess: () => {
      setTextValue("");
      invalidate();
    },
    onError: handleError,
  });

  const linkMutation = useMutation({
    mutationFn: () =>
      attachmentsApi.createLink({
        attachableType: type,
        attachableId: id,
        sourceChannelId: channelId!,
        url: linkValue,
      }),
    onSuccess: () => {
      setLinkValue("");
      invalidate();
    },
    onError: handleError,
  });

  function handleError(err: unknown): void {
    if (isAxiosError<ApiError>(err) && err.response?.data?.message) {
      setError(err.response.data.message);
    } else {
      setError(t("common.error"));
    }
  }

  const channelMissing = !channelId;

  return (
    <Space direction="vertical" size="middle" className="w-full">
      {error && (
        <Alert
          type="error"
          message={error}
          closable
          onClose={() => setError(null)}
          showIcon
        />
      )}

      <Space wrap>
        <Segmented
          value={mode}
          onChange={(v) => setMode(v as Mode)}
          options={[
            { label: locale === "ro" ? "Text" : "Text", value: "TEXT", icon: <MessageOutlined /> },
            { label: locale === "ro" ? "Fișier" : "File", value: "FILE", icon: <UploadOutlined /> },
            { label: locale === "ro" ? "Link" : "Link", value: "LINK", icon: <LinkOutlined /> },
          ]}
        />
        <ChannelSelector
          value={channelId}
          onChange={(id) => setChannelId(id)}
          placeholder={locale === "ro" ? "Canal sursă" : "Source channel"}
        />
      </Space>

      {mode === "TEXT" && (
        <Space direction="vertical" className="w-full">
          <TextArea
            rows={4}
            value={textValue}
            onChange={(e) => setTextValue(e.target.value)}
            placeholder={
              locale === "ro"
                ? "Lipește mesajul WhatsApp / corpul email-ului aici"
                : "Paste WhatsApp message / email body here"
            }
            disabled={channelMissing}
          />
          <Button
            type="primary"
            onClick={() => textMutation.mutate()}
            loading={textMutation.isPending}
            disabled={channelMissing || textValue.trim().length === 0}
          >
            {locale === "ro" ? "Atașează text" : "Attach text"}
          </Button>
        </Space>
      )}

      {mode === "FILE" && (
        <Upload
          beforeUpload={(file) => {
            fileMutation.mutate(file as File);
            return false;
          }}
          showUploadList={false}
          disabled={channelMissing}
        >
          <Button icon={<UploadOutlined />} disabled={channelMissing} loading={fileMutation.isPending}>
            {locale === "ro" ? "Selectează fișier" : "Choose file"}
          </Button>
        </Upload>
      )}

      {mode === "LINK" && (
        <Space direction="vertical" className="w-full">
          <Input
            value={linkValue}
            onChange={(e) => setLinkValue(e.target.value)}
            placeholder="https://"
            disabled={channelMissing}
          />
          <Button
            type="primary"
            onClick={() => linkMutation.mutate()}
            loading={linkMutation.isPending}
            disabled={channelMissing || linkValue.trim().length === 0}
          >
            {locale === "ro" ? "Atașează link" : "Attach link"}
          </Button>
        </Space>
      )}
    </Space>
  );
}
