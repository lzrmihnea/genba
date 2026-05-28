"use client";

import { PlusOutlined, TableOutlined } from "@ant-design/icons";
import { Button, Empty, Flex, Form, Input, Modal, Spin, Table, Tag } from "antd";
import type { TableProps } from "antd";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useLocale } from "next-intl";
import { useRouter } from "next/navigation";
import { useState } from "react";
import { VendorPicker } from "@/components/VendorPicker";
import { offersApi } from "@/lib/offers/api";
import type { OfferStatus, OfferSummary } from "@/lib/offers/types";

const STATUS_COLOR: Record<OfferStatus, string> = {
  DRAFT: "default",
  RECEIVED: "blue",
  ACCEPTED: "green",
  REJECTED: "red",
  EXPIRED: "default",
};

interface OfferListPanelProps {
  projectId: string;
}

interface NewOfferForm {
  vendorId: string;
  label?: string;
}

export function OfferListPanel({ projectId }: OfferListPanelProps): React.ReactElement {
  const locale = useLocale();
  const router = useRouter();
  const queryClient = useQueryClient();
  const [modalOpen, setModalOpen] = useState(false);
  const [form] = Form.useForm<NewOfferForm>();

  const { data, isLoading } = useQuery({
    queryKey: ["offers", projectId],
    queryFn: () => offersApi.list(projectId),
  });

  const createMutation = useMutation({
    mutationFn: (values: NewOfferForm) =>
      offersApi.create({ projectId, vendorId: values.vendorId, label: values.label }),
    onSuccess: (offer) => {
      void queryClient.invalidateQueries({ queryKey: ["offers", projectId] });
      setModalOpen(false);
      form.resetFields();
      router.push(`/projects/${projectId}/offers/${offer.id}`);
    },
  });

  const columns: TableProps<OfferSummary>["columns"] = [
    {
      title: locale === "ro" ? "Etichetă" : "Label",
      dataIndex: "label",
      render: (label: string | null, record) => (
        <a onClick={() => router.push(`/projects/${projectId}/offers/${record.id}`)}>
          {label ?? "—"}
        </a>
      ),
    },
    {
      title: locale === "ro" ? "Primită" : "Received",
      dataIndex: "receivedAt",
      width: 130,
      render: (d: string | null) => (d ? new Date(d).toLocaleDateString() : "—"),
    },
    {
      title: locale === "ro" ? "Total fără TVA" : "Total excl VAT",
      dataIndex: "totalAmountExclVat",
      width: 150,
      align: "right",
      render: (v: number, r) => `${v.toFixed(2)} ${r.currencyCode}`,
    },
    {
      title: "Status",
      dataIndex: "status",
      width: 120,
      render: (s: OfferStatus) => <Tag color={STATUS_COLOR[s]}>{s}</Tag>,
    },
  ];

  return (
    <div>
      <Flex justify="flex-end" gap="small" className="mb-3">
        {(data?.length ?? 0) >= 2 && (
          <Button icon={<TableOutlined />} onClick={() => router.push(`/projects/${projectId}/compare`)}>
            {locale === "ro" ? "Compară ofertele" : "Compare offers"}
          </Button>
        )}
        <Button type="primary" icon={<PlusOutlined />} onClick={() => setModalOpen(true)}>
          {locale === "ro" ? "Ofertă nouă" : "New offer"}
        </Button>
      </Flex>

      {isLoading ? (
        <Flex justify="center" className="py-8">
          <Spin />
        </Flex>
      ) : data && data.length > 0 ? (
        <Table<OfferSummary> rowKey="id" dataSource={data} columns={columns} pagination={false} />
      ) : (
        <Empty
          description={
            locale === "ro"
              ? "Nicio ofertă. Adaugă oferte de la furnizori pentru a le compara."
              : "No offers yet. Add vendor offers to compare them."
          }
        />
      )}

      <Modal
        open={modalOpen}
        onCancel={() => setModalOpen(false)}
        onOk={() => form.submit()}
        okText={locale === "ro" ? "Creează" : "Create"}
        cancelText={locale === "ro" ? "Anulează" : "Cancel"}
        confirmLoading={createMutation.isPending}
        title={locale === "ro" ? "Ofertă nouă" : "New offer"}
        destroyOnHidden
      >
        <Form form={form} layout="vertical" onFinish={(v) => createMutation.mutate(v)} requiredMark={false}>
          <Form.Item
            name="vendorId"
            label={locale === "ro" ? "Furnizor" : "Vendor"}
            rules={[{ required: true }]}
          >
            <VendorPicker projectId={projectId} />
          </Form.Item>
          <Form.Item name="label" label={locale === "ro" ? "Etichetă" : "Label"}>
            <Input placeholder={locale === "ro" ? "ex. Ofertă electrician v1" : "e.g. Electrician bid v1"} />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
}
