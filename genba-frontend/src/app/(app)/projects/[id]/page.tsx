"use client";

import { Card, Descriptions, Skeleton, Tabs, Tag, Typography } from "antd";
import { useQuery } from "@tanstack/react-query";
import { useLocale } from "next-intl";
import { useParams } from "next/navigation";
import { AttachmentList } from "@/components/AttachmentList";
import { AttachmentUpload } from "@/components/AttachmentUpload";
import { OfferListPanel } from "@/components/OfferListPanel";
import { projectsApi } from "@/lib/projects/api";
import type { ProjectStatus } from "@/lib/projects/types";

const { Title } = Typography;

const STATUS_COLOR: Record<ProjectStatus, string> = {
  PLANNING: "blue",
  IN_PROGRESS: "gold",
  ON_HOLD: "default",
  COMPLETED: "green",
  ARCHIVED: "default",
};

export default function ProjectDetailPage(): React.ReactElement {
  const { id } = useParams<{ id: string }>();
  const locale = useLocale();

  const { data, isLoading } = useQuery({
    queryKey: ["projects", id],
    queryFn: () => projectsApi.get(id),
    enabled: Boolean(id),
  });

  if (isLoading || !data) {
    return (
      <Card>
        <Skeleton active />
      </Card>
    );
  }

  return (
    <div className="space-y-4">
      <Card>
        <Title level={3}>{data.name}</Title>
        <Descriptions column={2} size="small" bordered>
          <Descriptions.Item label={locale === "ro" ? "Status" : "Status"}>
            <Tag color={STATUS_COLOR[data.status]}>{data.status}</Tag>
          </Descriptions.Item>
          <Descriptions.Item label={locale === "ro" ? "Monedă" : "Currency"}>
            {data.baseCurrency}
          </Descriptions.Item>
          <Descriptions.Item label={locale === "ro" ? "Adresă" : "Address"} span={2}>
            {data.address ?? "—"}
          </Descriptions.Item>
          {data.description && (
            <Descriptions.Item label={locale === "ro" ? "Descriere" : "Description"} span={2}>
              {data.description}
            </Descriptions.Item>
          )}
          <Descriptions.Item label={locale === "ro" ? "Creat" : "Created"}>
            {new Date(data.createdAt).toLocaleString()}
          </Descriptions.Item>
          <Descriptions.Item label={locale === "ro" ? "Actualizat" : "Updated"}>
            {new Date(data.updatedAt).toLocaleString()}
          </Descriptions.Item>
        </Descriptions>
      </Card>

      <Card>
        <Tabs
          defaultActiveKey="attachments"
          items={[
            {
              key: "attachments",
              label: locale === "ro" ? "Atașamente" : "Attachments",
              children: (
                <div className="space-y-6">
                  <AttachmentUpload type="PROJECT" id={data.id} />
                  <AttachmentList type="PROJECT" id={data.id} />
                </div>
              ),
            },
            {
              key: "offers",
              label: locale === "ro" ? "Oferte" : "Offers",
              children: <OfferListPanel projectId={data.id} />,
            },
            {
              key: "wbs",
              label: "WBS",
              disabled: true,
              children: (
                <Typography.Text type="secondary">
                  {locale === "ro"
                    ? "Disponibil în add-wbs-catalog (Phase 2)."
                    : "Lands with add-wbs-catalog (Phase 2)."}
                </Typography.Text>
              ),
            },
          ]}
        />
      </Card>
    </div>
  );
}
