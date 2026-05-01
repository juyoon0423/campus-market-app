"use client";

import { Client, type IMessage } from "@stomp/stompjs";
import Link from "next/link";
import { useRouter, useSearchParams } from "next/navigation";
import SockJS from "sockjs-client";
import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import { useAuth } from "@/src/context/AuthContext";
import api from "@/src/lib/api";
import {
  createOrGetChatRoom,
  getChatRoomMessages,
  getMyChatRooms,
} from "@/src/lib/apis/chatApi";
import type {
  ChatMessageRequest,
  ChatMessageResponse,
  ChatRoomResponse,
} from "@/src/types/chat";

function findNumericUserId(value: unknown): number | null {
  if (typeof value === "number" && Number.isFinite(value)) {
    return value;
  }

  if (typeof value === "string") {
    const direct = Number(value);
    if (!Number.isNaN(direct)) {
      return direct;
    }
  }

  if (Array.isArray(value)) {
    for (const item of value) {
      const found = findNumericUserId(item);
      if (found !== null) {
        return found;
      }
    }
    return null;
  }

  if (typeof value === "object" && value !== null) {
    const record = value as Record<string, unknown>;
    const priorityKeys = ["userId", "id", "user_id", "memberId", "sub"];

    for (const key of priorityKeys) {
      if (key in record) {
        const found = findNumericUserId(record[key]);
        if (found !== null) {
          return found;
        }
      }
    }

    for (const nested of Object.values(record)) {
      const found = findNumericUserId(nested);
      if (found !== null) {
        return found;
      }
    }
  }

  return null;
}

function extractUserIdFromToken(token: string | null): number | null {
  if (!token) {
    return null;
  }

  try {
    const payloadBase64 = token.split(".")[1];
    if (!payloadBase64) {
      return null;
    }

    const base64 = payloadBase64.replace(/-/g, "+").replace(/_/g, "/");
    const paddedBase64 = base64 + "=".repeat((4 - (base64.length % 4)) % 4);
    const payloadJson = JSON.parse(atob(paddedBase64));
    return findNumericUserId(payloadJson);
  } catch {
    return null;
  }
}

function getMessageRoomId(message: ChatMessageResponse): number | null {
  if (message.roomId) {
    return message.roomId;
  }

  const roomIdInObject = (message as unknown as { chatRoom?: { id?: number } }).chatRoom?.id;
  return typeof roomIdInObject === "number" ? roomIdInObject : null;
}

export default function ChatPage() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const { isLoggedIn, isHydrated, token } = useAuth();

  const [rooms, setRooms] = useState<ChatRoomResponse[]>([]);
  const [selectedRoomId, setSelectedRoomId] = useState<number | null>(null);
  const [messages, setMessages] = useState<ChatMessageResponse[]>([]);
  const [inputMessage, setInputMessage] = useState("");
  const [isLoadingRooms, setIsLoadingRooms] = useState(true);
  const [isLoadingMessages, setIsLoadingMessages] = useState(false);
  const [fallbackSenderId, setFallbackSenderId] = useState<number | null>(null);
  const [errorMessage, setErrorMessage] = useState("");
  const [isCreatingChatRoom, setIsCreatingChatRoom] = useState(false);
  const stompClientRef = useRef<Client | null>(null);
  const roomSubscriptionRef = useRef<{ unsubscribe: () => void } | null>(null);
  const pendingSubscribeRoomIdRef = useRef<number | null>(null);
  const connectTimeoutRef = useRef<number | null>(null);

  const senderIdFromToken = useMemo(() => extractUserIdFromToken(token), [token]);
  const senderId = senderIdFromToken ?? fallbackSenderId;

  const fetchSenderIdFallback = useCallback(async (): Promise<number | null> => {
    try {
      const response = await api.get<unknown>("/api/users/me");
      const parsed = findNumericUserId(response.data);
      if (parsed !== null) {
        setFallbackSenderId(parsed);
        return parsed;
      }
      return null;
    } catch {
      return null;
    }
  }, []);

  const roomIdParam = searchParams.get("roomId");
  const productIdParam = searchParams.get("productId");
  const roomIdFromQuery = roomIdParam ? Number(roomIdParam) : null;
  const productIdFromQuery = productIdParam ? Number(productIdParam) : null;
  const hasRoomIdQuery = roomIdFromQuery !== null && !Number.isNaN(roomIdFromQuery);
  const hasProductIdQuery =
    productIdFromQuery !== null && !Number.isNaN(productIdFromQuery);

  const connectStomp = useCallback(() => {
    if (stompClientRef.current?.active) {
      return;
    }

    const wsEndpoint = process.env.NEXT_PUBLIC_WS_ENDPOINT || "http://localhost:8080/ws-stomp";
    const client = new Client({
      reconnectDelay: 5000,
      webSocketFactory: () => new SockJS(wsEndpoint),
    });

    if (connectTimeoutRef.current !== null) {
      window.clearTimeout(connectTimeoutRef.current);
    }
    connectTimeoutRef.current = window.setTimeout(() => {
      if (!client.connected) {
        setErrorMessage("웹소켓 연결에 실패했습니다. 서버 endpoint 설정을 확인해 주세요.");
      }
    }, 5000);

    client.onConnect = () => {
      if (stompClientRef.current !== client || !client.connected) {
        return;
      }
      setErrorMessage("");
      const pendingRoomId = pendingSubscribeRoomIdRef.current;
      if (pendingRoomId !== null) {
        pendingSubscribeRoomIdRef.current = null;
        try {
          roomSubscriptionRef.current?.unsubscribe();
          roomSubscriptionRef.current = client.subscribe(
            `/sub/chat/room/${pendingRoomId}`,
            (frame: IMessage) => {
              try {
                const incoming = JSON.parse(frame.body) as ChatMessageResponse;
                const incomingRoomId = getMessageRoomId(incoming);
                if (incomingRoomId !== pendingRoomId) {
                  return;
                }
                setMessages((prev) => [...prev, incoming]);
              } catch {
                // Ignore invalid WS payloads
              }
            },
          );
        } catch {
          setErrorMessage("채팅방 구독에 실패했습니다.");
        }
      }
    };

    client.onWebSocketClose = () => {
    };

    client.onStompError = () => {
      setErrorMessage("웹소켓 연결 중 오류가 발생했습니다.");
    };

    client.onWebSocketError = () => {
      setErrorMessage("웹소켓 핸드셰이크에 실패했습니다.");
    };

    client.activate();
    stompClientRef.current = client;
  }, []);

  const subscribeRoom = useCallback((roomId: number) => {
    const client = stompClientRef.current;
    if (!client || !client.connected) {
      pendingSubscribeRoomIdRef.current = roomId;
      return;
    }

    try {
      roomSubscriptionRef.current?.unsubscribe();
      roomSubscriptionRef.current = client.subscribe(
        `/sub/chat/room/${roomId}`,
        (frame: IMessage) => {
          try {
            const incoming = JSON.parse(frame.body) as ChatMessageResponse;
            const incomingRoomId = getMessageRoomId(incoming);

            if (incomingRoomId !== roomId) {
              return;
            }

            setMessages((prev) => [...prev, incoming]);
          } catch {
            // Ignore invalid WS payloads
          }
        },
      );
    } catch {
      pendingSubscribeRoomIdRef.current = roomId;
    }
  }, []);

  const fetchRooms = useCallback(async () => {
    setIsLoadingRooms(true);
    setErrorMessage("");
    try {
      const roomList = await getMyChatRooms();
      setRooms(roomList);
      if (roomList.length > 0) {
        setSelectedRoomId((prev) => prev ?? roomList[0].id);
      }
    } catch {
      setErrorMessage("채팅방 목록을 불러오지 못했습니다.");
    } finally {
      setIsLoadingRooms(false);
    }
  }, []);

  const fetchMessages = useCallback(async (roomId: number) => {
    setIsLoadingMessages(true);
    try {
      const roomMessages = await getChatRoomMessages(roomId);
      setMessages(roomMessages);
    } catch {
      setErrorMessage("메시지를 불러오지 못했습니다.");
      setMessages([]);
    } finally {
      setIsLoadingMessages(false);
    }
  }, []);

  useEffect(() => {
    if (!isHydrated) {
      return;
    }

    if (!isLoggedIn) {
      router.replace("/login");
      return;
    }

    connectStomp();
    const timer = window.setTimeout(() => {
      fetchRooms();
    }, 0);

    return () => {
      window.clearTimeout(timer);
    };
  }, [isHydrated, isLoggedIn, router, connectStomp, fetchRooms]);

  useEffect(() => {
    if (!isLoggedIn) {
      return;
    }

    const openRequestedRoom = async () => {
      if (hasRoomIdQuery) {
        setSelectedRoomId(roomIdFromQuery);
        return;
      }

      if (hasProductIdQuery) {
        // Prevent duplicate room creation
        if (isCreatingChatRoom) {
          return;
        }

        setIsCreatingChatRoom(true);
        try {
          const room = await createOrGetChatRoom(productIdFromQuery);
          setSelectedRoomId(room.id);
          setRooms((prev) => {
            if (!Array.isArray(prev)) {
              return [room];
            }
            if (prev.some((item) => item.id === room.id)) {
              return prev;
            }
            return [room, ...prev];
          });
        } catch {
          try {
            // If room creation fails (e.g., room already exists with different response), fallback to list.
            const roomList = await getMyChatRooms();
            setRooms(roomList);
            if (roomList.length > 0) {
              setSelectedRoomId(roomList[0].id);
            } else {
              setErrorMessage("채팅방 생성에 실패했습니다.");
            }
          } catch {
            setErrorMessage("채팅방 생성에 실패했습니다.");
          }
        } finally {
          setIsCreatingChatRoom(false);
        }
      }
    };

    openRequestedRoom();
  }, [hasProductIdQuery, hasRoomIdQuery, isLoggedIn, productIdFromQuery, roomIdFromQuery, isCreatingChatRoom]);

  useEffect(() => {
    if (selectedRoomId === null) {
      return;
    }

    const fetchTimer = window.setTimeout(() => {
      fetchMessages(selectedRoomId);
    }, 0);

    const subscribe = () => subscribeRoom(selectedRoomId);
    const subscribeTimer = window.setTimeout(subscribe, 100);

    return () => {
      window.clearTimeout(fetchTimer);
      window.clearTimeout(subscribeTimer);
      roomSubscriptionRef.current?.unsubscribe();
      roomSubscriptionRef.current = null;
    };
  }, [selectedRoomId, fetchMessages, subscribeRoom]);

  useEffect(() => {
    if (!isLoggedIn || senderIdFromToken !== null) {
      return;
    }

    const timer = window.setTimeout(() => {
      fetchSenderIdFallback();
    }, 0);

    return () => {
      window.clearTimeout(timer);
    };
  }, [isLoggedIn, senderIdFromToken, fetchSenderIdFallback]);

  useEffect(() => {
    return () => {
      roomSubscriptionRef.current?.unsubscribe();
      pendingSubscribeRoomIdRef.current = null;
      if (connectTimeoutRef.current !== null) {
        window.clearTimeout(connectTimeoutRef.current);
      }
      stompClientRef.current?.deactivate();
    };
  }, []);

  const handleSendMessage = async () => {
    const roomId = selectedRoomId;
    const client = stompClientRef.current;
    const text = inputMessage.trim();

    if (!roomId) {
      setErrorMessage("채팅방을 먼저 선택해 주세요.");
      return;
    }

    if (!text) {
      setErrorMessage("메시지를 입력해 주세요.");
      return;
    }

    if (!client?.connected) {
      setErrorMessage("웹소켓이 아직 연결되지 않았습니다. 잠시 후 다시 시도해주세요.");
      return;
    }

    let resolvedSenderId = senderId;
    if (resolvedSenderId === null) {
      resolvedSenderId = await fetchSenderIdFallback();
    }

    if (resolvedSenderId === null) {
      setErrorMessage("사용자 ID를 확인할 수 없어 메시지를 전송할 수 없습니다.");
      return;
    }

    const payload: ChatMessageRequest = {
      roomId,
      senderId: resolvedSenderId,
      message: text,
    };

    client.publish({
      destination: "/pub/chat/message",
      body: JSON.stringify(payload),
    });
    setInputMessage("");
  };

  if (!isHydrated) {
    return null;
  }

  if (!isLoggedIn) {
    return null;
  }

  return (
    <div className="min-h-screen bg-slate-100 px-4 py-8">
      <main className="mx-auto w-full max-w-6xl rounded-2xl bg-white p-4 shadow-sm md:p-6">
        <header className="mb-4 flex items-center justify-between">
          <h1 className="text-2xl font-bold text-slate-900">채팅</h1>
          <Link
            href="/"
            className="rounded-lg border border-slate-300 px-4 py-2 text-sm text-slate-700 hover:bg-slate-50"
          >
            메인으로
          </Link>
        </header>

        {errorMessage ? (
          <p className="mb-4 rounded-lg bg-red-50 px-4 py-3 text-sm text-red-600">
            {errorMessage}
          </p>
        ) : null}
        {senderId === null ? (
          <p className="mb-4 rounded-lg bg-amber-50 px-4 py-3 text-sm text-amber-700">
            로그인 토큰에서 사용자 ID를 읽지 못해 메시지 전송이 제한됩니다.
          </p>
        ) : null}

        <section className="grid min-h-[560px] gap-4 md:grid-cols-[280px_1fr]">
          <aside className="rounded-xl border border-slate-200 p-3">
            <h2 className="mb-3 text-sm font-semibold text-slate-700">내 채팅방</h2>
            {isLoadingRooms ? (
              <p className="text-sm text-slate-500">채팅방을 불러오는 중...</p>
            ) : rooms.length === 0 ? (
              <p className="text-sm text-slate-500">참여 중인 채팅방이 없습니다.</p>
            ) : (
              <ul className="space-y-2">
                {rooms.map((room) => (
                  <li key={room.id}>
                    <button
                      type="button"
                      onClick={() => setSelectedRoomId(room.id)}
                      className={`w-full rounded-lg border px-3 py-2 text-left ${
                        room.id === selectedRoomId
                          ? "border-slate-900 bg-slate-900 text-white"
                          : "border-slate-200 bg-white text-slate-700 hover:bg-slate-50"
                      }`}
                    >
                      <p className="truncate text-sm font-semibold">{room.productName}</p>
                      <p className="truncate text-xs opacity-80">
                        {room.opponentName} · {room.lastMessage || "메시지 없음"}
                      </p>
                    </button>
                  </li>
                ))}
              </ul>
            )}
          </aside>

          <section className="flex flex-col rounded-xl border border-slate-200">
            <div className="border-b border-slate-200 px-4 py-3">
              <p className="text-sm font-semibold text-slate-700">
                {selectedRoomId ? `채팅방 #${selectedRoomId}` : "채팅방을 선택하세요"}
              </p>
            </div>

            <div className="flex-1 space-y-3 overflow-y-auto px-4 py-3">
              {selectedRoomId === null ? (
                <p className="text-sm text-slate-500">왼쪽에서 채팅방을 선택해 주세요.</p>
              ) : isLoadingMessages ? (
                <p className="text-sm text-slate-500">메시지를 불러오는 중...</p>
              ) : messages.length === 0 ? (
                <p className="text-sm text-slate-500">아직 메시지가 없습니다.</p>
              ) : (
                messages.map((message, index) => {
                  const isMine = senderId !== null && message.senderId === senderId;
                  return (
                    <div
                      key={`${message.id ?? "temp"}-${index}`}
                      className={`flex ${isMine ? "justify-end" : "justify-start"}`}
                    >
                      <div
                        className={`max-w-[70%] rounded-xl px-3 py-2 text-sm ${
                          isMine
                            ? "bg-slate-900 text-white"
                            : "bg-slate-100 text-slate-800"
                        }`}
                      >
                        {message.message}
                      </div>
                    </div>
                  );
                })
              )}
            </div>

            <div className="flex gap-2 border-t border-slate-200 p-3">
              <input
                value={inputMessage}
                onChange={(event) => setInputMessage(event.target.value)}
                onKeyDown={(event) => {
                  // IME (한글 입력) 조합 중에는 메시지 전송을 하지 않도록 처리
                  if (event.key === "Enter" && !event.nativeEvent.isComposing) {
                    event.preventDefault();
                    handleSendMessage();
                  }
                }}
                placeholder="메시지를 입력하세요"
                className="flex-1 rounded-lg border border-slate-200 px-3 py-2 text-sm outline-none focus:border-slate-400"
                disabled={selectedRoomId === null}
              />
              <button
                type="button"
                onClick={handleSendMessage}
                className="rounded-lg bg-slate-900 px-4 py-2 text-sm font-semibold text-white hover:bg-slate-700"
              >
                전송
              </button>
            </div>
          </section>
        </section>
      </main>
    </div>
  );
}
