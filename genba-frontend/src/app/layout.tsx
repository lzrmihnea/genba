import type { Metadata } from "next";
import { AntdRegistry } from "@ant-design/nextjs-registry";
import { NextIntlClientProvider } from "next-intl";
import { getLocale, getMessages } from "next-intl/server";
import { QueryProvider } from "@/lib/api/QueryProvider";
import { AuthProvider } from "@/lib/auth/AuthProvider";
import "./globals.css";

export const metadata: Metadata = {
  title: process.env.NEXT_PUBLIC_APP_TITLE ?? "Genba",
  description: "Construction-management oversight for homeowners",
};

export default async function RootLayout({
  children,
}: Readonly<{ children: React.ReactNode }>) {
  const locale = await getLocale();
  const messages = await getMessages();

  return (
    <html lang={locale}>
      <body className="min-h-screen bg-neutral-50 text-neutral-900 antialiased">
        <NextIntlClientProvider locale={locale} messages={messages}>
          <AntdRegistry>
            <QueryProvider>
              <AuthProvider>{children}</AuthProvider>
            </QueryProvider>
          </AntdRegistry>
        </NextIntlClientProvider>
      </body>
    </html>
  );
}
