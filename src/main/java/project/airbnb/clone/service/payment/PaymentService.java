package project.airbnb.clone.service.payment;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import project.airbnb.clone.common.clients.PaymentClient;
import project.airbnb.clone.common.exceptions.BusinessException;
import project.airbnb.clone.common.exceptions.ErrorCode;
import project.airbnb.clone.common.exceptions.factory.MemberExceptions;
import project.airbnb.clone.dto.payment.PaymentConfirmDto;
import project.airbnb.clone.dto.payment.PaymentConfirmReqDto;
import project.airbnb.clone.dto.payment.PaymentResDto;
import project.airbnb.clone.dto.payment.SavePaymentReqDto;
import project.airbnb.clone.entity.member.Member;
import project.airbnb.clone.entity.reservation.Payment;
import project.airbnb.clone.entity.reservation.Reservation;
import project.airbnb.clone.repository.dto.redis.TempPayment;
import project.airbnb.clone.repository.jpa.AccommodationRepository;
import project.airbnb.clone.repository.jpa.MemberRepository;
import project.airbnb.clone.repository.jpa.PaymentRepository;
import project.airbnb.clone.repository.jpa.ReservationRepository;
import project.airbnb.clone.repository.query.ReservationQueryRepository;
import project.airbnb.clone.repository.redis.TempPaymentRepository;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PaymentService {

    private final PaymentClient paymentClient;
    private final MemberRepository memberRepository;
    private final PaymentRepository paymentRepository;
    private final ReservationRepository reservationRepository;
    private final TempPaymentRepository tempPaymentRepository;
    private final AccommodationRepository accommodationRepository;
    private final ReservationQueryRepository reservationQueryRepository;

    @Transactional
    public PaymentResDto confirmPayment(PaymentConfirmReqDto paymentConfirmReqDto, Long memberId) {
        String orderId = paymentConfirmReqDto.orderId();
        Integer amount = paymentConfirmReqDto.amount();
        Long reservationId = paymentConfirmReqDto.reservationId();

        verifyTempPayment(orderId, amount);

        Reservation reservation = reservationRepository.findByIdWithPessimisticLock(reservationId)
                                                       .orElseThrow(() -> new BusinessException(ErrorCode.ENTITY_NOT_FOUND));
        accommodationRepository.findByIdWithPessimisticLock(reservation.getAccommodation().getId())
                               .orElseThrow();

        if (reservationQueryRepository.existsConfirmedReservation(
                reservation.getAccommodation(),
                reservation.getStartDate(),
                reservation.getEndDate())) {
            throw new BusinessException(ErrorCode.ALREADY_RESERVED);
        }

        PaymentConfirmDto paymentConfirmDTO = paymentConfirmReqDto.convert();
        JsonNode response = paymentClient.confirmPayment(paymentConfirmDTO);

        Member member = memberRepository.findById(memberId)
                                        .orElseThrow(() -> MemberExceptions.notFoundById(memberId));

        if (!reservation.getMember().getId().equals(member.getId())) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }

        paymentRepository.save(Payment.of(response, reservation));
        tempPaymentRepository.deleteById(orderId);
        reservation.confirm();

        String receiptUrl = response.get("receipt").get("url").asText(null);
        return new PaymentResDto(receiptUrl);
    }

    public void savePayment(SavePaymentReqDto savePaymentRequestDTO) {
        String orderId = savePaymentRequestDTO.orderId();
        Integer amount = savePaymentRequestDTO.amount();

        tempPaymentRepository.save(TempPayment.builder().orderId(orderId).amount(amount).build());
    }

    private void verifyTempPayment(String orderId, Integer amount) {
        TempPayment tempPayment = tempPaymentRepository.findById(orderId)
                                                       .orElseThrow(() -> new BusinessException(ErrorCode.PAYMENT_NOT_FOUND));

        if (tempPayment.notEqualsAmount(amount)) {
            throw new BusinessException(ErrorCode.NOT_EQUALS_AMOUNT);
        }
    }
}
