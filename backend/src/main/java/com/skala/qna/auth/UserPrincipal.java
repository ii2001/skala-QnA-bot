package com.skala.qna.auth;

import com.skala.qna.organization.UserRole;

public record UserPrincipal(Long userId, UserRole role) {
}
