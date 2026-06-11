package plz.lizi.supersteve.client.particle;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import plz.lizi.supersteve.api.PLZBase;
import plz.lizi.supersteve.client.renderer.SSRenders;
import java.util.Arrays;

public class AttackParticle extends Particle {
    private Vec2 size;
    private Vec3 rot;

    protected AttackParticle(ClientLevel level, double x, double y, double z, double data1, double data2, double data3) {
        super(level, x, y, z);
        System.out.println(data1 + "  " + data2 + " " + data3);
        var colorData = PLZBase.dbl2rgba(data1);
        var rotData = PLZBase.dbl2shrt(data2);
        var sizeAndLife = PLZBase.dbl2shrt(data3);
        System.out.println(Arrays.toString(colorData) + Arrays.toString(rotData) + Arrays.toString(sizeAndLife));
        this.rCol = (float) colorData[0] / 255f;
        this.gCol = (float) colorData[1] / 255f;
        this.bCol = (float) colorData[2] / 255f;
        this.alpha = (float) colorData[3] / 255f;
        this.lifetime = sizeAndLife[2];
        size = new Vec2(sizeAndLife[0] / 10F, sizeAndLife[1] / 10F);
        rot = new Vec3(rotData[0] / 10F, rotData[1] / 10F, rotData[2] / 10F);
        this.hasPhysics = false;
        this.gravity = 0;
        this.xd = 0;
        this.yd = 0;
        this.zd = 0;
    }

    @Override
    public ParticleRenderType getRenderType() {
        return SSRenders.ATT_PAR_TYPE;
    }

    @Override
    public void render(VertexConsumer vc, Camera camera, float partialTick) {
        // 计算当前的平滑动画进度 progress (0.0F - 1.0F)
        float currentAge = (float) this.age + partialTick;
        float progress = Mth.clamp(currentAge / (float) this.lifetime, 0.0F, 1.0F);
        // 获取粒子在世界中的插值坐标
        double renderX = Mth.lerp(partialTick, this.xo, this.x);
        double renderY = Mth.lerp(partialTick, this.yo, this.y);
        double renderZ = Mth.lerp(partialTick, this.zo, this.z);
        // 获取相机的坐标，计算出渲染相对偏移量
        double cameraX = camera.getPosition().x();
        double cameraY = camera.getPosition().y();
        double cameraZ = camera.getPosition().z();
        PoseStack poseStack = new PoseStack();
        poseStack.pushPose();
        // 将 PoseStack 移动到世界中粒子的真实位置
        poseStack.translate(renderX - cameraX, renderY - cameraY, renderZ - cameraZ);
        poseStack.mulPose(Axis.XP.rotationDegrees((float) rot.x));
        poseStack.mulPose(Axis.YP.rotationDegrees((float) rot.y));
        poseStack.mulPose(Axis.ZP.rotationDegrees((float) rot.z));
        poseStack.scale(size.x, size.y, size.x);
        SSRenders.renderAttack(vc, poseStack, progress, rCol, gCol, bCol, alpha);
        poseStack.popPose();
    }

    // 粒子工厂类，用于实例化粒子。这里将速度参数（speedX, speedY, speedZ）借用作 RGB 颜色输入
    public static class Provider implements ParticleProvider<SimpleParticleType> {
        public Provider(SpriteSet spriteSet) {
            // 自定义非纹理粒子不需要 spriteSet，但工厂构造函数保留
        }

        public Provider() {
        }

        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed) {
            return new AttackParticle(level, x, y, z, xSpeed, ySpeed, zSpeed);
        }
    }
}
