package net.tazgirl.everythingexplodes;

import net.minecraft.core.BlockPos;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageSources;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.ExplosionDamageCalculator;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent;
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
    private static Entity currentSourceEntity = null;




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

    public static class CustomExplosionDamageCalculator extends ExplosionDamageCalculator
    {
        @Override
        public boolean shouldDamageEntity(Explosion explosion, Entity entity)
        {
            return entity != currentSourceEntity;
        }

        @Override
        public float getEntityDamageAmount(Explosion explosion, Entity entity)
        {
            float f = explosion.radius() * 2.0F;
            Vec3 vec3 = explosion.center();
            double d0 = Math.sqrt(entity.distanceToSqr(vec3)) / (double)f;
            double d1 = (1.0 - d0) * (double)Explosion.getSeenPercent(vec3, entity);
            return ((float)((d1 * d1 + d1) / 2.0 * 7.0 * (double)f + 1.0)) * 0.333f;
        }

        @Override
        public boolean shouldBlockExplode(Explosion explosion, BlockGetter reader, BlockPos pos, BlockState state, float power)
        {
            return !(state.getBlock() instanceof BedBlock);
        }

        @Override
        public float getKnockbackMultiplier(Entity entity)
        {
            if(entity.isCrouching())
            {
                return 0.2f;
            }
            return 1.0f;
        }

    }

    public static class ItemPickupExplosionDamageCalculator extends ExplosionDamageCalculator
    {
        @Override
        public float getEntityDamageAmount(Explosion explosion, Entity entity)
        {
            float f = explosion.radius() * 2.0F;
            Vec3 vec3 = explosion.center();
            double d0 = Math.sqrt(entity.distanceToSqr(vec3)) / (double)f;
            double d1 = (1.0 - d0) * (double)Explosion.getSeenPercent(vec3, entity);
            return ((float)((d1 * d1 + d1) / 2.0 * 7.0 * (double)f + 1.0)) * 0.1f;
        }

        @Override
        public boolean shouldBlockExplode(Explosion explosion, BlockGetter reader, BlockPos pos, BlockState state, float power)
        {
            return false;
        }

        @Override
        public float getKnockbackMultiplier(Entity entity)
        {
            if(entity.isCrouching())
            {
                return 0.1f;
            }
            return 0.4f;
        }

    }




    @SubscribeEvent
    public static void OnBlockBreak(BlockEvent.BreakEvent event)
    {
        if(event.getLevel() instanceof Level level)
        {
            CauseExplosionAfterTick(level, event.getPos().getX() + 0.5f, event.getPos().getY() + 0.5f, event.getPos().getZ() + 0.5f, standardRadius * event.getState().getDestroySpeed(level, event.getPos()));
        }
    }

    @SubscribeEvent
    public static void OnItemPickup(ItemEntityPickupEvent.Post event)
    {
        CauseExplosionAfterTickSmallerMin(event.getPlayer().level(), event.getPlayer().getX(), event.getPlayer().getY(), event.getPlayer().getZ(), 0.1f * event.getOriginalStack().getCount());
    }

    @SubscribeEvent
    public static void OnEntityDamaged(LivingDamageEvent.Post event)
    {
        if(!event.getSource().is(DamageTypes.EXPLOSION))
        {
            currentSourceEntity = event.getEntity();
            CauseExplosionDontDamageSource(event.getEntity().level(), event.getEntity().getBoundingBox().getCenter().x, event.getEntity().getY(), event.getEntity().getBoundingBox().getCenter().z, standardRadius * event.getNewDamage());
        }
    }

    @SubscribeEvent
    public static void OnBlockPlaced(BlockEvent.EntityPlaceEvent event)
    {
        if(event.getLevel() instanceof Level level)
        {
            CauseExplosionNoBlockBreak(level, event.getPos().getX() + 0.5f, event.getPos().getY() + 0.5f, event.getPos().getZ() + 0.5f, standardRadius * event.getState().getDestroySpeed(level, event.getPos()));
        }
    }


    private static void CauseExplosionAfterTick(Level level, double x, double y, double z, float radius)
    {
        if (!level.isClientSide)
        {
            level.getServer().execute(() -> {level.explode(null, null, new CustomExplosionDamageCalculator(), x, y, z, Math.max(0.4f, Math.min(radius, 1000000000000f)), false, Level.ExplosionInteraction.MOB);});
            System.out.println(radius);
        }
    }

    private static void CauseExplosionAfterTickSmallerMin(Level level, double x, double y, double z, float radius)
    {
        if (!level.isClientSide)
        {
            level.getServer().execute(() -> {level.explode(null, null, new ItemPickupExplosionDamageCalculator(), x, y, z, radius, false, Level.ExplosionInteraction.MOB);});
            System.out.println(radius);
        }
    }

    private static void CauseExplosionDontDamageSource(Level level, double x, double y, double z, float radius)
    {
        if (!level.isClientSide)
        {
            level.explode(null, null, new CustomExplosionDamageCalculator(), x, y, z, Math.max(0.4f, Math.min(radius, 1000000000000f)), false, Level.ExplosionInteraction.MOB);
            currentSourceEntity = null;
            System.out.println(radius);
        }
    }

    private static void CauseExplosionNoBlockBreak(Level level, double x, double y, double z, float radius)
    {
        if (!level.isClientSide)
        {
            level.explode(null, null, new CustomExplosionDamageCalculator(), x, y, z, Math.max(0.4f, Math.min(radius, 1000000000000f)), false, Level.ExplosionInteraction.TRIGGER);
            System.out.println(radius);
        }
    }
}
