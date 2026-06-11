package plz.lizi.supersteve.client.sound;

import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.Entity;

public class SSMusic {
    private static final Map<ResourceLocation, SoundInstance> SOUNDS = new WeakHashMap<>();
    private static final Map<Entity, SoundInstance> ENTITY_BGMS = new HashMap<>();

    public static void play(SoundEvent event) {
        if (event == null)
            return;
        SoundInstance si = SOUNDS.get(event.getLocation());
        if (si != null && Minecraft.getInstance().getSoundManager().isActive(si))
            return;
        SoundInstance nsi = SimpleSoundInstance.forLocalAmbience(event, 1F, 0.1F);
        SOUNDS.put(event.getLocation(), nsi);
        Minecraft.getInstance().getSoundManager().play(nsi);
    }

    public static void end(SoundEvent event) {
        if (event == null) {
            for (SoundInstance si : SOUNDS.values()) {
                if (si != null)
                    Minecraft.getInstance().getSoundManager().stop(si);
            }
        } else {
            SoundInstance si = SOUNDS.get(event.getLocation());
            if (si != null)
                Minecraft.getInstance().getSoundManager().stop(si);
        }
    }

    public static void playWithEntity(Entity zhis, SoundEvent event, boolean loop) {
        if (zhis == null || ENTITY_BGMS.get(zhis) != null)
            return;
        SoundInstance sound = loop ? new BGM(event) : SimpleSoundInstance.forLocalAmbience(event, 1F, 0.1F);
        ENTITY_BGMS.put(zhis, sound);
        Minecraft.getInstance().getSoundManager().play(sound);
    }

    public static void endWithEntity(Entity zhis) {
        if (zhis == null)
            return;
        SoundInstance sound = ENTITY_BGMS.get(zhis);
        if (sound == null)
            return;
        ENTITY_BGMS.remove(zhis);
        Minecraft.getInstance().getSoundManager().stop(sound);
    }

    public static void endAllBgm() {
        for (SoundInstance sound : ENTITY_BGMS.values()) {
            Minecraft.getInstance().getSoundManager().stop(sound);
        }
        ENTITY_BGMS.clear();
    }
}
