import { createBrowserRouter, RouterProvider } from 'react-router-dom'

import { AboutPage } from '@/pages/AboutPage'
import { AddShippingPage } from '@/pages/AddShippingPage'
import { BusinessInfoPage } from '@/pages/BusinessInfoPage'
import { CartPage } from '@/pages/CartPage'
import { CouponPage } from '@/pages/CouponPage'
import { FAQPage } from '@/pages/FAQPage'
import { FindIdPage } from '@/pages/FindIdPage'
import { FindPasswordPage } from '@/pages/FindPasswordPage'
import { InquiryPage } from '@/pages/InquiryPage'
import { KakaoCallbackPage } from '@/pages/KakaoCallbackPage'
import { LiveChatPage } from '@/pages/LiveChatPage'
import { LoginPage } from '@/pages/LoginPage'
import { MainPage } from '@/pages/MainPage'
import { MyPage } from '@/pages/MyPage'
import { NoticePage } from '@/pages/NoticePage'
import { NotFoundPage } from '@/pages/NotFoundPage'
import { PrivacyPage } from '@/pages/PrivacyPage'
import { ShippingInfoPage } from '@/pages/ShippingInfoPage'
import { TermsPage } from '@/pages/TermsPage'
import { OrderDetailPage } from '@/pages/OrderDetailPage'
import { OrderSuccessPage } from '@/pages/OrderSuccessPage'
import { OrdersPage } from '@/pages/OrdersPage'
import { PaymentPage } from '@/pages/PaymentPage'
import { ProductDetailPage } from '@/pages/ProductDetailPage'
import { ProductListPage } from '@/pages/ProductListPage'
import { ShippingAddressPage } from '@/pages/ShippingAddressPage'
import { ShippingStatusPage } from '@/pages/ShippingStatusPage'
import { SignUpPage } from '@/pages/SignUpPage'
import { Layout } from '@/shared/components/Layout'
import { ProtectedRoute } from '@/shared/components/ProtectedRoute'
import { ScrollToTopWrapper } from '@/shared/components/ScrollToTopWrapper'

const router = createBrowserRouter([
  {
    element: <ScrollToTopWrapper />,
    children: [
      // Auth & support pages (no layout)
      { path: '/login', element: <LoginPage /> },
      { path: '/signup', element: <SignUpPage /> },
      { path: '/find-id', element: <FindIdPage /> },
      { path: '/find-password', element: <FindPasswordPage /> },
      { path: '/inquiry', element: <InquiryPage /> },
      { path: '/auth/kakao/callback', element: <KakaoCallbackPage /> },

      // Pages with Header + Footer (Layout)
      {
        element: <Layout />,
        children: [
          // Public browsing and community pages
          { path: '/', element: <MainPage /> },
          { path: '/shop', element: <ProductListPage /> },
          { path: '/product/:id', element: <ProductDetailPage /> },
          { path: '/cart', element: <CartPage /> },
          { path: '/live-chat', element: <LiveChatPage /> },
          { path: '/about', element: <AboutPage /> },
          { path: '/notice', element: <NoticePage /> },
          { path: '/faq', element: <FAQPage /> },
          { path: '/shipping', element: <ShippingInfoPage /> },
          { path: '/business', element: <BusinessInfoPage /> },
          { path: '/terms', element: <TermsPage /> },
          { path: '/privacy', element: <PrivacyPage /> },

          // Protected pages (require login)
          {
            element: <ProtectedRoute />,
            children: [
              { path: '/payment', element: <PaymentPage /> },
              { path: '/shipping-addresses', element: <ShippingAddressPage /> },
              { path: '/add-shipping', element: <AddShippingPage /> },
              { path: '/coupons', element: <CouponPage /> },
              { path: '/order-success', element: <OrderSuccessPage /> },
              { path: '/mypage', element: <MyPage /> },
              { path: '/orders', element: <OrdersPage /> },
              { path: '/orders/:id', element: <OrderDetailPage /> },
              { path: '/shipping-status/:orderId', element: <ShippingStatusPage /> },
            ],
          },
        ],
      },

      { path: '*', element: <NotFoundPage /> },
    ],
  },
])

export function AppRouter() {
  return <RouterProvider router={router} />
}
