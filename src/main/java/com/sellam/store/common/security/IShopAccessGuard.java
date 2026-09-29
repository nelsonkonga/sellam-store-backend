package com.sellam.store.common.security;

import org.springframework.security.core.Authentication;
import com.sellam.store.users.models.PermissionEnum;

import java.util.UUID;

public interface IShopAccessGuard
{
    AuthPrincipal requirePrincipal(Authentication authentication);
    AuthPrincipal requireShopAccess(Authentication authentication, UUID shopId);
    void checkShopAccess(AuthPrincipal principal, UUID shopId);
    void checkShopAccess(Authentication authentication, UUID shopId);
    void requirePermission(Authentication authentication, UUID shopId, PermissionEnum permission);
}
