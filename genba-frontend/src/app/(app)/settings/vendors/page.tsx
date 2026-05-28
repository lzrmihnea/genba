"use client";

import { DeleteOutlined, PlusOutlined } from "@ant-design/icons";
import { Button, Card, Empty, Flex, Form, Input, Modal, Popconfirm, Spin, Table, Typography } from "antd";
import type { TableProps } from "antd";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useLocale } from "next-intl";
import { useState } from "react";
import { vendorsApi } from "@/lib/vendors/api";
import type { CreateVendorInput, Vendor } from "@/lib/vendors/types";

const { Title } = Typography;

export default function VendorsPage(): React.ReactElement {
  const locale = useLocale();
  const queryClient = useQueryClient();
  const [open, setOpen] = useState(false);
  const [form] = Form.useForm<CreateVendorInput>();

  const { data, isLoading } = useQuery({
    queryKey: ["vendors", "org"],
    queryFn: () => vendorsApi.list(),
  });

  const createMutation = useMutation({
    mutationFn: (input: CreateVendorInput) => vendorsApi.create(input),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ["vendors"] });
      form.resetFields();
      setOpen(false);
    },
  });

  const deleteMutation = useMutation({
    mutationFn: (id: string) => vendorsApi.delete(id),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ["vendors"] }),
  });

  const columns: TableProps<Vendor>["columns"] = [
    { title: locale === "ro" ? "Nume" : "Name", dataIndex: "name" },
    { title: locale === "ro" ? "Contact" : "Contact", dataIndex: "contactName", render: (v) => v ?? "—" },
    { title: locale === "ro" ? "Telefon" : "Phone", dataIndex: "phone", render: (v) => v ?? "—" },
    { title: "CUI/VAT", dataIndex: "vatId", render: (v) => v ?? "—" },
    {
      title: "",
      width: 50,
      render: (_, record) => (
        <Popconfirm
          title={locale === "ro" ? "Ștergi furnizorul?" : "Delete vendor?"}
          onConfirm={() => deleteMutation.mutate(record.id)}
          okType="danger"
        >
          <Button type="text" danger size="small" icon={<DeleteOutlined />} />
        </Popconfirm>
      ),
    },
  ];

  return (
    <Card>
      <Flex justify="space-between" align="center" className="mb-4">
        <Title level={3} className="!mb-0">
          {locale === "ro" ? "Furnizori" : "Vendors"}
        </Title>
        <Button type="primary" icon={<PlusOutlined />} onClick={() => setOpen(true)}>
          {locale === "ro" ? "Furnizor nou" : "New vendor"}
        </Button>
      </Flex>

      {isLoading ? (
        <Flex justify="center" className="py-8">
          <Spin />
        </Flex>
      ) : data && data.length > 0 ? (
        <Table<Vendor> rowKey="id" dataSource={data} columns={columns} pagination={{ pageSize: 20, hideOnSinglePage: true }} />
      ) : (
        <Empty description={locale === "ro" ? "Niciun furnizor" : "No vendors yet"} />
      )}

      <Modal
        open={open}
        onCancel={() => setOpen(false)}
        onOk={() => form.submit()}
        okText={locale === "ro" ? "Creează" : "Create"}
        cancelText={locale === "ro" ? "Anulează" : "Cancel"}
        confirmLoading={createMutation.isPending}
        title={locale === "ro" ? "Furnizor nou" : "New vendor"}
        destroyOnHidden
      >
        <Form form={form} layout="vertical" onFinish={(v) => createMutation.mutate(v)} requiredMark={false}>
          <Form.Item name="name" label={locale === "ro" ? "Nume" : "Name"} rules={[{ required: true }]}>
            <Input autoFocus />
          </Form.Item>
          <Form.Item name="contactName" label={locale === "ro" ? "Persoană de contact" : "Contact name"}>
            <Input />
          </Form.Item>
          <Form.Item name="phone" label={locale === "ro" ? "Telefon" : "Phone"}>
            <Input />
          </Form.Item>
          <Form.Item name="email" label="Email" rules={[{ type: "email" }]}>
            <Input />
          </Form.Item>
          <Form.Item name="vatId" label="CUI / VAT ID">
            <Input />
          </Form.Item>
        </Form>
      </Modal>
    </Card>
  );
}
