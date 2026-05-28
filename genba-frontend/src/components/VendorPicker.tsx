"use client";

import { PlusOutlined } from "@ant-design/icons";
import { Button, Divider, Flex, Input, Select, Space } from "antd";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useLocale } from "next-intl";
import { useState } from "react";
import { vendorsApi } from "@/lib/vendors/api";

interface VendorPickerProps {
  projectId?: string;
  value?: string;
  onChange?: (vendorId: string) => void;
  disabled?: boolean;
}

export function VendorPicker({ projectId, value, onChange, disabled }: VendorPickerProps): React.ReactElement {
  const locale = useLocale();
  const queryClient = useQueryClient();
  const [newName, setNewName] = useState("");

  const { data, isLoading } = useQuery({
    queryKey: ["vendors", projectId ?? "org"],
    queryFn: () => vendorsApi.list(projectId),
  });

  const createMutation = useMutation({
    mutationFn: (name: string) => vendorsApi.create({ name, projectId }),
    onSuccess: (vendor) => {
      void queryClient.invalidateQueries({ queryKey: ["vendors"] });
      setNewName("");
      onChange?.(vendor.id);
    },
  });

  return (
    <Select
      value={value}
      onChange={onChange}
      loading={isLoading}
      disabled={disabled}
      showSearch
      optionFilterProp="label"
      placeholder={locale === "ro" ? "Selectează furnizor" : "Select vendor"}
      style={{ minWidth: 260 }}
      options={(data ?? []).map((v) => ({ value: v.id, label: v.name }))}
      popupRender={(menu) => (
        <>
          {menu}
          <Divider style={{ margin: "8px 0" }} />
          <Flex gap="small" className="px-2 pb-1">
            <Input
              placeholder={locale === "ro" ? "Furnizor nou" : "New vendor name"}
              value={newName}
              onChange={(e) => setNewName(e.target.value)}
              onKeyDown={(e) => e.stopPropagation()}
            />
            <Button
              type="text"
              icon={<PlusOutlined />}
              disabled={newName.trim().length === 0}
              loading={createMutation.isPending}
              onClick={() => createMutation.mutate(newName.trim())}
            >
              {locale === "ro" ? "Adaugă" : "Add"}
            </Button>
          </Flex>
        </>
      )}
    />
  );
}
