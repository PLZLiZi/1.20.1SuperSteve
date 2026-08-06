package plz.lizi.supersteve.level;

import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.entity.EntityAccess;
import net.minecraft.world.level.entity.EntityInLevelCallback;
import net.minecraft.world.level.entity.EntitySection;
import net.minecraft.world.level.entity.PersistentEntitySectionManager;
import net.minecraft.world.level.entity.Visibility;

public class SEntityCallback<T extends EntityAccess> implements EntityInLevelCallback {
    public PersistentEntitySectionManager<T> base;
    public T entity;
    public Entity realEntity;
    public long currentSectionKey;
    public EntitySection<T> currentSection;

    public SEntityCallback(PersistentEntitySectionManager<T> base, T p_157614_, long p_157615_, EntitySection<T> p_157616_) {
        this.base = base;
        this.entity = p_157614_;
        this.realEntity = p_157614_ instanceof Entity ? (Entity) p_157614_ : null;
        this.currentSectionKey = p_157615_;
        this.currentSection = p_157616_;
    }

    public void onMove() {
        BlockPos blockpos = this.entity.blockPosition();
        long i = SectionPos.asLong(blockpos);
        if (i != this.currentSectionKey) {
            Visibility visibility = this.currentSection.getStatus();
            if (!this.currentSection.remove(this.entity)) {
                PersistentEntitySectionManager.LOGGER.warn("Entity {} wasn't found in section {} (moving to {})", new Object[] { this.entity, SectionPos.of(this.currentSectionKey), i });
            }
            try {
                base.removeSectionIfEmpty(this.currentSectionKey, this.currentSection);
            } catch (Throwable e) {
            }
            EntitySection<T> entitysection = base.sectionStorage.getOrCreateSection(i);
            entitysection.add(this.entity);
            this.currentSection = entitysection;
            this.currentSectionKey = i;
            this.updateStatus(visibility, entitysection.getStatus());
        }
    }

    public void updateStatus(Visibility p_157621_, Visibility p_157622_) {
        Visibility visibility = PersistentEntitySectionManager.getEffectiveStatus(this.entity, p_157621_);
        Visibility visibility1 = PersistentEntitySectionManager.getEffectiveStatus(this.entity, p_157622_);
        if (visibility == visibility1) {
            if (visibility1.isAccessible()) {
                base.callbacks.onSectionChange(this.entity);
            }
        } else {
            boolean flag = visibility.isAccessible();
            boolean flag1 = visibility1.isAccessible();
            if (flag && !flag1) {
                base.stopTracking(this.entity);
            } else if (!flag && flag1) {
                base.startTracking(this.entity);
            }
            boolean flag2 = visibility.isTicking();
            boolean flag3 = visibility1.isTicking();
            if (flag2 && !flag3) {
                base.stopTicking(this.entity);
            } else if (!flag2 && flag3) {
                base.startTicking(this.entity);
            }
            if (flag1) {
                base.callbacks.onSectionChange(this.entity);
            }
        }
    }

    public void onRemove(Entity.RemovalReason p_157619_) {
        // if (!this.currentSection.remove(this.entity)) {
        // PersistentEntitySectionManager.LOGGER.warn("Entity {} wasn't found in section {} (destroying due to {})", new Object[] { this.entity, SectionPos.of(this.currentSectionKey), p_157619_ });
        // }
        // Visibility visibility = PersistentEntitySectionManager.getEffectiveStatus(this.entity, this.currentSection.getStatus());
        // if (visibility.isTicking()) {
        // base.stopTicking(this.entity);
        // }
        // if (visibility.isAccessible()) {
        // base.stopTracking(this.entity);
        // }
        // if (p_157619_.shouldDestroy()) {
        // base.callbacks.onDestroyed(this.entity);
        // }
        // base.knownUuids.remove(this.entity.getUUID());
        // this.entity.setLevelCallback(NULL);
        // base.removeSectionIfEmpty(this.currentSectionKey, this.currentSection);
    }
}
