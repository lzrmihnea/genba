export type ProjectStatus =
  | "PLANNING"
  | "IN_PROGRESS"
  | "ON_HOLD"
  | "COMPLETED"
  | "ARCHIVED";

export interface Project {
  id: string;
  orgId: string;
  name: string;
  description: string | null;
  address: string | null;
  baseCurrency: string;
  status: ProjectStatus;
  createdAt: string;
  updatedAt: string;
}

export interface CreateProjectInput {
  name: string;
  description?: string;
  address?: string;
  baseCurrency?: string;
}

export interface UpdateProjectInput {
  name?: string;
  description?: string;
  address?: string;
  baseCurrency?: string;
  status?: ProjectStatus;
}
