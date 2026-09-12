package com.myfave.api.domain.auth.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class FindEmailResponse {

    private String email;

    public static FindEmailResponse from(String email) {
        return new FindEmailResponse(email);
    }
}
