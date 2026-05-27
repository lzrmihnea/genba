"use client";

import { Card, Flex, Typography } from "antd";
import { useTranslations } from "next-intl";
import { LanguageSwitcher } from "@/components/LanguageSwitcher";

const { Title, Paragraph, Text } = Typography;

export default function Home(): React.ReactElement {
  const t = useTranslations("home");

  return (
    <main className="mx-auto max-w-3xl px-6 py-10">
      <Flex justify="flex-end" className="mb-4">
        <LanguageSwitcher />
      </Flex>
      <Card>
        <Title level={2}>{t("title")}</Title>
        <Paragraph>{t("description")}</Paragraph>
        <Paragraph type="secondary">
          <Text strong>{t("status.label")}</Text> {t("status.value")}
        </Paragraph>
        <Paragraph type="secondary">
          {t("links.backendApi")}: <Text code>http://localhost:8086/api</Text>
        </Paragraph>
        <Paragraph type="secondary">
          {t("links.swaggerUi")}:{" "}
          <Text code>http://localhost:8086/swagger-ui/index.html</Text>
        </Paragraph>
      </Card>
    </main>
  );
}
