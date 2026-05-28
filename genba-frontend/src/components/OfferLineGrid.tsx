"use client";

import { DeleteOutlined, PlusOutlined, TableOutlined } from "@ant-design/icons";
import { Button, Flex, InputNumber, Popconfirm, Space, Table, Typography } from "antd";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { useLocale } from "next-intl";
import { useState } from "react";
import { BulkPasteModal } from "@/components/BulkPasteModal";
import { offersApi } from "@/lib/offers/api";
import type { Offer, OfferLine, OfferLineInput } from "@/lib/offers/types";

const { Text } = Typography;

interface OfferLineGridProps {
  offer: Offer;
}

/**
 * Editable line grid. Each row's qty/unit/price/VAT are saved on blur via a
 * PUT mutation; line + offer totals come back from the server, so the cached
 * offer query is invalidated after every mutation to refresh the footer.
 */
export function OfferLineGrid({ offer }: OfferLineGridProps): React.ReactElement {
  const locale = useLocale();
  const queryClient = useQueryClient();
  const [bulkOpen, setBulkOpen] = useState(false);
  // Local edit buffer keyed by lineId so typing doesn't fight refetches.
  const [draft, setDraft] = useState<Record<string, Partial<OfferLine>>>({});

  const invalidate = (): void => {
    void queryClient.invalidateQueries({ queryKey: ["offer", offer.id] });
  };

  const updateLine = useMutation({
    mutationFn: ({ line, patch }: { line: OfferLine; patch: Partial<OfferLine> }) =>
      offersApi.updateLine(line.id, offer.id, toInput({ ...line, ...patch })),
    onSuccess: () => {
      setDraft({});
      invalidate();
    },
  });

  const addLine = useMutation({
    mutationFn: () =>
      offersApi.addLine(offer.id, {
        label: locale === "ro" ? "Linie nouă" : "New line",
        qty: 1,
        unitPrice: 0,
        vatRate: 19,
      }),
    onSuccess: invalidate,
  });

  const deleteLine = useMutation({
    mutationFn: (lineId: string) => offersApi.deleteLine(lineId, offer.id),
    onSuccess: invalidate,
  });

  const bulkAdd = useMutation({
    mutationFn: (lines: OfferLineInput[]) => offersApi.addBulk(offer.id, lines),
    onSuccess: () => {
      setBulkOpen(false);
      invalidate();
    },
  });

  const cell = (line: OfferLine, field: keyof OfferLine): number | string =>
    (draft[line.id]?.[field] as number | string | undefined) ?? (line[field] as number | string);

  const commit = (line: OfferLine, field: keyof OfferLine): void => {
    const patch = draft[line.id];
    if (!patch || patch[field] === undefined || patch[field] === line[field]) return;
    updateLine.mutate({ line, patch: { [field]: patch[field] } });
  };

  const setLocal = (lineId: string, field: keyof OfferLine, value: number | string): void => {
    setDraft((d) => ({ ...d, [lineId]: { ...d[lineId], [field]: value } }));
  };

  return (
    <div>
      <Flex justify="space-between" align="center" className="mb-3">
        <Text strong>{locale === "ro" ? "Linii ofertă" : "Offer lines"}</Text>
        <Space>
          <Button icon={<TableOutlined />} onClick={() => setBulkOpen(true)}>
            {locale === "ro" ? "Lipește din tabel" : "Paste from spreadsheet"}
          </Button>
          <Button type="primary" icon={<PlusOutlined />} onClick={() => addLine.mutate()} loading={addLine.isPending}>
            {locale === "ro" ? "Adaugă linie" : "Add line"}
          </Button>
        </Space>
      </Flex>

      <Table<OfferLine>
        rowKey="id"
        size="small"
        pagination={false}
        dataSource={offer.lines}
        columns={[
          {
            title: locale === "ro" ? "Etichetă" : "Label",
            dataIndex: "label",
            render: (_, line) => (
              <input
                className="w-full border-0 bg-transparent outline-none focus:bg-neutral-50"
                value={String(cell(line, "label"))}
                onChange={(e) => setLocal(line.id, "label", e.target.value)}
                onBlur={() => commit(line, "label")}
              />
            ),
          },
          {
            title: "Qty",
            dataIndex: "qty",
            width: 90,
            render: (_, line) => (
              <InputNumber
                value={Number(cell(line, "qty"))}
                min={0}
                controls={false}
                onChange={(v) => setLocal(line.id, "qty", v ?? 0)}
                onBlur={() => commit(line, "qty")}
                style={{ width: "100%" }}
              />
            ),
          },
          {
            title: "UM",
            dataIndex: "unit",
            width: 70,
            render: (_, line) => (
              <input
                className="w-full border-0 bg-transparent outline-none focus:bg-neutral-50"
                value={String(cell(line, "unit"))}
                onChange={(e) => setLocal(line.id, "unit", e.target.value)}
                onBlur={() => commit(line, "unit")}
              />
            ),
          },
          {
            title: locale === "ro" ? "Preț unitar" : "Unit price",
            dataIndex: "unitPrice",
            width: 110,
            render: (_, line) => (
              <InputNumber
                value={Number(cell(line, "unitPrice"))}
                min={0}
                controls={false}
                onChange={(v) => setLocal(line.id, "unitPrice", v ?? 0)}
                onBlur={() => commit(line, "unitPrice")}
                style={{ width: "100%" }}
              />
            ),
          },
          {
            title: "VAT%",
            dataIndex: "vatRate",
            width: 80,
            render: (_, line) => (
              <InputNumber
                value={Number(cell(line, "vatRate"))}
                min={0}
                max={100}
                controls={false}
                onChange={(v) => setLocal(line.id, "vatRate", v ?? 0)}
                onBlur={() => commit(line, "vatRate")}
                style={{ width: "100%" }}
              />
            ),
          },
          {
            title: locale === "ro" ? "Total fără TVA" : "Total excl VAT",
            dataIndex: "lineTotalExclVat",
            width: 130,
            align: "right",
            render: (v: number) => <Text>{v.toFixed(2)}</Text>,
          },
          {
            title: "",
            width: 50,
            render: (_, line) => (
              <Popconfirm
                title={locale === "ro" ? "Ștergi linia?" : "Delete line?"}
                onConfirm={() => deleteLine.mutate(line.id)}
                okType="danger"
              >
                <Button type="text" danger size="small" icon={<DeleteOutlined />} />
              </Popconfirm>
            ),
          },
        ]}
        summary={() => (
          <Table.Summary fixed>
            <Table.Summary.Row>
              <Table.Summary.Cell index={0} colSpan={5}>
                <Text strong>{locale === "ro" ? "Total ofertă" : "Offer total"}</Text>
              </Table.Summary.Cell>
              <Table.Summary.Cell index={5} align="right">
                <Text strong>
                  {offer.totalAmountExclVat.toFixed(2)} {offer.currencyCode}
                </Text>
              </Table.Summary.Cell>
              <Table.Summary.Cell index={6} />
            </Table.Summary.Row>
            <Table.Summary.Row>
              <Table.Summary.Cell index={0} colSpan={5}>
                <Text type="secondary">{locale === "ro" ? "Cu TVA" : "Incl VAT"}</Text>
              </Table.Summary.Cell>
              <Table.Summary.Cell index={5} align="right">
                <Text type="secondary">
                  {offer.totalAmountInclVat.toFixed(2)} {offer.currencyCode}
                </Text>
              </Table.Summary.Cell>
              <Table.Summary.Cell index={6} />
            </Table.Summary.Row>
          </Table.Summary>
        )}
      />

      <BulkPasteModal
        open={bulkOpen}
        onClose={() => setBulkOpen(false)}
        onConfirm={(lines) => bulkAdd.mutate(lines)}
        pending={bulkAdd.isPending}
      />
    </div>
  );
}

function toInput(line: OfferLine): OfferLineInput {
  return {
    label: line.label,
    description: line.description ?? undefined,
    qty: line.qty,
    unit: line.unit,
    unitPrice: line.unitPrice,
    vatRate: line.vatRate,
    notes: line.notes ?? undefined,
    lineOrder: line.lineOrder,
  };
}
