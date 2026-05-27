"use client";

import { Card, Descriptions, Empty, Typography } from "antd";
import { useLocale } from "next-intl";
import { useAuth } from "@/lib/auth/useAuth";

const { Title, Paragraph } = Typography;

export default function OrganizationSettingsPage(): React.ReactElement {
  const locale = useLocale();
  const { activeOrg } = useAuth();

  if (!activeOrg) {
    return (
      <Card>
        <Empty
          description={
            locale === "ro" ? "Nicio organizație activă" : "No active organization"
          }
        />
      </Card>
    );
  }

  return (
    <Card>
      <Title level={3}>{locale === "ro" ? "Organizație" : "Organization"}</Title>
      <Paragraph type="secondary">
        {locale === "ro"
          ? "Valorile sunt implicite pentru proiectele noi. Vor fi editabile odată cu add-permit-workflow (Phase 3)."
          : "These values seed defaults for new projects. They become editable when add-permit-workflow (Phase 3) ships."}
      </Paragraph>
      <Descriptions column={1} bordered size="small">
        <Descriptions.Item label={locale === "ro" ? "Nume" : "Name"}>
          {activeOrg.organizationName}
        </Descriptions.Item>
        <Descriptions.Item label={locale === "ro" ? "Rol-ul tău" : "Your role"}>
          {activeOrg.orgRole}
        </Descriptions.Item>
        <Descriptions.Item label={locale === "ro" ? "Țară" : "Country"}>
          {activeOrg.countryCode}
        </Descriptions.Item>
        <Descriptions.Item label={locale === "ro" ? "Monedă implicită" : "Default currency"}>
          {activeOrg.currencyCode}
        </Descriptions.Item>
        <Descriptions.Item label={locale === "ro" ? "Regim TVA" : "VAT regime"}>
          {activeOrg.vatRegime}
        </Descriptions.Item>
      </Descriptions>
    </Card>
  );
}
