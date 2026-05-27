"use client";

import { Card, Typography } from "antd";
import { useTranslations } from "next-intl";
import { useAuth } from "@/lib/auth/useAuth";

const { Title, Paragraph, Text } = Typography;

export default function Home(): React.ReactElement {
  const t = useTranslations("home");
  const { user, activeOrg } = useAuth();

  return (
    <main className="mx-auto max-w-3xl">
      <Card>
        <Title level={2}>{t("title")}</Title>
        <Paragraph>{t("description")}</Paragraph>
        <Paragraph type="secondary">
          <Text strong>{t("status.label")}</Text> {t("status.value")}
        </Paragraph>
        <Paragraph type="secondary">
          Signed in as <Text code>{user?.email}</Text>
          {activeOrg && (
            <>
              {" "}— org <Text code>{activeOrg.organizationName}</Text> as{" "}
              <Text code>{activeOrg.orgRole}</Text>
            </>
          )}
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
