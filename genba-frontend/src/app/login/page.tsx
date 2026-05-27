"use client";

import { Alert, Button, Card, Flex, Form, Input, Typography } from "antd";
import { isAxiosError } from "axios";
import { useLocale, useTranslations } from "next-intl";
import { useRouter } from "next/navigation";
import { useEffect, useState } from "react";
import { LanguageSwitcher } from "@/components/LanguageSwitcher";
import { useAuth } from "@/lib/auth/useAuth";
import type { ApiError } from "@/lib/auth/types";

const { Title, Paragraph } = Typography;

interface LoginFormValues {
  email: string;
  password: string;
}

export default function LoginPage(): React.ReactElement {
  const t = useTranslations();
  const locale = useLocale();
  const router = useRouter();
  const { status, login } = useAuth();
  const [submitting, setSubmitting] = useState(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  useEffect(() => {
    if (status === "authenticated") {
      router.replace("/");
    }
  }, [status, router]);

  async function onFinish(values: LoginFormValues): Promise<void> {
    setSubmitting(true);
    setErrorMessage(null);
    try {
      await login(values.email, values.password, locale);
      router.replace("/");
    } catch (error) {
      if (isAxiosError<ApiError>(error) && error.response?.data?.message) {
        setErrorMessage(error.response.data.message);
      } else {
        setErrorMessage(t("common.error"));
      }
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <main className="flex min-h-screen items-center justify-center px-4 py-10">
      <Card className="w-full max-w-md">
        <Flex justify="space-between" align="center" className="mb-4">
          <Title level={3} className="!mb-0">
            {t("auth.login.title")}
          </Title>
          <LanguageSwitcher />
        </Flex>
        <Paragraph type="secondary">{t("app.tagline")}</Paragraph>
        {errorMessage && (
          <Alert
            type="error"
            message={errorMessage}
            className="mb-4"
            showIcon
            closable
            onClose={() => setErrorMessage(null)}
          />
        )}
        <Form<LoginFormValues>
          layout="vertical"
          onFinish={onFinish}
          autoComplete="on"
          disabled={submitting}
          requiredMark={false}
        >
          <Form.Item
            name="email"
            label={t("auth.login.email")}
            rules={[{ required: true, type: "email" }]}
          >
            <Input autoFocus autoComplete="email" placeholder="you@example.com" />
          </Form.Item>
          <Form.Item
            name="password"
            label={t("auth.login.password")}
            rules={[{ required: true }]}
          >
            <Input.Password autoComplete="current-password" />
          </Form.Item>
          <Button type="primary" htmlType="submit" block loading={submitting}>
            {t("auth.login.submit")}
          </Button>
        </Form>
      </Card>
    </main>
  );
}
