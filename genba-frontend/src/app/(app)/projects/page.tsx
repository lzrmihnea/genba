"use client";

import { PlusOutlined } from "@ant-design/icons";
import { Button, Card, Empty, Flex, Spin, Table, Tag, Typography } from "antd";
import type { TableProps } from "antd";
import { useQuery } from "@tanstack/react-query";
import { useLocale } from "next-intl";
import Link from "next/link";
import { useState } from "react";
import { ProjectCreateDrawer } from "@/components/ProjectCreateDrawer";
import { projectsApi } from "@/lib/projects/api";
import type { Project, ProjectStatus } from "@/lib/projects/types";

const { Title } = Typography;

const STATUS_COLOR: Record<ProjectStatus, string> = {
  PLANNING: "blue",
  IN_PROGRESS: "gold",
  ON_HOLD: "default",
  COMPLETED: "green",
  ARCHIVED: "default",
};

export default function ProjectsPage(): React.ReactElement {
  const locale = useLocale();
  const [drawerOpen, setDrawerOpen] = useState(false);

  const { data, isLoading } = useQuery({
    queryKey: ["projects"],
    queryFn: () => projectsApi.list(),
  });

  const columns: TableProps<Project>["columns"] = [
    {
      title: locale === "ro" ? "Nume" : "Name",
      dataIndex: "name",
      render: (name: string, record) => <Link href={`/projects/${record.id}`}>{name}</Link>,
    },
    {
      title: locale === "ro" ? "Adresă" : "Address",
      dataIndex: "address",
      render: (a: string | null) => a ?? "—",
    },
    {
      title: locale === "ro" ? "Status" : "Status",
      dataIndex: "status",
      render: (status: ProjectStatus) => <Tag color={STATUS_COLOR[status]}>{status}</Tag>,
    },
    {
      title: locale === "ro" ? "Monedă" : "Currency",
      dataIndex: "baseCurrency",
      width: 100,
    },
    {
      title: locale === "ro" ? "Creat" : "Created",
      dataIndex: "createdAt",
      render: (d: string) => new Date(d).toLocaleDateString(),
      width: 140,
    },
  ];

  return (
    <Card>
      <Flex justify="space-between" align="center" className="mb-4">
        <Title level={3} className="!mb-0">
          {locale === "ro" ? "Proiecte" : "Projects"}
        </Title>
        <Button type="primary" icon={<PlusOutlined />} onClick={() => setDrawerOpen(true)}>
          {locale === "ro" ? "Proiect nou" : "New project"}
        </Button>
      </Flex>

      {isLoading ? (
        <Flex justify="center" className="py-8">
          <Spin />
        </Flex>
      ) : data && data.length > 0 ? (
        <Table<Project>
          rowKey="id"
          dataSource={data}
          columns={columns}
          pagination={{ pageSize: 20, hideOnSinglePage: true }}
        />
      ) : (
        <Empty
          description={
            locale === "ro"
              ? "Niciun proiect încă. Apasă „Proiect nou” pentru a începe."
              : "No projects yet. Click \"New project\" to get started."
          }
        />
      )}

      <ProjectCreateDrawer open={drawerOpen} onClose={() => setDrawerOpen(false)} />
    </Card>
  );
}
