import { request } from "@/lib/api-client";

export type BodyRecord = {
  id: number;
  weightKg: number;
  bodyFatPercentage: number;
  skeletalMuscleKg: number;
  measuredAt: string;
  memo: string | null;
  createdAt: string;
  updatedAt: string;
};

export type BodyChange = {
  weightKg: number;
  bodyFatPercentage: number;
  skeletalMuscleKg: number;
};

export type BodyTrend = {
  latest: BodyRecord | null;
  changeFromPrevious: BodyChange | null;
  records: BodyRecord[];
};

export type BodyRecordInput = {
  weightKg: number;
  bodyFatPercentage: number;
  skeletalMuscleKg: number;
  measuredAt: string;
  memo: string | null;
};

export const bodyApi = {
  getTrend: (days = 90) =>
    request<BodyTrend>("/api/body-records/trend?days=" + days),

  createRecord: (input: BodyRecordInput) =>
    request<BodyRecord>("/api/body-records", {
      method: "POST",
      body: JSON.stringify(input),
    }),

  updateRecord: (bodyRecordId: number, input: BodyRecordInput) =>
    request<BodyRecord>("/api/body-records/" + bodyRecordId, {
      method: "PUT",
      body: JSON.stringify(input),
    }),

  deleteRecord: (bodyRecordId: number) =>
    request<void>("/api/body-records/" + bodyRecordId, {
      method: "DELETE",
    }),
};
