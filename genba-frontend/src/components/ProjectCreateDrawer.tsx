"use client";

import { Alert, Button, Drawer, Form, Input, Space } from "antd";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { isAxiosError } from "axios";
import { useLocale, useTranslations } from "next-intl";
import { useState } from "react";
import { projectsApi } from "@/lib/projects/api";
import type { CreateProjectInput, Project } from "@/lib/projects/types";
import type { ApiError } from "@/lib/auth/types";
import { useAuth } from "@/lib/auth/useAuth";

const { TextArea } = Input;

interface ProjectCreateDrawerProps {
  open: boolean;
  onClose: () => void;
  onCreated?: (project: Project) => void;
}

export function ProjectCreateDrawer({ open, onClose, onCreated }: ProjectCreateDrawerProps): React.ReactElement {
  const t = useTranslations();
  const locale = useLocale();
  const queryClient = useQueryClient();
  const { activeOrg } = useAuth();
  const [form] = Form.useForm<CreateProjectInput>();
  const [error, setError] = useState<string | null>(null);

  const createMutation = useMutation({
    mutationFn: (input: CreateProjectInput) => projectsApi.create(input),
    onSuccess: (project) => {
      void queryClient.invalidateQueries({ queryKey: ["projects"] });
      form.resetFields();
      onCreated?.(project);
      onClose();
    },
    onError: (err) => {
      if (isAxiosError<ApiError>(err) && err.response?.data?.message) {
        setError(err.response.data.message);
      } else {
        setError(t("common.error"));
      }
    },
  });

  const onFinish = (values: CreateProjectInput): void => {
    setError(null);
    createMutation.mutate(values);
  };

  return (
    <Drawer
      open={open}
      onClose={onClose}
      title={locale === "ro" ? "Proiect nou" : "New project"}
      width={520}
      destroyOnHidden
      extra={
        <Space>
          <Button onClick={onClose}>{t("common.cancel")}</Button>
          <Button type="primary" onClick={() => form.submit()} loading={createMutation.isPending}>
            {locale === "ro" ? "Creează" : "Create"}
          </Button>
        </Space>
      }
    >
      {error && (
        <Alert
          type="error"
          message={error}
          className="mb-4"
          showIcon
          closable
          onClose={() => setError(null)}
        />
      )}
      <Form
        form={form}
        layout="vertical"
        onFinish={onFinish}
        initialValues={{ baseCurrency: activeOrg?.currencyCode ?? "RON" }}
        requiredMark={false}
      >
        <Form.Item
          name="name"
          label={locale === "ro" ? "Nume proiect" : "Project name"}
          rules={[{ required: true }]}
        >
          <Input autoFocus placeholder={locale === "ro" ? "Casa Mihnea" : "My new house"} />
        </Form.Item>
        <Form.Item name="address" label={locale === "ro" ? "Adresă" : "Address"}>
          <Input placeholder={locale === "ro" ? "Strada, oraș, județ" : "Street, city"} />
        </Form.Item>
        <Form.Item name="description" label={locale === "ro" ? "Descriere" : "Description"}>
          <TextArea rows={3} />
        </Form.Item>
        <Form.Item
          name="baseCurrency"
          label={locale === "ro" ? "Monedă" : "Currency"}
          rules={[{ pattern: /^[A-Z]{3}$/, message: "ISO 4217 3-letter code" }]}
          extra={
            locale === "ro"
              ? `Implicit din organizație: ${activeOrg?.currencyCode ?? "RON"}`
              : `Default from organization: ${activeOrg?.currencyCode ?? "RON"}`
          }
        >
          <Input maxLength={3} style={{ width: 100 }} />
        </Form.Item>
      </Form>
    </Drawer>
  );
}
