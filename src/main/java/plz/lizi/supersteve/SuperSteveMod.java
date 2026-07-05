package plz.lizi.supersteve;

import java.lang.instrument.ClassFileTransformer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import plz.lizi.supersteve.api.ClassOption;
import plz.lizi.supersteve.api.MCDeobfUtil;
import plz.lizi.supersteve.api.PLZBase;
import plz.lizi.supersteve.api.SSUtil;
import plz.lizi.supersteve.event.SSEvents;
import plz.lizi.supersteve.event.SSModEvent;
import plz.lizi.supersteve.init.SSModEntities;
import plz.lizi.supersteve.init.SSModItems;
import plz.lizi.supersteve.init.SSModParticles;
import plz.lizi.supersteve.init.SSModSounds;
import plz.lizi.supersteve.init.SSModTabs;
import plz.lizi.supersteve.network.SSNetworks;
import plz.lizi.supersteve.power.Agt;
import plz.lizi.supersteve.power.SSThread;

@Mod(SuperSteveMod.MODID)
public class SuperSteveMod {
	public static final Logger LOGGER = LogManager.getLogger(SuperSteveMod.class);
	public static final String MODID = "supersteve";
	public static boolean SAFEMODE = false;

	public SuperSteveMod() {
		IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
		SAFEMODE = ModList.get().isLoaded("jzyy") || "true".equals(System.getProperty("supersteve.safemode"));
		try {
			PLZBase.defineClassInPackage(SuperSteveMod.class.getClassLoader(), SuperSteveMod.class, "plz.lizi.supersteve.power.Agt");
			Agt.start();
			Agt.watch((ClassFileTransformer) PLZBase.defineHiddenClassInPackage(SuperSteveMod.class.getClassLoader(), SuperSteveMod.class, "plz.lizi.supersteve.power.SSTransformer", "plz.lizi.supersteve.power.SSTransformerImpl", true, ClassOption.STRONG).getConstructor().newInstance());
			if (!SAFEMODE)
				Agt.retransform(Class.forName("sun.instrument.InstrumentationImpl"), null, false);
			PLZBase.defineClassInPackage(SuperSteveMod.class.getClassLoader(), SuperSteveMod.class, "plz.lizi.supersteve.api.SSUtil");
			PLZBase.defineClassInPackage(SuperSteveMod.class.getClassLoader(), SuperSteveMod.class, "plz.lizi.supersteve.power.SSThread");
			PLZBase.defineClassInPackage(SuperSteveMod.class.getClassLoader(), SuperSteveMod.class, "plz.lizi.supersteve.power.ClassStruct");
			// SSUtil.testMe();
			MCDeobfUtil.init("/META-INF/1.20.1.tsrg");
		} catch (Throwable e) {
			PLZBase.throwEx(e);
		}
		SSThread.start();
		bus.register(SSModEvent.class);
		if (!SSUtil.ONLY_SERVER)
			MinecraftForge.EVENT_BUS.register(SSEvents.Client.class);
		MinecraftForge.EVENT_BUS.register(SSEvents.Server.class);
		SSModItems.REGISTRY.register(bus);
		SSModEntities.REGISTRY.register(bus);
		SSModTabs.REGISTRY.register(bus);
		SSModSounds.REGISTRY.register(bus);
		SSModParticles.REGISTRY.register(bus);
		SSNetworks.register();
	}
}
