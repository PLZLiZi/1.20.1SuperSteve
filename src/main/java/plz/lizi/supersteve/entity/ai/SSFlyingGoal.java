package plz.lizi.supersteve.entity.ai;

import java.util.EnumSet;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;

public class SSFlyingGoal extends Goal {
    private final Mob mob;
    private final double speed;
    private final float minDistance;
    private Vec3 targetPos = Vec3.ZERO;
    private int hoverTimer = 0;
    private int maxHoverTime = 60;

    public SSFlyingGoal(Mob mob, double speed, float minDistance) {
        this.mob = mob;
        this.speed = speed;
        this.minDistance = minDistance;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        return true;
    }

    @Override
    public void start() {
        hoverTimer = 0;
        pickNewHoverPos();
    }

    @Override
    public void tick() {
        LivingEntity target = mob.getTarget();
        if (target != null && target.isAlive()) {
            double dist = mob.distanceToSqr(target);
            double targetY = target.getY() + target.getBbHeight() * 0.5;
            if (dist > minDistance * minDistance) {
                mob.getMoveControl().setWantedPosition(target.getX(), targetY, target.getZ(), speed);
            } else {
                Vec3 dir = mob.position().subtract(target.position()).normalize();
                mob.getMoveControl().setWantedPosition(
                    target.getX() + dir.x * minDistance,
                    targetY,
                    target.getZ() + dir.z * minDistance,
                    speed * 0.5);
            }
        } else {
            hoverTimer++;
            if (hoverTimer > maxHoverTime || mob.position().distanceToSqr(targetPos) < 4.0) {
                pickNewHoverPos();
                hoverTimer = 0;
            }
            mob.getMoveControl().setWantedPosition(
                targetPos.x, targetPos.y, targetPos.z, speed * 0.3);
        }
    }

    private void pickNewHoverPos() {
        targetPos = mob.position().add(
            (mob.getRandom().nextDouble() - 0.5) * 16,
            (mob.getRandom().nextDouble() - 0.5) * 8,
            (mob.getRandom().nextDouble() - 0.5) * 16
        );
        targetPos = new Vec3(targetPos.x, Math.max(targetPos.y, mob.level().getMinBuildHeight() + 4), targetPos.z);
        maxHoverTime = mob.getRandom().nextInt(40) + 40;
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }
}