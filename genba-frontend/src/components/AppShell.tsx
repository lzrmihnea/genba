"use client";

import { HomeOutlined, LogoutOutlined, ProjectOutlined, SettingOutlined, ShopOutlined, UserOutlined } from "@ant-design/icons";
import { Button, Dropdown, Flex, Layout, Menu, type MenuProps, Typography } from "antd";
import { useLocale, useTranslations } from "next-intl";
import Link from "next/link";
import { usePathname } from "next/navigation";
import { LanguageSwitcher } from "@/components/LanguageSwitcher";
import { useAuth } from "@/lib/auth/useAuth";

const { Header, Sider, Content } = Layout;
const { Text } = Typography;

export function AppShell({ children }: { children: React.ReactNode }): React.ReactElement {
  const t = useTranslations();
  const locale = useLocale();
  const pathname = usePathname();
  const { user, organizations, activeOrg, setActiveOrg, logout } = useAuth();

  const navItems: MenuProps["items"] = [
    {
      key: "/",
      icon: <HomeOutlined />,
      label: <Link href="/">{locale === "ro" ? "Acasă" : "Home"}</Link>,
    },
    {
      key: "/projects",
      icon: <ProjectOutlined />,
      label: <Link href="/projects">{locale === "ro" ? "Proiecte" : "Projects"}</Link>,
    },
    {
      key: "/settings/vendors",
      icon: <ShopOutlined />,
      label: <Link href="/settings/vendors">{locale === "ro" ? "Furnizori" : "Vendors"}</Link>,
    },
    {
      key: "/settings/organization",
      icon: <SettingOutlined />,
      label: (
        <Link href="/settings/organization">
          {locale === "ro" ? "Organizație" : "Organization"}
        </Link>
      ),
    },
  ];

  const selectedKey = navItems
    .map((i) => (i?.key as string | undefined) ?? "")
    .filter((k) => k && pathname.startsWith(k))
    .sort((a, b) => b.length - a.length)[0] ?? "/";

  const orgItems: MenuProps["items"] = organizations.map((org) => ({
    key: org.organizationId,
    label: `${org.organizationName} (${org.orgRole})`,
    onClick: () => setActiveOrg(org.organizationId),
  }));

  const signOutLabel = locale === "ro" ? "Deconectare" : "Sign out";
  const userItems: MenuProps["items"] = [
    {
      key: "logout",
      label: signOutLabel,
      icon: <LogoutOutlined />,
      onClick: () => {
        void logout();
      },
    },
  ];

  return (
    <Layout className="min-h-screen">
      <Header
        className="bg-white border-b border-neutral-200 px-6"
        style={{ background: "#fff" }}
      >
        <Flex justify="space-between" align="center" className="h-full">
          <Link href="/" className="!text-current">
            <Text strong className="text-lg">
              {t("app.title")}
            </Text>
          </Link>
          <Flex gap="small" align="center">
            {organizations.length > 0 && activeOrg && (
              <Dropdown
                menu={{ items: orgItems, selectedKeys: [activeOrg.organizationId] }}
                placement="bottomRight"
              >
                <Button>{activeOrg.organizationName}</Button>
              </Dropdown>
            )}
            <LanguageSwitcher />
            <Dropdown menu={{ items: userItems }} placement="bottomRight">
              <Button icon={<UserOutlined />}>
                {user?.displayName ?? user?.email}
              </Button>
            </Dropdown>
          </Flex>
        </Flex>
      </Header>
      <Layout>
        <Sider width={220} className="bg-white border-r border-neutral-200" style={{ background: "#fff" }}>
          <Menu mode="inline" selectedKeys={[selectedKey]} items={navItems} className="border-none" />
        </Sider>
        <Content className="p-6">{children}</Content>
      </Layout>
    </Layout>
  );
}
