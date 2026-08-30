package com.sellam.store.common.security;

import org.springframework.security.core.Authentication;
import java.util.UUID;

public interface IShopAccessGuard
{
    AuthPrincipal requirePrincipal(Authentication authentication);
    AuthPrincipal requireShopAccess(Authentication authentication, UUID shopId);
    void checkShopAccess(AuthPrincipal principal, UUID shopId);
}
