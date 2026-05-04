package com.cubefury.vendingmachine.util;

import java.util.UUID;

import com.cubefury.vendingmachine.VMConfig;
import com.cubefury.vendingmachine.storage.NameCache;

public class TeamHelper {

    private static final UUID GLOBAL_TEAM_UUID = new UUID(0L, 0L);

    public static UUID GetTeamUUID(UUID player) {
        if (VMConfig.vendingMachineSettings.global_team) {
            NameCache.INSTANCE.addTeamUUID(GLOBAL_TEAM_UUID, "global");
            return GLOBAL_TEAM_UUID;
        }
        return null;
    }

}
