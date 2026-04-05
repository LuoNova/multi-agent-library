package com.library.constant;

import java.util.Set;

public final class EquipmentConstants {

    private EquipmentConstants() {
    }

    public static final String STATUS_NORMAL = "NORMAL";
    public static final String STATUS_FAULT = "FAULT";
    public static final String STATUS_MAINTAIN = "MAINTAIN";
    public static final String STATUS_DISABLED = "DISABLED";

    public static final Set<String> ALL_STATUSES = Set.of(
            STATUS_NORMAL, STATUS_FAULT, STATUS_MAINTAIN, STATUS_DISABLED);
}
