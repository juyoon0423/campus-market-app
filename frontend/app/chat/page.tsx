"use client";

import { Client, type IMessage } from "@stomp/stompjs";
import { useRouter, useSearchParams } from "next/navigation";
import SockJS from "sockjs-client";
import { Suspense, useCallback, useEffect, useMemo, useRef, useState } from "react";
import { ChevronLeft } from "lucide-react";
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
import SiteHeader from "@/src/components/SiteHeader";
import { decodeUserIdFromToken, findNumericUserId } from "@/src/hooks/useCurrentUserId";

function getMessageRoomId(message: ChatMessageResponse): number | null {
  if (message.roomId) {
    return message.roomId;
  }

  const roomIdInObject = (message as unknown as { chatRoom?: { id?: number } }).chatRoom?.id;
  return typeof roomIdInObject === "number" ? roomIdInObject : null;
}

export default function ChatPage() {
  return (
    <Suspense fallback={null}>
      <ChatPageContent />
    </Suspense>
  );
}

function ChatPageContent() {
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

  const senderIdFromToken = useMemo(() => decodeUserIdFromToken(token), [token]);
  const senderId = senderIdFromToken ?? fallbackSenderId;
  const selectedRoom = rooms.find((room) => room.id === selectedRoomId) ?? null;

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
      connectHeaders: token ? { Authorization: `Bearer ${token}` } : {},
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
  }, [token]);

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
    <div className="flex h-screen flex-col">
      <SiteHeader />

      <main className="mx-auto flex w-full max-w-6xl flex-1 flex-col gap-3 overflow-hidden px-4 py-4 sm:px-6 md:py-6">
        {errorMessage ? (
          <p className="shrink-0 rounded-field bg-red-soft px-4 py-2.5 text-sm text-red-ink">
            {errorMessage}
          </p>
        ) : null}
        {senderId === null ? (
          <p className="shrink-0 rounded-field bg-amber-soft px-4 py-2.5 text-sm text-amber-ink">
            로그인 토큰에서 사용자 ID를 읽지 못해 메시지 전송이 제한됩니다.
          </p>
        ) : null}

        <div className="flex flex-1 gap-4 overflow-hidden">
          {/* 채팅방 목록 — 모바일에서는 방을 선택하면 숨김 */}
          <aside
            className={`${selectedRoomId ? "hidden md:flex" : "flex"} w-full shrink-0 flex-col overflow-hidden rounded-card border border-border bg-surface md:w-72`}
          >
            <div className="shrink-0 border-b border-border px-4 py-3.5">
              <h2 className="text-sm font-bold text-text">내 채팅방</h2>
            </div>
            <div className="flex-1 overflow-y-auto p-2">
              {isLoadingRooms ? (
                <p className="p-2 text-sm text-text-muted">채팅방을 불러오는 중...</p>
              ) : rooms.length === 0 ? (
                <p className="p-2 text-sm text-text-muted">참여 중인 채팅방이 없습니다.</p>
              ) : (
                <ul className="space-y-1">
                  {rooms.map((room) => (
                    <li key={room.id}>
                      <button
                        type="button"
                        onClick={() => setSelectedRoomId(room.id)}
                        className={`w-full rounded-field px-3 py-2.5 text-left transition-colors ${
                          room.id === selectedRoomId
                            ? "bg-accent text-white"
                            : "text-text hover:bg-surface-alt"
                        }`}
                      >
                        <p className="truncate text-sm font-bold">{room.productName}</p>
                        <p
                          className={`truncate text-xs ${
                            room.id === selectedRoomId ? "text-white/80" : "text-text-faint"
                          }`}
                        >
                          {room.opponentName} · {room.lastMessage || "메시지 없음"}
                        </p>
                      </button>
                    </li>
                  ))}
                </ul>
              )}
            </div>
          </aside>

          {/* 대화창 — 모바일에서는 방을 선택했을 때만 표시 */}
          <section
            className={`${selectedRoomId ? "flex" : "hidden md:flex"} flex-1 flex-col overflow-hidden rounded-card border border-border bg-surface`}
          >
            <div className="flex shrink-0 items-center gap-2 border-b border-border px-4 py-3">
              <button
                type="button"
                onClick={() => setSelectedRoomId(null)}
                className="-ml-1 inline-flex h-7 w-7 items-center justify-center rounded-full text-text-muted hover:bg-surface-alt md:hidden"
                aria-label="채팅방 목록으로 돌아가기"
              >
                <ChevronLeft className="h-4 w-4" />
              </button>
              <div className="min-w-0">
                <p className="truncate text-sm font-bold text-text">
                  {selectedRoom ? selectedRoom.productName : "채팅방을 선택하세요"}
                </p>
                {selectedRoom ? (
                  <p className="truncate text-xs text-text-faint">{selectedRoom.opponentName}</p>
                ) : null}
              </div>
            </div>

            <div className="flex-1 space-y-3 overflow-y-auto px-4 py-3">
              {selectedRoomId === null ? (
                <p className="text-sm text-text-muted">왼쪽에서 채팅방을 선택해 주세요.</p>
              ) : isLoadingMessages ? (
                <p className="text-sm text-text-muted">메시지를 불러오는 중...</p>
              ) : messages.length === 0 ? (
                <p className="text-sm text-text-muted">아직 메시지가 없습니다.</p>
              ) : (
                messages.map((message, index) => {
                  const isMine = senderId !== null && message.senderId === senderId;
                  return (
                    <div
                      key={`${message.id ?? "temp"}-${index}`}
                      className={`flex ${isMine ? "justify-end" : "justify-start"}`}
                    >
                      <div
                        className={`max-w-[70%] rounded-2xl px-4 py-2.5 text-sm ${
                          isMine
                            ? "bg-accent text-white"
                            : "bg-surface-alt text-text"
                        }`}
                      >
                        {message.message}
                      </div>
                    </div>
                  );
                })
              )}
            </div>

            <div className="flex shrink-0 gap-2 border-t border-border p-3">
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
                className="flex-1 rounded-field border border-transparent bg-surface-alt px-3.5 py-2.5 text-sm text-text outline-none transition-colors placeholder:text-text-faint focus:border-accent focus:bg-surface"
                disabled={selectedRoomId === null}
              />
              <button
                type="button"
                onClick={handleSendMessage}
                className="rounded-field bg-accent px-4 py-2.5 text-sm font-bold text-white transition-colors hover:bg-accent-strong"
              >
                전송
              </button>
            </div>
          </section>
        </div>
      </main>
    </div>
  );
}
