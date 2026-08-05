package com.compus.campusmarket.domain.chat.service;

import com.compus.campusmarket.domain.chat.dto.ChatMessageRequest;
import com.compus.campusmarket.domain.chat.dto.ChatMessageResponse;
import com.compus.campusmarket.domain.chat.dto.ChatRoomResponse;
import com.compus.campusmarket.domain.chat.entity.ChatMessage;
import com.compus.campusmarket.domain.chat.entity.ChatRoom;
import com.compus.campusmarket.domain.chat.repository.ChatMessageRepository;
import com.compus.campusmarket.domain.chat.repository.ChatRoomRepository;
import com.compus.campusmarket.domain.product.entity.Product;
import com.compus.campusmarket.domain.product.repository.ProductRepository;
import com.compus.campusmarket.domain.user.entity.User;
import com.compus.campusmarket.domain.user.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// 방마다 반복 쿼리를 날리던 걸 단일 배치 쿼리(findLastMessagesByRoomIds)로 바꾼 뒤,
// 방마다 정확히 자기 자신의 마지막 메시지에 매핑되는지(다른 방 메시지가 섞이지 않는지) 검증한다.
@SpringBootTest
class ChatServiceTest {

    @Autowired
    private ChatService chatService;
    @Autowired
    private ChatRoomRepository chatRoomRepository;
    @Autowired
    private ChatMessageRepository chatMessageRepository;
    @Autowired
    private ProductRepository productRepository;
    @Autowired
    private UserRepository userRepository;

    private User seller;
    private User buyer;
    private Product product1;
    private Product product2;
    private ChatRoom room1;
    private ChatRoom room2;

    private void setUp() {
        seller = userRepository.save(newUser("seller"));
        buyer = userRepository.save(newUser("buyer"));
        product1 = productRepository.save(Product.create("상품1", "설명", 1000L, seller, "카테고리"));
        product2 = productRepository.save(Product.create("상품2", "설명", 2000L, seller, "카테고리"));
        room1 = chatRoomRepository.save(ChatRoom.builder().product(product1).seller(seller).buyer(buyer).build());
        room2 = chatRoomRepository.save(ChatRoom.builder().product(product2).seller(seller).buyer(buyer).build());
    }

    @AfterEach
    void tearDown() {
        if (room1 != null) chatMessageRepository.findAllByChatRoomIdOrderByCreatedAtAsc(room1.getId())
                .forEach(chatMessageRepository::delete);
        if (room2 != null) chatMessageRepository.findAllByChatRoomIdOrderByCreatedAtAsc(room2.getId())
                .forEach(chatMessageRepository::delete);
        if (room1 != null) chatRoomRepository.deleteById(room1.getId());
        if (room2 != null) chatRoomRepository.deleteById(room2.getId());
        if (product1 != null) productRepository.deleteById(product1.getId());
        if (product2 != null) productRepository.deleteById(product2.getId());
        if (seller != null) userRepository.deleteById(seller.getId());
        if (buyer != null) userRepository.deleteById(buyer.getId());
    }

    @Test
    void findAllRooms_각_방은_자기_자신의_마지막_메시지만_반환해야_한다() {
        setUp();
        chatMessageRepository.save(ChatMessage.builder().chatRoom(room1).senderId(buyer.getId()).message("room1-old").build());
        chatMessageRepository.save(ChatMessage.builder().chatRoom(room1).senderId(buyer.getId()).message("room1-new").build());
        chatMessageRepository.save(ChatMessage.builder().chatRoom(room2).senderId(buyer.getId()).message("room2-only").build());

        List<ChatRoomResponse> responses = chatService.findAllRooms(buyer.getId());

        ChatRoomResponse room1Response = responses.stream().filter(r -> r.getId().equals(room1.getId())).findFirst().orElseThrow();
        ChatRoomResponse room2Response = responses.stream().filter(r -> r.getId().equals(room2.getId())).findFirst().orElseThrow();

        assertThat(room1Response.getLastMessage()).isEqualTo("room1-new");
        assertThat(room2Response.getLastMessage()).isEqualTo("room2-only");
    }

    @Test
    void findMessagesByRoomId_참여자가_아니면_조회할_수_없다() {
        setUp();
        chatMessageRepository.save(ChatMessage.builder().chatRoom(room1).senderId(buyer.getId()).message("비밀 대화").build());
        User stranger = userRepository.save(newUser("stranger"));

        try {
            assertThatThrownBy(() -> chatService.findMessagesByRoomId(room1.getId(), stranger.getId()))
                    .isInstanceOf(IllegalStateException.class);

            List<ChatMessageResponse> asParticipant = chatService.findMessagesByRoomId(room1.getId(), buyer.getId());
            assertThat(asParticipant).extracting(ChatMessageResponse::getMessage).containsExactly("비밀 대화");
        } finally {
            userRepository.deleteById(stranger.getId());
        }
    }

    @Test
    void saveMessage_참여자가_아닌_senderId로는_메시지를_보낼_수_없다() {
        setUp();
        User stranger = userRepository.save(newUser("stranger"));
        ChatMessageRequest request = new ChatMessageRequest();
        request.setRoomId(room1.getId());
        request.setMessage("몰래 보내는 메시지");

        try {
            assertThatThrownBy(() -> chatService.saveMessage(request, stranger.getId()))
                    .isInstanceOf(IllegalStateException.class);

            ChatMessageResponse saved = chatService.saveMessage(request, buyer.getId());
            assertThat(saved.getSenderId()).isEqualTo(buyer.getId());
        } finally {
            userRepository.deleteById(stranger.getId());
        }
    }

    // Check-Then-Act 레이스로 같은 (product, buyer) 조합의 채팅방이 여러 개 생성되던 문제
    // (frontend/BACKEND_IMPROVEMENTS.md에 분석돼 있던 것) 재현 + 수정 검증.
    @Test
    void createOrGetRoom_동시_요청에도_채팅방은_하나만_생성된다() throws InterruptedException {
        User raceSeller = userRepository.save(newUser("raceSeller"));
        User raceBuyer = userRepository.save(newUser("raceBuyer"));
        Product raceProduct = productRepository.save(
                Product.create("동시성 채팅방 테스트 상품", "설명", 1000L, raceSeller, "카테고리"));

        int concurrency = 10;
        List<Long> resultRoomIds = Collections.synchronizedList(new ArrayList<>());
        ExecutorService executor = Executors.newFixedThreadPool(concurrency);
        CountDownLatch ready = new CountDownLatch(concurrency);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(concurrency);

        try {
            for (int i = 0; i < concurrency; i++) {
                executor.submit(() -> {
                    ready.countDown();
                    try {
                        start.await();
                        ChatRoomResponse response = chatService.createOrGetRoom(raceProduct.getId(), raceBuyer.getId());
                        resultRoomIds.add(response.getId());
                    } catch (InterruptedException ignored) {
                        Thread.currentThread().interrupt();
                    } finally {
                        done.countDown();
                    }
                });
            }
            ready.await();
            start.countDown();
            done.await(20, TimeUnit.SECONDS);
        } finally {
            executor.shutdownNow();
        }

        assertThat(resultRoomIds).hasSize(concurrency);
        assertThat(resultRoomIds.stream().distinct().count()).isEqualTo(1);

        List<ChatRoom> persistedRooms =
                chatRoomRepository.findByProductIdAndBuyerId(raceProduct.getId(), raceBuyer.getId());
        assertThat(persistedRooms).hasSize(1);

        chatRoomRepository.deleteById(persistedRooms.get(0).getId());
        productRepository.deleteById(raceProduct.getId());
        userRepository.deleteById(raceSeller.getId());
        userRepository.deleteById(raceBuyer.getId());
    }

    private User newUser(String tag) {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        return User.create(tag + "-" + suffix + "@test.campusmarket.com", tag, "S-" + suffix, "테스트학과", "password");
    }
}
