import api from "@/src/lib/api";
import type { ChatMessageResponse, ChatRoomResponse } from "@/src/types/chat";

const CHAT_BASE_PATH = "/api/chat";

function isObject(value: unknown): value is Record<string, unknown> {
  return typeof value === "object" && value !== null;
}

function normalizeChatRoomResponse(value: unknown): ChatRoomResponse | null {
  if (!isObject(value)) {
    return null;
  }

  const idCandidate = value.id ?? value.roomId;
  const opponentCandidate = value.opponentName ?? value.opponentNickname;
  const productName = value.productName;

  if (
    typeof idCandidate !== "number" ||
    typeof opponentCandidate !== "string" ||
    typeof productName !== "string"
  ) {
    return null;
  }

  const lastMessage =
    typeof value.lastMessage === "string" || value.lastMessage === null
      ? value.lastMessage
      : null;

  return {
    id: idCandidate,
    opponentName: opponentCandidate,
    productName,
    lastMessage,
  };
}

export async function createOrGetChatRoom(productId: number): Promise<ChatRoomResponse> {
  const response = await api.post<unknown>(`${CHAT_BASE_PATH}/room/${productId}`);
  const normalized = normalizeChatRoomResponse(response.data);
  if (!normalized) {
    throw new Error("Invalid chat room response");
  }
  return normalized;
}

export async function getMyChatRooms(): Promise<ChatRoomResponse[]> {
  const response = await api.get<unknown>(`${CHAT_BASE_PATH}/rooms`);

  if (!Array.isArray(response.data)) {
    throw new Error("Invalid chat rooms response");
  }

  return response.data
    .map((item) => normalizeChatRoomResponse(item))
    .filter((item): item is ChatRoomResponse => item !== null);
}

export async function getChatRoomMessages(roomId: number): Promise<ChatMessageResponse[]> {
  const response = await api.get<unknown>(`${CHAT_BASE_PATH}/room/${roomId}/messages`);

  if (!Array.isArray(response.data)) {
    throw new Error("Invalid chat messages response");
  }

  return response.data as ChatMessageResponse[];
}
