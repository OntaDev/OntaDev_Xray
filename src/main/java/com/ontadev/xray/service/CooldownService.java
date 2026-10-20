package com.ontadev.xray.service;

import com.ontadev.libs.ioc.annotation.stereotype.Service;
import org.bukkit.inventory.ItemStack;

@Service
public class CooldownService {
    private final LampDataService lampDataService;

    public CooldownService(LampDataService lampDataService) {
        this.lampDataService = lampDataService;
    }

    public long remainingSeconds(ItemStack lamp) {
        long lastUseMillis = lampDataService.getLastUse(lamp);
        long cooldownMillis = lampDataService.getCooldownSeconds(lamp) * 1000L;
        long elapsedMillis = System.currentTimeMillis() - lastUseMillis;
        long remainingMillis = cooldownMillis - elapsedMillis;

        if (remainingMillis <= 0) return 0L;
        return (remainingMillis + 999L) / 1000L;
    }

    public boolean isReady(ItemStack lamp) {
        return remainingSeconds(lamp) <= 0L;
    }
}
