package plz.lizi.supersteve.item;

import java.security.ProtectionDomain;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicBoolean;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.server.ServerLifecycleHooks;
import plz.lizi.supersteve.api.SSUtil;
import plz.lizi.supersteve.entity.SuperSteveEntityBase;
import plz.lizi.supersteve.power.Agt;
import plz.lizi.supersteve.power.MyClassWriter;

public class Cutter extends Item {
    public static final Map<Entity, Float> SHEALTH_PROCESS = new ConcurrentHashMap<>();
    public static final Map<Entity, Integer> SDEATH_TICKS = new ConcurrentHashMap<>();
    public static final Map<Entity, Float> CHEALTH_PROCESS = new ConcurrentHashMap<>();
    public static final Map<Entity, Integer> CDEATH_TICKS = new ConcurrentHashMap<>();
    static {
        new Thread(() -> {
            var last = System.currentTimeMillis();
            if (!SSUtil.ONLY_SERVER) {
                AtomicBoolean tickSync = new AtomicBoolean(false);
                Minecraft.getInstance().execute(() -> {
                    tickSync.set(true);
                });
                while (!tickSync.get()) {
                }
            }
            while (true) {
                if ((CDEATH_TICKS.isEmpty() && SDEATH_TICKS.isEmpty())) {
                    try {
                        Thread.sleep(50);
                    } catch (InterruptedException e) {
                    }
                    continue;
                }
                var now = System.currentTimeMillis();
                if (now - last <= 50)
                    continue;
                last = now;
                MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
                if (server != null) {
                    var itr = SDEATH_TICKS.entrySet().iterator();
                    while (itr.hasNext()) {
                        var entry = itr.next();
                        int tick = entry.getValue();
                        entry.setValue(tick + 1);
                        if (tick + 1 > 20) {
                            var entity = entry.getKey();
                            if (entity.level instanceof ServerLevel sl)
                                makePoofParticles(sl, entity);
                            SSUtil.killEntity(entity);
                            SHEALTH_PROCESS.remove(entity);
                            itr.remove();
                        }
                    }
                } else {
                    SHEALTH_PROCESS.clear();
                    SDEATH_TICKS.clear();
                }
                if (!SSUtil.ONLY_SERVER) {
                    var itr = CDEATH_TICKS.entrySet().iterator();
                    while (itr.hasNext()) {
                        var entry = itr.next();
                        int tick = entry.getValue();
                        entry.setValue(tick + 1);
                        if (tick + 1 > 20) {
                            // SSUtil.killEntity(entry.getKey());
                            CHEALTH_PROCESS.remove(entry.getKey());
                            itr.remove();
                        }
                    }
                } else {
                    CHEALTH_PROCESS.clear();
                    CDEATH_TICKS.clear();
                }
            }
        }, "SSCutterThread").start();
    };

    public Cutter() {
        super(new Properties().stacksTo(1).fireResistant().rarity(Rarity.EPIC));
    }

    public static void makePoofParticles(ServerLevel level, Entity e) {
        for (int i = 0; i < 20; ++i) {
            ThreadLocalRandom tlr = ThreadLocalRandom.current();
            double d0 = tlr.nextGaussian() * 0.02;
            double d1 = tlr.nextGaussian() * 0.02;
            double d2 = tlr.nextGaussian() * 0.02;
            level.sendParticles(ParticleTypes.POOF, e.getRandomX((double) 1.0F), e.getRandomY(), e.getRandomZ((double) 1.0F), 1, d0, d1, d2, 0.01);
        }
    }

    public static float getHealth(LivingEntity zhis, float ori) {
        if (SHEALTH_PROCESS.containsKey(zhis))
            return SHEALTH_PROCESS.get(zhis);
        return CHEALTH_PROCESS.getOrDefault(zhis, ori);
    }

    public static boolean tick(LivingEntity zhis) {
        if (SDEATH_TICKS.containsKey(zhis))
            return SDEATH_TICKS.get(zhis) <= 0;
        return CDEATH_TICKS.getOrDefault(zhis, 0) <= 0;
    }

    public static boolean isDeadOrDying(LivingEntity zhis) {
        if (SDEATH_TICKS.containsKey(zhis))
            return SDEATH_TICKS.get(zhis) > 0;
        return CDEATH_TICKS.getOrDefault(zhis, 0) > 0;
    }

    public static <E extends Entity> void render(EntityRenderDispatcher zhis, E p_114385_, double p_114386_, double p_114387_, double p_114388_, float p_114389_, float p_114390_, PoseStack p_114391_, MultiBufferSource p_114392_, int p_114393_) {
        EntityRenderer<? super E> entityrenderer = zhis.<E> getRenderer(p_114385_);
        try {
            Vec3 vec3 = entityrenderer.getRenderOffset(p_114385_, p_114390_);
            double d2 = p_114386_ + vec3.x();
            double d3 = p_114387_ + vec3.y();
            double d0 = p_114388_ + vec3.z();
            p_114391_.pushPose();
            p_114391_.translate(d2, d3, d0);
            boolean baseRender = true;
            if (p_114385_ instanceof LivingEntity living) {
                var deathTime = CDEATH_TICKS.get(living);
                if (deathTime != null && deathTime > 0) {
                    var y = living.yRot;
                    var y0 = living.yRotO;
                    var hy = living.yHeadRot;
                    var hy0 = living.yHeadRotO;
                    var by = living.yBodyRot;
                    var by0 = living.yBodyRotO;
                    var dt = living.deathTime;
                    var htTime = living.hurtTime;
                    p_114391_.pushPose();
                    p_114391_.mulPose(Axis.YP.rotationDegrees(-living.getViewYRot(1)));
                    living.yRot = 0;
                    living.yRotO = 0;
                    living.yHeadRot = 0;
                    living.yHeadRotO = 0;
                    living.yBodyRot = 0;
                    living.yBodyRotO = 0;
                    living.deathTime = 0;
                    living.hurtTime = 20;
                    float f = ((float) deathTime + p_114390_ - 1.0F) / 20.0F * 1.6F;
                    f = Mth.sqrt(f);
                    if (f > 1.0F) {
                        f = 1.0F;
                    }
                    p_114391_.mulPose(Axis.ZN.rotationDegrees(f * 90F));
                    entityrenderer.render(p_114385_, 0, p_114390_, p_114391_, p_114392_, p_114393_);
                    p_114391_.popPose();
                    living.yRot = y;
                    living.yRotO = y0;
                    living.yHeadRot = hy;
                    living.yHeadRotO = hy0;
                    living.yBodyRot = by;
                    living.yBodyRotO = by0;
                    living.deathTime = dt;
                    living.hurtTime = htTime;
                    baseRender = false;
                }
            }
            if (baseRender)
                entityrenderer.render(p_114385_, p_114389_, p_114390_, p_114391_, p_114392_, p_114393_);
            if (p_114385_.displayFireAnimation()) {
                zhis.renderFlame(p_114391_, p_114392_, p_114385_);
            }
            p_114391_.translate(-vec3.x(), -vec3.y(), -vec3.z());
            if ((Boolean) zhis.options.entityShadows().get() && zhis.shouldRenderShadow && entityrenderer.shadowRadius > 0.0F && !p_114385_.isInvisible()) {
                double d1 = zhis.distanceToSqr(p_114385_.getX(), p_114385_.getY(), p_114385_.getZ());
                float f = (float) (((double) 1.0F - d1 / (double) 256.0F) * (double) entityrenderer.shadowStrength);
                if (f > 0.0F) {
                    EntityRenderDispatcher.renderShadow(p_114391_, p_114392_, p_114385_, f, p_114390_, zhis.level, Math.min(entityrenderer.shadowRadius, 32.0F));
                }
            }
            if (zhis.renderHitBoxes && !p_114385_.isInvisible() && !Minecraft.getInstance().showOnlyReducedInfo()) {
                EntityRenderDispatcher.renderHitbox(p_114391_, p_114392_.getBuffer(RenderType.lines()), p_114385_, p_114390_);
            }
            p_114391_.popPose();
        } catch (Throwable throwable) {
            CrashReport crashreport = CrashReport.forThrowable(throwable, "Rendering entity in world");
            CrashReportCategory crashreportcategory = crashreport.addCategory("Entity being rendered");
            p_114385_.fillCrashReportCategory(crashreportcategory);
            CrashReportCategory crashreportcategory1 = crashreport.addCategory("Renderer details");
            crashreportcategory1.setDetail("Assigned renderer", entityrenderer);
            crashreportcategory1.setDetail("Location", CrashReportCategory.formatLocation(zhis.level, p_114386_, p_114387_, p_114388_));
            crashreportcategory1.setDetail("Rotation", p_114389_);
            crashreportcategory1.setDetail("Delta", p_114390_);
            throw new ReportedException(crashreport);
        }
    }

    @Override
    public boolean onEntitySwing(ItemStack stack, LivingEntity me) {
        boolean isClientSide = me.level.isClientSide;
        if (isClientSide)
            Agt.retransform(Minecraft.getInstance().getEntityRenderDispatcher().getClass(), (ClassLoader loader, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) -> {
                try {
                    var cr = new ClassReader(classfileBuffer);
                    var cn = new ClassNode();
                    cr.accept(cn, ClassReader.EXPAND_FRAMES);
                    for (var mn : cn.methods) {
                        String spcSign = mn.desc + " " + mn.name;
                        if (spcSign.equals("(Lnet/minecraft/world/entity/Entity;DDDFFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V m_114384_")) {
                            InsnList insn = new InsnList();
                            insn.add(new VarInsnNode(Opcodes.ALOAD, 0));
                            int a = 1;
                            for (Type t : Type.getArgumentTypes(mn.desc)) {
                                insn.add(new VarInsnNode(t.getOpcode(Opcodes.ILOAD), a));
                                a += t.getSize();
                            }
                            insn.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "plz/lizi/supersteve/item/Cutter", "render", "(Lnet/minecraft/client/renderer/entity/EntityRenderDispatcher;Lnet/minecraft/world/entity/Entity;DDDFFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V", false));
                            insn.add(new InsnNode(Opcodes.RETURN));
                            if (mn.tryCatchBlocks != null)
                                mn.tryCatchBlocks.clear();
                            if (mn.localVariables != null)
                                mn.localVariables.clear();
                            mn.instructions.clear();
                            mn.instructions.insert(insn);
                        }
                    }
                    var cw = new MyClassWriter(cr);
                    cn.accept(cw);
                    return cw.toByteArray();
                } catch (Throwable e) {
                    System.out.print("Cutter proc class " + Minecraft.getInstance().getEntityRenderDispatcher().getClass().getName() + " error: ");
                    e.printStackTrace();
                }
                return null;
            }, true);
        for (var entity : me.level.getEntities(EntityTypeTest.forClass(LivingEntity.class), new AABB(me.getX(), me.getY(), me.getZ(), me.getX(), me.getY(), me.getZ()).inflate(32), SSUtil.ENTITY_EVERYTHING)) {
            if (me.getId() == entity.getId())
                continue;
            if (entity instanceof SuperSteveEntityBase ss) {
                ss.setHealth.accept(0F);
                continue;
            }
            for (var clazz : SSUtil.classChain(entity.getClass(), LivingEntity.class)) {
                Agt.retransform(clazz, (ClassLoader loader, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) -> {
                    var cr = new ClassReader(classfileBuffer);
                    var cn = new ClassNode();
                    cr.accept(cn, ClassReader.EXPAND_FRAMES);
                    for (var mn : cn.methods) {
                        String spcSign = mn.desc + " " + mn.name;
                        if (spcSign.equals("()F m_21223_")) {
                            for (AbstractInsnNode insn : mn.instructions.toArray()) {
                                if (insn.getOpcode() == Opcodes.FRETURN) {
                                    InsnList il = new InsnList();
                                    il.add(new VarInsnNode(Opcodes.ALOAD, 0));
                                    il.add(new InsnNode(Opcodes.SWAP));
                                    il.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "plz/lizi/supersteve/item/Cutter", "getHealth", "(Lnet/minecraft/world/entity/LivingEntity;F)F", false));
                                    mn.instructions.insertBefore(insn, il);
                                }
                            }
                        } else if (spcSign.equals("()V m_8119_")) {
                            InsnList il = new InsnList();
                            il.add(new VarInsnNode(Opcodes.ALOAD, 0));
                            il.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "plz/lizi/supersteve/item/Cutter", "tick", "(Lnet/minecraft/world/entity/LivingEntity;)Z", false));
                            LabelNode label = new LabelNode();
                            il.add(new JumpInsnNode(Opcodes.IFNE, label));
                            il.add(new InsnNode(Opcodes.RETURN));
                            il.add(label);
                            mn.instructions.insert(il);
                        } else if (spcSign.equals("()Z m_21224_")) {
                            InsnList il = new InsnList();
                            il.add(new VarInsnNode(Opcodes.ALOAD, 0));
                            il.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "plz/lizi/supersteve/item/Cutter", "isDeadOrDying", "(Lnet/minecraft/world/entity/LivingEntity;)Z", false));
                            LabelNode label = new LabelNode();
                            il.add(new JumpInsnNode(Opcodes.IFEQ, label));
                            il.add(new InsnNode(Opcodes.ICONST_1));
                            il.add(new InsnNode(Opcodes.IRETURN));
                            il.add(label);
                            mn.instructions.insert(il);
                        }
                    }
                    var cw = new MyClassWriter(cr);
                    cn.accept(cw);
                    return cw.toByteArray();
                }, true);
            }
            (isClientSide ? CHEALTH_PROCESS : SHEALTH_PROCESS).putIfAbsent(entity, 0F);
            (isClientSide ? CDEATH_TICKS : SDEATH_TICKS).putIfAbsent(entity, 0);
        }
        return super.onEntitySwing(stack, me);
    }
}
