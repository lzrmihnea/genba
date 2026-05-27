"use client";

import { LogoutOutlined, UserOutlined } from "@ant-design/icons";
import { Button, Dropdown, Flex, Layout, type MenuProps, Typography } from "antd";
import { useTranslations } from "next-intl";
import { LanguageSwitcher } from "@/components/LanguageSwitcher";
import { useAuth } from "@/lib/auth/useAuth";

const { Header, Content } = Layout;
const { Text } = Typography;

export function AppShell({ children }: { children: React.ReactNode }): React.ReactElement {
  const t = useTranslations();
  const { user, organizations, activeOrg, setActiveOrg, logout } = useAuth();

  const orgItems: MenuProps["items"] = organizations.map((org) => ({
    key: org.organizationId,
    label: `${org.organizationName} (${org.orgRole})`,
    onClick: () => setActiveOrg(org.organizationId),
  }));

  const userItems: MenuProps["items"] = [
    {
      key: "logout",
      label: t("common.cancel") === "Anulează" ? "Deconectare" : "Sign out",
      icon: <LogoutOutlined />,
      onClick: () => {
        void logout();
      },
    },
  ];

  return (
    <Layout className="min-h-screen">
      <Header className="bg-white border-b border-neutral-200" style={{ background: "#fff" }}>
        <Flex justify="space-between" align="center" className="h-full">
          <Text strong className="text-lg">
            {t("app.title")}
          </Text>
          <Flex gap="small" align="center">
            {organizations.length > 0 && activeOrg && (
              <Dropdown menu={{ items: orgItems, selectedKeys: [activeOrg.organizationId] }} placement="bottomRight">
                <Button>{activeOrg.organizationName}</Button>
              </Dropdown>
            )}
            <LanguageSwitcher />
            <Dropdown menu={{ items: userItems }} placement="bottomRight">
              <Button icon={<UserOutlined />}>{user?.displayName ?? user?.email}</Button>
            </Dropdown>
          </Flex>
        </Flex>
      </Header>
      <Content className="p-6">{children}</Content>
    </Layout>
  );
}
