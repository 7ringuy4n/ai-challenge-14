package com.papaya.claims.claim.domain.persistence;

import com.papaya.claims.claim.domain.PaymentRepository;
import com.papaya.claims.claim.domain.mapper.EffectsMapper;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
public class MyBatisPaymentRepository implements PaymentRepository {
    private final EffectsMapper mapper;
    public MyBatisPaymentRepository(EffectsMapper mapper) { this.mapper=mapper; }
    @Override public void create(UUID claimId,UUID requestId,String currency,BigDecimal amount) {
        PaymentRow r=new PaymentRow(); r.claimId=claimId; r.requestId=requestId; r.currency=currency; r.amount=amount; mapper.createPayment(r);
    }
    @Override public boolean exists(UUID claimId) { return mapper.paymentExists(claimId); }
    @Override public Optional<Confirmation> confirmation(UUID claimId) {
        PaymentRow r=mapper.paymentConfirmation(claimId); return r==null ? Optional.empty() : Optional.of(new Confirmation(r.reference,r.confirmedAt));
    }
    @Override public boolean confirm(UUID claimId,String reference) { return mapper.confirmPayment(claimId,reference)==1; }
}
