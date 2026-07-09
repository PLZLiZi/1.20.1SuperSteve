package plz.lizi.supersteve.entity.ai;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import plz.lizi.supersteve.api.SSUtil;

// TODO: run when game is paused
public class GroundNavigationEx extends GroundPathNavigation {
    public GroundNavigationEx(Mob pMob, Level pLevel) {
        super(pMob, pLevel);
    }
    
    @Override
    protected boolean canUpdatePath() {
        return true;
        //boolean originalCheck = super.canUpdatePath();
        //if (originalCheck)
        //    return true;
        //return isGamePaused();
    }

    @Override
    public void tick() {
        if (!SSUtil.isGamePaused()) {
            super.tick();
            return;
        }
        this.tick++;
        if (this.hasDelayedRecomputation)
            this.recomputePath();
        if (this.isDone())
            return;
        if (this.canUpdatePath())
            this.followThePathWithoutStuckDetection();
        if (!this.isDone() && this.path != null) {
            Vec3 nextWaypoint = this.path.getNextEntityPos(this.mob);
            this.mob.getMoveControl().setWantedPosition(nextWaypoint.x,this.getGroundY(nextWaypoint),nextWaypoint.z,this.speedModifier);
        }
    }

    private void followThePathWithoutStuckDetection() {
        if (this.path == null)
            return;
        Vec3 currentPos = this.getTempMobPos();
        this.maxDistanceToWaypoint = this.mob.getBbWidth() > 0.75F ? this.mob.getBbWidth() / 2.0F : 0.75F - this.mob.getBbWidth() / 2.0F;
        net.minecraft.core.BlockPos nextNodePos = this.path.getNextNodePos();
        double dx = Math.abs(this.mob.getX() - ((double) nextNodePos.getX() + (double) (this.mob.getBbWidth() + 1.0F) / 2.0));
        double dy = Math.abs(this.mob.getY() - (double) nextNodePos.getY());
        double dz = Math.abs(this.mob.getZ() - ((double) nextNodePos.getZ() + (double) (this.mob.getBbWidth() + 1.0F) / 2.0));
        boolean reachedXz = dx <= (double) this.maxDistanceToWaypoint && dz <= (double) this.maxDistanceToWaypoint;
        boolean reachedY = dy < 1.0D;
        if ((reachedXz && reachedY) || (this.canCutCorner(this.path.getNextNode().type) && this.shouldTargetNextNodeInDirection(currentPos)))
            this.path.advance();
    }

    private boolean shouldTargetNextNodeInDirection(Vec3 pVec) {
        if (this.path == null || this.path.getNextNodeIndex() + 1 >= this.path.getNodeCount())
            return false;
        Vec3 vec3 = Vec3.atBottomCenterOf(this.path.getNextNodePos());
        if (!pVec.closerThan(vec3, 2.0D))
            return false;
        if (this.canMoveDirectly(pVec, this.path.getNextEntityPos(this.mob)))
            return true;
        Vec3 vec31 = Vec3.atBottomCenterOf(this.path.getNodePos(this.path.getNextNodeIndex() + 1));
        Vec3 vec32 = vec3.subtract(pVec);
        Vec3 vec33 = vec31.subtract(pVec);
        return vec33.lengthSqr() < vec32.lengthSqr() || vec32.lengthSqr() < 0.5D;
    }
}
