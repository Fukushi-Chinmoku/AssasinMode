package com.assassinmod.assassin;

import net.neoforged.neoforge.client.event.EntityRenderersEvent;

public final class AssassinClientModEvents {
    private AssassinClientModEvents() {
    }

    public static void registerShadowCloneRenderer(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(AssassinEntities.SHADOW_CLONE.get(), ShadowCloneRenderer::new);
    }
}
