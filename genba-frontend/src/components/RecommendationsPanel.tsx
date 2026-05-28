"use client";

import { CloseOutlined, PlusOutlined } from "@ant-design/icons";
import { Badge, Button, Empty, List, Space, Tag, Tooltip, Typography } from "antd";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useLocale } from "next-intl";
import { recommendationsApi } from "@/lib/compare/api";
import type { Recommendation } from "@/lib/compare/types";

const { Text } = Typography;

interface RecommendationsPanelProps {
  projectId: string;
  offerId: string;
}

/**
 * While-entering panel: lines present in other offers of the project that the
 * current offer is missing. "Add" copies the line in and links it via a match
 * group; "Dismiss" hides it for this offer.
 */
export function RecommendationsPanel({ projectId, offerId }: RecommendationsPanelProps): React.ReactElement {
  const locale = useLocale();
  const queryClient = useQueryClient();

  const { data, isLoading } = useQuery({
    queryKey: ["recommendations", projectId, offerId],
    queryFn: () => recommendationsApi.missingFromOthers(projectId, offerId),
  });

  const refresh = (): void => {
    void queryClient.invalidateQueries({ queryKey: ["recommendations", projectId, offerId] });
    void queryClient.invalidateQueries({ queryKey: ["offer", offerId] });
  };

  const addMutation = useMutation({
    mutationFn: (r: Recommendation) => recommendationsApi.addFromRecommendation(offerId, r.sourceLineId),
    onSuccess: refresh,
  });

  const dismissMutation = useMutation({
    mutationFn: (r: Recommendation) => recommendationsApi.dismiss(offerId, r.sourceLineId),
    onSuccess: refresh,
  });

  const count = data?.length ?? 0;

  return (
    <div>
      <Space className="mb-2">
        <Text strong>{locale === "ro" ? "Lipsesc din oferta ta" : "Missing from your offer"}</Text>
        <Badge count={count} showZero color={count > 0 ? "orange" : "green"} />
      </Space>

      {!isLoading && count === 0 ? (
        <Empty
          image={Empty.PRESENTED_IMAGE_SIMPLE}
          description={
            locale === "ro"
              ? "Nimic — oferta acoperă tot ce au celelalte."
              : "Nothing — this offer covers everything the others list."
          }
        />
      ) : (
        <List
          loading={isLoading}
          size="small"
          dataSource={data ?? []}
          renderItem={(r) => (
            <List.Item
              actions={[
                <Tooltip key="add" title={locale === "ro" ? "Adaugă în ofertă" : "Add to my offer"}>
                  <Button
                    type="text"
                    size="small"
                    icon={<PlusOutlined />}
                    loading={addMutation.isPending}
                    onClick={() => addMutation.mutate(r)}
                  />
                </Tooltip>,
                <Tooltip key="dismiss" title={locale === "ro" ? "Ignoră" : "Dismiss"}>
                  <Button
                    type="text"
                    size="small"
                    icon={<CloseOutlined />}
                    onClick={() => dismissMutation.mutate(r)}
                  />
                </Tooltip>,
              ]}
            >
              <Space direction="vertical" size={0}>
                <Space size="small">
                  <Text>{r.label}</Text>
                  {r.source === "MATCH_GROUP" && <Tag color="blue">matched</Tag>}
                  {r.similarCount > 1 && <Tag color="orange">{r.similarCount}× offers</Tag>}
                </Space>
                <Text type="secondary" className="text-xs">
                  {r.sourceVendorName}: {r.qty} {r.unit} × {r.unitPrice} {r.currencyCode}
                </Text>
              </Space>
            </List.Item>
          )}
        />
      )}
    </div>
  );
}
