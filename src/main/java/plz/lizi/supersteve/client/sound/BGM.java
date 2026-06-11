package plz.lizi.supersteve.client.sound;

import net.minecraft.client.resources.sounds.AbstractSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;

public class BGM extends AbstractSoundInstance {

    public BGM(SoundEvent p_235076_) {
        super(p_235076_, SoundSource.AMBIENT, SoundInstance.createUnseededRandom());
        this.looping = true;
        this.delay = 0;
        this.volume = 0.1F;
        this.pitch = 1.0F;
        this.relative = true;
    }
}
