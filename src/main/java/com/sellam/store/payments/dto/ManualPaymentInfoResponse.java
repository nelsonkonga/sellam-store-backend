package com.sellam.store.payments.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ManualPaymentInfoResponse
{
    private String mobileMoneyNumber;
    private String mobileMoneyHolderName;
    private String mobileMoneyOperator;
    private String supportContactUrl;
    private Integer planAmountXaf;
}
