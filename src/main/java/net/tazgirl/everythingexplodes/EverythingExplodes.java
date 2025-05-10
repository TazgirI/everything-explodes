package net.tazgirl.everythingexplodes;

import net.minecraft.world.level.Level;
import net.neoforged.neoforge.event.level.BlockEvent;
import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

// The value here should match an entry in the META-INF/neoforge.mods.toml file
@EventBusSubscriber
@Mod(EverythingExplodes.MODID)
public class EverythingExplodes
{
    //TODO: Implement Mixin to prevent items breaking, for the recording use EBNB and run "/gamerule ENID:all true"


    // Define mod id in a common place for everything to reference
    public static final String MODID = "everythingexplodes";
    // Directly reference a slf4j logger
    private static final Logger LOGGER = LogUtils.getLogger();

    static float standardRadius = 1.0f;




    // The constructor for the mod class is the first code that is run when your mod is loaded.
    // FML will recognize some parameter types like IEventBus or ModContainer and pass them in automatically.
    public EverythingExplodes(IEventBus modEventBus, ModContainer modContainer)
    {
        // Register the commonSetup method for modloading
        modEventBus.addListener(this::commonSetup);


        // Register ourselves for server and other game events we are interested in.
        // Note that this is necessary if and only if we want *this* class (ExampleMod) to respond directly to events.
        // Do not add this line if there are no @SubscribeEvent-annotated functions in this class, like onServerStarting() below.
        NeoForge.EVENT_BUS.register(EverythingExplodes.class);


    }

    private void commonSetup(final FMLCommonSetupEvent event)
    {

    }


    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event)
    {
        if(event.getLevel() instanceof Level level)
        {
            CauseExplosion(level, event.getPos().getX(), event.getPos().getY(), event.getPos().getZ(), standardRadius * event.getState().getDestroySpeed(level, event.getPos()));
        }
    }


    private static void CauseExplosion(Level level, double x, double y, double z, float radius)
    {
        if (!level.isClientSide)
        {
            level.getServer().execute(() -> {level.explode(null, x + 0.5, y + 0.5, z + 0.5, Math.max(1, Math.min(radius, 1000000)), Level.ExplosionInteraction.MOB);});

        }
    }
}
