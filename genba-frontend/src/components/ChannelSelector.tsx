"use client";

import { Select } from "antd";
import { useQuery } from "@tanstack/react-query";
import { useLocale } from "next-intl";
import { attachmentChannelsApi } from "@/lib/attachments/api";
import type { AttachmentSourceChannel } from "@/lib/attachments/types";

interface ChannelSelectorProps {
  value?: string;
  onChange?: (channelId: string, channel: AttachmentSourceChannel) => void;
  filter?: (channel: AttachmentSourceChannel) => boolean;
  placeholder?: string;
  disabled?: boolean;
}

export function ChannelSelector({
  value,
  onChange,
  filter,
  placeholder,
  disabled,
}: ChannelSelectorProps): React.ReactElement {
  const locale = useLocale();
  const { data, isLoading } = useQuery({
    queryKey: ["attachment-channels"],
    queryFn: () => attachmentChannelsApi.list(),
  });

  const channels = (data ?? []).filter((c) => (filter ? filter(c) : true));

  const options = channels.map((c) => ({
    value: c.id,
    label: (
      <span>
        {locale === "ro" ? c.labelRo : c.labelEn}
        {!c.systemManaged && (
          <span className="ml-1 text-xs text-neutral-400">(custom)</span>
        )}
      </span>
    ),
  }));

  return (
    <Select
      value={value}
      onChange={(id) => {
        const channel = channels.find((c) => c.id === id);
        if (channel) onChange?.(id, channel);
      }}
      options={options}
      placeholder={placeholder}
      loading={isLoading}
      disabled={disabled || isLoading}
      style={{ minWidth: 180 }}
    />
  );
}
