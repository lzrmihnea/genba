"use client";

import { ArrowLeftOutlined, BulbOutlined } from "@ant-design/icons";
import { Button, Card, Checkbox, Empty, Flex, Spin, Table, Tag, Tooltip, Typography } from "antd";
import type { TableProps } from "antd";
import { useQuery } from "@tanstack/react-query";
import { useLocale } from "next-intl";
import { useParams, useRouter } from "next/navigation";
import { useMemo, useState } from "react";
import { SuggestMatchesModal } from "@/components/SuggestMatchesModal";
import { compareApi } from "@/lib/compare/api";
import type { CompareRow } from "@/lib/compare/types";
import { offersApi } from "@/lib/offers/api";

const { Title, Text } = Typography;

export default function ComparePage(): React.ReactElement {
  const { id: projectId } = useParams<{ id: string }>();
  const locale = useLocale();
  const router = useRouter();
  const [selected, setSelected] = useState<string[] | null>(null);
  const [suggestOpen, setSuggestOpen] = useState(false);

  const { data: offers } = useQuery({
    queryKey: ["offers", projectId],
    queryFn: () => offersApi.list(projectId),
  });

  // Default selection = all comparable offers (let backend decide) until the user picks.
  const effectiveSelection = selected ?? (offers ?? []).map((o) => o.id);

  const { data: result, isLoading } = useQuery({
    queryKey: ["compare", projectId, effectiveSelection],
    queryFn: () => compareApi.compare(projectId, effectiveSelection),
    enabled: (offers?.length ?? 0) > 0,
  });

  const vendorLabel = (offerId: string): string => {
    const o = offers?.find((x) => x.id === offerId);
    return o?.label ?? offerId.slice(0, 8);
  };

  const columns: TableProps<CompareRow>["columns"] = useMemo(() => {
    const base: TableProps<CompareRow>["columns"] = [
      {
        title: locale === "ro" ? "Articol" : "Item",
        dataIndex: "label",
        fixed: "left",
        width: 260,
        render: (label: string, row) => (
          <span>
            {label}
            {row.kind === "GROUP" && (
              <Tag color="blue" className="ml-2">
                {locale === "ro" ? "grupat" : "matched"}
              </Tag>
            )}
            {row.warnings.map((w) => (
              <Tag key={w} color="orange" className="ml-1">
                {w === "CURRENCY_MISMATCH" ? "≠ currency" : w === "UNIT_MISMATCH" ? "≠ unit" : w}
              </Tag>
            ))}
          </span>
        ),
      },
    ];
    const offerCols = (result?.offerIds ?? []).map((offerId) => ({
      title: vendorLabel(offerId),
      key: offerId,
      align: "right" as const,
      width: 160,
      render: (_: unknown, row: CompareRow) => {
        const cell = row.cells[offerId];
        if (!cell) {
          return <Text type="danger">—</Text>;
        }
        return (
          <Tooltip title={`${cell.qty} ${cell.unit} × ${cell.unitPrice} · VAT ${cell.vatRate}%`}>
            <Text>{cell.lineTotalExclVat.toFixed(2)}</Text>
          </Tooltip>
        );
      },
    }));
    return [...base, ...offerCols];
  }, [result, offers, locale]);

  return (
    <div className="space-y-4">
      <Flex justify="space-between" align="center">
        <Button icon={<ArrowLeftOutlined />} onClick={() => router.push(`/projects/${projectId}`)}>
          {locale === "ro" ? "Înapoi la proiect" : "Back to project"}
        </Button>
        <Button icon={<BulbOutlined />} onClick={() => setSuggestOpen(true)}>
          {locale === "ro" ? "Sugestii de potrivire" : "Suggest matches"}
        </Button>
      </Flex>

      <Card>
        <Title level={3}>{locale === "ro" ? "Comparație oferte" : "Offer comparison"}</Title>

        {offers && offers.length > 0 && (
          <Checkbox.Group
            className="mb-4"
            value={effectiveSelection}
            onChange={(vals) => setSelected(vals as string[])}
            options={offers.map((o) => ({ label: o.label ?? o.id.slice(0, 8), value: o.id }))}
          />
        )}

        {isLoading ? (
          <Flex justify="center" className="py-8">
            <Spin />
          </Flex>
        ) : result && result.rows.length > 0 ? (
          <Table<CompareRow>
            rowKey="rowKey"
            size="small"
            dataSource={result.rows}
            columns={columns}
            pagination={false}
            scroll={{ x: "max-content" }}
            summary={() => (
              <Table.Summary fixed>
                <Table.Summary.Row>
                  <Table.Summary.Cell index={0}>
                    <Text strong>{locale === "ro" ? "Total fără TVA" : "Total excl VAT"}</Text>
                  </Table.Summary.Cell>
                  {result.offerIds.map((offerId, i) => (
                    <Table.Summary.Cell key={offerId} index={i + 1} align="right">
                      <Text strong>{result.perOfferAggregate[offerId]?.totalExclVat.toFixed(2) ?? "—"}</Text>
                    </Table.Summary.Cell>
                  ))}
                </Table.Summary.Row>
                <Table.Summary.Row>
                  <Table.Summary.Cell index={0}>
                    <Text type="secondary">{locale === "ro" ? "Lipsă" : "Missing"}</Text>
                  </Table.Summary.Cell>
                  {result.offerIds.map((offerId, i) => {
                    const missing = result.perOfferAggregate[offerId]?.missingCount ?? 0;
                    return (
                      <Table.Summary.Cell key={offerId} index={i + 1} align="right">
                        {missing > 0 ? <Text type="danger">{missing}</Text> : <Text type="secondary">0</Text>}
                      </Table.Summary.Cell>
                    );
                  })}
                </Table.Summary.Row>
              </Table.Summary>
            )}
          />
        ) : (
          <Empty
            description={
              locale === "ro" ? "Adaugă oferte cu linii pentru a compara." : "Add offers with lines to compare."
            }
          />
        )}
      </Card>

      <SuggestMatchesModal
        open={suggestOpen}
        onClose={() => setSuggestOpen(false)}
        projectId={projectId}
        offerIds={effectiveSelection}
      />
    </div>
  );
}
