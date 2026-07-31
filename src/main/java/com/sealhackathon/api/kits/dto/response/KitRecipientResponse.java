package com.sealhackathon.api.kits.dto.response;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class KitRecipientResponse {

    private Integer userId;
    private String fullName;
    private String studentCode;
    private String email;
    private String phone;
    private Integer teamId;
    private String teamName;
    private String preferredShirtSize;
    private String preferredShirtFit;
    private List<KitAllocationResponse> allocations;
}
