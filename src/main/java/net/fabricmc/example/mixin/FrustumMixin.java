package net.fabricmc.example.mixin;

import net.fabricmc.example.*;
import net.minecraft.client.render.*;
import net.minecraft.util.math.*;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

@Mixin(Frustum.class)
public class FrustumMixin implements FrustumPos {
    @Shadow private double x;

    @Shadow private double y;

    @Shadow private double z;

    @Inject(at = @At(value = "HEAD"), method = "init")
    private void getMatrixes(Matrix4f viewMatrix, Matrix4f projectionMatrix, CallbackInfo ci) {
        ExampleMod.currentFrustum = this;
    }

    @Override
    public double getX() {
        return x;
    }

    @Override
    public double getY() {
        return y;
    }

    @Override
    public double getZ() {
        return z;
    }
}
