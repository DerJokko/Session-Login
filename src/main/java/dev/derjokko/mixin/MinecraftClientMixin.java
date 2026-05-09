package dev.derjokko.mixin;

import com.mojang.authlib.minecraft.UserApiService;
import dev.derjokko.SessionLogin;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.session.ProfileKeys;
import net.minecraft.client.session.Session;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.io.File;
import java.nio.file.Path;
import java.util.UUID;

@Mixin(MinecraftClient.class)
public abstract class MinecraftClientMixin {

    @Shadow
    @Final
    public File runDirectory;

    @Unique
    private UUID lastProfileKeysUuid = null;

    @Unique
    private String lastProfileKeysToken = null;

    @Unique
    private ProfileKeys cachedProfileKeys = null;

    @Inject(method = "getSession", at = @At("HEAD"), cancellable = true)
    private void onGetSession(CallbackInfoReturnable<Session> cir) {
        if (!SessionLogin.overrideSession) {
            return;
        }

        cir.setReturnValue(SessionLogin.currentSession);
    }

    @Inject(method = "getProfileKeys", at = @At("HEAD"), cancellable = true)
    private void onGetProfileKeys(CallbackInfoReturnable<ProfileKeys> cir) {
        if (!SessionLogin.overrideSession) {
            return;
        }

        Session currentSession = SessionLogin.currentSession;
        UUID currentUuid = currentSession.getUuidOrNull();
        String currentToken = currentSession.getAccessToken();

        if (lastProfileKeysUuid == null ||
                !lastProfileKeysUuid.equals(currentUuid) ||
                lastProfileKeysToken == null ||
                !lastProfileKeysToken.equals(currentToken)) {

            lastProfileKeysUuid = currentUuid;
            lastProfileKeysToken = currentToken;

            try {
                UserApiService userApiService = UserApiService.OFFLINE;

                Path profileKeysPath =
                        runDirectory.toPath().resolve("profilekeys");

                cachedProfileKeys = ProfileKeys.create(
                        userApiService,
                        currentSession,
                        profileKeysPath);

            } catch (Exception e) {
                cachedProfileKeys = null;
            }
        }

        if (cachedProfileKeys != null) {
            cir.setReturnValue(cachedProfileKeys);
        }
    }
}