package com.myfave.api.global.config;

import com.myfave.api.domain.chat.entity.ChatRoom;
import com.myfave.api.domain.chat.repository.ChatRoomRepository;
import com.myfave.api.domain.saleevent.entity.SaleEvent;
import com.myfave.api.domain.saleevent.repository.SaleEventRepository;
import com.myfave.api.domain.shipping.entity.ShippingAddress;
import com.myfave.api.domain.shipping.repository.ShippingAddressRepository;
import com.myfave.api.domain.user.entity.User;
import com.myfave.api.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

// 부하 테스트(시나리오 A/D/E)용 시드 Runner.
// - 동일 평문 비밀번호의 BCrypt 해시를 1회만 계산해 1000건 재사용 → 시드 시간 단축
// - 멱등: 마지막 이메일이 이미 존재하면 skip
@Slf4j
@Component
@Profile("loadtest")
@RequiredArgsConstructor
public class SeedDataLoadTestRunner implements CommandLineRunner {

    private final UserRepository userRepository;
    private final ShippingAddressRepository shippingAddressRepository;
    private final SaleEventRepository saleEventRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${loadtest.seed.enabled:true}")
    private boolean enabled;

    @Value("${loadtest.seed.user-count:1000}")
    private int userCount;

    @Value("${loadtest.seed.password-plain:Password123!}")
    private String passwordPlain;

    private static final String EMAIL_PREFIX = "loadtest";
    private static final String EMAIL_DOMAIN = "@myfave.test";
    private static final String NICKNAME_PREFIX = "lt-user-";

    @Override
    @Transactional
    public void run(String... args) {
        if (!enabled) {
            log.info("[SeedDataLoadTestRunner] 시드 비활성화 (loadtest.seed.enabled=false)");
            return;
        }

        String sentinelEmail = EMAIL_PREFIX + userCount + EMAIL_DOMAIN;
        if (userRepository.existsByEmail(sentinelEmail)) {
            log.info("[SeedDataLoadTestRunner] 이미 {}명 시드 완료, skip", userCount);
            return;
        }

        log.info("[SeedDataLoadTestRunner] 시드 시작 — 유저 {}명, BCrypt 해시 1회 계산 후 재사용", userCount);
        String encodedPassword = passwordEncoder.encode(passwordPlain);

        List<User> users = new ArrayList<>(userCount);
        for (int i = 1; i <= userCount; i++) {
            String email = EMAIL_PREFIX + i + EMAIL_DOMAIN;
            if (userRepository.existsByEmail(email)) continue;
            User u = User.builder()
                    .email(email)
                    .password(encodedPassword)
                    .name("부하" + i)
                    .nickname(NICKNAME_PREFIX + i)
                    .phone(formatPhone(i))
                    .build();
            users.add(u);
        }
        userRepository.saveAll(users);
        log.info("[SeedDataLoadTestRunner] 유저 {}건 저장 완료", users.size());

        List<ShippingAddress> addresses = new ArrayList<>(users.size());
        for (User u : users) {
            addresses.add(ShippingAddress.builder()
                    .user(u)
                    .receiverName(u.getName())
                    .receiverPhone(u.getPhone())
                    .address("서울특별시 강남구 테헤란로 " + u.getUserId() + "길 1")
                    .addressDetail("부하테스트동 " + u.getUserId() + "호")
                    .zipCode("06000")
                    .deliveryRequest("문 앞에 두세요")
                    .isDefault(true)
                    .build());
        }
        shippingAddressRepository.saveAll(addresses);
        log.info("[SeedDataLoadTestRunner] ShippingAddress {}건 저장 완료", addresses.size());

        if (saleEventRepository.count() == 0 && !users.isEmpty()) {
            User host = users.get(0);
            ZonedDateTime now = ZonedDateTime.now();
            SaleEvent sale = SaleEvent.builder()
                    .eventName("부하 테스트 라이브 이벤트")
                    .saleStartAt(now)
                    .saleEndAt(now.plusDays(1))
                    .build();
            saleEventRepository.save(sale);

            ChatRoom room = ChatRoom.builder()
                    .user(host)
                    .saleEvent(sale)
                    .build();
            chatRoomRepository.save(room);
            log.info("[SeedDataLoadTestRunner] SaleEvent + ChatRoom 1건 생성 (roomId={})", room.getChatRoomId());
        }

        log.info("[SeedDataLoadTestRunner] 시드 완료");
    }

    // 010-{1000..1999}-{0000..9999} 패턴으로 unique 전화번호 생성 (최대 약 10,000명 지원)
    private String formatPhone(int n) {
        int high = 1000 + (n / 10000);
        int low = n % 10000;
        return String.format("010-%04d-%04d", high, low);
    }
}
