package com.tcmanna.tcsaddon.mixin;

import net.fabricmc.loader.api.FabricLoader;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

public class MixinPlugin implements IMixinConfigPlugin {

    private static boolean loaded;
    private static boolean isAeriiLibraryPresent;
    private static boolean isOdinClientPresent;
    private static boolean isNAPresent;

    @Override
    public void onLoad(String mixinPackage) {
        if (loaded) return;
        isAeriiLibraryPresent = FabricLoader.getInstance().isModLoaded("aerii-library");
        isOdinClientPresent = FabricLoader.getInstance().isModLoaded("odin-client");
        isNAPresent = FabricLoader.getInstance().isModLoaded("noammaddons");
        loaded = true;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        if (mixinClassName.startsWith("com.tcmanna.tcsaddon.mixin.skiesstarredlibrary")) {
            return isAeriiLibraryPresent;
        }
        if (mixinClassName.startsWith("com.tcmanna.tcsaddon.mixin.odinclient")) {
            return isOdinClientPresent;
        }
        if (mixinClassName.startsWith("com.tcmanna.tcsaddon.mixin.noammaddons")) {
            return isNAPresent;
        }
        return true;
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {

    }

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {

    }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {

    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }
}