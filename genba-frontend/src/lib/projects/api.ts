import { apiClient } from "@/lib/api/client";
import type {
  CreateProjectInput,
  Project,
  UpdateProjectInput,
} from "@/lib/projects/types";

export const projectsApi = {
  async list(): Promise<Project[]> {
    const response = await apiClient.get<Project[]>("/projects");
    return response.data;
  },

  async get(id: string): Promise<Project> {
    const response = await apiClient.get<Project>(`/projects/${id}`);
    return response.data;
  },

  async create(input: CreateProjectInput): Promise<Project> {
    const response = await apiClient.post<Project>("/projects", input);
    return response.data;
  },

  async update(id: string, input: UpdateProjectInput): Promise<Project> {
    const response = await apiClient.put<Project>(`/projects/${id}`, input);
    return response.data;
  },

  async delete(id: string): Promise<void> {
    await apiClient.delete(`/projects/${id}`);
  },
};
