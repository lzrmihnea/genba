"use client";

import { Alert, List, Modal, Tag, Typography } from "antd";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useLocale } from "next-intl";
import { useState } from "react";
import { compareApi } from "@/lib/compare/api";
import type { MatchSuggestion } from "@/lib/compare/types";

const { Text } = Typography;

interface SuggestMatchesModalProps {
  open: boolean;
  onClose: () => void;
  projectId: string;
  offerIds: string[];
}

export function SuggestMatchesModal({ open, onClose, projectId, offerIds }: SuggestMatchesModalProps): React.ReactElement {
  const locale = useLocale();
  const queryClient = useQueryClient();
  const [accepted, setAccepted] = useState<Set<string>>(new Set());

  const { data, isLoading } = useQuery({
    queryKey: ["suggest-matches", projectId, offerIds],
    queryFn: () => compareApi.suggestMatches(projectId, offerIds),
    enabled: open,
  });

  const acceptMutation = useMutation({
    mutationFn: (s: MatchSuggestion) => compareApi.createMatchGroup(projectId, s.candidateLineIds, s.label),
    onSuccess: (_void, s) => {
      setAccepted((prev) => new Set(prev).add(s.label));
      void queryClient.invalidateQueries({ queryKey: ["compare", projectId] });
    },
  });

  return (
    <Modal
      open={open}
      onCancel={onClose}
      onOk={onClose}
      okText={locale === "ro" ? "Gata" : "Done"}
      cancelButtonProps={{ style: { display: "none" } }}
      title={locale === "ro" ? "Sugestii de potrivire" : "Match suggestions"}
      width={680}
    >
      <Alert
        type="info"
        showIcon
        className="mb-3"
        message={
          locale === "ro"
            ? "Linii cu aceeași etichetă în mai multe oferte. Acceptă pentru a le îmbina pe un singur rând."
            : "Lines with identical labels across offers. Accept to merge them onto one comparison row."
        }
      />
      <List
        loading={isLoading}
        dataSource={data ?? []}
        locale={{ emptyText: locale === "ro" ? "Nicio sugestie" : "No suggestions" }}
        renderItem={(s) => {
          const done = accepted.has(s.label);
          return (
            <List.Item
              actions={[
                done ? (
                  <Tag color="green" key="done">
                    {locale === "ro" ? "Îmbinat" : "Merged"}
                  </Tag>
                ) : (
                  <a key="accept" onClick={() => acceptMutation.mutate(s)}>
                    {locale === "ro" ? "Acceptă" : "Accept"}
                  </a>
                ),
              ]}
            >
              <Text>{s.label}</Text>
              <Text type="secondary" className="ml-2">
                ({s.candidateLineIds.length} {locale === "ro" ? "linii" : "lines"})
              </Text>
            </List.Item>
          );
        }}
      />
    </Modal>
  );
}
