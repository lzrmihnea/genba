"use client";

import { ArrowLeftOutlined, CopyOutlined } from "@ant-design/icons";
import { Button, Card, Descriptions, Flex, Skeleton, Space, Tabs, Tag, Typography } from "antd";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useLocale } from "next-intl";
import { useParams, useRouter } from "next/navigation";
import { AttachmentList } from "@/components/AttachmentList";
import { AttachmentUpload } from "@/components/AttachmentUpload";
import { OfferLineGrid } from "@/components/OfferLineGrid";
import { offersApi } from "@/lib/offers/api";
import { OFFER_STATUS_TRANSITIONS, type OfferStatus } from "@/lib/offers/types";

const { Title, Text } = Typography;

const STATUS_COLOR: Record<OfferStatus, string> = {
  DRAFT: "default",
  RECEIVED: "blue",
  ACCEPTED: "green",
  REJECTED: "red",
  EXPIRED: "default",
};

export default function OfferEditorPage(): React.ReactElement {
  const { id: projectId, offerId } = useParams<{ id: string; offerId: string }>();
  const locale = useLocale();
  const router = useRouter();
  const queryClient = useQueryClient();

  const { data: offer, isLoading } = useQuery({
    queryKey: ["offer", offerId],
    queryFn: () => offersApi.get(offerId),
    enabled: Boolean(offerId),
  });

  const statusMutation = useMutation({
    mutationFn: (status: OfferStatus) => offersApi.changeStatus(offerId, status),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ["offer", offerId] }),
  });

  const cloneMutation = useMutation({
    mutationFn: () => offersApi.clone(offerId, {}),
    onSuccess: (clone) => {
      void queryClient.invalidateQueries({ queryKey: ["offers", projectId] });
      router.push(`/projects/${projectId}/offers/${clone.id}`);
    },
  });

  if (isLoading || !offer) {
    return (
      <Card>
        <Skeleton active />
      </Card>
    );
  }

  const nextStatuses = OFFER_STATUS_TRANSITIONS[offer.status];

  return (
    <div className="space-y-4">
      <Flex justify="space-between" align="center">
        <Button icon={<ArrowLeftOutlined />} onClick={() => router.push(`/projects/${projectId}`)}>
          {locale === "ro" ? "Înapoi la proiect" : "Back to project"}
        </Button>
        <Button icon={<CopyOutlined />} loading={cloneMutation.isPending} onClick={() => cloneMutation.mutate()}>
          {locale === "ro" ? "Clonează oferta" : "Clone offer"}
        </Button>
      </Flex>

      <Card>
        <Flex justify="space-between" align="flex-start">
          <Title level={3} className="!mb-0">
            {offer.label ?? (locale === "ro" ? "Ofertă" : "Offer")}
          </Title>
          <Space>
            <Tag color={STATUS_COLOR[offer.status]}>{offer.status}</Tag>
            {nextStatuses.map((s) => (
              <Button key={s} size="small" loading={statusMutation.isPending} onClick={() => statusMutation.mutate(s)}>
                → {s}
              </Button>
            ))}
          </Space>
        </Flex>
        <Descriptions column={2} size="small" className="mt-3">
          <Descriptions.Item label={locale === "ro" ? "Monedă" : "Currency"}>
            {offer.currencyCode}
          </Descriptions.Item>
          <Descriptions.Item label={locale === "ro" ? "Primită" : "Received"}>
            {offer.receivedAt ? new Date(offer.receivedAt).toLocaleDateString() : "—"}
          </Descriptions.Item>
          <Descriptions.Item label={locale === "ro" ? "Total fără TVA" : "Total excl VAT"}>
            <Text strong>{offer.totalAmountExclVat.toFixed(2)}</Text>
          </Descriptions.Item>
          <Descriptions.Item label={locale === "ro" ? "Total cu TVA" : "Total incl VAT"}>
            {offer.totalAmountInclVat.toFixed(2)}
          </Descriptions.Item>
        </Descriptions>
      </Card>

      <Card>
        <Tabs
          defaultActiveKey="lines"
          items={[
            {
              key: "lines",
              label: locale === "ro" ? "Linii" : "Lines",
              children: <OfferLineGrid offer={offer} />,
            },
            {
              key: "attachments",
              label: locale === "ro" ? "Atașamente" : "Attachments",
              children: (
                <div className="space-y-6">
                  <AttachmentUpload type="OFFER" id={offer.id} />
                  <AttachmentList type="OFFER" id={offer.id} />
                </div>
              ),
            },
          ]}
        />
      </Card>
    </div>
  );
}
