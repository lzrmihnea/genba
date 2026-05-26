"use client";

import { Card, Typography } from "antd";

const { Title, Paragraph, Text } = Typography;

export default function Home() {
  return (
    <main className="mx-auto max-w-3xl px-6 py-16">
      <Card>
        <Title level={2}>Genba</Title>
        <Paragraph>
          Construction-management oversight for homeowners — designed for the
          Romanian market first, architected to scale internationally.
        </Paragraph>
        <Paragraph type="secondary">
          <Text strong>Status:</Text> scaffold only. Auth, projects, and bid
          comparison land in subsequent OpenSpec changes.
        </Paragraph>
        <Paragraph type="secondary">
          Backend API: <Text code>http://localhost:8086/api</Text>
        </Paragraph>
        <Paragraph type="secondary">
          Swagger UI:{" "}
          <Text code>http://localhost:8086/swagger-ui/index.html</Text>
        </Paragraph>
      </Card>
    </main>
  );
}
