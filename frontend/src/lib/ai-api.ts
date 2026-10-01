import { request } from "@/lib/api-client";

export type AiConversation = {
  id: number;
  title: string;
  createdAt: string;
  updatedAt: string;
};

export type FoodPhotoItem = { foodName: string; servingDescription: string; caloriesPerServing: number };
export type FoodPhotoResult = { status: "FOOD" | "NOT_FOOD" | "UNCERTAIN"; items: FoodPhotoItem[] };

export type AiMessage = {
  id: number;
  role: "USER" | "ASSISTANT";
  queryType:
    | "WORKOUT"
    | "NUTRITION"
    | "BODY"
    | "GENERAL_FITNESS"
    | "COMPOSITE"
    | "OUT_OF_SCOPE";
  content: string;
  foodPhotoResult: FoodPhotoResult | null;
  createdAt: string;
};

export type AiClientContext = {
  screen?: string;
  selectedDate?: string;
  resourceId?: number;
};

export type AiSendMessageResponse = {
  conversation: AiConversation;
  userMessage: AiMessage;
  assistantMessage: AiMessage;
  providerCalled: boolean;
  policyDecision: "ALLOW" | "BLOCK" | "SAFE_REDIRECT" | "CLARIFY";
};

export const aiApi = {
  sendFoodPhoto: (conversationId: number, image: Blob) => {
    const body = new FormData();
    body.append("image", image, "food.jpg");
    return request<Omit<AiSendMessageResponse, "providerCalled" | "policyDecision">>(
      "/api/ai/conversations/" + conversationId + "/food-photos", { method: "POST", body },
    );
  },
  listConversations: () =>
    request<AiConversation[]>("/api/ai/conversations"),

  createConversation: () =>
    request<AiConversation>("/api/ai/conversations", {
      method: "POST",
    }),

  renameConversation: (conversationId: number, title: string) =>
    request<AiConversation>("/api/ai/conversations/" + conversationId, {
      method: "PATCH",
      body: JSON.stringify({ title }),
    }),

  deleteConversation: (conversationId: number) =>
    request<void>("/api/ai/conversations/" + conversationId, {
      method: "DELETE",
    }),

  listMessages: (conversationId: number) =>
    request<AiMessage[]>(
      "/api/ai/conversations/" + conversationId + "/messages",
    ),

  sendMessage: (
    conversationId: number,
    message: string,
    clientContext?: AiClientContext,
  ) =>
    request<AiSendMessageResponse>(
      "/api/ai/conversations/" + conversationId + "/messages",
      {
        method: "POST",
        keepalive: true,
        body: JSON.stringify({
          message,
          clientContext: clientContext ?? null,
        }),
      },
    ),
};
