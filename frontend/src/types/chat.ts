export type ChatRoomResponse = {
  id: number;
  buyerId: number;
  opponentName: string;
  productName: string;
  lastMessage?: string | null;
};

export type ChatMessageRequest = {
  roomId: number;
  senderId: number;
  message: string;
};

export type ChatMessageResponse = {
  id?: number;
  roomId?: number;
  senderId: number;
  message: string;
  createdAt?: string;
};
