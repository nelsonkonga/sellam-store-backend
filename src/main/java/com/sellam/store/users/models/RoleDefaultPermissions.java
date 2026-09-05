package com.sellam.store.users.models;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * Définit les permissions accordées par défaut à chaque rôle.
 * Ces valeurs servent de base ; elles peuvent être surchargées
 * individuellement par employé via UserEntity.grantedOverrides / revokedOverrides.
 */
public final class RoleDefaultPermissions
{
    private RoleDefaultPermissions() {}

    private static final Map<RoleEnum, Set<PermissionEnum>> DEFAULTS = Map.of(

            RoleEnum.MANAGER, EnumSet.allOf(PermissionEnum.class),

            RoleEnum.CASHIER, EnumSet.of(
                    PermissionEnum.VIEW_PRODUCTS,
                    PermissionEnum.CREATE_INVOICE,
                    PermissionEnum.DELETE_INVOICE_LINE,
                    PermissionEnum.VALIDATE_INVOICE,
                    PermissionEnum.VIEW_DAILY_BALANCE
                    // Note: APPLY_LINE_DISCOUNT et APPLY_GLOBAL_DISCOUNT exclus (réservé au MANAGER)
            ),

            RoleEnum.SECRETARY, EnumSet.of(
                    PermissionEnum.VIEW_PRODUCTS,
                    PermissionEnum.VIEW_SALES_HISTORY,
                    PermissionEnum.VIEW_DAILY_BALANCE
            )
    );

    public static Set<PermissionEnum> forRole(RoleEnum role)
    {
        return DEFAULTS.getOrDefault(role, Set.of());
    }
}