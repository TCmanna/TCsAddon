package com.tcmanna.tcsaddon.mixin;

import com.odtheking.odin.utils.Utils;
import com.tcmanna.tcsaddon.events.ClientModMessage;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.multiplayer.chat.GuiMessageSource;
import net.minecraft.client.multiplayer.chat.GuiMessageTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MessageSignature;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChatComponent.class)
public class MixinChatComponent {
    @Inject(method = "addMessage", at = @At("HEAD"))
    private void onAddClientSystemMessage(Component contents, MessageSignature signature, GuiMessageSource source, GuiMessageTag tag, CallbackInfo ci) {
        new ClientModMessage(Utils.getNoControlCodes(contents.getString()), contents).postAndCatch();
    }
}
