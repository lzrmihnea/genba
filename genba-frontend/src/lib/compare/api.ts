import { apiClient } from "@/lib/api/client";
import type { CompareResult, MatchSuggestion, Recommendation } from "@/lib/compare/types";

function offerIdParams(offerIds?: string[]): string {
  if (!offerIds || offerIds.length === 0) return "";
  return "?" + offerIds.map((id) => `offer_ids=${encodeURIComponent(id)}`).join("&");
}

export const compareApi = {
  async compare(projectId: string, offerIds?: string[]): Promise<CompareResult> {
    const response = await apiClient.get<CompareResult>(
      `/projects/${projectId}/compare${offerIdParams(offerIds)}`,
    );
    return response.data;
  },

  async suggestMatches(projectId: string, offerIds?: string[]): Promise<MatchSuggestion[]> {
    const response = await apiClient.post<MatchSuggestion[]>(
      `/projects/${projectId}/compare/suggest-matches${offerIdParams(offerIds)}`,
    );
    return response.data;
  },

  async createMatchGroup(projectId: string, lines: string[], label?: string): Promise<void> {
    await apiClient.post(`/projects/${projectId}/match-groups`, { lines, label });
  },

  async dissolveGroup(projectId: string, groupId: string): Promise<void> {
    await apiClient.delete(`/projects/${projectId}/match-groups/${groupId}`);
  },

  async removeLineFromGroup(projectId: string, groupId: string, lineId: string): Promise<void> {
    await apiClient.delete(`/projects/${projectId}/match-groups/${groupId}/lines/${lineId}`);
  },
};

export const recommendationsApi = {
  async missingFromOthers(projectId: string, offerId: string): Promise<Recommendation[]> {
    const response = await apiClient.get<Recommendation[]>(
      `/projects/${projectId}/offers/${offerId}/missing-from-others`,
    );
    return response.data;
  },

  async addFromRecommendation(offerId: string, sourceLineId: string): Promise<void> {
    await apiClient.post(`/offers/${offerId}/lines/from-recommendation`, { sourceLineId });
  },

  async dismiss(offerId: string, sourceLineId: string): Promise<void> {
    await apiClient.post(`/offers/${offerId}/recommendations/${sourceLineId}/dismiss`);
  },
};
