package com.compus.campusmarket.domain.chat.service;

import com.compus.campusmarket.domain.chat.entity.ChatRoom;
import com.compus.campusmarket.domain.chat.repository.ChatRoomRepository;
import com.compus.campusmarket.domain.product.entity.Product;
import com.compus.campusmarket.domain.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

// ChatService.createOrGetRoom의 호출 트랜잭션과 별도로 "조회 후 없으면 생성"을 통째로 격리한다
// (ProductLikeService와 동일한 이유 — self-invocation은 프록시를 안 거쳐 REQUIRES_NEW가 안 먹으므로
// 별도 빈으로 분리). 조회와 생성을 같은 트랜잭션 안에서 처리해야, 재시도(REQUIRES_NEW로 새 트랜잭션을
// 여는 것) 때마다 REPEATABLE READ 스냅샷도 새로 잡혀서 방금 다른 트랜잭션이 커밋한 방을 확실히 볼 수
// 있다. 호출부(ChatService)의 트랜잭션에서 미리 조회해두면 그 스냅샷이 고정돼버려서, 경쟁에서 진
// 요청이 재조회해도 계속 빈 결과를 보는 문제가 있었다(실측으로 확인).
@Service
@RequiredArgsConstructor
public class ChatRoomCreationService {

    private final ChatRoomRepository chatRoomRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public ChatRoom getOrCreateRoom(Product product, User seller, User buyer) {
        List<ChatRoom> existingRooms =
                chatRoomRepository.findByProductIdAndBuyerIdWithAssociations(product.getId(), buyer.getId());
        if (!existingRooms.isEmpty()) {
            return existingRooms.get(0);
        }

        ChatRoom room = chatRoomRepository.save(
                ChatRoom.builder().product(product).seller(seller).buyer(buyer).build());
        // IDENTITY 생성 전략이라 save() 시점에 INSERT가 즉시 나가긴 하지만, unique 제약 위반은
        // 여기서 flush를 강제해 이 메서드(=이 트랜잭션) 안에서 확정시켜야 호출부의 catch가 안전하게 잡는다.
        chatRoomRepository.flush();
        return room;
    }
}
