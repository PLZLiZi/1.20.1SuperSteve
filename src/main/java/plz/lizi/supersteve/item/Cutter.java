package plz.lizi.supersteve.item;

import java.security.ProtectionDomain;
import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;
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
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.server.ServerLifecycleHooks;
import plz.lizi.supersteve.api.SSUtil;
import plz.lizi.supersteve.client.renderer.CutterItemEx;
import plz.lizi.supersteve.entity.SuperSteveEntityBase;
import plz.lizi.supersteve.network.SSNetworks;
import plz.lizi.supersteve.power.Agt;
import plz.lizi.supersteve.power.VerifyCW;

public class Cutter extends Item {
    public static boolean ERD_TDF = false;
    public static final Map<Entity, Float> SHEALTH_PROCESS = Collections.synchronizedMap(new WeakHashMap<>());
    public static final Map<Entity, Integer> SDEATH_TICKS = Collections.synchronizedMap(new WeakHashMap<>());
    public static final Map<Entity, Float> CHEALTH_PROCESS = Collections.synchronizedMap(new WeakHashMap<>());
    public static final Map<Entity, Integer> CDEATH_TICKS = Collections.synchronizedMap(new WeakHashMap<>());
    private static float PARTIAL_TICK = 0;
    static {
        new Thread(() -> {
            var last = System.currentTimeMillis();
            while (true) {
                if ((CDEATH_TICKS.isEmpty() && SDEATH_TICKS.isEmpty())) {
                    try {
                        Thread.sleep(50);
                    } catch (InterruptedException e) {
                    }
                    Thread.yield();
                    continue;
                }
                var now = System.currentTimeMillis();
                PARTIAL_TICK = (now - last) / 50F;
                if (now - last <= 50)
                    continue;
                last = now;
                MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
                if (server != null) {
                    synchronized (SDEATH_TICKS) {
                        var itr = SDEATH_TICKS.entrySet().iterator();
                        while (itr.hasNext()) {
                            var entry = itr.next();
                            int tick = entry.getValue();
                            entry.setValue(tick + 1);
                            if (tick + 1 == 21) {
                                var entity = entry.getKey();
                                if (entity.level instanceof ServerLevel sl)
                                    makePoofParticles(sl, entity);
                                SSUtil.killEntity(entity);
                                if (!(entity instanceof Player))
                                    SHEALTH_PROCESS.remove(entity);
                                itr.remove();
                            }
                        }
                    }
                } else {
                    SHEALTH_PROCESS.clear();
                    SDEATH_TICKS.clear();
                }
                if (!SSUtil.ONLY_SERVER) {
                    synchronized (CDEATH_TICKS) {
                        var itr = CDEATH_TICKS.entrySet().iterator();
                        while (itr.hasNext()) {
                            var entry = itr.next();
                            int tick = entry.getValue();
                            entry.setValue(tick + 1);
                            if (tick + 1 == 30) {
                                var entity = entry.getKey();
                                SSUtil.killEntity(entity);
                                if (!(entity instanceof Player))
                                    CHEALTH_PROCESS.remove(entity);
                                itr.remove();
                            }
                        }
                    }
                } else {
                    CHEALTH_PROCESS.clear();
                    CDEATH_TICKS.clear();
                }
            }
        }, "SSCutter").start();
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
        return Math.min(ori, SHEALTH_PROCESS.getOrDefault(zhis, CHEALTH_PROCESS.getOrDefault(zhis, ori)));
    }

    public static boolean tick(LivingEntity zhis) {
        return SDEATH_TICKS.getOrDefault(zhis, CDEATH_TICKS.getOrDefault(zhis, 0)) <= 0;
    }

    public static boolean isDeadOrDying(LivingEntity zhis) {
        return SDEATH_TICKS.getOrDefault(zhis, CDEATH_TICKS.getOrDefault(zhis, 0)) > 0;
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
                    deathTime = deathTime > 20 ? 20 : deathTime;
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
                    float f = ((float) deathTime + PARTIAL_TICK - 1.0F) / 20.0F * 1.6F;
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

    public static void cutHealth(LivingEntity me, LivingEntity entity, float health) {
        if (!SSUtil.ONLY_SERVER && !ERD_TDF) {
            ERD_TDF = true;
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
                    var cw = new VerifyCW(cr);
                    cn.accept(cw);
                    return cw.toByteArray();
                } catch (Throwable e) {
                    System.out.print("Cutter proc class " + Minecraft.getInstance().getEntityRenderDispatcher().getClass().getName() + " error: ");
                    e.printStackTrace();
                }
                return null;
            }, true);
        }
        if (me != null && me.getId() == entity.getId())
            return;
        if (entity instanceof SuperSteveEntityBase ss) {
            ss.health.operate(ss.health, 0F);
            return;
        }
        boolean isClientSide = entity.level.isClientSide;
        if (!isClientSide)
            SSNetworks.PACKET_HANDLER.send(PacketDistributor.ALL.noArg(), new SSNetworks.CutterSH(entity.getId(), health));
        for (var clazz : SSUtil.classChainReverse(entity.getClass(), LivingEntity.class)) {
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
                var cw = new VerifyCW(cr);
                cn.accept(cw);
                return cw.toByteArray();
            }, true);
        }
        if (me != null)
            SSUtil.forceHurt(entity, me.level.damageSources.mobAttack(me), 0);
        health = Math.max(0, health);
        (isClientSide ? CHEALTH_PROCESS : SHEALTH_PROCESS).put(entity, health);
        if (health <= 0)
            (isClientSide ? CDEATH_TICKS : SDEATH_TICKS).putIfAbsent(entity, 0);
    }

    @Override
    public boolean onEntitySwing(ItemStack stack, LivingEntity me) {
        for (var entity : me.level.getEntities(EntityTypeTest.forClass(LivingEntity.class), new AABB(me.getX(), me.getY(), me.getZ(), me.getX(), me.getY(), me.getZ()).inflate(32), SSUtil.ENTITY_EVERYTHING)) {
            cutHealth(me, entity, 0);
        }
        return super.onEntitySwing(stack, me);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level pLevel, Player pPlayer, InteractionHand pUsedHand) {
        pPlayer.startUsingItem(pUsedHand);
        return super.use(pLevel, pPlayer, pUsedHand);
    }

    @Override
    public UseAnim getUseAnimation(ItemStack pStack) {
        return UseAnim.NONE;
    }

    @Override
    public int getUseDuration(ItemStack pStack) {
        return 10;
    }

    @Override
    public boolean onLeftClickEntity(ItemStack stack, Player player, Entity entity) {
        if (entity instanceof LivingEntity l)
            cutHealth(player, l, 0);
        return false;
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(CutterItemEx.INSTANCE);
    }

    @Override
    public void inventoryTick(ItemStack pStack, Level pLevel, Entity pEntity, int pSlotId, boolean pIsSelected) {
        
    }
}
