package com.healthlog.demo.dto.profile;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PinSettingRequest {
    private String action;
    private String currentPin;
    private String newPin;
    private String confirmation;
}
