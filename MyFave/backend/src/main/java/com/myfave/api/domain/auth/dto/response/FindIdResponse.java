package com.myfave.api.domain.auth.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class FindIdResponse {

    private String maskedEmail;

    public static FindIdResponse from(String maskedEmail) {
        return new FindIdResponse(maskedEmail);
    }
}
