package com.myfave.api.domain.shipping.service;

import com.myfave.api.domain.order.entity.Order;
import com.myfave.api.domain.order.repository.OrderRepository;
import com.myfave.api.domain.shipping.client.TrackerDeliveryClient;
import com.myfave.api.domain.shipping.dto.response.TrackingResponse;
import com.myfave.api.domain.shipping.entity.Delivery;
import com.myfave.api.domain.shipping.repository.DeliveryRepository;
import com.myfave.api.domain.shipping.repository.ShippingAddressRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.myfave.api.domain.shipping.dto.response.ShippingAddressResponse;
import com.myfave.api.domain.shipping.entity.ShippingAddress;
import com.myfave.api.domain.user.entity.User;
import com.myfave.api.domain.user.repository.UserRepository;
import com.myfave.api.global.error.CustomException;
import com.myfave.api.global.error.ErrorCode;
import com.myfave.api.domain.shipping.dto.request.ShippingAddressRequest;

import com.myfave.api.domain.shipping.entity.DeliveryStatus;

import java.util.List;

import com.myfave.api.domain.shipping.dto.response.DefaultAddressResponse;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ShippingService {

    private final ShippingAddressRepository shippingAddressRepository;
    private final DeliveryRepository deliveryRepository;
    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final TrackerDeliveryClient trackerDeliveryClient;

    // 7-1. 배송지 목록 조회
    public List<ShippingAddressResponse> getShippingAddresses(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        return shippingAddressRepository.findByUser(user).stream()
                .map(ShippingAddressResponse::from)
                .toList();
    }

    // 7-2. 배송지 추가
    @Transactional
    public ShippingAddressResponse addShippingAddress(Long userId, ShippingAddressRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        ShippingAddress shippingAddress = ShippingAddress.builder()
                .user(user)
                .receiverName(request.getReceiverName())
                .receiverPhone(request.getReceiverPhone())
                .address(request.getAddress())
                .addressDetail(request.getAddressDetail())
                .zipCode(request.getZipCode())
                .deliveryRequest(request.getDeliveryRequest())
                .isDefault(false)
                .build();

        shippingAddressRepository.save(shippingAddress);
        return ShippingAddressResponse.from(shippingAddress);
    }

    // 7-3. 배송지 삭제
    @Transactional
    public void deleteShippingAddress(Long userId, Long addressId) {
        // 1) 배송지가 존재하는지 확인
        ShippingAddress shippingAddress = shippingAddressRepository.findById(addressId)
                .orElseThrow(() -> new CustomException(ErrorCode.SHIPPING_ADDRESS_NOT_FOUND));

        // 2) 본인 배송지인지 확인
        if (!shippingAddress.getUser().getUserId().equals(userId)) {
            throw new CustomException(ErrorCode.AUTH_FORBIDDEN);
        }

        // 3) db에서 삭제
        shippingAddressRepository.delete(shippingAddress);
    }

    // 7-4. 기본 배송지 설정
    @Transactional
    public DefaultAddressResponse setDefaultAddress(Long userId, Long addressId) {
        ShippingAddress shippingAddress = shippingAddressRepository.findById(addressId)
                .orElseThrow(() -> new CustomException(ErrorCode.SHIPPING_ADDRESS_NOT_FOUND));

        if (!shippingAddress.getUser().getUserId().equals(userId)) {
            throw new CustomException(ErrorCode.AUTH_FORBIDDEN);
        }

        User user = shippingAddress.getUser();

        // 기존 기본 배송지 해제
        shippingAddressRepository.findByUserAndIsDefaultTrue(user)
                .ifPresent(existing -> existing.unsetDefault());

        // 새 기본 배송지 설정
        shippingAddress.setAsDefault();

        return DefaultAddressResponse.from(shippingAddress);
    }

    // 7-5. 배송 추적
    // 외부 API 호출이 트랜잭션 범위에 포함되어 있으나, DELIVERED 조기 반환으로 평균 트랜잭션 시간을 최소화함
    // 고트래픽 환경에서는 읽기/쓰기 분리 리팩토링 필요
    @Transactional
    public TrackingResponse trackDelivery(Long userId, Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new CustomException(ErrorCode.ORDER_NOT_FOUND));

        if (!order.getUser().getUserId().equals(userId)) {
            throw new CustomException(ErrorCode.AUTH_FORBIDDEN);
        }

        Delivery delivery = deliveryRepository.findByOrder(order)
                .orElseThrow(() -> new CustomException(ErrorCode.TRACKING_NOT_REGISTERED));

        if (delivery.getTrackingNumber() == null || delivery.getCarrierId() == null) {
            throw new CustomException(ErrorCode.TRACKING_NOT_REGISTERED);
        }

        // 이미 배송 완료된 경우 외부 API 호출 스킵
        if (delivery.getDeliveryStatus() == DeliveryStatus.DELIVERED) {
            return TrackingResponse.builder()
                    .trackingNumber(delivery.getTrackingNumber())
                    .carrierId(delivery.getCarrierId())
                    .statusCode("DELIVERED")
                    .statusName("배송 완료")
                    .build();
        }

        TrackerDeliveryClient.TrackResult result =
                trackerDeliveryClient.track(delivery.getCarrierId(), delivery.getTrackingNumber());

        TrackerDeliveryClient.EventData lastEvent = result.getLastEvent();
        if (lastEvent != null && lastEvent.getStatus() != null
                && "DELIVERED".equals(lastEvent.getStatus().getCode())) {
            delivery.deliver();
            order.completeDelivery();
        }

        return TrackingResponse.from(delivery.getCarrierId(), result);
    }
}