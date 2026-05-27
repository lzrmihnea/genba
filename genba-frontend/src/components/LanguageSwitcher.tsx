"use client";

import { GlobalOutlined } from "@ant-design/icons";
import { Button, Dropdown, type MenuProps } from "antd";
import { useLocale, useTranslations } from "next-intl";

const SUPPORTED_LOCALES = ["en", "ro"] as const;
const COOKIE_NAME = "genba-locale";
const COOKIE_MAX_AGE = 60 * 60 * 24 * 365; // one year

function setLocaleCookie(locale: string): void {
  if (typeof document === "undefined") return;
  document.cookie = `${COOKIE_NAME}=${locale}; path=/; max-age=${COOKIE_MAX_AGE}; samesite=lax`;
}

export function LanguageSwitcher(): React.ReactElement {
  const locale = useLocale();
  const t = useTranslations("common.language");

  const items: MenuProps["items"] = SUPPORTED_LOCALES.map((code) => ({
    key: code,
    label: code === "en" ? t("english") : t("romanian"),
    onClick: () => {
      setLocaleCookie(code);
      // Cookie is read by the server on the next request — full reload picks
      // it up immediately and re-renders all server components.
      window.location.reload();
    },
  }));

  return (
    <Dropdown menu={{ items, selectedKeys: [locale] }} placement="bottomRight">
      <Button icon={<GlobalOutlined />} aria-label={t("switch")}>
        {locale.toUpperCase()}
      </Button>
    </Dropdown>
  );
}
