"use client";

import { DeleteOutlined, DownloadOutlined } from "@ant-design/icons";
import { Button, Empty, List, Popconfirm, Space, Tag, Typography } from "antd";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useLocale } from "next-intl";
import { attachmentChannelsApi, attachmentsApi } from "@/lib/attachments/api";
import type { AttachableType, Attachment } from "@/lib/attachments/types";

const { Text, Paragraph } = Typography;

interface AttachmentListProps {
  type: AttachableType;
  id: string;
}

const KIND_TAG_COLOR: Record<Attachment["kind"], string> = {
  FILE: "blue",
  TEXT: "purple",
  LINK: "geekblue",
};

export function AttachmentList({ type, id }: AttachmentListProps): React.ReactElement {
  const queryClient = useQueryClient();
  const locale = useLocale();

  const { data, isLoading } = useQuery({
    queryKey: ["attachments", type, id],
    queryFn: () => attachmentsApi.list(type, id),
  });

  const { data: channels } = useQuery({
    queryKey: ["attachment-channels"],
    queryFn: () => attachmentChannelsApi.list(),
  });

  const channelLabel = (channelId: string): string => {
    const channel = channels?.find((c) => c.id === channelId);
    if (!channel) return "";
    return locale === "ro" ? channel.labelRo : channel.labelEn;
  };

  const deleteMutation = useMutation({
    mutationFn: (attachmentId: string) => attachmentsApi.delete(attachmentId),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ["attachments", type, id] });
    },
  });

  if (!isLoading && (!data || data.length === 0)) {
    return <Empty description={locale === "ro" ? "Niciun atașament" : "No attachments"} />;
  }

  return (
    <List
      loading={isLoading}
      itemLayout="vertical"
      dataSource={data ?? []}
      renderItem={(item) => (
        <List.Item
          key={item.id}
          actions={[
            item.kind === "FILE" && (
              <a
                key="download"
                href={attachmentsApi.downloadUrl(item.id)}
                target="_blank"
                rel="noreferrer"
              >
                <DownloadOutlined /> {locale === "ro" ? "Descarcă" : "Download"}
              </a>
            ),
            item.kind === "LINK" && item.fileUrl && (
              <a key="open" href={item.fileUrl} target="_blank" rel="noreferrer">
                {locale === "ro" ? "Deschide" : "Open"}
              </a>
            ),
            <Popconfirm
              key="delete"
              title={locale === "ro" ? "Ștergi atașamentul?" : "Delete attachment?"}
              onConfirm={() => deleteMutation.mutate(item.id)}
              okType="danger"
            >
              <Button type="text" danger icon={<DeleteOutlined />} size="small">
                {locale === "ro" ? "Șterge" : "Delete"}
              </Button>
            </Popconfirm>,
          ].filter(Boolean) as React.ReactNode[]}
        >
          <Space size="small" className="mb-1">
            <Tag color={KIND_TAG_COLOR[item.kind]}>{item.kind}</Tag>
            <Tag>{channelLabel(item.sourceChannelId)}</Tag>
            <Text type="secondary" className="text-xs">
              {new Date(item.uploadedAt).toLocaleString()}
            </Text>
          </Space>
          {item.kind === "FILE" && (
            <div>
              <Text strong>{item.originalFilename}</Text>
              {item.byteSize !== null && (
                <Text type="secondary" className="ml-2 text-xs">
                  {(item.byteSize / 1024).toFixed(1)} KB
                </Text>
              )}
            </div>
          )}
          {item.kind === "TEXT" && item.rawText && (
            <Paragraph ellipsis={{ rows: 4, expandable: true }}>{item.rawText}</Paragraph>
          )}
          {item.kind === "LINK" && item.fileUrl && (
            <Text className="break-all">{item.fileUrl}</Text>
          )}
        </List.Item>
      )}
    />
  );
}
