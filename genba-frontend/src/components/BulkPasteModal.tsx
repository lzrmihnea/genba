"use client";

import { Alert, Modal, Table, Typography } from "antd";
import { useMemo, useState } from "react";
import { useLocale } from "next-intl";
import type { OfferLineInput } from "@/lib/offers/types";

const { Paragraph, Text } = Typography;

interface BulkPasteModalProps {
  open: boolean;
  onClose: () => void;
  onConfirm: (lines: OfferLineInput[]) => void;
  pending?: boolean;
}

/**
 * Parses a tab- or comma-separated paste. Column order:
 *   label, qty, unit, unit_price, vat_rate
 * Missing trailing columns fall back to sensible defaults so a bare
 * "label<TAB>qty<TAB>price" paste still works.
 */
function parse(text: string): OfferLineInput[] {
  return text
    .split(/\r?\n/)
    .map((line) => line.trim())
    .filter((line) => line.length > 0)
    .map((line) => {
      const cols = line.includes("\t") ? line.split("\t") : line.split(",");
      const [label, qty, unit, unitPrice, vatRate] = cols.map((c) => c.trim());
      return {
        label: label ?? "",
        qty: Number(qty ?? "1") || 1,
        unit: unit && unit.length > 0 ? unit : "buc",
        unitPrice: Number(unitPrice ?? "0") || 0,
        vatRate: vatRate !== undefined && vatRate.length > 0 ? Number(vatRate) || 19 : 19,
      } satisfies OfferLineInput;
    })
    .filter((l) => l.label.length > 0);
}

export function BulkPasteModal({ open, onClose, onConfirm, pending }: BulkPasteModalProps): React.ReactElement {
  const locale = useLocale();
  const [raw, setRaw] = useState("");

  const parsed = useMemo(() => parse(raw), [raw]);

  return (
    <Modal
      open={open}
      onCancel={onClose}
      onOk={() => onConfirm(parsed)}
      okButtonProps={{ disabled: parsed.length === 0, loading: pending }}
      okText={locale === "ro" ? `Adaugă ${parsed.length} linii` : `Add ${parsed.length} lines`}
      cancelText={locale === "ro" ? "Anulează" : "Cancel"}
      title={locale === "ro" ? "Lipește din tabel" : "Paste from spreadsheet"}
      width={760}
    >
      <Paragraph type="secondary">
        {locale === "ro"
          ? "O linie per rând. Coloane (tab sau virgulă): etichetă, cantitate, UM, preț unitar, TVA %."
          : "One row per line. Columns (tab- or comma-separated): label, qty, unit, unit price, VAT %."}
      </Paragraph>
      <textarea
        className="w-full rounded border border-neutral-300 p-2 font-mono text-sm"
        rows={8}
        value={raw}
        onChange={(e) => setRaw(e.target.value)}
        placeholder={"Săpătură fundație\t80\tm3\t15\t19\nHidroizolație\t120\tm2\t3.75\t19"}
      />
      {parsed.length > 0 && (
        <>
          <Text strong className="mt-3 block">
            {locale === "ro" ? "Previzualizare" : "Preview"}
          </Text>
          <Table
            size="small"
            className="mt-2"
            rowKey={(_, i) => String(i)}
            pagination={false}
            scroll={{ y: 240 }}
            dataSource={parsed}
            columns={[
              { title: locale === "ro" ? "Etichetă" : "Label", dataIndex: "label" },
              { title: "Qty", dataIndex: "qty", width: 80 },
              { title: "UM", dataIndex: "unit", width: 70 },
              { title: locale === "ro" ? "Preț" : "Price", dataIndex: "unitPrice", width: 90 },
              { title: "VAT%", dataIndex: "vatRate", width: 70 },
            ]}
          />
        </>
      )}
      {raw.trim().length > 0 && parsed.length === 0 && (
        <Alert
          className="mt-2"
          type="warning"
          showIcon
          message={locale === "ro" ? "Nicio linie validă detectată" : "No valid lines detected"}
        />
      )}
    </Modal>
  );
}
