package com.myfave.api.domain.shipping.service;

import com.myfave.api.domain.order.entity.Order;
import com.myfave.api.domain.order.repository.OrderRepository;
import com.myfave.api.domain.shipping.client.TrackerDeliveryClient;
import com.myfave.api.domain.shipping.dto.response.TrackingResponse;
import com.myfave.api.domain.shipping.entity.Delivery;
import com.myfave.api.domain.shipping.entity.DeliveryStatus;
import com.myfave.api.domain.shipping.repository.DeliveryRepository;
import com.myfave.api.domain.shipping.repository.ShippingAddressRepository;
import com.myfave.api.domain.user.entity.User;
import com.myfave.api.domain.user.repository.UserRepository;
import com.myfave.api.global.error.CustomException;
import com.myfave.api.global.error.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DeliveryTrackingServiceTest {

    @Mock private OrderRepository orderRepository;
    @Mock private DeliveryRepository deliveryRepository;
    @Mock private ShippingAddressRepository shippingAddressRepository;
    @Mock private UserRepository userRepository;
    @Mock private TrackerDeliveryClient trackerDeliveryClient;

    @InjectMocks
    private ShippingService shippingService;

    private static final Long ORDER_ID = 1L;
    private static final Long USER_ID = 10L;
    private static final Long OTHER_USER_ID = 99L;

    private Order mockOrder(Long userId) {
        Order order = mock(Order.class);
        User user = mock(User.class);
        lenient().when(order.getUser()).thenReturn(user);
        lenient().when(user.getUserId()).thenReturn(userId);
        return order;
    }

    private Delivery mockDelivery(String carrierId, String trackingNumber) {
        Delivery delivery = mock(Delivery.class);
        lenient().when(delivery.getCarrierId()).thenReturn(carrierId);
        lenient().when(delivery.getTrackingNumber()).thenReturn(trackingNumber);
        lenient().when(delivery.getDeliveryStatus()).thenReturn(DeliveryStatus.SHIPPING);
        return delivery;
    }

    private TrackerDeliveryClient.TrackResult mockTrackResult(String statusCode) {
        TrackerDeliveryClient.TrackResult result = mock(TrackerDeliveryClient.TrackResult.class);
        TrackerDeliveryClient.EventData event = mock(TrackerDeliveryClient.EventData.class);
        TrackerDeliveryClient.StatusData status = mock(TrackerDeliveryClient.StatusData.class);
        TrackerDeliveryClient.LocationData location = mock(TrackerDeliveryClient.LocationData.class);

        lenient().when(result.getTrackingNumber()).thenReturn("1234567890");
        lenient().when(result.getLastEvent()).thenReturn(event);
        lenient().when(event.getStatus()).thenReturn(status);
        lenient().when(status.getCode()).thenReturn(statusCode);
        lenient().when(status.getName()).thenReturn("배송 중");
        lenient().when(event.getLocation()).thenReturn(location);
        lenient().when(location.getName()).thenReturn("서울 강남");
        lenient().when(event.getDescription()).thenReturn("배송 중입니다.");
        return result;
    }

    @Test
    @DisplayName("trackDelivery 성공 - IN_TRANSIT 상태면 DB 변경 없이 응답 반환")
    void trackDelivery_inTransit_noDbChange() {
        Order order = mockOrder(USER_ID);
        Delivery delivery = mockDelivery("kr.cupost", "1234567890");
        TrackerDeliveryClient.TrackResult result = mockTrackResult("IN_TRANSIT");

        given(orderRepository.findById(ORDER_ID)).willReturn(Optional.of(order));
        given(deliveryRepository.findByOrder(order)).willReturn(Optional.of(delivery));
        given(trackerDeliveryClient.track("kr.cupost", "1234567890")).willReturn(result);

        TrackingResponse response = shippingService.trackDelivery(USER_ID, ORDER_ID);

        assertThat(response.getStatusCode()).isEqualTo("IN_TRANSIT");
        assertThat(response.getCarrierId()).isEqualTo("kr.cupost");
        verify(delivery, never()).deliver();
        verify(order, never()).completeDelivery();
    }

    @Test
    @DisplayName("trackDelivery 성공 - DELIVERED 상태면 delivery.deliver() + order.completeDelivery() 호출")
    void trackDelivery_delivered_syncDb() {
        Order order = mockOrder(USER_ID);
        Delivery delivery = mockDelivery("kr.cupost", "1234567890");
        TrackerDeliveryClient.TrackResult result = mockTrackResult("DELIVERED");

        given(orderRepository.findById(ORDER_ID)).willReturn(Optional.of(order));
        given(deliveryRepository.findByOrder(order)).willReturn(Optional.of(delivery));
        given(trackerDeliveryClient.track("kr.cupost", "1234567890")).willReturn(result);

        TrackingResponse response = shippingService.trackDelivery(USER_ID, ORDER_ID);

        assertThat(response.getStatusCode()).isEqualTo("DELIVERED");
        verify(delivery).deliver();
        verify(order).completeDelivery();
    }

    @Test
    @DisplayName("trackDelivery 실패 - 주문 없으면 ORDER_NOT_FOUND")
    void trackDelivery_orderNotFound() {
        given(orderRepository.findById(ORDER_ID)).willReturn(Optional.empty());

        assertThatThrownBy(() -> shippingService.trackDelivery(USER_ID, ORDER_ID))
                .isInstanceOf(CustomException.class)
                .satisfies(ex -> assertThat(((CustomException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.ORDER_NOT_FOUND));
    }

    @Test
    @DisplayName("trackDelivery 실패 - 본인 주문 아니면 AUTH_FORBIDDEN")
    void trackDelivery_notOwner() {
        Order order = mockOrder(OTHER_USER_ID);
        given(orderRepository.findById(ORDER_ID)).willReturn(Optional.of(order));

        assertThatThrownBy(() -> shippingService.trackDelivery(USER_ID, ORDER_ID))
                .isInstanceOf(CustomException.class)
                .satisfies(ex -> assertThat(((CustomException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.AUTH_FORBIDDEN));
    }

    @Test
    @DisplayName("trackDelivery 실패 - Delivery 없으면 TRACKING_NOT_REGISTERED")
    void trackDelivery_deliveryNotFound() {
        Order order = mockOrder(USER_ID);
        given(orderRepository.findById(ORDER_ID)).willReturn(Optional.of(order));
        given(deliveryRepository.findByOrder(order)).willReturn(Optional.empty());

        assertThatThrownBy(() -> shippingService.trackDelivery(USER_ID, ORDER_ID))
                .isInstanceOf(CustomException.class)
                .satisfies(ex -> assertThat(((CustomException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.TRACKING_NOT_REGISTERED));
    }

    @Test
    @DisplayName("trackDelivery 실패 - trackingNumber null이면 TRACKING_NOT_REGISTERED")
    void trackDelivery_trackingNumberNull() {
        Order order = mockOrder(USER_ID);
        Delivery delivery = mockDelivery("kr.cupost", null);
        given(orderRepository.findById(ORDER_ID)).willReturn(Optional.of(order));
        given(deliveryRepository.findByOrder(order)).willReturn(Optional.of(delivery));

        assertThatThrownBy(() -> shippingService.trackDelivery(USER_ID, ORDER_ID))
                .isInstanceOf(CustomException.class)
                .satisfies(ex -> assertThat(((CustomException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.TRACKING_NOT_REGISTERED));
    }

    @Test
    @DisplayName("trackDelivery 성공 - 이미 DELIVERED 상태면 외부 API 호출 없이 반환")
    void trackDelivery_alreadyDelivered_skipApiCall() {
        Order order = mockOrder(USER_ID);
        Delivery delivery = mock(Delivery.class);
        lenient().when(delivery.getCarrierId()).thenReturn("kr.cupost");
        lenient().when(delivery.getTrackingNumber()).thenReturn("1234567890");
        lenient().when(delivery.getDeliveryStatus()).thenReturn(DeliveryStatus.DELIVERED);

        given(orderRepository.findById(ORDER_ID)).willReturn(Optional.of(order));
        given(deliveryRepository.findByOrder(order)).willReturn(Optional.of(delivery));

        TrackingResponse response = shippingService.trackDelivery(USER_ID, ORDER_ID);

        assertThat(response.getStatusCode()).isEqualTo("DELIVERED");
        verify(trackerDeliveryClient, never()).track(anyString(), anyString());
    }
}
