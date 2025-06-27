/*
 * This file is part of Baritone.
 *
 * Baritone is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Baritone is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Baritone.  If not, see <https://www.gnu.org/licenses/>.
 */

package baritone.launch.mixins;

import baritone.api.BaritoneAPI;
import baritone.api.IBaritone;
import baritone.api.event.events.PlayerUpdateEvent;
import baritone.api.event.events.SprintStateEvent;
import baritone.api.event.events.type.EventState;
import baritone.api.utils.input.Input;
import baritone.behavior.LookBehavior;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Abilities;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

/**
 * @author Brady
 * @since 8/1/2018
 */
@Mixin(value = LocalPlayer.class, priority = 2000)
public class MixinClientPlayerEntityNoisium {

    @Unique
    private static final MethodHandle MAY_FLY = baritone$resolveMayFly();

    @Unique
    private static MethodHandle baritone$resolveMayFly() {
        try {
            var lookup = MethodHandles.publicLookup();
            return lookup.findVirtual(LocalPlayer.class, "mayFly", MethodType.methodType(boolean.class));
        } catch (NoSuchMethodException e) {
            return null;
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    @Inject(
            method = "tick",
            at = @At(
                    value = "INVOKE",
                    target = "net/minecraft/client/player/AbstractClientPlayer.tick()V",
                    shift = At.Shift.AFTER
            )
    )
    private void onPreUpdate(CallbackInfo ci) {
        IBaritone baritone = BaritoneAPI.getProvider().getBaritoneForPlayer((LocalPlayer) (Object) this);
        if (baritone != null) {
            baritone.getGameEventHandler().onPlayerUpdate(new PlayerUpdateEvent(EventState.PRE));
        }
    }

    // FINAL SOLUTION: Force movement by bypassing all input systems entirely
    @Inject(
            method = "tick", 
            at = @At("TAIL")
    )
    private void forceMovementControl(CallbackInfo ci) {
        IBaritone baritone = BaritoneAPI.getProvider().getBaritoneForPlayer((LocalPlayer) (Object) this);
        if (baritone == null || !baritone.getPathingBehavior().isPathing()) {
            return;
        }
        
        LocalPlayer player = (LocalPlayer) (Object) this;
        
        // SOLUTION: Force movement by directly calling player movement methods
        // This bypasses the input system entirely
        try {
            // Get current movement from pathfinder
            var current = baritone.getPathingBehavior().getCurrent();
            if (current != null) {
                
                // METHOD 1: Force input states using InputOverrideHandler
                baritone.getInputOverrideHandler().setInputForceState(Input.MOVE_FORWARD, true);
                
                // METHOD 2: Force input states in multiple ways simultaneously
                baritone.getInputOverrideHandler().setInputForceState(Input.MOVE_FORWARD, true);
                player.input.up = true;
                player.input.forwardImpulse = 1.0F;
                player.input.leftImpulse = 0.0F;
                
                // METHOD 3: Direct velocity manipulation as last resort
                // If input system fails, directly modify player velocity
                var vec = player.getDeltaMovement();
                var forward = player.getLookAngle();
                forward = forward.normalize().scale(0.1); // Small movement speed
                player.setDeltaMovement(vec.x + forward.x, vec.y, vec.z + forward.z);
                
            }
        } catch (Exception e) {
            // Silent fallback - continue with whatever works
        }
    }

    // Flying control redirect - field access (optional)
    @Redirect(
            method = "aiStep",
            at = @At(value = "FIELD", target = "net/minecraft/world/entity/player/Abilities.mayfly:Z"),
            require = 0
    )
    private boolean isAllowFlying(Abilities capabilities) {
        IBaritone baritone = BaritoneAPI.getProvider().getBaritoneForPlayer((LocalPlayer) (Object) this);
        if (baritone == null) {
            return capabilities.mayfly;
        }
        return !baritone.getPathingBehavior().isPathing() && capabilities.mayfly;
    }

    // Flying control redirect - method access (optional, NeoForge)
    @Redirect(
        method = "aiStep",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;mayFly()Z"),
        require = 0
    )
    private boolean onMayFlyNeoforge(LocalPlayer instance) throws Throwable {
        IBaritone baritone = BaritoneAPI.getProvider().getBaritoneForPlayer((LocalPlayer) (Object) this);
        if (baritone == null) {
            return MAY_FLY != null ? (boolean) MAY_FLY.invokeExact(instance) : instance.getAbilities().mayfly;
        }
        return !baritone.getPathingBehavior().isPathing() && (MAY_FLY != null ? (boolean) MAY_FLY.invokeExact(instance) : instance.getAbilities().mayfly);
    }

    // Riding behavior injection
    @Inject(
            method = "rideTick",
            at = @At("HEAD"),
            require = 0
    )
    private void updateRidden(CallbackInfo cb) {
        IBaritone baritone = BaritoneAPI.getProvider().getBaritoneForPlayer((LocalPlayer) (Object) this);
        if (baritone != null) {
            ((LookBehavior) baritone.getLookBehavior()).pig();
        }
    }

    // Elytra control redirect (optional)
    @Redirect(
            method = "aiStep",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;tryToStartFallFlying()Z"),
            require = 0
    )
    private boolean tryToStartFallFlying(final LocalPlayer instance) {
        IBaritone baritone = BaritoneAPI.getProvider().getBaritoneForPlayer(instance);
        if (baritone != null && baritone.getPathingBehavior().isPathing()) {
            return false;
        }
        return instance.tryToStartFallFlying();
    }
} 