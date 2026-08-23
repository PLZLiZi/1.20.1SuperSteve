package plz.lizi.supersteve.entity.ai;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.phys.Vec3;

public class SSFlyingMoveControl extends MoveControl {
	private static final float LERP_SPEED = 0.25F;
	private static final double MAX_SPEED = 1.5;

	public SSFlyingMoveControl(Mob mob) {
		super(mob);
	}

	@Override
	public void tick() {
		if (operation == MoveControl.Operation.MOVE_TO) {
			operation = MoveControl.Operation.WAIT;
			Vec3 toTarget = new Vec3(wantedX - mob.getX(), wantedY - mob.getY(), wantedZ - mob.getZ());
			double dist = toTarget.length();
			if (dist < 1.0E-4) {
				mob.setDeltaMovement(mob.getDeltaMovement().scale(0.8));
				return;
			}
			double speed = speedModifier * mob.getAttributeValue(Attributes.MOVEMENT_SPEED);
			double clampedSpeed = Math.min(speed, MAX_SPEED);
			Vec3 desiredVel = toTarget.normalize().scale(Math.min(dist, clampedSpeed));
			mob.setDeltaMovement(mob.getDeltaMovement().lerp(desiredVel, LERP_SPEED));
			mob.setYRot(rotlerp(mob.getYRot(), (float) (Mth.atan2(toTarget.z, toTarget.x) * 57.2957763671875) - 90.0F, 90.0F));
			mob.setXRot(rotlerp(mob.getXRot(), (float) (-Mth.atan2(toTarget.y, Math.sqrt(toTarget.x * toTarget.x + toTarget.z * toTarget.z)) * 57.2957763671875), 10));
		} else {
			mob.setDeltaMovement(mob.getDeltaMovement().scale(0.85));
		}
	}
}