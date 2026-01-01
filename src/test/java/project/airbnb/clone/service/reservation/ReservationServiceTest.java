package project.airbnb.clone.service.reservation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import project.airbnb.clone.TestContainerSupport;
import project.airbnb.clone.common.clients.PaymentClient;
import project.airbnb.clone.consts.ReservationStatus;
import project.airbnb.clone.dto.payment.PaymentConfirmDto;
import project.airbnb.clone.dto.payment.PaymentConfirmReqDto;
import project.airbnb.clone.dto.payment.SavePaymentReqDto;
import project.airbnb.clone.dto.reservation.PostReservationReqDto;
import project.airbnb.clone.dto.reservation.PostReservationResDto;
import project.airbnb.clone.repository.jpa.PaymentRepository;
import project.airbnb.clone.repository.jpa.ReservationRepository;
import project.airbnb.clone.service.payment.PaymentService;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@Sql("/sql/concurrency_reservation.sql")
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class ReservationConcurrencyTest extends TestContainerSupport {

    @Autowired EntityManager em;
    @Autowired PaymentService paymentService;
    @Autowired PaymentRepository paymentRepository;
    @Autowired ReservationService reservationService;
    @Autowired ReservationRepository reservationRepository;

    @MockitoBean PaymentClient paymentClient;

    @AfterEach
    void tearDown() {
        paymentRepository.deleteAllInBatch();
        reservationRepository.deleteAllInBatch();
    }

    @Test
    @DisplayName("여러 명이 동시에 같은 날짜의 예약을 결제 시도할 때 하나만 성공해야 한다")
    void concurrencyTest() throws InterruptedException {
        // Given
        Long accommodationId = 1L;
        int threadCount = 10;

        List<Long> reservationIds = new ArrayList<>();

        for (int i = 1; i <= threadCount; i++) {
            PostReservationReqDto req = new PostReservationReqDto(
                    LocalDate.now().plusDays(10), LocalDate.now().plusDays(11), 2, 0, 0
            );
            PostReservationResDto response = reservationService.postReservation((long) i, accommodationId, req);
            Long resId = response.reservationId();
            paymentService.savePayment(new SavePaymentReqDto("order_" + resId, 100_000));
            reservationIds.add(resId);
        }

        ObjectMapper mapper = new ObjectMapper();
        
        given(paymentClient.confirmPayment(any())).willAnswer(invocation -> {
            PaymentConfirmDto arg = invocation.getArgument(0);
            ObjectNode mockResponse = mapper.createObjectNode();
            
            ObjectNode receiptNode = mapper.createObjectNode();
            receiptNode.put("url", "test-url");
            mockResponse.set("receipt", receiptNode);
            
            mockResponse.put("orderId", arg.orderId());
            mockResponse.put("paymentKey", "pk_" + arg.orderId()); 
            mockResponse.put("status", "DONE");
            mockResponse.put("method", "계좌이체");
            mockResponse.put("requestedAt", "2025-12-31T18:00:00+09:00");
            mockResponse.put("approvedAt", "2025-12-31T18:00:00+09:00");
            mockResponse.put("totalAmount", arg.amount());
            
            return mockResponse;
        });

        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger();
        AtomicInteger failCount = new AtomicInteger();

        // When
        for (int i = 0; i < threadCount; i++) {
            Long resId = reservationIds.get(i);
            Long memberId = (long) (i + 1);
            executorService.execute(() -> {
                try {
                    PaymentConfirmReqDto reqDto = new PaymentConfirmReqDto("pk", "order_" + resId, 100_000, resId);
                    paymentService.confirmPayment(reqDto, memberId);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    System.err.println("결제 실패: " + e.getMessage());
                    failCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();

        // Then
        System.out.println("성공 횟수: " + successCount.get());
        System.out.println("실패 횟수: " + failCount.get());

        assertThat(successCount.get()).isEqualTo(1);
        assertThat(em.createQuery("SELECT count(r) FROM Reservation r WHERE r.status = :status", Long.class)
                     .setParameter("status", ReservationStatus.CONFIRMED).getSingleResult())
                .isEqualTo(1);
    }

    @Test
    @DisplayName("날짜가 부분적으로 겹치는 여러 결제 요청이 동시에 올 때 하나만 성공해야 한다")
    void partialOverlapConcurrencyTest() throws InterruptedException {
        // Given
        Long accommodationId = 1L;
        int threadCount = 2; // 이해를 돕기 위해 2개로 설정 (12/10~12/12 vs 12/11~12/13)
        List<Long> reservationIds = new ArrayList<>();

        // 1. 첫 번째 예약: 10일 ~ 12일 (2박)
        PostReservationReqDto req1 = new PostReservationReqDto(
                LocalDate.now().plusDays(10), LocalDate.now().plusDays(12), 2, 0, 0
        );
        PostReservationResDto res1 = reservationService.postReservation(1L, accommodationId, req1);
        paymentService.savePayment(new SavePaymentReqDto("order_" + res1.reservationId(), 100_000));
        reservationIds.add(res1.reservationId());

        // 2. 두 번째 예약: 11일 ~ 13일 (2박) -> 11일 밤이 겹침
        PostReservationReqDto req2 = new PostReservationReqDto(
                LocalDate.now().plusDays(11), LocalDate.now().plusDays(13), 2, 0, 0
        );
        PostReservationResDto res2 = reservationService.postReservation(2L, accommodationId, req2);
        paymentService.savePayment(new SavePaymentReqDto("order_" + res2.reservationId(), 100_000));
        reservationIds.add(res2.reservationId());

        // Mocking (기존과 동일)
        ObjectMapper mapper = new ObjectMapper();
        given(paymentClient.confirmPayment(any())).willAnswer(invocation -> {
            PaymentConfirmDto arg = invocation.getArgument(0);
            ObjectNode mockResponse = mapper.createObjectNode();
            ObjectNode receiptNode = mapper.createObjectNode();
            receiptNode.put("url", "test-url");
            mockResponse.set("receipt", receiptNode);
            mockResponse.put("orderId", arg.orderId());
            mockResponse.put("paymentKey", "pk_" + arg.orderId()); 
            mockResponse.put("status", "DONE");
            mockResponse.put("method", "계좌이체");
            mockResponse.put("requestedAt", "2025-12-31T18:00:00+09:00");
            mockResponse.put("approvedAt", "2025-12-31T18:00:00+09:00");
            mockResponse.put("totalAmount", arg.amount());
            return mockResponse;
        });

        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger();
        AtomicInteger failCount = new AtomicInteger();

        // When
        for (int i = 0; i < threadCount; i++) {
            Long resId = reservationIds.get(i);
            Long memberId = (long) (i + 1);
            executorService.execute(() -> {
                try {
                    PaymentConfirmReqDto reqDto = new PaymentConfirmReqDto("pk", "order_" + resId, 100_000, resId);
                    paymentService.confirmPayment(reqDto, memberId);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    System.err.println("결제 실패: " + e.getMessage());
                    failCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }
        latch.await();

        // Then
        System.out.println("성공 횟수: " + successCount.get());
        
        // 날짜가 겹치므로 둘 중 하나만 성공해야 함
        assertThat(successCount.get()).isEqualTo(1);
        assertThat(em.createQuery("SELECT count(r) FROM Reservation r WHERE r.status = :status", Long.class)
                     .setParameter("status", ReservationStatus.CONFIRMED).getSingleResult())
                .isEqualTo(1);
    }
}