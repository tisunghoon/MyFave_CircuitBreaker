package com.myfave.api.domain.order.service;

import com.myfave.api.domain.cart.repository.CartItemRepository;
import com.myfave.api.domain.order.repository.OrderItemRepository;
import com.myfave.api.domain.order.repository.OrderRepository;
import com.myfave.api.domain.product.repository.ProductRepository;
import com.myfave.api.domain.shipping.repository.DeliveryRepository;
import com.myfave.api.domain.shipping.repository.ShippingAddressRepository;
import com.myfave.api.domain.user.repository.UserRepository;
import com.myfave.api.global.error.CustomException;
import com.myfave.api.global.error.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
class OrderServiceAuthTest {

    @InjectMocks
    private OrderService orderService;

    @Mock private OrderRepository orderRepository;
    @Mock private OrderItemRepository orderItemRepository;
    @Mock private UserRepository userRepository;
    @Mock private ProductRepository productRepository;
    @Mock private CartItemRepository cartItemRepository;
    @Mock private ShippingAddressRepository shippingAddressRepository;
    @Mock private DeliveryRepository deliveryRepository;

    @Test
    @DisplayName("createOrder: userId null → AUTH_UNAUTHORIZED")
    void createOrder_nullUserId_throwsUnauthorized() {
        assertThatThrownBy(() -> orderService.createOrder(null, null))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.AUTH_UNAUTHORIZED);
    }

    @Test
    @DisplayName("getOrders: userId null → AUTH_UNAUTHORIZED")
    void getOrders_nullUserId_throwsUnauthorized() {
        assertThatThrownBy(() -> orderService.getOrders(null, PageRequest.of(0, 20)))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.AUTH_UNAUTHORIZED);
    }

    @Test
    @DisplayName("getOrderDetail: userId null → AUTH_UNAUTHORIZED")
    void getOrderDetail_nullUserId_throwsUnauthorized() {
        assertThatThrownBy(() -> orderService.getOrderDetail(null, 1L))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.AUTH_UNAUTHORIZED);
    }

    @Test
    @DisplayName("confirmOrder: userId null → AUTH_UNAUTHORIZED")
    void confirmOrder_nullUserId_throwsUnauthorized() {
        assertThatThrownBy(() -> orderService.confirmOrder(null, 1L))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.AUTH_UNAUTHORIZED);
    }
}
