package com.pizzastudio.eum.core.member.domain;

import lombok.Getter;

/**
 * 권한. 신청자와 기관 담당자 둘뿐이다.
 */
@Getter
public enum Role {

    USER("ROLE_USER", "신청자"),
    ADMIN("ROLE_ADMIN", "기관 담당자");

    private final String key;
    private final String title;

    Role(String key, String title) {
        this.key = key;
        this.title = title;
    }

    public static Role of(String key) {
        for (Role role : values()) {
            if (role.key.equals(key)) {
                return role;
            }
        }
        return USER;
    }
}
